package bg.bas.dcl.anaphora;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class MissingSubjectRecogniser {

	ArrayList<SubjectRule> rules = new ArrayList<SubjectRule>();

	// kind -> lemmas and word forms
	HashMap<String,HashSet<String>> impersonal = new HashMap<String,HashSet<String>>();

	public Mention findMissingSubject(Document doc, Sentence sent, Predicate p){

		if (p.subjects.size() > 0) return null;

		for (SubjectRule r : rules){

			if (p.imperative && !r.hasLemmaCondition()) continue;

			if (!p.imperative) {
				if (!r.person.equals("-") && !("," + r.person + ",").contains("," + p.person + ",")) continue;
				if (!r.number.equals("-") && !r.number.equals(p.number)) continue;
			}

			boolean ok = true;
			for (String c : r.conditions) {
				if (!checkCondition(doc, sent, p, c)) {
					ok = false;
					break;
				}
			}
			if (!ok) continue;

//			System.out.println("SUBJECT "+r.id+"\t"+sent.get(p.pos).form);

			Mention m = new Mention("ZERO", sent, p.pos);
			m.isSubject = true;
			m.type = r.type;
			m.person = p.person;
			m.number = p.number;
			m.gender = p.gender;

			// impersonal: 3rd person singular
			if (r.type.equals("Impersonal")) {
				m.person = "3";
				m.number = "ед";
			}
			m.rules.add(r.id);

			return m;
		}

		return null;
	}

	boolean checkCondition(Document doc, Sentence sent, Predicate p, String c){

		Word w = sent.get(p.pos);

		if (c.startsWith("LEMMA=")) {
			HashSet<String> l = impersonal.get(c.substring(6));
			return l != null && (l.contains(w.baseLemma()) || l.contains(w.lower()));
		}
		if (c.equals("DA_CLAUSE")) return p.hasDaClause;
		if (c.equals("COPULA")) return p.copula;
		if (c.equals("SE")) return p.se;
		if (c.equals("PRESENT")) return p.tense.equals("Pres");
		if (c.equals("NO_ADDRESSEE")) return !doc.hasAddressee(sent.paragraph);
		if (c.equals("NO_PLURAL_ANTECEDENT")) return !doc.hasPluralBefore(sent, p.pos);

		if (c.equals("DA_CONTROLLED")) {
			if (!p.daClause || p.governor < 0) return false;
			Predicate g = sent.getPredicate(p.governor);
			return g != null && g.person.equals(p.person) && g.number.equals(p.number);
		}

		return false;
	}
}
