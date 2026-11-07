package bg.bas.dcl.anaphora;

import java.util.ArrayList;

public class Document {

	public ArrayList<Sentence> sentences = new ArrayList<Sentence>();
	public ArrayList<Mention> mentions = new ArrayList<Mention>();

	// 2nd person singular pronoun or imperative in the paragraph
	public boolean hasAddressee(int paragraph){

		for (Sentence s : sentences) {
			if (s.paragraph != paragraph) continue;
			for (Word w : s) {
				if (!"2".equals(w.getFeat("Person")) || !"Sing".equals(w.getFeat("Number"))) continue;
				if (w.upos.equals("PRON") || "Imp".equals(w.getFeat("Mood"))) return true;
			}
		}

		return false;
	}

	// a possible plural referent earlier in the sentence or in the previous sentence
	public boolean hasPluralBefore(Sentence sent, int pos){

		for (Mention m : mentions) {
			if (!m.number.equals("мн") || m.person.equals("1") || m.person.equals("2")) continue;
			if (m.sent == sent && m.pos < pos) return true;
			if (m.sent.index == sent.index-1) return true;
		}

		return false;
	}

	public Mention getMention(Sentence sent, int pos, String kind){
		for (Mention m : mentions) {
			if (m.sent == sent && m.pos == pos && m.kind.equals(kind)) return m;
		}
		return null;
	}

	// explicit or missing subject of a clause
	public Mention getSubject(Sentence sent, int clause){

		if (clause < 0) return null;

		for (Mention m : mentions) {
			if (m.sent != sent) continue;
			if (m.kind.equals("ZERO") && m.pos == clause) return m;
			if (m.isSubject && !m.kind.equals("ZERO") && m.clause == clause) return m;
		}

		return null;
	}
}
