BssModuleLibrary {
	var <effects, <samplers;

	*new {
		^super.newCopyArgs().init; 
	}

	init {
		effects = IdentityDictionary(); 
		samplers = IdentityDictionary(); 
	}

	addEffect { |name, func, cond|
		name = name.asSymbol;
		effects.put(name, BssModule(name, func, cond));
	}

	addSampler { |name, func, cond|
		name = name.asSymbol;
		samplers.put(name.asSymbol, BssModule(name, func, cond));
	}

	show {
		"show module library not yet implemented.".warn;
	}
}
