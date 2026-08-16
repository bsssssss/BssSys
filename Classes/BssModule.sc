BssModule {
	var <name, <func, <cond;

	*new { |name, func, cond|
		^this.newCopyArgs(name, func, cond ? true);
	}

	value { |event|
		if (cond.value) { func.value(event) };
	}

	specs {
		^[name, func, cond];
	}
}
