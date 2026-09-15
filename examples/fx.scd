(
	s.waitForBoot {
		~bss = Bss();
		~bss.start; s.sync;
		~bss.sl.loadSoundFiles("~/sounds/sc/recordings", "sc"); s.sync;
		~bss.sl.show;
	};
)

(
	~bss.snd(\bss_sine, freq: 1000 * exprand(1,2), sustain: 1)
		.addFx(\tilt_env, [tilt: 0.5, curveRise: 4])
		.play;
)
