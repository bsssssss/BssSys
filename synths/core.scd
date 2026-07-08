(
	{
		var numChannels = ~bss.numChannels;

		// Because we cannot declare the number of channels of a soundfile at synth instanciation time, we need to define a sampler synth for each case.
		// By default `maxSoundFileNumChannels` is 2, so this will define 2 synths: "bss_sampler_1ch" and "bss_sampler_2ch".
		// `numChannels` is the number of output channels of this bss instance (2 by default)

		(1..Bss.maxSoundFileNumChannels).do { |soundFileNumChannels|

			var name = format("bss_sampler_%ch", soundFileNumChannels);
			SynthDef(name, {
				var snd, phase;
				var bufnum = \bufnum.ir(0); 
				var rate = \rate.kr(1);
				var numFrames = max(BufFrames.ir(bufnum), 1);
				var phaseRate = rate * BufSampleRate.ir(bufnum);
				var ev = Line.kr(dur: \sustain.ir(1), doneAction: Done.freeSelf);

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
				* \amp.kr(0.4);

				snd = BssPan.ar(snd, numChannels, \pan.kr(0));
				OffsetOut.ar(\out.kr(0), snd);
			}).add;

		};

		SynthDef("bss_track_monitor_%ch".format(numChannels), {
			var sig = In.ar(\inBus.ir(0), numChannels);
			Out.ar(\outBus.ir(0), sig * -12.dbamp);
		}).add;

		SynthDef("bss_output_monitor_%ch".format(numChannels), {
			var sig = In.ar(\inBus.ir(0), numChannels);
			sig = Limiter.ar(sig);
			ReplaceOut.ar(\outBus.ir(0), sig);
		}).add;

		/***********
		* Modules *
		***********/

		~bss.addModule('sound', { |bssEvent|
			var args;
			if (~buffer.notNil) {
				args = [
					bufnum: ~bufnum,
					rate: ~rate,
					sustain: ~sustain ? ~buffer.duration,
					begin: ~begin,
					pan: ~pan,
					amp: ~amp,
				];
				bssEvent.sendSynth(~instrument, args);
			} {
				if (~instrument.notNil) {
					bssEvent.sendSynth(~instrument); // arguments are handled with msgFunc
				} {
					"no sound or synth named %, dropping event...".format(~s).warn
				}
			}
		});
	}.value
)
