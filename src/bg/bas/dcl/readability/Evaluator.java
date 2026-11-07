package bg.bas.dcl.readability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeMap;

public class Evaluator {

	// key -> group, name, direction, unit
	public LinkedHashMap<String,String[]> features = new LinkedHashMap<String,String[]>();

	// feature -> group -> mean, p25, p50, p75
	public HashMap<String,HashMap<String,double[]>> reference = new HashMap<String,HashMap<String,double[]>>();

	TreeMap<Double,Double> freAnchors = new TreeMap<Double,Double>();
	HashMap<String,Double> cefr = new HashMap<String,Double>();
	HashMap<Integer,String> groups = new HashMap<Integer,String>();

	public HashMap<String,String[]> explanations = new HashMap<String,String[]>();

	public void readResources(){

		for (String[] d : Resources.readTable("features.tsv")) {
			if (d.length < 5) continue;
			features.put(d[0].trim(), new String[]{d[1].trim(), d[2].trim(), d[3].trim(), d[4].trim()});
		}

		for (String[] d : Resources.readTable("levels.tsv")) {
			if (d.length < 3) continue;
			String kind = d[0].trim();
			if (kind.equals("fre")) freAnchors.put(Double.valueOf(d[1].trim()), Double.valueOf(d[2].trim()));
			else if (kind.equals("cefr")) cefr.put(d[1].trim().toUpperCase(), Double.valueOf(d[2].trim()));
			else if (kind.equals("group")) groups.put(Integer.valueOf(d[1].trim()), d[2].trim());
		}

		for (String[] d : Resources.readTable("reference_values.tsv")) {
			if (d.length < 7) continue;
			if (!reference.containsKey(d[0].trim())) reference.put(d[0].trim(), new HashMap<String,double[]>());
			reference.get(d[0].trim()).put(d[1].trim(), new double[]{
					Double.valueOf(d[2]), Double.valueOf(d[3]), Double.valueOf(d[4]), Double.valueOf(d[5])});
		}

		for (String[] d : Resources.readTable("explanations_bg.tsv")) {
			if (d.length < 3) continue;
			explanations.put(d[0].trim(), new String[]{d[1].trim(), d[2].trim()});
		}
	}

	// linear interpolation between the anchors: 100 = grade 1, 60 = grade 12
	public double freToGrade(double fre){

		Double hi = freAnchors.lastKey(), lo = freAnchors.firstKey();
		double ghi = freAnchors.get(hi), glo = freAnchors.get(lo);

		return ghi + (hi - fre) * (glo - ghi) / (hi - lo);
	}

	public double gradeToFre(double grade){

		Double hi = freAnchors.lastKey(), lo = freAnchors.firstKey();
		double ghi = freAnchors.get(hi), glo = freAnchors.get(lo);

		return hi - (grade - ghi) * (hi - lo) / (glo - ghi);
	}

	public Evaluation evaluate(TextFeatures tf, Profile p, String lexicon){

		Evaluation e = new Evaluation();

		if (p.isL2()) {
			Double g = cefr.get(p.get("cefr", "A2").toUpperCase());
			e.readerGrade = g == null ? 4 : g;
		} else {
			double g;
			try {
				g = Double.valueOf(p.get("grade", ""));
			} catch (NumberFormatException ex) {
				g = Double.valueOf(p.get("age", "10")) - 6;
			}
			e.readerGrade = Math.max(1, Math.min(12, g));
		}

		e.readerGroup = groups.containsKey((int) Math.round(e.readerGrade)) ? groups.get((int) Math.round(e.readerGrade)) : "10";
		e.strict = e.readerGrade < 4 || (p.isL2() && p.get("cefr", "").equalsIgnoreCase("A1"));

		e.fre = tf.values.get("fre_bg");
		e.textGrade = freToGrade(e.fre);
		e.readerFre = gradeToFre(e.readerGrade);

		ArrayList<String> known = p.known();

		for (String f : features.keySet()) {

			String[] def = features.get(f);
			int dir = Integer.parseInt(def[2]);
			String cat;

			if (f.equals("fre_bg")) {
				if (e.textGrade <= e.readerGrade) cat = "лесно";
				else if (e.textGrade <= e.readerGrade + 2) cat = "приемливо";
				else cat = "трудно";
			} else if (f.startsWith("coverage_")) {
				cat = coverage(tf, p, e, lexicon, f);
			} else {
				Double v = tf.values.get(f);
				double[] r = reference.containsKey(f) ? reference.get(f).get(e.readerGroup) : null;
				if (v == null || r == null || dir == 0) cat = "сведение";
				else {
					boolean strict = e.strict || (p.isDeaf() && (def[0].equals("морфологично") || def[0].equals("синтактично") || def[0].equals("текстово и дискурсно")));
					cat = classify(v, dir, r, strict);
				}
			}

			if (known.contains(f) && cat.equals("трудно")) cat = "приемливо";
			e.categories.put(f, cat);
		}

		int classified = 0, difficult = 0;
		for (String f : e.categories.keySet()) {
			String c = e.categories.get(f);
			// the indices repeat the surface features
			if (features.get(f)[0].equals("индекси") || c.equals("сведение")) continue;
			classified++;
			if (c.equals("трудно")) difficult++;
		}
		double share = classified == 0 ? 0 : (double) difficult / classified;
		String fc = e.categories.get("fre_bg");

		if (fc.equals("трудно") || share > 0.4) e.overall = "труден";
		else if (fc.equals("лесно") && share <= 0.15) e.overall = "лесен";
		else e.overall = "подходящ";

		return e;
	}

	String coverage(TextFeatures tf, Profile p, Evaluation e, String lexicon, String f){

		double[] c = tf.coverage.get(lexicon);
		if (c == null) return "сведение";

		e.lexicon = lexicon;
		e.coverageDirect = c[0];
		e.coverageExtended = c[1];
		e.coverageTokens = c[2];

		if (f.equals("coverage_direct")) return "сведение";

		// L1 list (grades 1-4): compared with the grade 4 exam texts
		if (lexicon.equals("L1")) {
			if (p.isL2() || e.readerGrade > 4 || !reference.containsKey("coverage_extended_L1")) return "сведение";
			return classify(c[1], -1, reference.get("coverage_extended_L1").get("4"), e.strict);
		}

		// other lists: share of known words in the text, 98 % and 95 %
		if (c[2] >= 98) return "лесно";
		if (c[2] >= 95) return "приемливо";
		return "трудно";
	}

	// normal: median and 3rd quartile of the reader's group; strict: 1st quartile and median
	public String classify(double v, int dir, double[] r, boolean strict){

		double t1 = strict ? r[1] : r[2];
		double t2 = strict ? r[2] : r[3];

		if (dir < 0) {
			t1 = strict ? r[3] : r[2];
			t2 = strict ? r[2] : r[1];
			if (v >= t1) return "лесно";
			if (v >= t2) return "приемливо";
			return "трудно";
		}

		if (v <= t1) return "лесно";
		if (v <= t2) return "приемливо";
		return "трудно";
	}
}
