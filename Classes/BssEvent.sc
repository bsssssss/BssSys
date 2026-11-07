BssEvent {
	var <bss, <server;
	var <track, <group;
	var <specs, <ctx;
	var <sourceEvents;
	var <sourceSynths, <>effectChain, <gateSynth;

	*new { |bss, specs|
		^super.newCopyArgs(
			bss: bss,
			specs: specs
		).init;
	}

	init {
		track = bss.tracks.wrapAt(specs[\track] ? 0);
		server = bss.server;
		sourceEvents = [];
		sourceSynths = [];
		effectChain  = [];
	}

	getMsgFunc { |instrument|
		var msgFunc = SynthDescLib.global.synthDescs.at(instrument).msgFunc;
		if (msgFunc.notNil) {
			^msgFunc;
		} {
			format("could not get msgFunc for '%'", instrument).error;
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
			var synth, module;
			// TODO: use switch ?
			if (ev.at(\sourceType) == \buffer) {
				module = bss.moduleLibrary.samplers.at(this.specAt(\sampler, i) ? \bufrd);
				synth = module.value(this, ev);
			};
			if (ev.at(\sourceType) == \synth) {
				ev.parent = ctx;
				ev.use {
					synth = this.sendSynth(ev.at(\instrument));
				}
			};
			if (synth.notNil) {
				sourceSynths = sourceSynths.add(synth) 
			};
		};
	}

	sendGateSynth {
		var name = "bss_gate";
		var synth = Synth.tail(group, name ++ ctx.at(\numChannels), [
			in: track.synthBus,
			out: track.trackBus,
			sustain: ctx.at(\sustain),
			fadeIn: ctx.at(\fadeIn) ? 0,
			fadeOut: ctx.at(\fadeOut) ? 0.04
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

	sendEffectChain {
		effectChain.do { |fx|
			if (fx.notNil) { fx.module.value(this, fx.specs) };
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
		var synthDesc; 
		var bufEvent, synthEvent;

		var name, bank, n, type;
		name = this.specAt(\sound, index);
		bank = this.specAt(\bank, index);
		n    = this.specAt(\n, index);
		type = this.specAt(\sourceType, index) ? \buffer;

		if (bank.notNil) {
			name = format("%_%", bank, name) 
		};

		bufEvent = bss.soundLibrary.getBufferEvent(name, n);
		if (bufEvent.notNil) {
			bufEvent.put(\sustain, this.specAt(\sustain, index) ? bufEvent.at(\duration) ? 1.0);
			bufEvent.put(\sourceType, \buffer);
			bufEvent = bufEvent ++ this.voiceSpecs(index); // get scalar values from specs for this voice
		};

		synthDesc = SynthDescLib.at(name);
		if (synthDesc.notNil) {
			synthEvent = (instrument: synthDesc.name);
			synthEvent.put(\sustain, this.specAt(\sustain, index) ? 1.0);
			synthEvent.put(\sourceType, \synth);
			synthEvent = synthEvent ++ this.voiceSpecs(index); // get scalar values from specs for this voice
		};

		if (bufEvent.isNil) {
			if (synthEvent.isNil) {
				"could not resolve voice: '%' not found.".error;
				^nil;
			} {
				^synthEvent;
			};
		} {
			^bufEvent;
		};
	}

	numVoices {
		var n = 1;
		specs.keysValuesDo { |k, v|
			if (v.isArray) { n = max(n, v.size) };
		};
		^n;
	}

	// resolveSource { ^this.subclassResponsibility(thisMethod) }
	resolveSource {
		var events;
		var n = this.numVoices;
		// TODO: remove nil elements if any
		events = n.collect { |i| this.resolveVoice(i) };
		^events;
	}

	maxSustain {
		var res = 0;
		sourceEvents.do { |e| res = max(res, e.at(\sustain)) };
		^res;
	}

	play {
		ctx = specs.copy;
		ctx.parent = track.defaultParentEvent;
		ctx.use {
			sourceEvents = this.resolveSource;
			if (sourceEvents.isEmpty) {
				"could not play %: sourceEvents is empty".warn;
				^this 
			};
			ctx.put(\sustain, this.maxSustain);
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
		// e.parent = ctx;
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
