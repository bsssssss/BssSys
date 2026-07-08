BssEvent {
	var <track, <modules, <event;

	*new { |track, modules, event|
		^super.newCopyArgs(track, modules, event);
	}

	play {
		event.parent = track.defaultParentEvent;
		event.use {
			this.mergeSoundEvent;
			this.playSynths;
		};
		^event;
	}

	mergeSoundEvent {
		var soundEvent = track.bss.soundLibrary.getEvent(~s, ~n);
		if (soundEvent.notNil) {
			currentEnvironment.proto = soundEvent;
		}
	}

	getMsgFunc { |instrument|
		var msgFunc = SynthDescLib.global.synthDescs.at(instrument).msgFunc;
		if (msgFunc.notNil) {
			^msgFunc;
		} {
			track.bss.logger.error( "(%): no msgFunc for instrument %", thisMethod, instrument);
		};
	}

	sendSynth { |instrument, args|
		args = args ?? { this.getMsgFunc(instrument.asSymbol).valueEnvir }; // get synth arguments from event if nil
		args.flop.do { |argList| 
			Synth.tail(track.group, instrument, argList);
		};
	}

	playSynths {
		track.server.bind {
			modules.do(_.value(this));
		}
	}

	show { |event|
		event.keysValuesDo { |k, v|
			format("%: %", k, v).postln;
		}
	}
}
