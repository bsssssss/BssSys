(
s.waitForBoot {
    ~bss = Bss();
    ~bss.start; s.sync;
    ~bss.sounds.show;
};
)

s.plotTree;

(
// .addFx(\name, [specs]) to append an effect to the chain
~bss.snd(\bss_sine, [freq: 100 * exprand(1,40), sustain: 1])
    .addFx(\tilt_env, [tilt: 0.5, curveRise: 8])
    .play;
)

(
// [specs] takes precedence over parent specs
~bss.snd(\bss_sine, [freq: 100 * exprand(1,40), sustain: 10])
    .addFx(\tilt_env, [tilt: 0.0, curveRise: 4, sustain: 0.01])
    .addFx(\gverb, [decay: 10, size: exprand(1,100)])
    .play;
)

(
// [specs] takes precedence over parent specs
// 10 seconds sustain
~bss.snd(\bss_sine, [freq: exprand(100,1000), sustain: 10]) 
    .addFx(\tilt_env, [tilt: 0.0, curveRise: 4, sustain: 0.2])
    .addFx(\gverb, [decay: 100000, size: 150])
    // but envelope cut after 1 second
    .addFx(\tilt_env, [tilt: 0.9, hold: 0.2, curveRise: 8, sustain: 1]) 
    // synth will still play for 10 seconds
    .play;
)

(
	// can call an effect multiple times
	~r1 = r {
		var bnz = Pbrown(1,10,1/3).asStream;
		loop {
			var dur = exprand(1/64,exprand(1/30,0.9999)).pow(3) * rrand(1, bnz.next);
			var ev = ~bss.snd(\bss_sine, [
				freq: 40 + exprand(1,10000),
				sustain: dur + exprand(0.001, 10),
				amp: rrand(-24,3.0).dbamp,
				pan: rrand(-0.5,0.5),
			]);

			ev.addFx(\tilt_env, [tilt: 0.0, curveFall: rrand(-8,4.0), sustain: exprand(0.01, 1).pow(1.7)]);
			ev.addFx(\gverb, [decay: 10000, size: rrand(1,300.0)]);

			if (0.9.coin) {
				ev.addFx(\tilt_env, [tilt: [0.99,0].choose, curveRise: rrand(4,12.0)])
			};

			ev.play;
			dur.wait;
		}
	}.play;
)

~r1.stop;
