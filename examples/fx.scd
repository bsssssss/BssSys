(
	s.waitForBoot {
		~bss = Bss();
		~bss.start; s.sync;
		~bss.sl.loadSoundFiles("~/sounds/sc/recordings", "sc"); s.sync;
		~bss.sl.show;
	};
)

(
	// .addFx(\name, [specs]) to append an effect to the chain
	~bss.snd(\bss_sine, [freq: 100 * exprand(1,40), sustain: 1])
		.addFx(\tilt_env, [tilt: 0.0, curveRise: 4])
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
	// can call an effect multiple times
	~bss.snd(\bss_sine, [freq: 100 * exprand(1,10), sustain: exprand(0.01,20)])
		.addFx(\tilt_env, [tilt: 0.0, curveRise: 4, sustain: 0.2])
		.addFx(\gverb, [decay: 10000, size: exprand(1,300)])
		.addFx(\tilt_env, [tilt: [ 0.99,0 ].choose, curveRise: 4])
		.play;
)

