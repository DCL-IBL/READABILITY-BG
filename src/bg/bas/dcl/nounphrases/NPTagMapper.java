package bg.bas.dcl.nounphrases;

public class NPTagMapper {

	// BTB xpos -> tags in np_tags.tsv
	public void setTag(Word w) {

		String x = w.xpos;
		w.tag = "";

		if (x.length() == 0 || x.equals("_") || w.upos.equals("PUNCT")) return;

		char c = x.charAt(0);

		if (w.lower().equals("всичките")) {
			w.tag = "PGde";
		} else if (c == 'N') {
			w.tag = "N" + getDefiniteness(w);
		} else if (c == 'A') {
			w.tag = "A" + getDefiniteness(w);
		} else if (c == 'V') {
			// participles used as adjectives
			if (x.length() > 4 && x.charAt(4) == 'c'
					&& (w.upos.equals("ADJ") || w.deprel.equals("amod"))) w.tag = "A" + getDefiniteness(w);
		} else if (c == 'M') {
			char t = x.length() > 1 ? x.charAt(1) : '-';
			String d = getDefiniteness(w);
			if (t == 'o') w.tag = "MO" + d;
			else if (t == 'c') w.tag = d.equals("i") ? "MCi" : "MCde";
			else if (t == 'd' || t == 'y') w.tag = d.equals("i") ? "Qi" : "Qdo";
		} else if (c == 'P' && x.length() > 2) {
			setPronounTag(w);
		}
	}

	private void setPronounTag(Word w) {

		String x = w.xpos;
		char type = x.charAt(1);
		char ref = x.charAt(2);

		// possessive: Ps + owner + l (full) / t (short)
		if (type == 's') {
			if (x.length() > 3 && x.charAt(3) == 't') w.tag = "PC";
			else if (x.length() > 3 && x.charAt(3) == 'l') w.tag = "PS" + getDefiniteness(w);
			return;
		}

		if (ref == 'q') {
			w.tag = "Qi";
			return;
		}

		if (ref != 'e' && ref != 'a' && ref != 'p') return;

		if (type == 'd') w.tag = "PD";
		else if (type == 'i') w.tag = "PI";
		else if (type == 'r') w.tag = "PR";
		else if (type == 'f') w.tag = "PF";
		else if (type == 'n') w.tag = "PN";
		else if (type == 'c') w.tag = "Def".equals(w.getFeat("Definite")) ? "PGde" : "PGi";
	}

	// i, or d + f (full), s (short), a (-та), o (-то), e (plural)
	public String getDefiniteness(Word w) {

		if (!"Def".equals(w.getFeat("Definite"))) return "i";

		String n = w.getFeat("Number");
		if ("Plur".equals(n) || "Ptan".equals(n) || "Count".equals(n)) return "de";

		String f = w.lower();
		if (f.endsWith("ът") || f.endsWith("ят")) return "df";
		if (f.endsWith("та")) return "da";
		if (f.endsWith("то")) return "do";
		if (f.endsWith("те")) return "de";

		return "ds";
	}
}
