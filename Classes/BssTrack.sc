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
		name   = "track %".format(id + 1);
		group  = server.nextPermNodeID;

		synthBus = Bus.audio(server, bss.numChannels);
		trackBus = Bus.audio(server, bss.numChannels);

		this.makeDefaultParentEvent;
		defaultParentEvent.parent = bss.defaultParentEvent.copy;
	}

	initNodeTree {
		server.makeBundle(nil, {
			server.sendMsg("/g_new", group, 0, bss.group);
			Synth.after(group, "bss_monitor" ++ bss.numChannels, [
				in: trackBus,
				out: outBus
			]);
		})
	}

	makeDefaultParentEvent {
		defaultParentEvent = Event.make {
			~track    = this;
			~out      = synthBus;
			~trackBus = trackBus;
			~amp      = 1.0;
			~pan      = 0.0;
			~gate     = 1;
		};
	}
}
