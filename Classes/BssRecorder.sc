BssRecorder {
	var <recordingsDir;
	var <server, <recorder, <numChannels;

	*new { |recordingsDir, numChannels, server|
		^super.newCopyArgs(
			recordingsDir: recordingsDir ? thisProcess.nowExecutingPath.dirname,
			numChannels: numChannels ? 2, 
			server: server ? Server.default,
		).init;
	}

	init {
		recordingsDir = recordingsDir.absolutePath;
		recorder = Recorder(server);
	}

	recordingsDir_ { |path|
		recordingsDir = path.absolutePath;
	}

	timestamp {
		var date = Date.getDate;
		var ms = date.rawSeconds.asString.split($.)[1].padRight(4, "0");
		^date.stamp.asString ++ $_ ++ ms;
	}

	makePath { |filename|
		^recordingsDir +/+ filename ++ $_ ++ this.timestamp ++ $. ++ recorder.recHeaderFormat;
	}

	record { |name, duration(1), bus(0), node|
		var filePath = this.makePath(name);
		recorder.prepareForRecord(filePath, numChannels);
		server.sync;
		recorder.record(duration: duration);
		server.sync;
	}
}
