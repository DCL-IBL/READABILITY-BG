package bg.bas.dcl.readability;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;

public class AnnotationPipeline {

	// adds the missing DCL columns with the annotation tools, in the order they depend on each other
	public static File annotate(File in, TextDocument check) throws Exception {

		File current = in;
		PrintStream err = System.err;

		// the tools report their statistics on stderr
		System.setErr(new PrintStream(new OutputStream() {
			public void write(int b) {}
		}));

		try {
			if (check.column("DCL:VERBFORM") < 0) current = run("vf", current, new String[]{"-set", "full"});
			if (check.column("DCL:CLAUSE") < 0) current = run("cl", current, null);
			if (check.column("DCL:NP") < 0) current = run("np", current, null);
			if (check.column("DCL:ANAPHORA") < 0) current = run("ar", current, null);
		} finally {
			System.setErr(err);
		}

		return current;
	}

	static File run(String tool, File in, String[] extra) throws Exception {

		File out = File.createTempFile("readability_" + tool, ".conllup");
		out.deleteOnExit();

		String[] args = new String[]{"-i", in.getPath(), "-o", out.getPath()};
		if (extra != null) {
			String[] a = new String[args.length + extra.length];
			System.arraycopy(extra, 0, a, 0, extra.length);
			System.arraycopy(args, 0, a, extra.length, args.length);
			args = a;
		}

		if (tool.equals("vf")) bg.bas.dcl.verbforms.VerbFormTagger.main(args);
		else if (tool.equals("cl")) bg.bas.dcl.clausesplitter.ClauseSplitter.main(args);
		else if (tool.equals("np")) bg.bas.dcl.nounphrases.NounPhraseRecogniser.main(args);
		else bg.bas.dcl.anaphora.AnaphoraResolver.main(args);

		return out;
	}
}
