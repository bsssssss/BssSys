(
	s.waitForBoot {
		// make a new instance of the system and make it accessible to this environment
		~bss = Bss(numChannels: 2, logLevel: \debug); 
		// load a folder containing sound files and bind it to the folder name in the library
		// loading the same folder again will overwrite the previous one.
		~bss.loadSoundFiles("~/Sounds/samples/addor");
		// can use wildcard to load a list of folders !
		~bss.loadSoundFiles("~/Sounds/samples/*");
		~bss.loadSoundFiles("~/Sounds/tidal_samples/legowelt/*");
	};
);

// some practical accesses ///////////////////////////////////////////////////

~bss.server;      // the server we're running on
~bss.numChannels; // global number of output channels

~bss.soundLibrary;         // the instance of sound library bound to ~bss
~bss.soundLibrary.show;    // post state of sound library
~bss.soundLibrary.buffers; // buffers as an array of (folder_name -> [buffers])

// when a buffer is registered, it also create an associated event with infos 
// about itself, this event is then merged in the high level Bss.play function.
~bss.soundLibrary.bufferEvents; 

~bss.logger;
~bss.logLevel;
~bss.logLevel = \debug; // can be set
~bss.defaultParentEvent;

~bss.play((s: \addor, n: 8, begin: 0.0, rate: 1, amp: 0.6));
~bss.play((s: \blip, freq: exprand(100, 1000), delta: 8));
s.meter;
s.plotTree;

// Routine tests /////////////////////////////////////////////////////////////

( // is timing accurate ?
	Routine {
		inf.do {
			~bss.play((s: \leg_hh, n: 0, sustain: 0.1));
			0.01.wait;
		}
	}.play;
)
( // a little song
	r.stop;
	r = Routine {
		inf.do { |i|
			var dur = 1 / ([3, 3, 2] @@ i) * 1;
			//if (1.0.rrand(0) < 0.6) { dur = 1/8 };

			if (i % 1 == 0) {
				~bss.play((
					s: \leg_bd,
					n: 27,
					rate: 2,
					sustain: 1.1,
					amp: -12.dbamp
				));
			};

			~bss.play((
				s: \addor,
				n: 0,
				rate: (10/12),
				//sustain: dur * 2,
				amp: -12.dbamp
			));

			dur.wait;
			dur = dur * [1/2, 2/3, 2].choose;

			if (rrand(0.0,1) > 0.67) {
				~bss.play((
					s: \addor,
					n: 3,
					rate: 10/12,
					sustain: dur,
					amp: -12.dbamp
				));
			};

			if (i % 4 == 0) {
				(dur).wait;
				~bss.play((
					s: \leg_sd,
					n: 28,
					rate: 2,
					sustain: 0.2,
					amp: -18.dbamp
				));
			};

			dur.wait;
		}
	};
	r.play;
)

{ SinOsc.ar(1) * 0.9 }.plot(1);
