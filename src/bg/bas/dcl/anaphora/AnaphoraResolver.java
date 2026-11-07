package bg.bas.dcl.anaphora;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

public class AnaphoraResolver {

	public static final String VERSION = "1.0";

	static void usage() {
		System.err.println(
				"AnaphoraResolver " + VERSION + "\n"
				+ "Usage: java -jar AnaphoraResolver.jar -i <input file or folder> -o <output file or folder>\n"
				+ "           [-resources <folder>]\n"
				+ "  -resources  folder with resource files that replace the built-in ones\n"
				+ "  -list       print the rules and exit");
	}

	public static void main(String[] args) throws Exception {

		String in = null, out = null, resources = null;
		boolean list = false;

		for (int i=0; i<args.length; i++) {
			String a = args[i];
			if (a.equals("-i")) in = args[++i];
			else if (a.equals("-o")) out = args[++i];
			else if (a.equals("-resources")) resources = args[++i];
			else if (a.equals("-list")) list = true;
			else {
				usage();
				System.exit(1);
			}
		}

		AnaphoraFileProcessor afp = new AnaphoraFileProcessor();
		afp.resourceDir = resources;
		afp.readResources();

		if (list) {
			PrintStream ps = new PrintStream(System.out, true, "UTF-8");
			for (SubjectRule r : afp.msr.rules) ps.println(r.id + "\t" + r.type + "\t" + r.person + "\t" + r.number + "\t" + String.join(",", r.conditions));
			for (AnaphoraRule r : afp.ar.rules) ps.println(r.id + "\t" + r.kinds + "\t" + r.person + "\t" + r.strategy + "\t" + r.scope + "\t" + r.conditions);
			int n = 0;
			for (ArrayList<PronounForm> l : afp.ar.pronouns.values()) n += l.size();
			ps.println("Pronoun forms: " + n);
			return;
		}

		if (in == null || out == null) {
			usage();
			System.exit(1);
		}

		File fin = new File(in);
		File fout = new File(out);
		ArrayList<File> files = new ArrayList<File>();

		if (fin.isDirectory()) {
			File[] all = fin.listFiles();
			Arrays.sort(all);
			for (File f : all) {
				if (f.getName().endsWith(".conllu") || f.getName().endsWith(".conllup")) files.add(f);
			}
			fout.mkdirs();
		} else files.add(fin);

		for (File f : files) {
			File o = fout;
			if (fin.isDirectory()) o = new File(fout, f.getName().replaceAll("\\.conllup?$", "") + ".conllup");
			afp.processFile(f, o);
		}

		PrintStream err = new PrintStream(System.err, true, "UTF-8");
		err.println("AnaphoraResolver " + VERSION + ", subject rules: " + afp.msr.rules.size()
				+ ", anaphora rules: " + afp.ar.rules.size());
		err.println("Files: " + files.size() + ", sentences: " + afp.sentences + ", " + afp.stats);
	}
}
