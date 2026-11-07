package bg.bas.dcl.verbforms;

public class TagMapper {

	public String lang = "bg";

	public TagMapper(String lang) {
		this.lang = lang;
	}

	public void setTag(Word w) {
		if (lang.equals("en")) setTagEn(w);
		else setTagBg(w);
	}

	// BTB xpos -> tags in the rules
	// Vr pres, Vd imperf, Ve aor, Vu cond (бих), VI imper
	// Vx / Vw aor / imperf active part, Vq passive part, Vy pres part, Vz adv part
	public void setTagBg(Word w) {

		String x = w.xpos;

		if (x.equals("punct") || w.upos.equals("PUNCT")) {
			w.tag = "U";
			return;
		}

		if (!x.startsWith("V") || x.length() < 5) {
			w.tag = x;
			return;
		}

		char form = x.charAt(4);
		String person = at(x, 7);
		String number = at(x, 8);

		if (form == 'f') {
			char tense = x.charAt(6);
			String t = "r";
			if (tense == 'm') t = "d";
			else if (tense == 'o') t = "e";
			// бях, беше - both imperfect and aorist
			else if (tense == 't') t = "de";
			w.tag = "V" + t + person + number;
		} else if (form == 'u') {
			w.tag = "Vu" + person + number;
			w.ruleLemma = "бих";
		} else if (form == 'z') {
			w.tag = "VI" + person + number;
		} else if (form == 'c') {
			char voice = x.charAt(5);
			char tense = x.charAt(6);
			String t = "x";
			if (voice == 'v') t = "q";
			else if (tense == 'm') t = "w";
			else if (tense == 'r') t = "y";
			w.tag = "V" + t + number + "o";
		} else if (form == 'g') {
			w.tag = "Vz";
		} else {
			w.tag = x;
		}

		if (w.lemma.equals("бида")) w.ruleLemma = "бивам";
	}

	// PTB xpos -> tags in the rules
	public void setTagEn(Word w) {

		String x = w.xpos;
		String u = w.upos;

		if (x.equals("VB")) w.tag = "Vb";
		else if (x.equals("VBP")) w.tag = "Vr";
		else if (x.equals("VBZ")) w.tag = "Vr3s";
		else if (x.equals("VBD")) w.tag = "Ve";
		else if (x.equals("VBN")) w.tag = "Vx";
		else if (x.equals("VBG")) w.tag = "Vy";
		else if (x.equals("MD")) w.tag = "Vm";
		else if (u.equals("PUNCT")) w.tag = "U";
		else if (u.equals("CCONJ") || u.equals("SCONJ")) w.tag = "C" + x;
		else if (u.equals("PRON")) w.tag = "P" + x;
		else if (u.equals("DET")) w.tag = "P" + x;
		else if (u.equals("PART")) w.tag = "T" + x;
		else if (u.equals("ADV")) w.tag = "D" + x;
		else if (u.equals("ADJ")) w.tag = "A" + x;
		else if (u.equals("ADP")) w.tag = "R" + x;
		else if (u.equals("NUM")) w.tag = "M" + x;
		else if (u.equals("INTJ")) w.tag = "I" + x;
		else if (u.equals("NOUN") || u.equals("PROPN")) w.tag = "N" + x;
		else w.tag = "X" + x;

		w.ruleLemma = w.lemma.toLowerCase();
	}

	private String at(String s, int i) {
		if (i >= s.length()) return "";
		char c = s.charAt(i);
		if (c == '-') return "";
		return String.valueOf(c);
	}
}
