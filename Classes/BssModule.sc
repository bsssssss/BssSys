BssModule {
	var <name, <func, <cond;

	*new { |name, func, cond|
		^this.newCopyArgs(name, func, cond ? true);
	}

	value { |bssEvent|
		if (cond.value) { func.value(bssEvent) };
	}

	specs {
		^[name, func, cond];
	}
}
