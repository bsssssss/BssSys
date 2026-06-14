(
	SynthDef("debug", { |out, gate, sustain, amp, freq|
		var trig = Impulse.kr(0);
		out.poll(trig, "out");
		gate.poll(trig, "gate");
		sustain.poll(trig, "sustain");
		amp.poll(trig, "amp");
		freq.poll(trig, "freq");
		DC.kr(0).poll(trig, "\n");
	}).add;

	SynthDef("blip", { |out, delta, freq, amp, pan|
		var snd, env;
		snd = SinOsc.ar(freq: freq) 
			* Env.perc(0.01, delta).ar(Done.freeSelf)
		    * amp;
		snd = snd ! 2;
		snd = BssPan.ar(snd);
		Out.ar(bus:out, channelsArray:snd)
	}).add;
)
