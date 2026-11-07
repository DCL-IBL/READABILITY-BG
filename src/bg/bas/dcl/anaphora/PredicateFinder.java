package bg.bas.dcl.anaphora;

import java.util.HashMap;

public class PredicateFinder {

	static final String RENARRATIVE = " root conj ccomp advcl parataxis acl:relcl csubj ";

	public void findPredicates(Sentence sent){

		for (Word w : sent) {
			w.children.clear();
			w.headpos = -1;
		}
		for (Word w : sent) {
			Word h = sent.getById(w.head);
			if (h != null) {
				w.headpos = h.sentpos;
				h.children.add(w.sentpos);
			}
		}

		// ще, няма + да: the subject is on the auxiliary
		HashMap<Integer,Integer> subjectFrom = new HashMap<Integer,Integer>();

		for (Word w : sent) {

			if (isAux(w)) continue;

			if ((w.baseLemma().equals("нямам") || w.baseLemma().equals("ща")) && daChild(sent, w) >= 0) {
				subjectFrom.put(daChild(sent, w), w.sentpos);
				continue;
			}

			Predicate p = new Predicate();
			p.pos = w.sentpos;

			if ("Fin".equals(w.getFeat("VerbForm"))) p.finite = w.sentpos;
			else {
				for (int c : w.children) {
					Word a = sent.get(c);
					if (isAux(a) && "Fin".equals(a.getFeat("VerbForm"))) {
						if (p.finite < 0 || a.getFeat("Person") != null) p.finite = c;
					}
					if (a.deprel.equals("cop")) p.copula = true;
				}
			}

			Word f = p.finite >= 0 ? sent.get(p.finite) : null;

			// participle without auxiliary: renarrative, 3rd person
			if (f == null) {
				if (!w.upos.equals("VERB") || !"Part".equals(w.getFeat("VerbForm"))
						|| "Pass".equals(w.getFeat("Voice"))
						|| !RENARRATIVE.contains(" " + w.deprel + " ")) continue;
				p.person = "3";
			} else {
				p.person = val(f.getFeat("Person"));
				p.tense = val(f.getFeat("Tense"));
				p.imperative = "Imp".equals(f.getFeat("Mood"));
			}

			p.number = number(w.getFeat("Number") != null ? w.getFeat("Number") : f == null ? null : f.getFeat("Number"));
			p.gender = gender(w.getFeat("Gender"));
			if (p.number.equals("")) p.number = number(f == null ? null : f.getFeat("Number"));
			if (p.person.equals("") && w.getFeat("VerbForm") != null) p.person = "3";

			for (int c : w.children) {
				Word a = sent.get(c);
				if (a.deprel.startsWith("nsubj")) p.subjects.add(c);
				if (a.lower().equals("се") && a.deprel.equals("expl")) p.se = true;
				if (a.lower().equals("да") && (isAux(a) || a.deprel.equals("mark"))) p.daClause = true;
			}
			p.hasDaClause = daChild(sent, w) >= 0;

			sent.predicates.add(p);
		}

		for (Predicate p : sent.predicates) {
			if (subjectFrom.containsKey(p.pos)) {
				for (int c : sent.get(subjectFrom.get(p.pos)).children) {
					if (sent.get(c).deprel.startsWith("nsubj")) p.subjects.add(c);
				}
			}
		}

		sent.clauseOf = new int[sent.size()];
		for (Word w : sent) sent.clauseOf[w.sentpos] = findClause(sent, w.sentpos);

		for (Predicate p : sent.predicates) {
			int h = sent.get(p.pos).headpos;
			// skip ще/няма above the clause
			while (h >= 0 && sent.clauseOf[h] == p.pos) h = sent.get(h).headpos;
			p.governor = h < 0 ? -1 : sent.clauseOf[h];
		}
	}

	// nearest predicate up the tree
	int findClause(Sentence sent, int pos){

		int i = pos, steps = 0;

		while (i >= 0 && steps++ < sent.size()) {
			if (sent.getPredicate(i) != null) return i;
			if (subjectOwner(sent, i) >= 0) return subjectOwner(sent, i);
			i = sent.get(i).headpos;
		}

		return -1;
	}

	// the clause of ще/няма is the clause of its да-complement
	int subjectOwner(Sentence sent, int i){
		Word w = sent.get(i);
		if (!w.baseLemma().equals("нямам") && !w.baseLemma().equals("ща")) return -1;
		int c = daChild(sent, w);
		return c >= 0 && sent.getPredicate(c) != null ? c : -1;
	}

	int daChild(Sentence sent, Word w){
		for (int c : w.children) {
			Word d = sent.get(c);
			if (!d.deprel.equals("ccomp") && !d.deprel.equals("xcomp") && !d.deprel.startsWith("csubj")) continue;
			for (int cc : d.children) {
				if (sent.get(cc).lower().equals("да")) return c;
			}
		}
		return -1;
	}

	boolean isAux(Word w){
		return w.deprel.equals("aux") || w.deprel.equals("aux:pass") || w.deprel.equals("cop");
	}

	String val(String s){
		return s == null ? "" : s;
	}

	public static String number(String n){
		if (n == null) return "";
		if (n.equals("Sing")) return "ед";
		return "мн";
	}

	public static String gender(String g){
		if (g == null) return "";
		if (g.equals("Masc")) return "м";
		if (g.equals("Fem")) return "ж";
		if (g.equals("Neut")) return "ср";
		return "";
	}
}
