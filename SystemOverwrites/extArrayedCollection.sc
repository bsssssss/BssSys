// default discrete mode to \bars

+ ArrayedCollection {
	plot { |name, bounds, discrete = false, numChannels, minval, maxval, separately = true, parent|
		var array, plotter, depth, hasSubArrays;
		array = this.as(Array);

		depth = array.maxDepth;
		hasSubArrays = depth > 1;

		if(depth > 2) {
			"Cannot currently plot an array with more than 2 dimensions".warn;
			^nil
		};

		plotter = Plotter(name, bounds, parent);
		if(discrete) {
			plotter.plotMode = \bars
		} {
			// if lines, show discrete values with dots if data density is low enough
			if(array.size < (Window.screenBounds.width / 2.5)) {
				plotter.plotMode = \dlines;
			}
		};

		numChannels !? { array = array.unlace(numChannels) };
		array = array.collect {|elem, i|
			case {elem.isKindOf(Env)}		{elem.asMultichannelSignal.flop}
				 {elem.isKindOf(Wavetable)} {elem.asSignal}
				 {elem.isNil}				{Error("Cannot plot array: non-numeric value at index %".format(i)).throw}
  				 {hasSubArrays}				{elem.asArray} 
			  	 {elem};
		};

		plotter.setValue(
			array,
			findSpecs: true,
			refresh: true,
			separately: separately,
			minval: minval,
			maxval: maxval
		);

		^plotter
	}
}
