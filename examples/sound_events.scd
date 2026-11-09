(
	s.waitForBoot {
		~bss = Bss(server: Server.default, numChannels: 2); // register a new Bss instance
		~bss.start(buses: [0]);  
		s.sync;
		~bss.soundLibrary.loadSoundFiles("~/sounds/sc/recordings", "sc"); s.sync;
		~bss.soundLibrary.show;
	};
)

// A sound event consists of a node group with:
//		- a source synth (the generator).
//		- an arbitrary number of chained effects synths.
//		- a gate synth.
//
// The gate handle the event's lifecycle (they free the whole group).

// a basic sound event: 
//		Bss.snd(<name>, [specs]) -> a BssEvent that can be played
// 		name: the name of a synthdef registered in SynthDescLib, or the name of a sound library folder
~bss.snd(\bss_sine, [freq: exprand(400,600), sustain: 1, amp: 1]).play;

// playing the event also returns itself so we can talk to it later;
(
	r {
		var ev = ~bss.snd(\bss_sine, [freq: 1000, sustain: 8]);

		"bip".postln;
		ev.play;
		2.wait;

		"kwargs".postln;
		ev.set(freq: 100, amp: 1);
		2.wait;
	}.play;
)

// sustain can be infinite: mono synths with glide yay!!
( 
	r {
		loop {
			var ev = ~bss.snd(\bss_sine, [sustain: inf, fadeIn: 0.3, scale: Scale.minor]).play;
			var note = rrand(-4,7);
			var rep = rrand(2,9);

			rep.do { |i|
				var t = i / rep;
				ev.set(\note, note, \lag, 0.1);
				note = note + [-6,-2,-1,1,2,6].wchoose([2,10,20,40,3,1].normalizeSum);
				(0.8 / (t+1 * 2)).wait;
			};
			exprand(0.01,1).wait;

			ev.release([2,4,7].choose);
			exprand(0.2,2).wait;
	};
	}.play;
)

// auto-wrapped in server.bind for timing accuracy
(
	r {
		inf.do { |i|
			var t = i / 2000;
			var dur = t.linexp(0,1,0.001,10);
			~bss.snd(\bss_sine, [freq: t.linexp(0,1,100,22000), sustain: dur * 0.001, fadeIn: 0, fadeOut: 0.08]).play;
			dur.wait;
		}
	}.play;
)
