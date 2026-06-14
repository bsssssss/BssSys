BssEvent {
	var <bss;   // a Bss instance
	var <event; // the event for an instance of this class

	*new { |bss, event|
		^super.newCopyArgs(bss: bss, event: event);
	}

	play {
		event.parent = bss.defaultParentEvent;
		event.use {
			this.mergeSoundEvent;
			this.playSynth;
		}
	}

	mergeSoundEvent {
		var soundEvent = bss.soundLibrary.getEvent(~s, ~n);
		if (soundEvent.notNil) {
			currentEnvironment.proto = soundEvent;
		}
	}

	playSynth {
		bss.server.bind {
			this.playSound;	
		}
	}

	playSound {
		var instrument, args;
		bss.logger.debug("(%): got %: %", thisMethod, ~instrument, args);
		if (~buffer.notNil) {
			args = [
				bufnum: ~bufnum,
				rate: ~rate,
				sustain: ~sustain ?? ~duration,
				begin: ~begin,
				pan: ~pan,
				amp: ~amp,
			];
			this.sendSynth(~instrument, args);
		} {
			if (~instrument.notNil) {
				this.sendSynth(~instrument, args);
			} {
				"no sound or synth named %, dropping event...".format(~s).warn
			}
		}
	}

	getMsgFunc { |instrument|
		var sd = SynthDescLib.global.synthDescs.at(instrument.asSymbol).msgFunc;
		if (sd.isNil) {
			bss.logger.error("(%): no msgFunc for instrument %, either synth does not exist, or there is a problem with the synthDesc...", thisMethod, instrument);
		} {
			^sd;
		};
	}

	sendSynth { |instrument, args|
		args = args ?? { this.getMsgFunc(instrument).valueEnvir };
		bss.logger.debug("sending to scsynth: %%", instrument, args);
		Synth(instrument, args);
	}

	show { |event|
		event.keysValuesDo { |k, v|
			format("%: %", k, v).postln;
		}
	}
}
