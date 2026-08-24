BssSoundLibrary {
	var <server, <numChannels, <logger;
	var <>soundFileExtensions = #["wav", "aiff"];
	var <>doNotReadYet = false;
	var <buffers, <bufferEvents, <banks;

	*new { |server, numChannels, logger|
		^super.newCopyArgs(server, numChannels, logger).init;
	}

	init {
		buffers = IdentityDictionary();
		bufferEvents = IdentityDictionary();
		banks = IdentityDictionary();
	}

	/*
	 * SOUND FILES
	 */

	loadSoundFilesToBank { |paths, bankName, namingFunc, append(false)|
		var form = "%_%";
		if (bankName.isNil) { logger.error("need a bank name") };
		namingFunc = namingFunc ?? { { |x| format(form, bankName, x.basename) } };
		this.loadSoundFiles(paths, namingFunc, append);
		this.addBank(bankName);
	}

	loadSoundFiles { |paths, namingFunc(_.basename), append(false)|
		var folderPaths;

		if (paths.isNil) {
			logger.warning("%: needs a path");
			^this 
		};

		folderPaths = if (paths.isString) { pathMatch(paths) } { paths.asArray };
		folderPaths = folderPaths.select(_.endsWith(Platform.pathSeparator.asString));
		if (folderPaths.isEmpty) {
			logger.warning("invalid folder path: %", paths);
			^this 
		};

		logger.info("will load % folder%: %", folderPaths.size, if(folderPaths.size > 1, "s", ""), folderPaths);
		folderPaths.do { |folderPath|
			this.loadSoundFileFolder(folderPath, namingFunc.(folderPath), append);
		};
	}

	freeSoundFiles { |names|
		names.asArray.do { |name|
			if (buffers[name].isNil) {
				logger.warn("no entry for %, skipping...", name); 
			} {
				logger.debug("deleting entry: % (% buffers)", name, buffers[name].size);
				buffers.removeAt(name).asArray.do { |buf|
					logger.debug("freeing %", buf);
					buf.free;
				};
				bufferEvents.removeAt(name);
			};
		};
	}

	loadSoundFileFolder { |folderPath, name, append(false)|
		var filePaths;

		if (name.isNil) { logger.error("need a name"); ^nil };
		if (File.exists(folderPath).not) { logger.error("% does not exist", folderPath); ^nil };

		filePaths = pathMatch(folderPath.standardizePath +/+ "*");
		filePaths = filePaths.select({ |path| this.isSoundFilePath(path)} );

		if (filePaths.isEmpty) { logger.warning("no soundfile in %, skipping...", folderPath); ^nil };

		logger.info("loading folder: %", folderPath);
		filePaths.do { |p|
			this.loadSoundFile(p, name, append);
			append = true; // append the rest
		};
	}

	loadSoundFile { |path, name, append(false)|
		var buf;
		if (name.isNil) { logger.error("need a name"); ^nil };

		buf = this.readSoundFile(path);
		if (buf.notNil) {
			logger.debug("read % into buffer: %", path.basename, buf);
			this.addBuffer(buf, name, append);
		} {
			logger.error("buf read failed for %", path);
		};
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


	/*
	 * Set
	 */

	addBuffer { |buffer, name, append(false)|
		var event;

		if (buffer.isNil) { logger.error("buffer is nil"); ^nil };
		if (name.isNil) { logger.error("need a name"); ^nil };

		name = name.asSymbol;

		if (buffers[name].notNil and: { append.not }) {
			logger.info("% is already registered, replacing...", name);
			this.freeSoundFiles(name);
		};

		if (buffers[name].isNil) { 
			logger.debug("adding buffers entry: %", name) 
		};

		buffers[name] = buffers[name].add(buffer);
		event = this.makeBufferEvent(buffer);
		bufferEvents[name] = bufferEvents[name].add(event);
	}

	addBank { |name|
		if (name.isNil) { logger.error("need a bank name") };
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


	/*
	 * Get
	 */

	at { |name| 
		^bufferEvents[name];
	}

	getSynthEvent { |name|
		var synthDesc;
		name = name.asSymbol;
		synthDesc = SynthDescLib.at(name);
		if (synthDesc.isNil) { ^nil };
		^(instrument: name) 
	}

	getBufferEvent { |name, index|
		var events = this.at(name.asSymbol);
		if (events.isNil) { ^nil };
		^events.wrapAt(index.asInteger) 
	}

	getEvent { |name, index, method(\auto)| 
		switch (method,
			\auto,  { ^this.getBufferEvent(name, index) ? this.getSynthEvent(name) },
			\synth, { ^this.getSynthEvent(name) },
			\buffer, { ^this.getBufferEvent(name, index) },
			{ "% is not a valid method, expecting 'auto'|'synth'|'buffer'".format(method).error };
		)
	}


	/*
	 * Show
	 */

	showSoundFiles {
		if (buffers.size == 0) {
			"sound file pool is empty...".postln 
		} {
			format("% folder%:", buffers.size, if(buffers.size > 1, "s", "")).postln;
			buffers.keysValuesDo { |k,v|
				format("\t% (% file%)", k.asString.padRight(30,"."), v.size, if(v.size > 1, "s", "")).postln;
			}
		};
	}

	show {
		"*** Sound Library ***".postln;
		this.showSoundFiles;
	}
}
