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

	playSynth {
		event.parent = track.defaultParentEvent;
		event.use {
			this.mkSoundName;
			this.mergeSynthEvent;
			this.mkEffectChain;
			this.playSynths;
		};
		^event;
	}

	playBuf {
		event.parent = track.defaultParentEvent;
		event.use {
			this.mkSoundName;
			this.mergeBufferEvent;
			this.mkEffectChain;
			this.playSynths;
		};
		^event;
	}

	mkSoundName {
		var sound = ~s ? ~sound;
		if (~bank.notNil) { sound = format("%_%", ~bank, sound) };
		~sound = sound.asSymbol;
	}

	mergeSoundEvent {
		var event = this.mergeBufferEvent ? this.mergeSynthEvent;
		if (event.isNil) { track.bss.logger.error("no synth/sample named %", ~sound) };
	}

	mergeBufferEvent {
		var event = track.bss.soundLibrary.getBufferEvent(~sound, ~n);
		if (event.notNil) {
			currentEnvironment.proto = event 
		} { ^nil };
	}

	mergeSynthEvent {
		var event, synthDesc;
		synthDesc = SynthDescLib.at(~sound);
		if (synthDesc.notNil) {
			currentEnvironment.proto = (instrument: ~sound) 
		} { ^nil };
	}

	mkEffectChain {
		effectChain = ~fx ? ~chain ? ~fxChain ? ~effects ? ~effectChain ? ~effectsChain;
		effectChain = effectChain.select(_.notNil);
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
