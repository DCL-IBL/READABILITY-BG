package bg.bas.dcl.nounphrases;

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
	public String deprel = "";

	// tag as used in the rules (np_tags.tsv), empty if the word cannot be part of a noun phrase
	public String tag = "";

	// from the input columns DCL:VERBFORM and DCL:CLAUSE, if present
	public boolean inVerbForm = false;
	public String clause = "";

	public int sentpos = 0;

	public Word(String line, int sentpos) {

		cols = line.split("\t", -1);

		id = Integer.parseInt(cols[0]);
		form = cols[1];
		lemma = cols[2];
		upos = cols[3];
		xpos = cols[4];
		feats = cols[5];
		deprel = cols[7];

		try {
			head = Integer.parseInt(cols[6]);
		} catch (NumberFormatException e) {
			head = -1;
		}

		this.sentpos = sentpos;
	}

	public String lower() {
		return form.toLowerCase();
	}

	public String getFeat(String name) {
		return getFeats().get(name);
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

	public String getPos() {
		int i = 0;
		while (i < tag.length() && Character.isUpperCase(tag.charAt(i))) i++;
		return tag.substring(0, i);
	}

	public boolean isArticled() {
		return tag.length() > getPos().length() && tag.substring(getPos().length()).contains("d");
	}
}
