package bg.bas.dcl.verbforms;

import java.util.TreeMap;

public class Word {

	public String[] cols;

	public int id = 0;
	public String form = "";
	public String lemma = "";
	public String upos = "";
	public String xpos = "";
	public String feats = "";
	public int head = 0;

	// tag and lemma as used in the rules
	public String tag = "";
	public String ruleLemma = "";

	public int sentpos = 0;

	public Word(String line, int sentpos) {

		cols = line.split("\t", -1);

		id = Integer.parseInt(cols[0]);
		form = cols[1];
		lemma = cols[2];
		upos = cols[3];
		xpos = cols[4];
		feats = cols[5];

		try {
			head = Integer.parseInt(cols[6]);
		} catch (NumberFormatException e) {
			head = -1;
		}

		this.sentpos = sentpos;
		ruleLemma = lemma;
	}

	public String lower() {
		return form.toLowerCase();
	}

	public TreeMap<String, String> getFeats() {

		TreeMap<String, String> res = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

		if (feats.equals("_") || feats.equals("")) return res;

		for (String f : feats.split("\\|")) {
			int i = f.indexOf("=");
			if (i > 0) res.put(f.substring(0, i), f.substring(i + 1));
		}

		return res;
	}

	public boolean isReflexive() {
		return upos.equals("PRON") && feats.contains("Reflex=Yes") && !feats.contains("Poss=Yes")
				&& (lower().equals("се") || lower().equals("си"));
	}
}
