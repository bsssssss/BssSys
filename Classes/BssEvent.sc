BssEvent {
	var <bss, <server, <event, <>searchMethod;
	var <track, <group;
	var <effectChain, <synths, <gateSynth;

	*new { |bss, event, searchMethod|
		^super.newCopyArgs(
			bss: bss,
			event: event,
			searchMethod: searchMethod ? \auto,
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
		var synth, ev = ();
		args.pairsDo { |k, v| ev.put(k, v) };
		kwargs.pairsDo { |k, v| ev.put(k, v) };
		if (ev.isNil) { ^this };

		// target source synth only
		synth = synths[0];

		ev.parent = event;
		ev.use {
			// valueEnvir.asControlInput allows passing defaultParentEvent
			// params like 'note'
			var specs = this.getMsgFunc(synth.defName).valueEnvir.asControlInput;
			server.bind { synth.set(*specs) };
		};
	}

	release { |fadeTime|
		var specs = [\gate, 0];
		if (fadeTime.notNil) {
			specs = specs ++ [\fadeOut, fadeTime] 
		};
		server.bind {
			gateSynth.set(*specs);
		};
	}

	addFx { |name, specs|
		var fx, withSpecs;
		if (name.isNil) { ^this };

		fx = bss.moduleLibrary.atEffect(name);
		if (fx.isNil) { ^this };

		effectChain = effectChain.add((module: fx, specs: specs));
	}

	effectChain_ { |specArray|
		effectChain = specArray.select(_.notNil);
	}

	sendEffectChain {
		effectChain.do { |fx|
			fx.module.value(this, fx.specs); 
		};
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

	sendSynth { |instrument, specs|
		specs = specs ?? { this.getMsgFunc(instrument.asSymbol).valueEnvir };
		specs.asControlInput.flop.do { |argList|
			var synth = Synth.tail(group, instrument, argList);
			synths = synths.add(synth);
		};
	}

	sendSourceSynth {
		if (~buffer.notNil) { 
			var name = ~sampler ? \bufrd;
			bss.moduleLibrary.atSampler(name).value(this);
		} {
			if (~instrument.notNil) {
				this.sendSynth(~instrument);
			}
		}
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
		server.bind {
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
