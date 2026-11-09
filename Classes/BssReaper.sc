BssReaperSession {
	var <bss;
	var <project, <projectPath, <control;

	*new { |bss|
		if (ReaProj.asClass.isNil) {
			^"could not create BssReaperSession: ReaCollider quark not installed".error;
		};

		^super.newCopyArgs(bss: bss).init;
	}

	init {
		var basePath, numTracks, tracks;

		if (bss.server.options.outDevice.isNil or: { bss.server.options.outDevice.contains("BlackHole").not }) {
			^"could not create reaper project: output device is not 'BlackHole'".warn;
		};

		basePath = "~/Sounds/reaper/sc/bss/%".format(Date.getDate.format("%y-%m-%d")).asAbsolutePath;
		File.mkdir(basePath);

		projectPath = "~/Sounds/reaper/sc/bss/%/%.RPP".format(Date.getDate.format("%y-%m-%d"), Date.getDate.hourStamp).asAbsolutePath;
		project = ReaProj();

		numTracks = bss.outBuses.as(Set).size;
		if (bss.server.options.numOutputBusChannels < (numTracks * bss.numChannels)) {
			^"Failed to create reaper project: Server numOutputBusChannels should be at least %" .format(numTracks * bss.numChannels).warn;
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
		format("Created reaper project at '%'", projectPath).postln;
	}

	open {
		if (project.isNil) {
			"could not open Reaper project. project is nil".error;
			^this;
		};
		projectPath.openOS;
		control = ReaperControl.start;
	}

}
