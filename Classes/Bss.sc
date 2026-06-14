Bss {
	var <server;
	var <numChannels;  // number of channels of this instance
	var <soundLibrary; // an instance of BssSoundLibrary
	var logLevel;      // global log level. can be either \debug \info \warning \error

	var <>defaultParentEvent;

	classvar <>maxSoundFileNumChannels = 2; // expected maximum number of channels for sound files

	*new {|numChannels=2, server, logLevel|
		^super.newCopyArgs(
			numChannels: numChannels,
			server:      server   ? Server.default,
			logLevel:    logLevel ? \warning,
		).init;
	}

	init {
		this.initLogger;

		this.makeDefaultParentEvent;
		this.loadSynthDefs("../synths".resolveRelative);
		soundLibrary = BssSoundLibrary(server, numChannels, this.logger);

		this.logger.info("*** Bss's sonic system initialized ***");
	}

	initLogger {
		this.logger.level = logLevel;
		this.logger.logFile = "/tmp/sclang_bss.log";
		this.logger.shouldPost = true;
		this.logger.logToFile(true);
		this.logger.formatter = { |item, log| 
            format("%:% ", log.name.asString.toUpper, (item[\level].asString ++ ":").toUpper.padRight(8)) ++ item[\string];
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

	loadSoundFiles { |paths| 
		soundLibrary.loadSoundFiles(paths);
	}

	loadSynthDefs { |path|
		var paths = pathMatch(standardizePath(path +/+ "*")).select(_.endsWith(".scd"));
		if (paths.isNil) {
			this.logger.warning("no .scd files in %", path) 
		};
		paths.do { |p|
			this.logger.info("loading synthdefs in %", p);
			(bss:this).use { p.load };
		};
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

	/*
	 * Events
	 */

	makeDefaultParentEvent {
		defaultParentEvent = Event.make {
			~amp = -12.dbamp;
			~dur = 1.0;
			~rate = 1.0;
		}
	}

	play { |event|
		^BssEvent(this, event).play;
	}

}
