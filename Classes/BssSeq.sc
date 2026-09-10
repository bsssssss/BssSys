BssSeq {
	var clock;

	*new { |clock|
		^super.newCopyArgs(
			clock: clock,
		);
	}

	repeat { |steps(1), dt(1), cb, onDone, fork(false)|
		var f = {
			dt = dt.asArray;
			steps.do { |i|
				var phase = i / steps;
				var wt = dt.wrapAt(i);
				cb.value(i, phase, wt);
				wt.wait;
			};
			onDone.value;
		};
		
		if (fork) { f.fork(clock) } { f.value };
	}

	interpolate { |dur, durFrom, durTo, cb, onDone, fork(false)|
		var f = {
			var then = clock.beats;
			var elapsed = { clock.beats - then  };
			var times = Array.interpolation(512, durFrom, durTo);
			var count = 0;

			fork({
				while (elapsed < dur) {
					var phase = elapsed.value / dur;
					var dt = times.at(phase * times.size);
					cb.value(count, phase, dt);
					dt.wait;
					count = count + 1;
				};
				onDone.value;
			}, clock);
			dur.wait;
		};

		if (fork) { f.fork(clock) } { f.value };
	}

}
