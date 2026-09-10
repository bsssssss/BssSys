(
	var numChannels = ~bss.numChannels;

	/*
	 * These are instanciated at startup time
	 */

	SynthDef("bss_monitor" ++ numChannels, {
		var sig = In.ar(\in.ir(0), numChannels);
		sig = Limiter.ar(sig * -12.dbamp);
		Out.ar(\out.ir(0), sig);
	}).add;

	/*
	 * These are instanciated per event
	 */

	// gate is special, always last in the audio chain before going out to monitoring synths.
	// It is responsible for the time offset of the event and freeing the event's group.
	// Synthdefs played through this system should only use Out.ar (no OffsetOut!)

	SynthDef("bss_gate_sustain" ++ numChannels, { |in, out, sustain, fadeIn(0), fadeOut(0.04)|
		var sig, env;
		sig = In.ar(in, numChannels);
		env = Env([0,1,1,0], [fadeIn, sustain, fadeOut]).ar(Done.freeGroup);
		sig = sig * env;
		OffsetOut.ar(out, sig);
		ReplaceOut.ar(in, Silent.ar(numChannels)); // because their could be multiple parallel synths playing on the same bus
	}, [\ir, \ir]).add;

	SynthDef("bss_gate" ++ numChannels, { |in, out, gate(1), fadeIn(0), fadeOut(0.04)|
		var sig, env;
		sig = In.ar(in, numChannels);
		env = EnvGen.ar(
			Env.new(
				levels: [0,1,0],
				times: [fadeIn, fadeOut],
				curve: \sin,
				releaseNode: 1
			),
			gate: gate,
			doneAction: Done.freeGroup
		);
		sig = sig * env;
		OffsetOut.ar(out, sig);
		ReplaceOut.ar(in, Silent.ar(numChannels));
	}, [\ir, \ir]).add;

	// Because we cannot declare the number of channels of a soundfile at synth instanciation time, we need to define a sampler synth for each case.
	// By default `maxSoundFileNumChannels` is 2, so this will define 2 synths: "bss_sampler_1ch" and "bss_sampler_2ch".
	// `numChannels` is the number of output channels of this bss instance (2 by default)

	(1..Bss.maxSoundFileNumChannels).do { |soundFileNumChannels|
		SynthDef("bss_sampler_bufrd" ++ soundFileNumChannels, {
			var snd, phase, bufnum, rate, freq, numFrames, phaseRate;

			bufnum = \bufnum.ir(0);
			freq   = \freq.kr(440);
			rate = freq / 60.midicps;
			numFrames = max(BufFrames.ir(bufnum), 1);
			phaseRate = rate * BufSampleRate.ir(bufnum);

			phase = Sweep.ar(1, phaseRate) 
				  * rate
				  + (\begin.kr(0) * numFrames);

			snd = BufRd.ar(
				soundFileNumChannels,
				bufnum,
				phase,
				\loop.kr(0), 
				4
			) 
			* \amp.kr(1);

			snd = BssPan.ar(snd, numChannels, \pan.kr(0));
			Out.ar(\out.kr(0), snd);
		}).add;
	};

	~bss.addSamplerModule('bufrd', { |bssEvent|
		bssEvent.sendSynth("bss_sampler_bufrd" ++ ~bufNumChannels, [
			bufnum: ~bufnum,
			begin: ~begin,
			freq: ~freq,
			rate: ~rate,
			sustain: ~sustain ? ~buffer.duration,
			pan: ~pan,
			amp: ~amp,
			out: ~out,
		]);
	});
)
