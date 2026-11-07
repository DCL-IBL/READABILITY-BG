package bg.bas.dcl.readability;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;

public class ReadabilityEvaluator {

	public static final String VERSION = "1.0";

	static final String[] GROUPS = {"фонологично и графично", "лексикално", "морфологично", "синтактично", "текстово и дискурсно", "индекси"};

	static void usage() {
		System.err.println(
				"ReadabilityEvaluator " + VERSION + "\n"
				+ "Usage: java -jar ReadabilityEvaluator.jar -i <text.conllu> [-p <profile.txt>] [-json <result.json>]\n"
				+ "           [-o <annotated.conllup>] [-resources <folder>]\n"
				+ "       java -jar ReadabilityEvaluator.jar -reference <folder> [-metadata <metadata.tsv>] -out <reference_values.tsv>\n"
				+ "  -i          text in CoNLL-U (UDPipe 2), with or without the DCL annotation columns\n"
				+ "  -p          profile of the reader (key: value); without it only the features are computed\n"
				+ "  -json       result as JSON (used by index.html)\n"
				+ "  -o          the text with all annotations (CoNLL-U Plus)\n"
				+ "  -resources  folder with resource files that replace the built-in ones");
	}

	public static void main(String[] args) throws Exception {

		String in = null, profile = null, json = null, out = null, reference = null, metadata = null, refOut = null;

		for (int i=0; i<args.length; i++) {
			String a = args[i];
			if (a.equals("-i")) in = args[++i];
			else if (a.equals("-p")) profile = args[++i];
			else if (a.equals("-json")) json = args[++i];
			else if (a.equals("-o")) out = args[++i];
			else if (a.equals("-resources")) Resources.resourceDir = args[++i];
			else if (a.equals("-reference")) reference = args[++i];
			else if (a.equals("-metadata")) metadata = args[++i];
			else if (a.equals("-out")) refOut = args[++i];
			else {
				usage();
				System.exit(1);
			}
		}

		PrintStream ps = new PrintStream(System.out, true, "UTF-8");

		FeatureExtractor fe = new FeatureExtractor();
		fe.readConjunctions(Resources.readTable("conj_bg.tsv"));

		ArrayList<String[]> derivation = Resources.readTable("derivation.tsv");
		LinkedHashMap<String,Lexicon> lexicons = new LinkedHashMap<String,Lexicon>();
		lexicons.put("L1", new Lexicon("L1", Resources.readTable("lexicons/lexicon_L1_grades1-4.tsv"), derivation));
		lexicons.put("L2", new Lexicon("L2", Resources.readTable("lexicons/lexicon_L2_A1-A2.tsv"), derivation));

		for (String[] d : Resources.readTable("lexicons/abstract_nouns_bg.tsv")) fe.abstractNouns.add(d[0].trim().toLowerCase());

		if (reference != null) {
			ReferenceBuilder.build(new File(reference), metadata == null ? null : new File(metadata),
					new File(refOut == null ? "reference_values.tsv" : refOut), fe, lexicons);
			return;
		}

		if (in == null) {
			usage();
			System.exit(1);
		}

		Profile p = null;
		String lexicon = "L1";

		if (profile != null) {
			p = new Profile();
			p.read(new File(profile));
			lexicon = p.get("lexicon", p.isL2() ? "L2" : "L1");
			if (!lexicons.containsKey(lexicon)) {
				lexicons.put(lexicon, new Lexicon(lexicon, Resources.readTable(lexicon), derivation));
			}
		}

		TextDocument check = new TextDocument();
		check.read(new File(in));
		File annotated = AnnotationPipeline.annotate(new File(in), check);

		TextDocument doc = new TextDocument();
		doc.read(annotated);
		if (out != null) Files.copy(annotated.toPath(), new File(out).toPath(), StandardCopyOption.REPLACE_EXISTING);

		TextFeatures tf = fe.extract(doc, lexicons);

		Evaluator ev = new Evaluator();
		ev.readResources();

		Evaluation e = p == null ? null : ev.evaluate(tf, p, lexicon);

		if (json != null) {
			Writer w = new OutputStreamWriter(new FileOutputStream(json), StandardCharsets.UTF_8);
			w.write(JsonWriter.toJson(new File(in).getName(), doc, tf, e, p));
			w.close();
		}

		if (e == null) {
			for (String k : tf.values.keySet()) ps.println(k + "\t" + fmt(tf.values.get(k)));
			for (String k : tf.coverage.keySet()) ps.println("coverage_" + k + "\t" + fmt(tf.coverage.get(k)[0]) + "\t" + fmt(tf.coverage.get(k)[1]) + "\t" + fmt(tf.coverage.get(k)[2]));
			return;
		}

		report(ps, new File(in).getName(), doc, tf, e, ev, p);
	}

	static String fmt(double d){
		return String.format(Locale.ROOT, "%.1f", d).replace('.', ',');
	}

	static String grade(double g){
		if (g > 12.5) return "над 12. клас";
		if (g < 1) return "1. клас";
		return fmt(g) + " клас";
	}

