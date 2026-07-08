BssPan {
	classvar <>defaultPanningFunction;

	*initClass {
		defaultPanningFunction = #{ |signals, numChannels, pan, mul|
			switch(numChannels,
				1, {
					signals.sum * mul;
				},
				2, {
					BssPan2.ar(signals, \span.ir(1), pan, mul);
				},
				{
					Error("numChannels > 2 not implemented").throw;
				}
			);
		};
	}

	*ar { |signal, numChannels=2, pan=0.0, mul=1, panningFunction|
		panningFunction = panningFunction ? defaultPanningFunction;
		^panningFunction.(
			signal.asArray, // signal can be an array of arbitrary size
			numChannels,
			pan,
			mul
		);
	}
}

BssPan2 : UGen {
	*ar { |signals, span=1, pan=0.0, mul=1| 
		signals = signals.asArray;
		if (signals.size == 0) { Error("BssPan2 input has 0 channel").throw };
		if (signals.size == 1) {
			^Pan2.ar(signals[0], pan, mul);
		};
		if (signals.size == 2) {
			^Balance2.ar(signals[0], signals[1], pan, mul);
		};
		if (signals.size > 2) {
			^Splay.ar(signals, span);
		}
	}
}
