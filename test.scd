(
	s.waitForBoot {
		// make a new instance of the system and make it accessible to this environment
		~bss = Bss(numChannels: 2, logLevel: \info); 
		~bss.start([0]);
	};
);

// load a folder containing sound files and bind it to the folder name in the library
// loading the same folder again will overwrite the previous one.
~bss.loadSoundFiles("~/Sounds/samples/addor");

// can use wildcard to load a list of folders !
~bss.loadSoundFiles("~/Sounds/samples/*");
~bss.loadSoundFiles("~/Sounds/tidal_samples/legowelt/*");

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
~bss.logLevel = \info; // can be set
~bss.tracks[0].defaultParentEvent;

~bss.play((s: \addor, n: 8, begin: 0.2, rate: 1, amp: 0.6));
~bss.play((s: \blip, freq: [500,540], sustain: 0.2));
Synth("blip", [freq: 400]);
~bss.modules

s.meter;
s.plotTree;

// Routine tests /////////////////////////////////////////////////////////////

( // is timing accurate ?
	Routine {
		inf.do {
			~bss.play((s: \blip, freq: 50, sustain: 0.1));
			(1/40).wait;
		}
	}.play;
)

(
	e = (
		sliceInto: #{ |arr, n=1|
			var chunkSize;
			n = n.clip(0, arr.size);
			chunkSize = (arr.size / n);
			n.collect { |i|
				var s = (i * chunkSize);
				var e = ((s + chunkSize - 1).clip(0, arr.size - 1));
				var slice = arr[(s.round..e.round)];
				slice;
			};
		}
	);
)
(
	var n = 17;
	var durs;
	Routine {
		fork {
			inf.do { |i|
				if (i % 2 == 0) {
					durs = Array.fill(n, { |i| i/n }) * 0.8;
					// durs = Array.fill(n, { [2, 1, 1/2, 2/3, 3/4, 1/8, 1/6].choose });
					durs = e[\sliceInto].(durs, 5).scramble;
					durs = durs * 1;
					// durs.trunc(0.01).postln;
				};
				~bss.play((s: \leg_kawaik4, n: 134, sustain: 10));
				durs.do { |seq, i|
					if (i % 3 == 0) { 
						~bss.play((s: \leg_korg, n: 38, sustain: 0.03));
						(seq[0] * 1.5).wait;
					} {
						~bss.play((s: \leg_korg, n: 35, sustain: 0.05));
					};
					seq.do { |dur|
						~bss.play((s: \leg_hh, n: 32, sustain: 0.02));
						dur.wait;
					}
				};
			}
		};
	}.play;
)