	static void report(PrintStream ps, String name, TextDocument doc, TextFeatures tf, Evaluation e, Evaluator ev, Profile p){

		ps.println("ОЦЕНКА НА ЧЕТИВНОСТТА – " + name);
		ps.println();
		ps.println("Читател: " + (p.isL2() ? "български като втори език, ниво " + p.get("cefr", "A2") + " (≈ " + (int) e.readerGrade + ". клас)"
				: "български като първи език, " + (int) e.readerGrade + ". клас")
				+ (p.isDeaf() ? ", глух читател" : "")
				+ "; референтна група: " + e.readerGroup + ". клас" + (e.readerGroup.equals("10") ? " (10. и 12. клас)" : ""));
		ps.println("Текст: " + tf.get("sentences") + " изречения, " + tf.get("words") + " думи");
		ps.println();
		ps.println("ОБЩА ОЦЕНКА: текстът е " + e.overall.toUpperCase() + " за читателя.");
		ps.println("FRE_bg = " + fmt(e.fre) + " (ниво ≈ " + grade(e.textGrade) + "); очаквано за читателя: " + fmt(e.readerFre)
				+ " – " + e.categories.get("fre_bg"));
		if (!e.lexicon.equals("")) {
			ps.println("Лексикално покритие (" + e.lexicon + "): пряко " + fmt(e.coverageDirect) + " %, разширено "
					+ fmt(e.coverageExtended) + " % от пълнозначните думи; " + fmt(e.coverageTokens)
					+ " % от всички думи – " + e.categories.get("coverage_extended"));
		}

		for (String g : GROUPS) {
			ps.println();
			ps.println(g.toUpperCase());
			for (String f : ev.features.keySet()) {
				String[] def = ev.features.get(f);
				if (!def[0].equals(g) || f.startsWith("coverage_") || f.equals("fre_bg")) continue;
				Double v = tf.values.get(f);
				if (v == null) continue;
				double[] r = ev.reference.containsKey(f) ? ev.reference.get(f).get(e.readerGroup) : null;
				ps.println(String.format("  %-60s %8s %-6s", def[1], fmt(v), def[3])
						+ (r == null ? "" : "  (група: " + fmt(r[1]) + "–" + fmt(r[3]) + ")")
						+ "  " + e.categories.get(f));
			}
		}

		ps.println();
		ps.println("КАКВО МОЖЕ ДА ЗАТРУДНИ ЧИТАТЕЛЯ");
		boolean any = false;
		for (String level : new String[]{"трудно", "приемливо"}) {
			for (String f : e.categories.keySet()) {
				if (!e.categories.get(f).equals(level)) continue;
				String[] x = ev.explanations.get(f + ":" + level);
				if (x == null) continue;
				any = true;
				ps.println("  [" + level + "] " + ev.features.get(f)[1] + ": " + x[0]);
				ps.println("      → " + x[1]);
			}
		}
		if (!any) ps.println("  Няма характеристики над обичайното ниво за читателя.");

		ps.println();
		ps.println("МЕСТА В ТЕКСТА");

		LinkedHashMap<String,LinkedHashSet<String>> unknown = new LinkedHashMap<String,LinkedHashSet<String>>();
		int shown = 0;

		for (Place pl : tf.places) {

			if (pl.key.equals("place:unknown_word")) {
				if (!pl.lexicon.equals(e.lexicon)) continue;
				if (!unknown.containsKey("")) unknown.put("", new LinkedHashSet<String>());
				unknown.get("").add(pl.detail);
				continue;
			}
			if (!relevant(pl.key, e)) continue;
			if (shown++ >= 30) continue;

			String[] x = ev.explanations.get(pl.key);
			ps.println("  " + doc.sentIds.get(pl.sentence) + ": " + mark(doc, pl) + " – " + pl.detail
					+ (x == null ? "" : ". " + x[0] + " → " + x[1]));
		}
		if (shown > 30) ps.println("  … и още " + (shown - 30));

		if (unknown.containsKey("") && !e.categories.get("coverage_extended").equals("сведение")) {
			ps.println();
			ps.println("ДУМИ ИЗВЪН ЛЕКСИКАЛНИЯ МИНИМУМ (" + e.lexicon + "): " + String.join(", ", unknown.get("")));
		}
	}

	// places are shown for the features that are not easy for the reader
	static boolean relevant(String key, Evaluation e){
		String f = "";
		if (key.equals("place:long_sentence")) f = "asl";
		else if (key.equals("place:many_clauses")) f = "clauses_per_sent";
		else if (key.equals("place:long_word")) f = "asw";
		else if (key.equals("place:complex_verb")) f = "analytic_per_sent";
		else if (key.equals("place:abstract_word")) f = "abstract_pct";
		else if (key.equals("place:evidential_verb")) f = "evidential_per100";
		else if (key.equals("place:passive_verb")) f = "passive_per100";
		else if (key.equals("place:far_antecedent") || key.equals("place:unresolved")) f = "antecedent_distance";
		String c = e.categories.get(f);
		return c != null && !c.equals("лесно");
	}

	static String mark(TextDocument doc, Place pl){

		if (pl.tokens.size() == 0) {
			String t = doc.sentTexts.get(pl.sentence);
			return "„" + (t.length() > 90 ? t.substring(0, 87) + "…" : t) + "“";
		}

		ArrayList<String> words = new ArrayList<String>();
		for (int i : pl.tokens) words.add(doc.sentences.get(pl.sentence).get(i).form);

		return "„" + String.join(" ", words) + "“";
	}
}
