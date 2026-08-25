BssEvent {
	var <bss, <event;
	var <track, <>effectChain, <>method;

	*new { |bss, event|
		^super.newCopyArgs(bss:bss, event:event).init;
	}

	init {
		track = bss.tracks.wrapAt(event[\track] ? 0);
	}

	play { |method(\auto)|
		event.parent = track.defaultParentEvent;
		event.use {
			this.resolveSoundName;
			this.resolveProtoEvent;
			this.playSynths;
		};
	}

	fx { |specs| 
		effectChain = specs;
	}

	resolveSoundName {
		var sound = ~s ? ~sound;
		if (~bank.notNil) {
			sound = format("%_%", ~bank, sound) 
		};
		~sound = sound.asSymbol;
	}

	resolveProtoEvent {
		var event = bss.soundLibrary.getEvent(~sound, ~n, method);
		if (event.isNil) {
			"no sound % in library".format(~sound).error;
		} {
			currentEnvironment.proto = event;
		};
	}

	getMsgFunc { |intrument|
		var msgFunc = SynthDescLib.global.synthDescs.at(intrument).msgFunc;
		if (msgFunc.notNil) {
			^msgFunc;
		} {
			bss.logger.error( "(%): no msgFunc for instrument %", thisMethod, intrument);
		};
	}

	createSynthGroup { |outerGroup|
		~synthGroup = Group(outerGroup ? track.group);
	}

	sendSynth { |instrument, args|
		args = args ?? { this.getMsgFunc(instrument.asSymbol).valueEnvir };
		args.flop.do { |argList|
			Synth.tail(~synthGroup, instrument, argList) 
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
			fxEvent.use {
				bss.modules['effects'][name].value(this) 
			};
		};
	}

	playSynths {
		track.server.bind {
			this.createSynthGroup;
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
