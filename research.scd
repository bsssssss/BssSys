(
	s.waitForBoot {
		s.freeAll;
		~bss = Bss(numChannels: 2, logLevel: \info); 
		~bss.loadSoundFiles("~/Sounds/tidal_samples/legowelt/*");
		~bss.start(0 ! 16);
	};
	s.plotTree;
);

// HANDLING SYNTHS ///////////////////////////////////////////////////////////

// this is used in Bss for filling up arguments in an arbitrary synth event call 
// with what the synth expects

// so can see the array of controls defined for the synth, "out", "delta" etc...
SynthDescLib.global.synthDescs.at(\blip).controls

// a function we can use to get back an array of params we can call this synth with
// a bit like a filter
SynthDescLib.global.synthDescs.at(\blip).msgFunc
~blipArgs = SynthDescLib.global.synthDescs.at(\blip).msgFunc.value(out: 0, freq: #{ rrand(100, 1000) }, sustain: 8)
~blipArgs = SynthDescLib.global.synthDescs.at(\blip).msgFunc.value(blabla: 0, bloblo: 1);
Synth("blip", ~blipArgs);

// BUSSES ////////////////////////////////////////////////////////////////////

s.scope;
s.meter;

Synth("blip", [freq: exprand(100, 2000) , sustain: 0.09, out: 0, amp: 0.5]);

~revBus = Bus.audio(s, 2);
r = Synth("reverb", [in: ~revBus], addAction: \addToTail);
r.set(\room, 1, \damp, 0)

Synth("blip", [freq: exprand(100, 2000) , sustain: 0.09, out: ~revBus, amp: 0.5]);


(
	SynthDef("reverb", {
		|in, out, mix = 0.5, room = 0.5, damp = 0.5|
		var snd;
		in = In.ar(in, 2);
		snd = FreeVerb.ar(in:in, mix:mix, room:room, damp:damp, mul:1.0, add:0.0);
		snd = BssPan.ar(snd);
		Out.ar(out, snd);
	}).add;
)

s.plotTree
s.boot

// GROUPS & BUSSES ////////////////////////////////////////////////////////////////////

s.boot;
s.plotTree;
s.meter;

(
	r {
		SynthDef("monitor", { |synthBus, fxBus, outBus|
			var synthSig = In.ar(synthBus, 2);
			var fxSig    = In.ar(fxBus, 2);
			var sig = Limiter.ar(synthSig + fxSig);
			Out.ar(outBus, sig);
		}).add;

		SynthDef("reverb", { |in, out|
			var inSig = In.ar(in, 2);
			var sig = GVerb.ar(inSig, 3, 20, 0.2, drylevel: 0);
			Out.ar(out, sig);
		}).add;

		SynthDef("blip", { |out|
			var env = Env.linen(0.01, 0, 0.4).ar(Done.freeSelf);
			var sig = SinOsc.ar(Line.ar(1, 0, 0.04).linexp(0,1,40,10000)) ! 2;
			sig = sig * env;
			OffsetOut.ar(out, sig * 0.2);
		}).add;

		s.sync;

		~synthGroup = Group.new;
		~fxGroup    = Group.after(~synthGroup);

		~synthBus = Bus.audio(s, 2);
		~fxBus    = Bus.audio(s, 2);
		~outBus   = Bus.audio(s, 2);

		s.sync;

		Synth("reverb",  [in: ~synthBus, out: ~fxBus], ~fxGroup);
		Synth("monitor", [synthBus: ~synthBus, fxBus: ~fxBus, outBus: 0], ~fxGroup, addAction: \addToTail);
	}.play;
)

Synth("blip", [out: ~synthBus], ~synthGroup);

s.freeAll;

s.boot;
s.plotTree;

~mainGroup = s.nextPermNodeID;
~track1Group = s.nextPermNodeID;
s.sendMsg("/g_new", ~mainGroup, 0, 1);
s.sendMsg("/g_new", ~track1Group, 0, ~mainGroup);
~track2Group = s.nextPermNodeID;
s.sendMsg("/g_new", ~mainGroup, 0, 1);
s.sendMsg("/g_new", ~track2Group, 0, ~mainGroup);

s.nodeTreeView
s.freeAll;
