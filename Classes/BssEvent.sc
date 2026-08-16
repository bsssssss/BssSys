BssEvent {
	var <track, <event, <effectModules, <effectChain;

	*new { |track, effectModules, event|
		^super.newCopyArgs(track:track, effectModules:effectModules, event:event);
	}

	play {
		event.parent = track.defaultParentEvent;
		event.use {
			this.mkSoundName;
			this.mergeSoundEvent;
			this.mkEffectChain;
			this.playSynths;
		};
		^event;
	}

	mkSoundName {
		var sound = ~sound ? ~s;

		if (~bank.notNil) {
			sound = format("%_%", ~bank, sound) 
		};

		~s = sound.asSymbol;
	}

	mkSoundChain {
		
	}

	mergeSoundEvent {
		var soundEvent = track.bss.soundLibrary.getEvent(~s, ~n);
		if (soundEvent.notNil) {
			currentEnvironment.proto = soundEvent;
		}
	}

	mkEffectChain {
		effectChain = ~chain ? ~fx ? ~fxChain ? ~effects ? ~effectChain ? ~effectsChain;
		effectChain = effectChain.select(_.notNil);
		effectChain.postln;
	}

	getMsgFunc { |synthDefName|
		var msgFunc = SynthDescLib.global.synthDescs.at(synthDefName).msgFunc;
		if (msgFunc.notNil) {
			^msgFunc;
		} {
			track.bss.logger.error( "(%): no msgFunc for instrument %", thisMethod, instrument);
		};
	}

	makeSynthGroup { |outerGroup|
		~synthGroup = Group(outerGroup ? track.group);
	}

	sendSynth { |instrument, args|
		args = args ?? { this.getMsgFunc(instrument.asSymbol).valueEnvir }; // get synth arguments from event if nil
		args.flop.do { |argList| 
			Synth.tail(~synthGroup, instrument, argList);
		};
	}

	sendSourceSynth {
		if (~buffer.notNil) {
			this.sendSynth(~instrument, [
				bufnum: ~bufnum,
				freq: ~freq,
				sustain: ~sustain ? ~duration ? ~buffer.duration,
				begin: ~begin,
				pan: ~pan,
				amp: ~amp,
				out: ~out,
			]);
		} {
			if (~instrument.notNil) {
				this.sendSynth(~instrument); // arguments are handled with msgFunc
			} {
				"no sound or synth named %, dropping event...".format(~s).warn
			}
		}
	}

	sendGateSynth {
		^Synth.tail(~synthGroup, "bss_gate" ++ ~numChannels, [
			in: track.synthBus,
			out: track.trackBus,
			sustain: ~sustain,
		]);
	}

	sendEffectChain {
		effectChain.do { |spec|
			var name = spec[0].asSymbol;
			var fxEvent = spec[1].asEvent;
			fxEvent.parent = event;
			fxEvent.use { effectModules[name].value(this) };
		};
	}

	playSynths {
		track.server.bind {
			this.makeSynthGroup;
			this.sendSourceSynth;
			this.sendEffectChain;
			this.sendGateSynth; // LAST
		}
	}

	show { |event|
		event.keysValuesDo { |k, v|
			format("%: %", k, v).postln;
		}
	}
}
