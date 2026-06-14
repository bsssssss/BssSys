(
	s.waitForBoot {
		~bss = Bss(numChannels: 2, logLevel: \info); 
		~bss.loadSoundFiles("~/Sounds/tidal_samples/legowelt/*");
	};
);

// HANDLING SYNTHS ///////////////////////////////////////////////////////////

// given a synthdef name, gets a function we can call with some params to 
// get back an array of parameters that we can call the synth with
SynthDescLib.global.synthDescs.at(\blip).msgFunc

// so can see the array of controls defined for the synth, "out", "delta" etc...
SynthDescLib.global.synthDescs.at(\blip).controls

// then if we call msgFunc with some params, we get back an argument array that we can use
~blipArgs = SynthDescLib.global.synthDescs.at(\blip).msgFunc.value(out: 0, freq: 1000, delta: 1)
Synth("blip", ~blipArgs);

// this is used in Bss for filling up arguments in an arbitrary synth event call 
// with what the synth expects
