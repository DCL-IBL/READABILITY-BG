package bg.bas.dcl.nounphrases;

import java.util.ArrayList;

public class NounPhraseComponent {

	public String form = "";
	public String lemma = "";
	public String pos = "";
	public String gram = "";

	public boolean isHead = false;

	public boolean optional = false;
	public boolean isNegative = false;
	public int optionalNum = 0;
	public ArrayList<NounPhraseComponent> optionalVariants =
			new ArrayList<NounPhraseComponent>();

	public void readComponentFromString(String comp, int n){

		if (comp.endsWith("!")) {
			isHead = true;
			comp = comp.substring(0, comp.length()-1);
		}

		if (comp.startsWith("(") && comp.endsWith(")")) {

			comp = comp.substring(1, comp.length()-1);

			String options1 = comp.substring(0,comp.indexOf(","));
			String options2 = comp.substring(comp.indexOf(",")+1);

			optional = true;

			if (options1.equals("n")) optionalNum = n;
			else optionalNum = Integer.valueOf(options1);

			if (options2.startsWith("{") && options2.endsWith("}")){
				String[] variants = options2.substring(1,options2.length()-1)
						.split(",");

				for (String v : variants){
					NounPhraseComponent npc = new NounPhraseComponent();
					npc.readSingleComponentFromString(v.trim());
					optionalVariants.add(npc);
				}
			}

		} else readSingleComponentFromString(comp);
	}

	public void readSingleComponentFromString(String comp){

		comp = comp.trim();

		if (comp.startsWith("-")) {
			isNegative = true;
			comp = comp.substring(1);
		}

		if (comp.contains(":")) {
			lemma = comp.substring(0,comp.indexOf(":"));
			comp = comp.substring(comp.indexOf(":")+1);
		} else {
			if (comp.length()>0 && (comp.charAt(0)<'A' || comp.charAt(0)>'Z')) {
				lemma = comp;
				comp = "";
			}
		}

		while (comp.length()>0 && comp.charAt(0)>='A' && comp.charAt(0)<='Z'){
			pos += comp.charAt(0);
			comp = comp.substring(1);
		}

		gram = comp;
	}

	public boolean isMatch(Word t){

		if (!optional) {

			if (!lemma.equals("") && !lemma.equals(t.lemma.toLowerCase()) && !lemma.equals(t.lower())) return false;
			if (pos.equals("")) return true;

			if (!t.getPos().equals(pos)) return false;

			for (int i=0; i<gram.length(); i++){
				if (t.tag.indexOf(gram.charAt(i)) < 0) return false;
			}

			return true;
		}

		// ONLY OPTIONAL CAN BE NEGATIVE
		if (optionalVariants.get(0).isNegative){
			for (NounPhraseComponent nc : optionalVariants) {
				if (nc.isMatch(t)) return false;
			}
			return true;
		}

		for (NounPhraseComponent vc : optionalVariants) {
			if (vc.isMatch(t)) return true;
		}

		return false;
	}
}
