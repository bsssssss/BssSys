(
	var numChannels = ~bss.numChannels;

	SynthDef("debug", { 
		|out = -1, gate = -1, sustain = -1, amp = -1, freq = -1|
		var trig = Impulse.kr(0);
		out.poll(trig, "out");
		gate.poll(trig, "gate");
		sustain.poll(trig, "sustain");
		amp.poll(trig, "amp");
		freq.poll(trig, "freq");
		DC.kr(0).poll(trig, "\n");
	}).add;

	SynthDef("blip", { 
		|out, sustain = 1, freq = 440, amp = 0.4|
		var snd, env;

		snd = SinOsc.ar(freq) 
			* Env.perc(\atk.kr(0), sustain).ar(Done.freeSelf)
		    * amp 
			* AmpComp.ar(K2A.ar(freq), 20);

		snd = snd ! numChannels;
		snd = BssPan.ar(snd, numChannels, \pan.kr(0));

		OffsetOut.ar(out, snd)
	}).add;
)
