BssEvent {
	var <bss, <server;
	var <event;
	var <track, <group;
	var <effectChain, <synths, <gateSynth;

	*new { |bss, event|
		^super.newCopyArgs(bss: bss, event: event).init;
	}

	init {
		track = bss.tracks.wrapAt(event[\track] ? 0);
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

	play {
		event.parent = track.defaultParentEvent;
		event.use {
			this.resolveSource; // implementation in subclasses
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
		var name, event;
		name = ~s ? ~sound;
		if (~bank.notNil) {
			name = format("%_%", ~bank, name) 
		};
		~sound = name.asSymbol;

		event = bss.soundLibrary.getBufferEvent(~sound, ~n);
		if (event.notNil) {
			currentEnvironment.proto = event; 
			~sustain = ~sustain ? ~buffer.duration;
		};
	}

	sendSourceSynth {
		var samplerModule = ~sampler ? \bufrd;
		if (~buffer.notNil) {
			bss.moduleLibrary.atSampler(samplerModule).value(this); 
		};
	}
}

BssSynthEvent : BssEvent {
	getSynthEvent { |name|
		var synthDesc;
		synthDesc = SynthDescLib.at(name.asSymbol);
		if (synthDesc.notNil) {
			^(instrument: synthDesc.name);
		};
		"no synth named % in SynthDescLib".format(name).warn;
		^nil;
	}

	resolveSource {
		var name, event;
		name = ~s ? ~sound;
		~sound = name.asSymbol;

		event = this.getSynthEvent(~sound);
		if (event.notNil) {
			currentEnvironment.proto = event; 
			~sustain = ~sustain ? 1;
		};
	}

	sendSourceSynth {
		if (~instrument.notNil) {
			this.sendSynth(~instrument);
		}
	}
}
