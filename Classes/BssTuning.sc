BssTuning {
	var <>current, <>default, <>octave;
	var <>archivePath, <>tunings;

	*new { |default, octave, archivePath|
		^super.newCopyArgs(
			default: default ? Tuning.et12,
			archivePath: archivePath ? "~/Documents/scala-archive".absolutePath,
			octave: octave ? 5
		).init;
	}

	init {
		tunings = PathName(archivePath).entries.collect { |t| Scala(t.fullPath) };
		current = default;
	}

	reset {
		current = default;
	}

	getAllTunings {
		^tunings;
	}

	noteToMidinote { |note|
		var midinote = current.wrapAt(note ? 0) 
		             + (12 * floor(note / current.size)) // raise octave on wrap
		             + (12 * octave);

		^max(0, midinote);
	}

	noteToFreq { |note|
		^this.noteToMidinote(note).midicps;
	}
}
