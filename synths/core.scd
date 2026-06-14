(

	var numChannels = ~bss.numChannels;

	// Because we cannot declare the number of channels of a soundfile at synth instanciation time, we need to define a sampler synth for each case.
	// By default `maxSoundFileNumChannels` is 2, so this will define 2 synths: "bss_sampler_1ch" and "bss_sampler_2ch".
	// `numChannels` is the number of output channels of this bss instance (2 by default)

	(1..Bss.maxSoundFileNumChannels).do { |soundFileNumChannels|

		var name = format("bss_sampler_%ch", soundFileNumChannels);
		SynthDef(name, { |out, gate, bufnum, amp, begin=0, rate=1, loop=0, pan=0, sustain|
			var snd, phase;
			var numFrames = max(BufFrames.ir(bufnum), 1);
			var phaseRate = rate * BufSampleRate.ir(bufnum);

			var ev = Line.kr(dur: sustain, doneAction: Done.freeSelf);
			//duration.poll(0, "duration");
			//sustain.poll(0, "sustain");

			phase = Sweep.ar(1, phaseRate) 
			      * rate
			      + (begin * numFrames);

			snd = BufRd.ar(
				soundFileNumChannels,
				bufnum,
				phase,
				loop, 4
			) 
			* amp;

			snd = BssPan.ar(snd, numChannels, pan);
			OffsetOut.ar(out, snd);
		}, [\ir, \ir, \ir, \ir, \ir, \ir, \ir, \ir, \ir]).add;

	};
)
