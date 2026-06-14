BssRecord {
	// TODO: default path for windows
	var <>recordingsDir = "~/Sounds/samples/sc/rrec";
	var <>fileName;

	baseDir {
		^PathName.new(recordingsDir);
	}

	makeFileName { |number|
		var format = ".wav";
		number = number.asString.padLeft(3, "0"); 

		if (fileName != nil) {
			^fileName.asString ++ "_" ++ number ++ format;
		} {
			^number ++ format;
		};
	}

	fullPath {
		var number = 0;
		var filePath = this.baseDir.fullPath +/+ this.makeFileName(number);
		while { File.existsCaseSensitive(filePath) } {
			number = number + 1;
			filePath = this.baseDir.fullPath +/+ this.makeFileName(number);
		};
		^filePath;
	}

	record { |server, bus=0, numChannels=2, duration=1|
		var s = server ?? Server.default;
		var recorder = Recorder(s);
		recorder.prepareForRecord(this.fullPath, numChannels);
		s.sync;
		recorder.record(duration: duration);
		s.sync;
	}
}
