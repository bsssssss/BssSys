BssSoundLibrary {
	var <server, <numChannels;
	var <>soundFileExtensions = #["wav", "aiff"];
	var <>doNotReadYet = false;
	var <buffers, <bufferEvents, <banks;
	var <loadingMethods;

	*new { |server, numChannels|
		^super.newCopyArgs(server, numChannels).init;
	}

	init {
		loadingMethods = #[\replace, \append];
		buffers = IdentityDictionary();
		bufferEvents = IdentityDictionary();
		banks = IdentityDictionary();
	}

	freeSoundFiles { |names|
		names.asArray.do { |name|
			if (buffers[name].isNil) {
				format("'%' not in sound library, doing nothing", name).warn; 
			} {
				format("freeing '%' in sound library", name).postln; 
				buffers.removeAt(name).asArray.do { |b| b.free };
				bufferEvents.removeAt(name);
			};
		};
	}

	prIsValidMethod { |method|
		if (loadingMethods.indexOf(method).notNil) {
			^true 
		} {
			format("unrecognized method '%'. possible options are %", method, loadingMethods).error;
			^false 
		};
	}

	loadSoundFilesToBank { |paths, bankName, namingFunc, method(\replace)|
		var form = "%_%";
		if (this.prIsValidMethod(method).not) { ^this };

		if (bankName.isNil) {
			"could not load files to bank: name is nil".error;
			^this; 
		};
		namingFunc = namingFunc ?? { { |x| format(form, bankName, x.basename) } };
		this.loadSoundFiles(paths, namingFunc, method);
		this.addBank(bankName);
	}

	loadSoundFiles { |paths, namingFunc(_.basename), method(\replace)|
		var folderPaths;
		if (this.prIsValidMethod(method).not) { ^this };

		if (paths.isNil) {
			"could not load files: paths is nil".error;
			^this; 
		};

		folderPaths = if (paths.isString) { pathMatch(paths) } { paths.asArray };
		folderPaths = folderPaths.select(_.endsWith(Platform.pathSeparator.asString));

		if (folderPaths.isEmpty) {
			"could not load files: no folders in paths".error;
			^this 
		};
		folderPaths.do { |folderPath|
			this.loadSoundFolder(folderPath, namingFunc.(folderPath), method);
		};
	}

	loadSoundFolder { |folderPath, name, method(\replace)|
		var errMsg, filePaths;
		if (this.prIsValidMethod(method).not) { ^this };

		errMsg = "could not load sound folder.";

		if (name.isNil) {
			(errMsg + "name is nil").error;
			^this;
		};
		if (File.exists(folderPath).not) {
			format(errMsg + "% does not exist", folderPath).error;
			^this;
		};

		filePaths = pathMatch(folderPath.standardizePath +/+ "*");
		filePaths = filePaths.select({ |p| this.isSoundFilePath(p)} );

		if (filePaths.isEmpty) {
			format(errMsg + "no sound files in %", folderPath).error;
			^this;
		};

		format("loading '%' to '%' (% files)", folderPath, name, filePaths.size).postln;
		filePaths.do { |path|
			this.loadSoundFile(path, name, method);
			method = \append;
		};
	}

	loadSoundFile { |path, name, method(\replace)|
		var errMsg, buf, fileName;
		if (this.prIsValidMethod(method).not) { ^this };

		errMsg = "could not load sound file."; 
		buf = this.readSoundFile(path);
		fileName = PathName(path).fileName;
		name = name.asSymbol;

		if (buf.isNil) {
			format(errMsg + "failed to read buffer from '%'", path).error;
			^this;
		};

		if (name.isNil) {
			(errMsg + "name is nil").error;
			^this 
		};

		if (buffers[name].notNil and: { method == \replace }) {
			format("'%' already exists, replacing...", name).warn;
			this.freeSoundFiles(name);
		};

		//format("appending '%' to '%'", fileName, name).postln;
		this.addBuffer(buf, name);
	}

	// return true if file path extensions is one of soundFileExtensions
	isSoundFilePath { |path|
		var fileExt = (path.splitext.last ? "").toLower;
		^soundFileExtensions.any({ |ext|
			fileExt == ext;
		})
	}

	readSoundFile { |path|
		^BssBuffer.readWithInfo(server, path, onlyHeader: doNotReadYet);
	}

	addBuffer { |buffer, name|
		var event = this.makeBufferEvent(buffer);
		buffers[name] = buffers[name].add(buffer);
		bufferEvents[name] = bufferEvents[name].add(event);
	}

	addBank { |name|
		name = name.asSymbol;
		banks[name] = banks[name].add(name);
	}

	makeBufferEvent { |buffer|
		var event = (
			bufnum: buffer.bufnum,
			buffer: buffer,
			filename: buffer.path.basename,
			bufNumChannels: buffer.numChannels,
			bufNumFrames: buffer.numFrames,
			duration: buffer.duration,
		);
		^event;
	}

	at { |name|
		^bufferEvents[name];
	}

	getSynthEvent { |name|
		var synthDesc;
		synthDesc = SynthDescLib.at(name.asSymbol);
		if (synthDesc.notNil) {
			^(instrument: synthDesc.name);
		};
		^nil;
	}

	getBufferEvent { |name, index|
		var events = this.at(name.asSymbol);
		if (events.notNil) {
			^events.wrapAt(index.asInteger) 
		};
		^nil;
	}

	getEvent { |name, index, searchMethod(\auto)| 
		var event;
		switch (searchMethod,
			\auto, {
				event = this.getBufferEvent(name, index) ? this.getSynthEvent(name) 
			},
			\synth, {
				event = this.getSynthEvent(name) 
			},
			\buffer, {
				event = this.getBufferEvent(name, index) 
			},
			{
				"unrecognized searchMethod '%' for getEvent. Expecting 'auto'|'synth'|'buffer'".format(searchMethod).error;
			};
		);
		if (event.isNil) { "no sound % in library (% search)".format(~sound, searchMethod).error }
		^event;
	}

	show {
		"Sound Library:".postln;
		if (buffers.size == 0) {
			"sound file pool is empty...".postln 
		} {
			format("% entr%", buffers.size, if(buffers.size > 1, "ies", "y")).postln;
			buffers.keysValuesDo { |k,v|
				format("\t% (% file%)", k.asString.padRight(30,"."), v.size, if(v.size > 1, "s", "")).postln;
			}
		};
		"".postln;
	}
}
