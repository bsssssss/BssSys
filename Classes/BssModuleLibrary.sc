BssModuleLibrary {
	var <modules, <effects, <samplers, <types;

	*new {
		^super.newCopyArgs().init;
	}

	init {
		types = [\effect, \sampler];
		modules = IdentityDictionary();
		effects = IdentityDictionary();
		samplers = IdentityDictionary();
		modules.put(\effect, effects);
		modules.put(\sampler, samplers);
	}

	add { |type, name, func, cond|
		var module;
		if (types.indexOf(type).isNil) {
			"unrecognized module type %. Expecting %".format(type, types).error;
			^nil;
		};
		name = name.asSymbol;
		module = BssModule(name, func, cond);
		modules[type].put(name, module);
	}

	show {
		"Module Library:".postln;
		modules.keysValuesDo { |k, v|
			format("% (% module%)", k, v.size, if(v.size > 1, "s", "")).postln;
			v.do { |module, i|
				("\t* " ++ module.name).postln; 
			};
		};
		"".postln;
	}
}
