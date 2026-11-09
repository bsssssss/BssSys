Bss {
	classvar <>maxSoundFileNumChannels = 2; // expected maximum number of channels for sound files

	var <server, <numChannels;

	var <tracks, <outBusses, <group;
	var <moduleLibrary, <soundLibrary;
	var <tuning, <clock;
	var <defaultParentEvent;

	*new {|server, numChannels|
		^super.newCopyArgs(
			server: server ? Server.default,
			numChannels: numChannels ? 2,
		).init;
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

	doOnServerTree {
		this.initNodeTree;
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

	start { |buses([0])|
	/* Startup */

		outBusses = busses.asArray;
		this.initNodeTree;
		this.createTracks;
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

	/* Utils */

	show {
		moduleLibrary.show;
		soundLibrary.show;
	}
}
