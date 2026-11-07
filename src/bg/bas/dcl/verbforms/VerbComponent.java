package bg.bas.dcl.verbforms;

import java.util.ArrayList;

public class VerbComponent {

	public String lemma = "";
	public String pos = "";
	public String gram = "";

	public boolean optional = false;
	public int optionalNum = 0;
	public ArrayList<VerbComponent> optionalVariants = new ArrayList<VerbComponent>();

	public void readComponentFromString(String comp) {

		comp = comp.trim();
		if (comp.contains(":")) {
			lemma = comp.substring(0, comp.indexOf(":"));
			comp = comp.substring(comp.indexOf(":") + 1);
		} else {
			if (comp.length() > 0 && comp.charAt(0) != '-'
					&& (comp.charAt(0) < 'A' || comp.charAt(0) > 'Z')) {
				lemma = comp;
				comp = "";
			}
		}

		while (comp.length() > 0
				&& (comp.charAt(0) == '-'
				|| (comp.charAt(0) >= 'A' && comp.charAt(0) <= 'Z'))) {
			pos += comp.charAt(0);
			comp = comp.substring(1);
		}

		gram = comp;
	}

	public void readComponentTypeFromString(String comp, int n) {

		if (comp.startsWith("(") && comp.endsWith(")")) {

			comp = comp.substring(1, comp.length() - 1);

			String options1 = comp.substring(0, comp.indexOf(","));
			String options2 = comp.substring(comp.indexOf(",") + 1);

			optional = true;

			if (options1.equals("n")) optionalNum = n;
			else optionalNum = Integer.valueOf(options1);

			if (options2.startsWith("{") && options2.endsWith("}")) {
				String[] variants = options2.substring(1, options2.length() - 1).split(",");

				for (String v : variants) {
					VerbComponent vc = new VerbComponent();
					vc.readComponentFromString(v.trim());
					optionalVariants.add(vc);
				}
			}

		} else readComponentFromString(comp);
	}

	// tag only, no lemma - the main verb
	public boolean isMain() {
		return !optional && lemma.equals("") && pos.startsWith("V");
	}

	public boolean isMatchPosAndGram(Word t) {

		if (!t.tag.startsWith(pos)) return false;

		for (int i=0; i<gram.length(); i++) {
			if (t.tag.indexOf(gram.charAt(i)) < 0) return false;
		}

		return true;
	}

	public boolean isMatch(Word t) {

		if (!optional) {

			if (!lemma.equals("")) {
				if (pos.equals("")) {
					return lemma.equals(t.ruleLemma) || lemma.equals(t.lower());
				}
				if (!lemma.equals(t.ruleLemma)) return false;
			}

			return isMatchPosAndGram(t);
		}

		if (optionalVariants.get(0).pos.startsWith("-")) {
			for (VerbComponent vc : optionalVariants) {
				if (t.tag.startsWith(vc.pos.substring(1))) return false;
			}
			return true;
		}

		for (VerbComponent vc : optionalVariants) {
			if (vc.isMatch(t)) return true;
		}

		return false;
	}

	public String getTrigger() {
		if (!lemma.equals("")) return lemma;
		return pos.substring(0, 1);
	}
}
