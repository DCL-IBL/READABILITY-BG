package bg.bas.dcl.clausesplitter;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import bg.bas.dcl.clausesplitter.rules.ClauseSplittingRule;
import bg.bas.dcl.clausesplitter.rules.StrongPunctuationClauseSplittingRule.PunctuationException;
import bg.bas.dcl.verbforms.VerbFormFormalism;

public class ClauseSplitter {

	public static final String VERSION = "2.0";

	static void usage() {
		System.err.println(
				"ClauseSplitter " + VERSION + "\n"
				+ "Usage: java -jar ClauseSplitter.jar -i <input file or folder> -o <output file or folder>\n"
				+ "           [-lang bg|en] [-resources <folder>] [-gap <n>]\n"
				+ "  -lang       language of the input (default bg)\n"
				+ "  -resources  folder with resource files that replace the built-in ones\n"
				+ "  -gap        maximum length of the gaps in the verb form rules (default 3)\n"
				+ "  -list       print the clause splitting rules and exit");
	}

	public static void main(String[] args) throws Exception {

		String in = null, out = null, lang = "bg", resources = null;
		int gap = 3;
		boolean list = false;

		for (int i=0; i<args.length; i++) {
			String a = args[i];
			if (a.equals("-i")) in = args[++i];
			else if (a.equals("-o")) out = args[++i];
			else if (a.equals("-lang")) lang = args[++i];
			else if (a.equals("-resources")) resources = args[++i];
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

		ClauseSplittingFileProcessor csfp = new ClauseSplittingFileProcessor();
		csfp.resourceDir = resources;

		ArrayList<Conjunction> conj = csfp.readConjunctionsFromFile(lang);
		HashMap<String,ArrayList<String>> verbs = csfp.readSpecialVerbsFromFile(lang);
		ArrayList<PunctuationException> exc = csfp.readExceptionsFromFile();
		ArrayList<ClauseSplittingRule> rules = csfp.readRulesFromFile(lang, conj, verbs, exc);

		if (list) {
			PrintStream ps = new PrintStream(System.out, true, "UTF-8");
			for (ClauseSplittingRule r : rules) {
				ps.println(r.order + "\t" + r.name + "\t" + r.scope + "\t" + r.type
						+ "\t" + r.options + "\t" + r.direction + "\t" + r.condition + "\t" + r.action);
			}
			return;
		}

		if (in == null || out == null) {
			usage();
			System.exit(1);
		}

		VerbFormFormalism vff = new VerbFormFormalism(gap);
		// full lists of verb forms only
		vff.readModels(csfp.open("verb_form_rules_" + lang + "_full.tsv"));

		RuleBasedClauseSplitter rbcs = new RuleBasedClauseSplitter(vff, rules, lang);

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
			rbcs.processFile(f, o);
		}

		System.err.println("ClauseSplitter " + VERSION + ", language: " + lang
				+ ", conjunctions: " + conj.size() + ", rules: " + rules.size());
		System.err.println("Files: " + files.size() + ", sentences: " + rbcs.sentences
				+ ", verb forms: " + rbcs.forms + ", clauses: " + rbcs.clauses);
	}
}
