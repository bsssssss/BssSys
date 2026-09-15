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
	// Bss.snd(<name>, ...kwargs) -> a BssEvent that can be played
	// name: the name of a synthdef registered in SynthDescLib, or the name of a sound library folder
	r {
		~bss.snd(\bss_sine,
			// after synthdef name, named arguments are used to fill an event 
			// with parameters. this event is then used as the environment for spawning the synths
			freq: exprand(400,600),
			sustain: 1,
			amp: 1,
		).play;
	}.play;
)
(
	// playing the event also returns itself so we can talk to it later;
	r {
		var ev = ~bss.snd(\bss_sine, freq: 1000, sustain: 8);

		"bip".postln;
		ev.play;
		2.wait;

		"kwargs".postln;
		ev.set(freq: 100, amp: 1);
		2.wait;

		"symbol/value pairs".postln;
		ev.set(\freq, 200, \amp, 0.2);
		2.wait;

		"both: kwargs take precedence if colliding".postln;
		ev.set(\freq, 200, \amp, 0.1, freq: 10000);
		2.wait;
	}.play;
)
(
	// sustain can be infinite: mono synths with glide yay!!
	r {
		loop {
			var ev = ~bss.snd(\bss_sine, sustain: inf, fadeIn: 1).play;
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
		loop {
			var dur = 0.03;
			~bss.snd(\bss_sine, sustain: dur*0.5, fadeIn: 0.002).play;
			dur.wait;
		}
	}.play;
)
