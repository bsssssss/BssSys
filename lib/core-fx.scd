(
	var numChannels = ~bss.numChannels;

	SynthDef("bss_tilt_env" ++ numChannels, { |out, sustain, tilt(0.5), hold(0), curveRise(0), curveFall(0)|
		var sig;
		sig = In.ar(out, numChannels);
		sig = sig * BssGrainEnv.ar(tilt, hold, curveRise, curveFall, sustain);
		ReplaceOut.ar(out, sig);
	}, [\ir]).add;

	~bss.addEffectModule('tilt_env', #{ |bssEvent|
		bssEvent.sendSynth("bss_tilt_env" ++ ~numChannels, [
			tilt: ~tilt,
			hold: ~hold,
			curveRise: ~curveRise,
			curveFall: ~curveFall,
			sustain: ~sustain,
			out: ~out,
		])
	});

	SynthDef("bss_gverb" ++ numChannels, { |out, sustain, roomsize(14), revtime(1.2)|
		var sig;
		var trig = Impulse.kr(0);

		sig = In.ar(out, numChannels);
		sig = GVerb.ar(sig, roomsize, revtime);
		sig = sig * Env([1,1,0], [sustain - 0.05, 0.05], \sin).ar;
		ReplaceOut.ar(out, sig);
	}, [\ir]).add;

	~bss.addEffectModule('gverb', #{ |bssEvent|
		bssEvent.sendSynth("bss_gverb" ++ ~numChannels, [
			roomsize: ~size ? 14,
			revtime: ~decay ? 1,
			sustain: ~sustain,
			out: ~out,
		])
	});
)
