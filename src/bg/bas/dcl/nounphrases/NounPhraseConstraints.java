package bg.bas.dcl.nounphrases;

import java.util.ArrayList;

public class NounPhraseConstraints {

	public ArrayList<String> active = new ArrayList<String>();

	static final String CAPABLE = " N A PS MO MC Q PG ";
	static final String DETERMINERS = " PD PI PR PF PN PG ";

	public boolean check(Sentence sent, NounPhrase np, NounPhraseModel m){

		ArrayList<Integer> comp = np.getComponents();
		int head = comp.indexOf(np.headPosition);

		for (String c : active) {
			boolean ok = true;

			if (c.equals("HEAD_LAST")) ok = headLast(sent, comp, head);
			else if (c.equals("DETERMINERS_FIRST")) ok = determinersFirst(sent, comp);
			else if (c.equals("ARTICLE_FIRST")) ok = articleFirst(sent, comp);
			else if (c.equals("DEMONSTRATIVE_NO_ARTICLE")) ok = demonstrativeNoArticle(sent, comp);
			else if (c.equals("CLITIC_AFTER_ARTICLE")) ok = cliticAfterArticle(sent, comp);
			else if (c.equals("CLITIC_NO_ARTICLE")) ok = cliticNoArticle(sent, comp, head);
			else if (c.equals("AGREEMENT")) ok = agreement(sent, comp, head, m.agreement);

//			if (!ok) System.out.println("CONSTRAINT "+c+" "+m.id);

			if (!ok) return false;
		}

		return true;
	}

	boolean is(Word t, String list){
		return list.contains(" " + t.getPos() + " ");
	}

	boolean headLast(Sentence sent, ArrayList<Integer> comp, int head){
		for (int k=head+1; k<comp.size(); k++) {
			String p = sent.get(comp.get(k)).getPos();
			if (!p.equals("PC") && !p.equals("N")) return false;
		}
		return true;
	}

	boolean determinersFirst(Sentence sent, ArrayList<Integer> comp){
		boolean other = false;
		for (int i : comp) {
			Word t = sent.get(i);
			if (t.getPos().equals("")) continue;
			if (is(t, DETERMINERS)) {
				if (other) return false;
			} else other = true;
		}
		return true;
	}

	boolean articleFirst(Sentence sent, ArrayList<Integer> comp){
		boolean first = true;
		for (int i : comp) {
			Word t = sent.get(i);
			if (!is(t, CAPABLE)) continue;
			// много, малко before an adjective do not take the article
			if (t.getPos().equals("Q") && !t.isArticled()) continue;
			if (!first && t.isArticled()) return false;
			first = false;
		}
		return true;
	}

	boolean demonstrativeNoArticle(Sentence sent, ArrayList<Integer> comp){
		for (int k=0; k<comp.size()-1; k++) {
			Word next = sent.get(comp.get(k+1));
			if (sent.get(comp.get(k)).getPos().equals("PD")
					&& next.isArticled() && !next.getPos().equals("PG")) return false;
		}
		return true;
	}

	int firstCapable(Sentence sent, ArrayList<Integer> comp){
		for (int k=0; k<comp.size(); k++) {
			if (is(sent.get(comp.get(k)), CAPABLE)) return k;
		}
		return -1;
	}

	boolean cliticAfterArticle(Sentence sent, ArrayList<Integer> comp){
		for (int k=1; k<comp.size(); k++) {
			if (!sent.get(comp.get(k)).getPos().equals("PC")) continue;
			if (sent.get(comp.get(k-1)).isArticled() && k-1 != firstCapable(sent, comp)) return false;
		}
		return true;
	}

	// майка ми, тази им идея
	boolean cliticNoArticle(Sentence sent, ArrayList<Integer> comp, int head){
		for (int k=1; k<comp.size(); k++) {
			if (!sent.get(comp.get(k)).getPos().equals("PC")) continue;

			Word prev = sent.get(comp.get(k-1));
			if (prev.isArticled() || prev.getPos().equals("PD")) continue;

			if (!(k-1 == head && k-1 == 0 && prev.getPos().equals("N")
					&& "Sing".equals(prev.getFeat("Number")))) return false;
		}
		return true;
	}

	boolean agreement(Sentence sent, ArrayList<Integer> comp, int head, String features){

		if (head < 0 || features.equals("-") || features.equals("")) return true;

		Word h = sent.get(comp.get(head));
		String number = plural(h.getFeat("Number"));
		String gender = h.getFeat("Gender");

		for (int k=0; k<comp.size(); k++) {

			if (k == head) continue;

			Word t = sent.get(comp.get(k));
			String p = t.getPos();
			if (p.equals("") || p.equals("N") || p.equals("PC") || p.equals("Q")) continue;

			String n = plural(t.getFeat("Number"));

			if (features.contains("число") && number != null && n != null && !n.equals(number)) return false;

			// gender only in the singular
			if (features.contains("род") && "Sing".equals(number) && !"Plur".equals(n)
					&& gender != null && t.getFeat("Gender") != null
					&& !t.getFeat("Gender").equals(gender)) return false;
		}

		return true;
	}

	String plural(String n){
		if (n == null) return null;
		if (n.equals("Count") || n.equals("Ptan")) return "Plur";
		return n;
	}
}
