Bss {
	classvar <>maxSoundFileNumChannels = 2; // expected maximum number of channels for sound files

	var <server, <numChannels;

	var <tracks, <outBusses, <group;
	var <moduleLibrary, <ml, <soundLibrary, <sl;
	var <tuning, <clock;

	var <reaperProjectPath, <reaper;

	*new {|numChannels, server|
		^super.newCopyArgs(
			numChannels: numChannels ? 2,
			server: server ? Server.default,
		).init;
	}

	init {
		moduleLibrary = BssModuleLibrary();
		ml = moduleLibrary;
		soundLibrary = BssSoundLibrary(server, numChannels);
		sl = soundLibrary;

		group = server.nextPermNodeID;
		tuning = BssTuning();
		clock = TempoClock.new(60/60).permanent_(true);

		this.loadSynthDefs("../lib".resolveRelative);
		ServerTree.add(this, server);

		"*** Bss Sonic System initialized ***".postln;
	}

	createTracks {
		outBusses.do { |outBus, id|
			var track = BssTrack(this, outBus, id);
			tracks = tracks.add(track);
		};
	}

	initNodeTree {
		server.sendMsg("/g_new", group, 0, 1);
		tracks.do(_.initNodeTree);
	}

	// modules get added here as well
	loadSynthDefs { |path|
		var paths = pathMatch(standardizePath(path +/+ "*")).select(_.endsWith(".scd"));

		if (paths.isNil) {
			format("cannot load synthdefs. No .scd files in '%'", path);
			^this;
		};

		paths.do { |p|
			format("loading synthdefs in '%'", p.basename).postln;
			(bss: this).use { p.load };
		};

		moduleLibrary.modules.keysValuesDo { |k, v|
			format("% (% modules)", k, v.size).postln;
			"\t".post;
			v.do { |module|
				module.name.post; ",".post
			};
			"".postln;
		};
	}


	/*
	 * Modules
	 */

	addEffectModule { |name, func, cond|
		moduleLibrary.add(\effect, name, func, cond);
	}

	addSamplerModule { |name, func, cond|
		moduleLibrary.add(\sampler, name, func, cond);
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
			// stereo rec inputs starts from 1024
			props.put(\REC, "1 % 1 0 0 0 0".format((i * 2) + 1024));
			ReaTrack.new(props)
		};

		tracks.do{|track|
			project.addTrack(track)
		};

		project.write(projectPath);
		reaperProjectPath = projectPath;
		format("Created reaper project in '%'", projectPath).postln;

		projectPath.openOS;
		reaper = ReaperControl.start;
	}

	/*
	 * Startup
	 */

	start { |busses([0]), withReaper(false)|
		outBusses = busses.asArray;
		this.initNodeTree;
		this.createTracks;
		if (withReaper) { this.mkReaperProject };
	}

	doOnServerTree {
		this.initNodeTree;
	}

	/*
	 * Utils
	 */

	show {
		soundLibrary.show;
	}

	/*
	 * Playing
	 */

	// sounds 

	snd { |name ...args, kwargs|
		var event = (sound: name.asSymbol);
		kwargs.pairsDo { |k, v| event[k] = v };
		^BssEvent(this, event).method_(\auto);
	}

	syn { |name ...args, kwargs|
		var event = (sound: name.asSymbol);
		kwargs.pairsDo { |k, v| event[k] = v };
		^BssEvent(this, event).method_(\synth);
	}

	buf { |name ...args, kwargs|
		var event = (sound: name.asSymbol);
		kwargs.pairsDo { |k, v| event[k] = v };
		^BssEvent(this, event).method_(\buffer);
	}

	// sequences 

	seq {
		^BssSeq(this.clock);
	}
}
