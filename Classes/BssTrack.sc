BssTrack {
	var <bss, <server;
	var <id, <>name;
	var <group;
	var <synthBus, <trackBus, <>outBus;
	var <>defaultParentEvent;

	*new { |bss, outBus, id|
		^super.newCopyArgs(bss: bss, outBus: outBus, id: id).init;
	}

	init {
		server = bss.server;
		name   = "track %".format(id);
		group  = server.nextPermNodeID;

		synthBus = Bus.audio(server, bss.numChannels);
		trackBus = Bus.audio(server, bss.numChannels);

		this.initNodeTree;
		this.makeDefaultParentEvent;
	}

	initNodeTree {
		server.makeBundle(nil, {
			server.sendMsg("/g_new", group, 0, bss.group);
			Synth.after(group, "bss_monitor" ++ bss.numChannels, [in: trackBus, out: outBus]);
		})
	}

	makeDefaultParentEvent {
		defaultParentEvent = Event.make {
			~bss = bss;
			~server = server;
			~numChannels = bss.numChannels;
			~track = this;

			~octave = 5;
			~note = 0.0;
			~scale = Scale.chromatic;
			~midinote = #{ ~note.degreeToKey(~scale) + (12.0 * ~octave) };
			~freq = #{ ~midinote.value.midicps };

			~n = 0;

			~amp = 1.0;
			~rate = 1.0;

			~sustain = 1.0;
			// ~duration = 1.0;
			// ~dur = 1.0;

			~pan = 0.0;

			~out = synthBus;
			~trackBus = trackBus;
		}
	}

	play { |event|
		BssEvent(this, bss.modules, event).play;
	}
}
