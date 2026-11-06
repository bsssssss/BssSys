BssEvent {
	var <bss, <server, <event;
	var <track, <group;
	var <specs, <env, <source;
	var <sourceSynths, <effectChain, <gateSynth;
	var <sourceEvents;

	*new { |bss, specs|
		^super.newCopyArgs(bss: bss, specs: specs).init;
	}

	init {
		track = bss.tracks.wrapAt(specs[\track] ? 0);
		server = bss.server;
		sourceEvents = [];
		sourceSynths = [];
		effectChain = [];
	}

	numVoices {
		var n = 1;
		specs.keysValuesDo { |k, v|
			if (v.isArray) { n = max(n, v.size) };
		};
		^n;
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
		var synths = [];
		specs = specs ?? { this.getMsgFunc(instrument.asSymbol).valueEnvir };
		specs.asControlInput.flop.do { |argList|
			var synth = Synth.tail(group, instrument, argList);
			synths = synths.add(synth);
		};
		^synths;
	}

	// sendSourceSynth { ^this.subclassResponsibility(thisMethod) }
	sendSourceSynth {
		sourceEvents.do { |ev, i|
			var samplerName = this.specAt(\sampler, i) ? \bufrd;
			var module = bss.moduleLibrary.samplers.at(samplerName);
			var synth = module.value(this, ev);
			if (synth.notNil) {
				sourceSynths = sourceSynths.add(synth) 
			};
		};
	}

	sendGateSynth {
		var name = "bss_gate";
		var synth = Synth.tail(group, name ++ env.at(\numChannels), [
			in: track.synthBus,
			out: track.trackBus,
			sustain: env.at(\sustain).debug("gateSynth::~sustain"),
			fadeIn: env.at(\fadeIn) ? 0,
			fadeOut: env.at(\fadeOut) ? 0.04
		]);
		gateSynth = synth;
	}

	addFx { |name, specs|
		var fx;
		if (name.isNil) { ^this };

		fx = bss.moduleLibrary.effects.at(name);
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
			this.sendSourceSynth;
			this.sendEffectChain;
			this.sendGateSynth; // LAST
		}
	}

	specAt { |specName, index|
		var spec = specs.at(specName);
		if (spec.isArray.not) { spec = [spec] };
		^spec.wrapAt(index);
	}

	voiceSpecs { |index|
		var res = ();
		specs.keysValuesDo { |k, v|
			if (v.isArray) { res.put(k, v.wrapAt(index)) };
		};
		^res;
	}

	resolveVoice { |index|
		var name = this.specAt(\sound, index);
		var bank = this.specAt(\bank, index);
		var n    = this.specAt(\n, index);
		var event;

		if (bank.notNil) {
			name = format("%_%", bank, name) 
		};

		event = bss.soundLibrary.getBufferEvent(name, n);
		if (event.notNil) {
			event = event.put(\sourceType, \buffer);
			event = event.put(\sustain, this.specAt(\sustain, index) ? event.at(\duration) ? 1.0);
			event = event ++ this.voiceSpecs(index); // get scalar values from specs for this voice
			^event;
		};

		// TODO: search synthDescLib
		^nil;
	}

	// resolveSource { ^this.subclassResponsibility(thisMethod) }
	resolveSource {
		var events;
		var n = this.numVoices;
		// TODO: remove nil elements if any
		events = n.collect { |i| this.resolveVoice(i) };
		^events;
	}

	// defaultSustain { ^this.subclassResponsibility(thisMethod) }
	defaultSustain { }

	maxSustain {
		var res = 0;
		sourceEvents.do { |e| res = max(res, e.at(\sustain)) };
		^res;
	}

	play {
		env = specs.copy;
		env.parent = track.defaultParentEvent;
		env.use {
			sourceEvents = this.resolveSource;
			if (sourceEvents.isEmpty) { ^this };
			env.put(\sustain, this.maxSustain);
			env.at(\sustain).debug("play::env.sustain");
			~sustain.debug("~sustain");
			this.playSynths;
		};
	}

	set { |...args, kwargs|
		"set method not implemented".warn;
		^this;
		// var synth;
		//
		// var e = ();
		// args.pairsDo { |k, v| e.put(k, v) };
		// kwargs.pairsDo { |k, v| e.put(k, v) };
		// if (e.isNil) { ^this };
		//
		// // target source synth only
		// synth = synths[0];
		//
		// e.parent = env;
		// e.use {
		// 	// valueEnvir.asControlInput allows passing defaultParentEvent params like 'note'
		// 	var specs = this.getMsgFunc(synth.defName).valueEnvir.asControlInput;
		// 	server.bind { synth.set(*specs) };
		// };
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

	defaultSustain {
		^source[\duration] 
	}

	sendSourceSynth {
		sourceSynths = bss.moduleLibrary.samplers.at(specs[\sampler] ? \bufrd).value(this); 
	}
}

BssSynthEvent : BssEvent {
	resolveSource {
		var name = specs[\sound].asSymbol;
		var synthDesc = SynthDescLib.at(name);
		if (synthDesc.isNil) {
			"cannot resolve source synth: no synth named '%' in SynthDescLib".format(name).warn;
			^nil;
		};
		^(instrument: synthDesc.name);
	}

	defaultSustain { ^1 }

	sendSourceSynth {
		sourceSynths = this.sendSynth(source[\instrument]);
	}
}
