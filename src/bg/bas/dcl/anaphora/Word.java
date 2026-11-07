package bg.bas.dcl.anaphora;

import java.util.ArrayList;
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

	public int sentpos = 0;
	public int headpos = -1;
	public ArrayList<Integer> children = new ArrayList<Integer>();

	// head of a noun phrase in the input column DCL:NP
	public boolean npHead = false;

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

	// lemma without -(се), -(си)
	public String baseLemma() {
		String l = lemma.toLowerCase();
		if (l.contains("-(")) l = l.substring(0, l.indexOf("-("));
		return l;
	}

	public String getFeat(String name) {

		if (feats.equals("_") || feats.equals("")) return null;

		for (String f : feats.split("\\|")) {
			int i = f.indexOf("=");
			if (i > 0 && f.substring(0, i).equals(name)) return f.substring(i + 1);
		}

		return null;
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
}
