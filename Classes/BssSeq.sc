BssSeq {
	var clock;

	*new { |clock|
		^super.newCopyArgs(
			clock: clock,
		);
	}

	steps { |steps(1), dt(1), cb, onDone, fork(false)|
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
		
		if (fork) { ^f.fork(clock) } { ^f };
	}
	step { |steps(1), dt(1), cb, onDone, fork(false)|
		^this.steps(steps, dt, cb, onDone, fork);
	}
	stp { |steps(1), dt(1), cb, onDone, fork(false)|
		^this.steps(steps, dt, cb, onDone, fork);
	}
	repeat { |steps(1), dt(1), cb, onDone, fork(false)|
		^this.steps(steps, dt, cb, onDone, fork);
	}

	while { |duration(1), dt(1), cb, onDone, fork(false)|
		var f = {
			var then = clock.beats;
			var elapsed = { clock.beats - then  };
			var times = dt.asArray;
			var count = 0;

			fork({
				while (elapsed < duration) {
					var phase = elapsed.value / duration;
					var dt = times.wrapAt(count);
					cb.value(count, phase, dt);
					dt.wait;
					count = count + 1;
				};
				onDone.value;
			}, clock);

			duration.wait;
		};
		
		if (fork) { ^f.fork(clock) } { ^f };
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

		if (fork) { ^f.fork(clock) } { ^f };
	}

	interp { |dur, durFrom, durTo, cb, onDone, fork(false)| 
		^this.interpolate(dur, durFrom, durTo, cb, onDone, fork);
	}
}
