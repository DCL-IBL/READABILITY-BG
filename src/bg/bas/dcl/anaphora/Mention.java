package bg.bas.dcl.anaphora;

import java.util.ArrayList;

public class Mention {

	// NP (head of a noun phrase), PRONOUN, ZERO (missing subject, on the predicate), EVENT (predicate)
	public String kind = "";

	public Sentence sent;
	public int pos = -1;
	public int clause = -1;
	public boolean isSubject = false;

	public String person = "";
	public String number = "";
	public String gender = "";

	// type for the output: Pro-drop, Impersonal ... or Personal, Possessive ...
	public String type = "";
	public String pronounKind = "";
	public String label = "";
	public ArrayList<String> rules = new ArrayList<String>();

	public Mention antecedent = null;
	Mention parent = this;

	public Mention(String kind, Sentence sent, int pos){
		this.kind = kind;
		this.sent = sent;
		this.pos = pos;
		if (pos >= 0) clause = sent.clauseOf[pos];
	}

	public Mention root(){
		Mention m = this;
		while (m.parent != m) m = m.parent;
		return m;
	}

	public void link(Mention ante){
		antecedent = ante;
		Mention a = root(), b = ante.root();
		if (a != b) a.parent = b;
	}

	// sentence and word order in the document
	public boolean before(Mention m){
		if (sent.index != m.sent.index) return sent.index < m.sent.index;
		return pos < m.pos;
	}

	public String getFeats(){

		ArrayList<String> res = new ArrayList<String>();

		if (!gender.equals("")) {
			String g = "";
			for (String x : gender.split(",")) {
				if (!g.equals("")) g += ",";
				g += x.equals("м") ? "Masc" : x.equals("ж") ? "Fem" : "Neut";
			}
			res.add("Gender=" + g);
		}
		if (!number.equals("")) res.add("Number=" + (number.equals("ед") ? "Sing" : "Plur"));
		if (!person.equals("")) res.add("Person=" + person);

		if (res.size() == 0) return "_";
		return String.join("|", res);
	}
}
