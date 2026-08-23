BssEvent {
	var <track, <event, <modules, <effectChain;

	*new { |track, modules, event|
		^super.newCopyArgs(
			track: track,
			modules: modules,
			event: event
		);
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
		if (~bank.notNil) { sound = format("%_%", ~bank, sound) };
		~s = sound.asSymbol;
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
			track.bss.logger.error( "(%): no msgFunc for instrument %", thisMethod, synthDefName);
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
		if (~buffer.notNil) { // buffer event from BssSoundLibrary 
			var sampler = ~sampler ? "bufrd";
			modules[\samplers][sampler.asSymbol].value(this);
		} {
			if (~instrument.notNil) { // synthdesc event
				this.sendSynth(~instrument); // arguments are handled with msgFunc
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
			fxEvent.use { modules['effects'][name].value(this) };
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
