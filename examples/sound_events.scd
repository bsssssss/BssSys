(
	s.waitForBoot {
		~bss = Bss();
		~bss.start; s.sync;
		~bss.sl.loadSoundFiles("~/sounds/sc/recordings", "sc"); s.sync;
		~bss.sl.show;
	};
)

// A sound event consists of a node group with:
//		- a source synth (the generator).
//		- an arbitrary number of chained effects synths.
//		- a gate synth.
//
// The gate handle the event's lifecycle (they free the whole group).

(
	// a basic sound event: 
	// Bss.snd(<name>, [specs], \searchMethod) -> a BssEvent that can be played
	// name: the name of a synthdef registered in SynthDescLib, or the name of a sound library folder
	r {
		~bss.snd(\bss_sine, [freq: exprand(400,600), sustain: 1, amp: 1]).play;
	}.play;
)
(
	// playing the event also returns itself so we can talk to it later;
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
(
	// sustain can be infinite: mono synths with glide yay!!
	r {
		loop {
			var ev = ~bss.snd(\bss_sine, [sustain: inf, fadeIn: 1]).play;
			16.do { |i|
				var t = i / 16;
				ev.set(\freq, 100 * (i + 1), \lag, 0.1);
				(0.3 / (t+1)).wait;
			};
			ev.release(3);
		}
	}.play;
)
(
	// auto-wrapped in server.bind for timing accuracy
	r {
		inf.do { |i|
			var t = i / 2000;
			var dur = t.linexp(0,1,0.001,10);
			~bss.snd(\bss_sine, [freq: t.linexp(0,1,100,22000), sustain: dur * 0.001, fadeIn: 0.000, fadeOut: 0.1]).play;
			dur.wait;
		}
	}.play;
)
