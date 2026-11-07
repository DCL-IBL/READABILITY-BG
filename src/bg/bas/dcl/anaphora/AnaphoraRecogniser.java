package bg.bas.dcl.anaphora;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class AnaphoraRecogniser {

	ArrayList<AnaphoraRule> rules = new ArrayList<AnaphoraRule>();
	HashMap<String,ArrayList<PronounForm>> pronouns = new HashMap<String,ArrayList<PronounForm>>();

	// the kind of a pronoun from its form and BTB xpos
	public PronounForm getPronoun(Word w){

		String f = w.lower();
		if (!pronouns.containsKey(f)) return null;

		String x = w.xpos;
		String kind = "";

		if (f.equals("ѝ")) kind = w.deprel.equals("det") || w.deprel.equals("nmod") ? "притежателно_кратко" : "лично_дат";
		else if (f.equals("себе")) kind = "възвратно";
		else if (x.startsWith("Pp") && x.length() > 4 && x.charAt(2) != 'x') {
			if (x.charAt(3) == '-') kind = "лично_им";
			else if (x.charAt(3) == 'l') kind = "лично_пълно";
			else if (x.charAt(3) == 't') kind = x.charAt(4) == 'd' ? "лично_дат" : "лично_вин";
		}
		else if (x.startsWith("Ps") && x.length() > 3) {
			if (x.charAt(2) == 'x') kind = "възвратно_притежателно";
			else kind = x.charAt(3) == 't' ? "притежателно_кратко" : "притежателно";
		}
		else if (x.startsWith("Pr")) kind = f.startsWith("чи") ? "относително_притежателно" : "относително";
		// demonstratives only when used on their own
		else if (!w.deprel.equals("det") && !w.deprel.equals("amod")) kind = "показателно";

		for (PronounForm pf : pronouns.get(f)) {
			if (pf.kind.equals(kind)) return pf;
		}

		return null;
	}

	public void resolve(Document doc, Mention m){

		if (m.kind.equals("ZERO") && !m.type.equals("Pro-drop")) return;

		for (AnaphoraRule r : rules){

			if (!r.appliesTo(m)) continue;

			Mention c = null;

			if (r.strategy.equals("RELATIVE_HEAD")) c = relativeHead(doc, m);
			else if (r.strategy.equals("CLAUSE_SUBJECT")) c = doc.getSubject(m.sent, m.clause);
			else if (r.strategy.equals("SAME_PERSON")) c = samePerson(doc, m, r);
			else if (r.strategy.equals("PREV_SUBJECT")) c = prevSubject(doc, m, r, true);
			else if (r.strategy.equals("NEAREST")) c = nearest(doc, m, r);
			else if (r.strategy.equals("EVENT")) c = event(doc, m);

			if (c == null || c == m) continue;

//			System.out.println("RESOLVED "+r.id+"\t"+m.sent.get(m.pos).form+" -> "+c.sent.get(c.pos).form);

			m.link(c);
			m.rules.add(r.id);

			// features not known from the form (свой, си, a past tense verb) come from the referent
			if (m.number.equals("")) m.number = c.number;
			if (m.gender.equals("") && !m.number.equals("мн")) m.gender = c.gender;
			if (m.person.equals("")) m.person = c.person;

			return;
		}
	}

	Mention relativeHead(Document doc, Mention m){

		if (m.clause < 0) return null;

		Word p = m.sent.get(m.clause);
		if (!p.deprel.startsWith("acl") || p.headpos < 0) return null;

		Mention c = doc.getMention(m.sent, p.headpos, "NP");
		if (c == null) c = doc.getMention(m.sent, p.headpos, "PRONOUN");
		if (c == null) {
			c = newNP(m.sent, p.headpos);
			doc.mentions.add(c);
		}

		if (!m.pronounKind.equals("относително_притежателно") && !agree(m, c)) return null;

		return c;
	}

	Mention samePerson(Document doc, Mention m, AnaphoraRule r){

		for (Mention c : preceding(doc, m)) {
			if (c.kind.equals("NP") || !inScope(c, m, r.scope)) continue;
			if (c.person.equals(m.person) && c.number.equals(m.number)) return c;
		}

		return null;
	}

	// subject of the governing clause, then of the preceding clauses
	Mention prevSubject(Document doc, Mention m, AnaphoraRule r, boolean checkAgreement){

		ArrayList<Mention> subjects = new ArrayList<Mention>();

		int g = governor(m.sent, m.clause);

		int steps = 0;
		while (g >= 0 && steps++ < m.sent.size()) {
			subjects.add(doc.getSubject(m.sent, g));
			g = governor(m.sent, g);
		}

		for (int k = m.sent.predicates.size()-1; k >= 0; k--) {
			Predicate p = m.sent.predicates.get(k);
			if (p.pos < m.pos && p.pos != m.clause) subjects.add(doc.getSubject(m.sent, p.pos));
		}

		for (int s = m.sent.index-1; s >= 0; s--) {
			Sentence prev = doc.sentences.get(s);
			if (!inScope(prev, m, r == null ? "paragraph" : r.scope)) break;
			for (int k = prev.predicates.size()-1; k >= 0; k--) subjects.add(doc.getSubject(prev, prev.predicates.get(k).pos));
		}

		for (Mention c : subjects) {
			if (c == null || c == m || c.root() == m.root()) continue;
			if (!checkAgreement) return c;
			if (c.kind.equals("ZERO") && !c.type.equals("Pro-drop")) continue;
			if (agree(m, c)) return c;
		}

		return null;
	}

	Mention nearest(Document doc, Mention m, AnaphoraRule r){

		Mention subj = doc.getSubject(m.sent, m.clause);
		Mention prevSubj = r.has("NOT_PREV_SUBJECT") ? prevSubject(doc, m, null, false) : null;

		for (int pass = r.has("SUBJECT_FIRST") ? 0 : 1; pass < 2; pass++) {

			for (Mention c : preceding(doc, m)) {

				if (pass == 0 && !c.isSubject) continue;
				if (c.kind.equals("ZERO") && !c.type.equals("Pro-drop")) continue;
				if (!inScope(c, m, r.scope) || !agree(m, c)) continue;

				if (r.has("OUTSIDE_CLAUSE") && c.sent == m.sent && c.clause == m.clause) continue;
				if (r.has("NOT_SUBJECT") && subj != null && subj != m && c.root() == subj.root()) continue;
				if (prevSubj != null && c.root() == prevSubj.root()) continue;

				// possessives: not the noun they modify
				if (m.pronounKind.startsWith("притежателно") && c.sent == m.sent && c.pos == m.sent.get(m.pos).headpos) continue;

				return c;
			}
		}

		return null;
	}

	// това, онова: the predicate of the preceding clause
	Mention event(Document doc, Mention m){

		if (!m.gender.equals("ср") || !m.number.equals("ед")) return null;

		int pos = -1;
		Sentence s = m.sent;

		for (Predicate p : m.sent.predicates) {
			if (p.pos < m.pos && p.pos != m.clause) pos = p.pos;
		}

		if (pos < 0 && m.sent.index > 0) {
			s = doc.sentences.get(m.sent.index-1);
			if (s.paragraph != m.sent.paragraph) return null;
			for (Predicate p : s.predicates) {
				if (p.governor < 0) pos = p.pos;
			}
		}

		if (pos < 0) return null;

		Mention c = doc.getMention(s, pos, "EVENT");
		if (c == null) {
			c = new Mention("EVENT", s, pos);
			doc.mentions.add(c);
		}

		return c;
	}

	int governor(Sentence sent, int clause){
		if (clause < 0) return -1;
		Predicate p = sent.getPredicate(clause);
		return p == null ? -1 : p.governor;
	}

	ArrayList<Mention> preceding(Document doc, Mention m){

		ArrayList<Mention> res = new ArrayList<Mention>();

		for (Mention c : doc.mentions) {
			if (c != m && !c.kind.equals("EVENT") && c.before(m)) res.add(c);
		}

		// nearest first
		Collections.sort(res, new Comparator<Mention>() {
			public int compare(Mention a, Mention b) {
				return a.before(b) ? 1 : b.before(a) ? -1 : 0;
			}
		});

		return res;
	}

	boolean inScope(Mention c, Mention m, String scope){
		return inScope(c.sent, m, scope) && (!scope.equals("clause") || c.clause == m.clause);
	}

	boolean inScope(Sentence s, Mention m, String scope){
		if (scope.equals("clause") || scope.equals("sentence")) return s == m.sent;
		if (scope.equals("paragraph")) return s.paragraph == m.sent.paragraph;
		return s.paragraph >= m.sent.paragraph-1;
	}

	boolean agree(Mention m, Mention c){

		if (!m.person.equals("") && !c.person.equals("") && !m.person.equals(c.person)) return false;
		if (!m.number.equals("") && !c.number.equals("") && !m.number.equals(c.number)) return false;

		if (!m.number.equals("мн") && !c.number.equals("мн") && !m.gender.equals("") && !c.gender.equals("")) {
			for (String g : c.gender.split(",")) {
				if (("," + m.gender + ",").contains("," + g + ",")) return true;
			}
			return false;
		}

		return true;
	}

	public Mention newNP(Sentence sent, int pos){

		Word w = sent.get(pos);
		Mention m = new Mention("NP", sent, pos);

		m.person = "3";
		m.number = PredicateFinder.number(w.getFeat("Number"));
		m.gender = PredicateFinder.gender(w.getFeat("Gender"));
		m.isSubject = w.deprel.startsWith("nsubj");

		return m;
	}

	public Mention newPronoun(Sentence sent, int pos, PronounForm pf){

		Word w = sent.get(pos);
		Mention m = new Mention("PRONOUN", sent, pos);

		m.pronounKind = pf.kind;
		m.type = pf.getType();
		m.person = pf.person.equals("-") ? "" : pf.person;
		m.number = pf.number.equals("-") ? "" : pf.number;
		m.gender = pf.gender.equals("-") ? "" : pf.gender;
		m.isSubject = w.deprel.startsWith("nsubj");

		return m;
	}
}
