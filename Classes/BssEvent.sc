BssEvent {
	var <bss, <event;
	var <track, <effectChain;

	*new { |bss, event|
		^super.newCopyArgs(bss:bss, event:event).init;
	}

	init {
		track = bss.tracks.wrapAt(event[\track] ? 0);
	}

	play {
		event.parent = track.defaultParentEvent;
		event.use {
			this.resolveSoundName;
			this.resolveProtoEvent;
			this.mkEffectChain;
			this.playSynths;
		};
		^this;
	}

	playSynth {
		event.parent = track.defaultParentEvent;
		event.use {
			this.resolveSoundName;
			this.resolveSynthEvent;
			this.mkEffectChain;
			this.playSynths;
		};
		^this;
	}

	playBuf {
		event.parent = track.defaultParentEvent;
		event.use {
			this.resolveSoundName;
			this.resolveBufferEvent;
			this.mkEffectChain;
			this.playSynths;
		};
		^this;
	}

	resolveSoundName {
		var sound = ~s ? ~sound;
		if (~bank.notNil) { sound = format("%_%", ~bank, sound) };
		~sound = sound.asSymbol;
	}

	resolveProtoEvent {
		currentEnvironment.proto = bss.soundLibrary.getEvent(~sound, ~n);
	}

	resolveBufferEvent {
		currentEnvironment.proto = bss.soundLibrary.getBufferEvent(~sound, ~n);
	}

	resolveSynthEvent {
		currentEnvironment.proto = bss.soundLibrary.getSynthEvent(~sound);
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
			bss.modules[\samplers][sampler.asSymbol].value(this);
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
			fxEvent.use { bss.modules['effects'][name].value(this) };
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
