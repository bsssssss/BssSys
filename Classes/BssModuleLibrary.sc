
BssModuleLibrary {
	var <modules;

	*new {
		^super.newCopyArgs().init;
	}

	init {
		modules = IdentityDictionary();
	}

	add { |name, func, cond, role|
		var m;
		name = name.asSymbol;
		m = BssModule(name, func, cond, role);
		modules.put(name, m);
	}

	addEffect { |name, func, cond|
		this.add(name, func, cond, \effect);
		"effect modules <- % ".format(name).postln;
	}

	addSampler { |name, func, cond|
		this.add(name, func, cond, \sampler);
		"sampler modules <- % ".format(name).postln;
	}

	at { |name|
		var m = modules.at(name.asSymbol);
		if (m.isNil) {
			"could not find module '%' in module lib".format(name).warn;
		};
		^m;
	}

	atKind { |name, kind|
		var m = this.at(name);
		if (m.notNil and: { m.kind.notNil } and: { m.kind != kind.asSymbol }) {
			"% exists but is of kind %, not %".format(name, m.kind, kind).warn;
			^nil;
		};
		^m;
	}

	atEffect { |name|
		^this.atKind(name, \effect);
	}

	atSampler { |name|
		^this.atKind(name, \sampler);
	}

	show {
		"Module Library:".postln;
		"!!! WIP !!!".postln;
		"".postln;
	}
}
