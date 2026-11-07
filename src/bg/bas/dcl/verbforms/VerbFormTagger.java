package bg.bas.dcl.verbforms;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

public class VerbFormTagger {

	static void usage() {
		System.err.println(
				"Usage: java -jar VerbFormTagger.jar -i <input file or folder> -o <output file or folder>\n"
				+ "           [-lang bg|en] [-set basic|full] [-rules <rules.tsv>] [-gap <n>]\n"
				+ "  -lang   language of the input (default bg)\n"
				+ "  -set    built-in rule set for Bulgarian: basic (default) or full\n"
				+ "  -rules  external rules file (overrides -set)\n"
				+ "  -gap    maximum length of the (n,{...}) gaps in the rules (default 3)\n"
				+ "  -list   print the rules in order of application and exit");
	}

	public static void main(String[] args) throws Exception {

		String in = null, out = null, lang = "bg", set = "basic", rules = null;
		int gap = 3;
		boolean list = false;

		for (int i=0; i<args.length; i++) {
			String a = args[i];
			if (a.equals("-i")) in = args[++i];
			else if (a.equals("-o")) out = args[++i];
			else if (a.equals("-lang")) lang = args[++i];
			else if (a.equals("-set")) set = args[++i];
			else if (a.equals("-rules")) rules = args[++i];
			else if (a.equals("-gap")) gap = Integer.parseInt(args[++i]);
			else if (a.equals("-list")) list = true;
			else {
				usage();
				System.exit(1);
			}
		}

		if (!lang.equals("bg") && !lang.equals("en")) {
			System.err.println("Unknown language: " + lang);
			System.exit(1);
		}

		VerbFormFormalism vff = new VerbFormFormalism(gap);
		InputStream rs;
		String rulesName;

		if (rules != null) {
			rs = new FileInputStream(rules);
			rulesName = rules;
		} else {
			if (lang.equals("en")) set = "full";
			rulesName = "verb_form_rules_" + lang + "_" + set + ".tsv";
			rs = VerbFormTagger.class.getResourceAsStream("/rules/" + rulesName);
			if (rs == null) {
				System.err.println("No built-in rules: " + rulesName);
				System.exit(1);
			}
		}

		vff.readModels(rs);

		if (list) {
			PrintStream ps = new PrintStream(System.out, true, "UTF-8");
			for (VerbFormModel m : vff) {
				ps.println(m.id + "\t" + m.getFormModelLen() + "\t" + m.tense + "\t" + m.pattern);
			}
			return;
		}

		if (in == null || out == null) {
			usage();
			System.exit(1);
		}

		ConllFileProcessor p = new ConllFileProcessor(vff, new TagMapper(lang));

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
			p.processFile(f, o);
		}

		System.err.println("Rules: " + rulesName + " (" + vff.size() + ")");
		System.err.println("Files: " + files.size() + ", sentences: " + p.sentences
				+ ", verb forms: " + p.forms + " (analytic: " + p.analytic
				+ ", ambiguous: " + p.ambiguous + ")");
	}
}
