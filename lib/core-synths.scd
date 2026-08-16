(
	var numChannels = ~bss.numChannels;

	/*
	 * STATIC - These are instanciated at startup time
	 */

	SynthDef("bss_monitor" ++ numChannels, {
		var sig = In.ar(\in.ir(0), numChannels);
		sig = Limiter.ar(sig * -12.dbamp);
		Out.ar(\out.ir(0), sig);
	}).add;

	/*
	 * SOUND MODULES - These are instanciated per event
	 */

	// gate is special, always last in the audio chain before going out to monitoring synths.
	// It is responsible for the time offset of the event and freeing the event's group.
	// !!! Synthdefs played through this system should only use Out.ar (no OffsetOut!)

	SynthDef("bss_gate" ++ numChannels, { |in, out, sustain|
		var sig = In.ar(in, numChannels);
		var env = Env([0,1,1,0], [0, sustain, 0]).ar(Done.freeGroup);
		sig = sig * env;
		OffsetOut.ar(out, sig);
		ReplaceOut.ar(in, Silent.ar(numChannels));
	}, [\ir, \ir]).add;

	// Because we cannot declare the number of channels of a soundfile at synth instanciation time, we need to define a sampler synth for each case.
	// By default `maxSoundFileNumChannels` is 2, so this will define 2 synths: "bss_sampler_1ch" and "bss_sampler_2ch".
	// `numChannels` is the number of output channels of this bss instance (2 by default)

	(1..Bss.maxSoundFileNumChannels).do { |soundFileNumChannels|

		SynthDef("bss_sampler" ++ soundFileNumChannels, {
			var snd, phase, bufnum, rate, freq, numFrames, phaseRate;
			var trig = Trig.kr(1);

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
			) * \amp.kr(1);

			snd = BssPan.ar(snd, numChannels, \pan.kr(0));
			Out.ar(\out.kr(0), snd);
		}).add;
	};

	// ~bss.addModule('sound', { |bssEvent|
	// 	if (~buffer.notNil) {
	// 		bssEvent.sendSynth(~instrument, [
	// 			bufnum: ~bufnum,
	// 			freq: ~freq,
	// 			sustain: ~sustain ? ~duration ? ~buffer.duration,
	// 			begin: ~begin,
	// 			pan: ~pan,
	// 			amp: ~amp,
	// 			out: ~out,
	// 		]);
	// 	} {
	// 		if (~instrument.notNil) {
	// 			bssEvent.sendSynth(~instrument); // arguments are handled with msgFunc
	// 		} {
	// 			"no sound or synth named %, dropping event...".format(~s).warn
	// 		}
	// 	}
	// });

	/*
	 * FX MODULES -
	 */

	SynthDef("bss_grain_env" ++ numChannels, { |out, sustain, tilt(0.5), hold(0), curveRise(0), curveFall(0)|
		var sig;
		sig = In.ar(out, numChannels);
		sig = sig * BssGrainEnv.ar(tilt, hold, curveRise, curveFall, sustain);
		ReplaceOut.ar(out, sig);
	}, [\ir]).add;

	~bss.addEffectModule('grain_env', #{ |event|
		event.sendSynth("bss_grain_env" ++ ~numChannels, [
			tilt: ~tilt,
			hold: ~hold,
			curveRise: ~curveRise,
			curveFall: ~curveFall,
			sustain: ~sustain,
			out: ~out,
		])
	});

	SynthDef("bss_gverb" ++ numChannels, { |out, sustain, roomsize(10), revtime(3)|
		var sig;
		var trig = Impulse.kr(0);

		sig = In.ar(out, numChannels);
		sig = GVerb.ar(sig, roomsize, revtime);
		sig = sig * Env([1,1,0], [sustain - 0.05, 0.05], \sin).ar;
		ReplaceOut.ar(out, sig);
	}, [\ir]).add;

	~bss.addEffectModule('gverb', #{ |event|
		event.sendSynth("bss_gverb" ++ ~numChannels, [
			roomsize: ~roomsize ? 10,
			revtime: ~revtime ? 3,
			sustain: ~sustain,
			out: ~out,
		])
	});
)
