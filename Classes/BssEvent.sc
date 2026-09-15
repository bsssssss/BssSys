BssEvent {
	var <bss, <server, <event, <>searchMethod;
	var <track, <group;
	var <effectChain, <synths, <gateSynth;

	*new { |bss, event|
		^super.newCopyArgs(
			bss: bss,
			event: event
		).init;
	}

	init {
		track = bss.tracks.wrapAt(event[\track] ? 0);
		server = bss.server;
		synths = [];
		effectChain = [];
	}

	play {
		event.parent = track.defaultParentEvent;
		event.use {
			this.resolveSoundName;
			this.resolveProtoEvent;
			this.calculateTimeSpan;
			this.playSynths;
		};
	}

	set { |...args, kwargs|
		args.pairsDo { |k, v| event.put(k, v) };
		kwargs.pairsDo { |k, v| event.put(k, v) };
		event.use {
			synths.do { |synth|
				var name = synth.defName;
				var msgFunc = this.getMsgFunc(name);
				var args = msgFunc.valueEnvir.asControlInput;
				server.bind {
					synth.set(*args) 
				};
			};
		};
	}

	release { |fadeTime|
		var args = [\gate, 0];
		if (fadeTime.notNil) {
			args = args ++ [\fadeOut, fadeTime] 
		};
		server.bind {
			gateSynth.set(*args);
		};
	}

	addFx { |name, args|
		if (name.notNil) {
			effectChain = effectChain.add([name.asSymbol, args ? #[]]);
		};
	}

	effectChain_ { |specArray|
		effectChain = specArray.select(_.notNil);
	}

	resolveSoundName {
		var sound = ~s ? ~sound;
		if (~bank.notNil) {
			sound = format("%_%", ~bank, sound) 
		};
		~sound = sound.asSymbol;
	}

	resolveProtoEvent {
		var soundEvent = bss.soundLibrary.getEvent(~sound, ~n, searchMethod);
		if (soundEvent.notNil) {
			currentEnvironment.proto = soundEvent;
		}
	}

	calculateTimeSpan {
		var sustain;
		if (~buffer.notNil) {
			sustain = ~sustain ? ~buffer.duration;
		} {
			sustain = ~sustain ? 1;
		};
		~sustain = sustain;
	}

	getMsgFunc { |intrument|
		var msgFunc = SynthDescLib.global.synthDescs.at(intrument).msgFunc;
		if (msgFunc.notNil) {
			^msgFunc;
		} {
			format( "could not get msgFunc for '%'", intrument).error;
		};
	}

	createSynthGroup { |outerGroup|
		group = Group(outerGroup ? track.group);
	}

	sendSynth { |instrument, args|
		args = args ?? { this.getMsgFunc(instrument.asSymbol).valueEnvir };
		args.asControlInput.flop.do { |argList|
			var synth = Synth.tail(group, instrument, argList);
			synths = synths.add(synth);
		};
	}

	sendSourceSynth {
		if (~buffer.notNil) { 
			var sampler = ~sampler ? \bufrd;
			bss.moduleLibrary.samplers[sampler.asSymbol].value(this);
		} {
			if (~instrument.notNil) {
				this.sendSynth(~instrument);
			}
		}
	}

	sendEffectChain {
		effectChain.do { |spec|
			var name, fxEvent;
			name = spec[0].asSymbol;
			fxEvent = spec[1].asEvent;
			fxEvent.parent = event;
			fxEvent.use {
				bss.moduleLibrary.effects[name].value(this) 
			};
		};
	}

	sendGateSynth {
		var name = "bss_gate";
		var synth = Synth.tail(group, name ++ ~numChannels, [
			in: track.synthBus,
			out: track.trackBus,
			sustain: ~sustain,
			fadeIn: ~fadeIn ? 0,
			fadeOut: ~fadeOut ? 0.04
		]);
		gateSynth = synth;
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
