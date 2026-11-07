Bss {
	classvar <>maxSoundFileNumChannels = 2; // expected maximum number of channels for sound files

	var <server, <numChannels;

	var <tracks, <outBusses, <group;
	var <moduleLibrary, <soundLibrary;
	var <tuning, <clock;
	var <defaultParentEvent;

	var <reaperProjectPath, <reaper;

	*new {|server, numChannels|
		^super.newCopyArgs(
			server: server ? Server.default,
			numChannels: numChannels ? 2,
		).init;
	}

	makeDefaultParentEvent {
		defaultParentEvent = Event.make {
			~bss         = this;
			~server      = server;
			~numChannels = numChannels;
			~octave      = 5;
			~note        = 0.0;
			~scale       = Scale.chromatic;
			~midinote    = #{ ~note.degreeToKey(~scale) + (12.0 * ~octave) };
			~freq        = #{ ~midinote.value.midicps };
			~n           = 0;
		}
	}

	init {
		moduleLibrary = BssModuleLibrary();
		soundLibrary = BssSoundLibrary(server, numChannels);

		group = server.nextPermNodeID;
		tuning = BssTuning();
		clock = TempoClock.new(60/60).permanent_(true);

		this.loadSynthDefs("../lib".resolveRelative);
		ServerTree.add(this, server);
		this.makeDefaultParentEvent;

		"ready.\n\n".post;
	}

	slib { ^soundLibrary; }
	mlib { ^moduleLibrary; }

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

	loadSynthDefs { |path|
		var paths = pathMatch(standardizePath(path +/+ "*")).select(_.endsWith(".scd"));

		if (paths.isNil) {
			format("cannot load synthdefs. No .scd files in '%'", path);
			^this;
		};

		paths.do { |p|
			format("loading '%'", p.basename).postln;
			(bss: this).use { p.load };
		};
	}

	/* Modules */

	addEffectModule { |name, func, cond|
		moduleLibrary.addEffect(name, func, cond);
	}

	addSamplerModule { |name, func, cond|
		moduleLibrary.addSampler(name, func, cond);
	}

	/* Reaper */

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

	/* Startup */

	start { |busses([0]), withReaper(false)|
		outBusses = busses.asArray;
		this.initNodeTree;
		this.createTracks;
		if (withReaper) { this.mkReaperProject };
	}

	doOnServerTree {
		this.initNodeTree;
	}

	/* Utils */

	show {
		moduleLibrary.show;
		soundLibrary.show;
	}

	/* Playing */

	snd { |name, specs|
		specs = (sound: name) ++ (specs ? ()).asEvent;
		^BssEvent(this, specs);
	}

	syn { |name, specs|
		specs = (sound: name) ++ (specs ? ()).asEvent;
		"Bss.syn not implemented. Use Bss.snd instead".warn;
	}

	seq {
		^BssSeq(this.clock);
	}
}
