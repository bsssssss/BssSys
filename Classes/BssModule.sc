BssModule {
	var <name, <func, <cond, <kind;

	*new { |name, func, cond, kind|
		^this.newCopyArgs(name, func, cond ? true, kind);
	}

	value { |bssEvent, specs|
		var env = (specs ? ()).asEvent.copy;
		env.parent = bssEvent.env;
		env.use {
			if (cond.value) {
				^func.value(bssEvent) 
			};
		};
	}
}
