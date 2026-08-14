Bss {
	classvar <>maxSoundFileNumChannels = 2; // expected maximum number of channels for sound files

	var <server, <numChannels, logLevel;
	var <tracks, <outBusses, <group;
	var <reaperProjectPath, <reaper;
	var <modules, <soundLibrary, <tuning;

	*new {|numChannels, server, logLevel|
		^super.newCopyArgs(
			numChannels: numChannels ? 2,
			server: server ? Server.default,
			logLevel: logLevel ? \info,
		).init;
	}

	init {
		this.initLogger;
		modules = [];
		group = server.nextPermNodeID;
		soundLibrary = BssSoundLibrary(server, numChannels, this.logger);
		tuning = BssTuning();
		this.loadSynthDefs("../synths".resolveRelative);
		ServerTree.add(this, server);
		"*** Bss Sonic System initialized ***".postln;
	}

	initLogger {
		this.logger.level = logLevel;
		this.logger.logFile = "/tmp/sclang_bss.log";
		this.logger.shouldPost = true;
		this.logger.logToFile(true);
		this.logger.formatter = { |item, log| 
            format("%% ", 
				log.name.asString.toUpper,
				(":" ++ item[\level].asString ++ ":").toUpper
			) ++ item[\string];
		};
	}

	mkTracks {
		outBusses.do { |outBus, id|
			var track = BssTrack(this, outBus, id);
			tracks = tracks.add(track);
		};
	}

	initNodeTree {
		server.sendMsg("/g_new", group, 0, 1);
		outBusses.as(OrderedIdentitySet).do { |bus| // filter out duplicate busses
			Synth.after(group, "bss_output_monitor" ++ numChannels, 
				[inBus: bus, outBus: bus]
			);
		};
	}

	/*
	 * File Loading
	 */

	doNotReadYet {
		^soundLibrary.doNotReadYet;
	}

	doNotReadYet_ { |bool|
		^soundLibrary.doNotReadYet = bool;
	}

	loadSoundFilesToBank { |paths, bankName| 
		soundLibrary.loadSoundFilesToBank(paths, bankName);
	}

	loadSoundFiles { |paths| 
		soundLibrary.loadSoundFiles(paths);
	}

	loadSoundFile { |path, name, append(false)|
		soundLibrary.loadSoundFile(path, name, append);
	}

	// modules get added here as well
	loadSynthDefs { |path|
		var paths = pathMatch(standardizePath(path +/+ "*")).select(_.endsWith(".scd"));
		if (paths.isNil) {
			this.logger.warning("no .scd files in %", path) 
		};
		paths.do { |p|
			this.logger.info("loading synthdefs in %", p);
			(bss: this).use { p.load };
		};
	}

	/*
	 * Modules
	 */

	addModule { |name, func, cond|
		var module, index;

		name = name.asSymbol;
		module = BssModule(name, func, cond);

		index = modules.indexOfEqual(module);
		if (index.notNil) {
			modules.put(index, module);
		} {
			modules.add(module);
		};
	}

	/*
	 * Events
	 */

	play { |...args, kwargs|
		var track, event = ();
		
		if (args.size > 0 and: {args[0].isKindOf(Event)}) {
			event = args[0];
		};

		// allow `bss.play(freq: 666, amp: 0.2)`
		kwargs.pairsDo { |k,v|
			event[k] = v;
		};

		track = tracks @@ (event[\track] ? 0);

		^BssEvent(track, modules, event).play
	}

	/*
	 * Logging
	 */

	logger {
		^BssLog(\Bss);
	}

	logLevel {
		^this.logger.level;
	}

	logLevel_ { |level|
		this.logger.level = level;
	}

	show {
		soundLibrary.show;
	}

	/*
	 * Reaper
	 */

	mkReaperProject {
		var basePath, projectPath, masterFxNode, project, numTracks, tracks;

		if (ReaProj.asClass.isNil) {
			^"Failed to create reaper project: Need ReaCollider quark".warn;
		};

		if (server.options.outDevice.isNil or: { server.options.outDevice.contains("BlackHole").not }) {
			^"Failed to create reaper project: output device is not 'BlackHole'".warn;
		};

		basePath = "~/Sounds/reaper/sc/bss/%".format(Date.getDate.format("%y-%m-%d")).asAbsolutePath;
		File.mkdir(basePath);

		projectPath = "~/Sounds/reaper/sc/bss/%/%.RPP".format(Date.getDate.format("%y-%m-%d"), Date.getDate.hourStamp).asAbsolutePath;
		project = ReaProj();

		numTracks = outBusses.as(Set).size;
		if (server.options.numOutputBusChannels < (numTracks * numChannels)) {
			^"Failed to create reaper project: s.options.numOutputBusChannels should be at least %" .format(numTracks * numChannels).warn;
		};

		tracks = numTracks.collect{ |i|
			var props = ();
			props.put(\NAME, "track_%".format(i+1));
			props.put(\REC, "1 % 1 0 0 0 0".format((i * 2) + 1024)); // stereo rec inputs starts from 1024
			ReaTrack.new(props)
		};

		tracks.do{|track|
			project.addTrack(track)
		};

		project.write(projectPath);
		reaperProjectPath = projectPath;
		this.logger.info("Created reaper project at %", projectPath);

		projectPath.openOS;
		reaper = ReaperControl.start;
	}

	/*
	 * Startup
	 */

	start { |busArray, withReaper = false|
		if (busArray.isNil) {
			outBusses = [0];
		} {
			outBusses = busArray.asArray;
		};
		this.initNodeTree;
		this.mkTracks;
		if (withReaper) { this.mkReaperProject };
	}

	doOnServerTree {
		this.initNodeTree;
		tracks.do(_.initNodeTree);
	}
}
