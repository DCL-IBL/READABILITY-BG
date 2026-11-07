package bg.bas.dcl.verbforms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.TreeMap;

public class VerbForm extends ArrayList<Integer> {

	private static final long serialVersionUID = 1L;

	public ArrayList<Integer> fixed = new ArrayList<Integer>();
	public int mainword = -1;

	public ArrayList<String> tense = new ArrayList<String>();
	public ArrayList<String> rules = new ArrayList<String>();

	public void addPosition(int i) {
		if (!contains(i)) add(i);
		Collections.sort(this);
	}

	public void addTense(String label, String rule) {

		for (String t : label.split("\\|")) {
			if (!tense.contains(t)) tense.add(t);
		}

		if (!rules.contains(rule)) rules.add(rule);
	}

	public String getLabel() {
		return String.join("|", tense);
	}

	public boolean isAnalytic() {
		return size() > 1;
	}

	// features of the whole form - from the main verb + the label
	// readings that differ only in tense share one set
	public String getGrammTag(Sentence sent) {

		Word main = sent.get(mainword);
		TreeMap<String, String> base = main.getFeats();

		// person/number from the auxiliary if the main verb is a participle
		if (!base.containsKey("Person")) {
			for (int i : fixed) {
				if (i == mainword) continue;
				TreeMap<String, String> f = sent.get(i).getFeats();
				if ("Fin".equals(f.get("VerbForm")) && f.containsKey("Person")) {
					base.put("Person", f.get("Person"));
					if (!base.containsKey("Number") && f.containsKey("Number"))
						base.put("Number", f.get("Number"));
					break;
				}
			}
		}

		LinkedHashSet<String> readings = new LinkedHashSet<String>();
		TreeMap<String, ArrayList<String>> tenses = new TreeMap<String, ArrayList<String>>();

		for (String t : tense) {

			TreeMap<String, String> f = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
			f.putAll(base);
			ArrayList<String> tenseNames = new ArrayList<String>();

			applyLabel(t, f, tenseNames);

			f.remove("Tense");
			String key = toString(f);
			readings.add(key);

			if (!tenses.containsKey(key)) tenses.put(key, new ArrayList<String>());
			for (String n : tenseNames) {
				if (!tenses.get(key).contains(n)) tenses.get(key).add(n);
			}
		}

		ArrayList<String> res = new ArrayList<String>();

		for (String key : readings) {

			TreeMap<String, String> f = parse(key);
			ArrayList<String> tn = tenses.get(key);

			if (tn.size() > 0) f.put("Tense", String.join(",", tn));
			else if (base.containsKey("Tense") && !"Fin".equals(f.get("VerbForm"))) f.put("Tense", base.get("Tense"));

			res.add(toString(f));
		}

		return String.join("/", res);
	}

	private void applyLabel(String label, TreeMap<String, String> f, ArrayList<String> tenseNames) {

		String[] parts = label.split("\\+");
		boolean finite = true;

		for (String p : parts) {

			if (p.equals("Pass")) f.put("Voice", "Pass");
			else if (p.equals("Neg")) f.put("Polarity", "Neg");
			else if (p.equals("Concl") || p.equals("Ren") || p.equals("Dub")) f.put("Evident", p);
			else if (p.equals("Imperative")) f.put("Mood", "Imp");
			else if (p.equals("Conditional")) f.put("Mood", "Cnd");
			else if (p.equals("Conditional_Perfect")) {
				f.put("Mood", "Cnd");
				tenseNames.add("Perfect");
			}
			else if (p.equals("NVerb") || p.equals("DVerb")) finite = false;
			else if (p.equals("Infinitive")) {
				finite = false;
				f.put("VerbForm", "Inf");
			}
			else if (p.endsWith("Participle")) {
				finite = false;
				if (p.startsWith("Perfect")) f.put("Aspect", "Perf");
			}
			else {
				for (String t : p.split("-")) tenseNames.add(camel(t));
			}
		}

		if (finite) {
			f.put("VerbForm", "Fin");
			f.remove("Definite");
			if (!f.containsKey("Mood")) f.put("Mood", "Ind");
		} else {
			tenseNames.clear();
		}
	}

	private String camel(String s) {

		StringBuilder sb = new StringBuilder();

		for (String p : s.split("_")) {
			if (p.length() == 0) continue;
			sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
		}

		return sb.toString();
	}

	private String toString(TreeMap<String, String> f) {

		if (f.size() == 0) return "_";

		ArrayList<String> res = new ArrayList<String>();
		for (String k : f.keySet()) res.add(k + "=" + f.get(k));

		return String.join("|", res);
	}

	private TreeMap<String, String> parse(String s) {

		TreeMap<String, String> res = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

		if (s.equals("_")) return res;

		for (String kv : s.split("\\|")) {
			int i = kv.indexOf("=");
			res.put(kv.substring(0, i), kv.substring(i + 1));
		}

		return res;
	}
}
