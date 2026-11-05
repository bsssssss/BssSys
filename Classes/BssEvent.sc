BssEvent {
	var <bss, <server, <event;
	var <specs, <env, <source;
	var <track, <group;
	var <effectChain, <synths, <gateSynth;

	*new { |bss, specs|
		^super.newCopyArgs(bss: bss, specs: specs).init;
	}

	init {
		track = bss.tracks.wrapAt(specs[\track] ? 0);
		server = bss.server;
		synths = [];
		effectChain = [];
	}

	getMsgFunc { |instrument|
		var msgFunc = SynthDescLib.global.synthDescs.at(instrument).msgFunc;
		if (msgFunc.notNil) {
			^msgFunc;
		} {
			format( "could not get msgFunc for '%'", instrument).error;
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

	sendSourceSynth { ^this.subclassResponsibility(thisMethod) }

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

	addFx { |name, specs|
		var fx;
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

	playSynths {
		server.bind {
			this.createSynthGroup;
			this.sendSourceSynth; // implementation in subclasses
			this.sendEffectChain;
			this.sendGateSynth; // LAST
		}
	}

	resolveSource { ^this.subclassResponsibility(thisMethod) }
	defaultSustain { ^this.subclassResponsibility(thisMethod) }

	play {
		env = specs.copy;
		env.parent = track.defaultParentEvent;
		env.use {
			source = this.resolveSource; // implementation in subclasses
			if (source.isNil) { ^this };
			env.put(\sustain, specs[\sustain] ? this.defaultSustain);
			currentEnvironment.proto = source; 
			this.playSynths;
		};
	}

	set { |...args, kwargs|
		var synth;

		var e = ();
		args.pairsDo { |k, v| e.put(k, v) };
		kwargs.pairsDo { |k, v| e.put(k, v) };
		if (e.isNil) { ^this };

		// target source synth only
		synth = synths[0];

		e.parent = env;
		e.use {
			// valueEnvir.asControlInput allows passing defaultParentEvent params like 'note'
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

	show { |event|
		event.keysValuesDo { |k, v|
			format("%: %", k, v).postln;
		}
	}
}

BssBufferEvent : BssEvent {
	resolveSource {
		var name = specs[\sound];
		var bank = specs[\bank];

		if (bank.notNil) {
			name = format("%_%", bank, name) 
		};

		^bss.soundLibrary.getBufferEvent(name, specs[\n] ? 0);
	}

	defaultSustain { ^source[\duration] }

	sendSourceSynth {
		bss.moduleLibrary.atSampler(specs[\sampler] ? \bufrd).value(this); 
	}
}

BssSynthEvent : BssEvent {
	getSynthEvent { |name|
		var synthDesc = SynthDescLib.at(name.asSymbol);
		if (synthDesc.isNil) {
			"no synth named % in SynthDescLib".format(name).warn;
			^nil;
		};
		^(instrument: synthDesc.name);
	}

	resolveSource {
		^this.getSynthEvent(specs[\sound]);
	}

	defaultSustain { ^1 }

	sendSourceSynth {
		this.sendSynth(source[\instrument]);
	}
}
