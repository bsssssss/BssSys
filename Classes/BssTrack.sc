BssTrack {
	var <bss, <server;
	var <id, <>name;
	var <group;
	var <trackBus, <>outBus;
	var <>defaultParentEvent;

	*new { |bss, outBus, id|
		^super.newCopyArgs(bss: bss, outBus: outBus, id: id).init;
	}

	init {
		server = bss.server;
		name   = "track %".format(id);
		group  = server.nextPermNodeID;

		trackBus = Bus.audio(bss.server, bss.numChannels);

		this.initNodeTree;
		this.makeDefaultParentEvent;
	}

	initNodeTree {
		server.makeBundle(nil, {
			server.sendMsg("/g_new", group, 0, bss.group);
			Synth.after(group, "bss_track_monitor_%ch".format(bss.numChannels), 
				[inBus: trackBus, outBus: outBus]
			);
		})
	}

	makeDefaultParentEvent {
		defaultParentEvent = Event.make {
			~bss = bss;
			~server = server;
			~numChannels = bss.numChannels;
			~track = this;

			~tuning = #{ ~bss.tuning.current };
			// ~scale = Scale.chromatic;
			~octave = 5;
			~note = #{ ~note ? 0 };
			~midinote = #{ ~note.degreeToKey(~scale) + (12 * ~octave) };
			~freq = #{ ~midinote.value.midicps };

			~n = \none;

			~amp  = 1.0;
			~rate = 1.0;
			~dur  = 1.0;

			~out = trackBus;
		}
	}

	play { |event|
		BssEvent(this, bss.modules, event).play;
	}
}
