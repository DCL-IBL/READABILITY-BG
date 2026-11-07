package bg.bas.dcl.anaphora;

import java.util.ArrayList;

public class Sentence extends ArrayList<Word> {

	private static final long serialVersionUID = 1L;

	// all lines incl. comments and multiword tokens
	public ArrayList<String> lines = new ArrayList<String>();

	public String sentId = "";
	public int index = 0;
	public int paragraph = 0;

	// predicates of the simple clauses, see PredicateFinder
	public ArrayList<Predicate> predicates = new ArrayList<Predicate>();
	public int[] clauseOf;

	public void addLine(String line) {

		lines.add(line);

		String id = line.substring(0, line.indexOf("\t"));
		if (id.matches("\\d+")) add(new Word(line, size()));
	}

	public Word getById(int id) {
		for (Word w : this) {
			if (w.id == id) return w;
		}
		return null;
	}

	public Predicate getPredicate(int pos) {
		for (Predicate p : predicates) {
			if (p.pos == pos) return p;
		}
		return null;
	}
}
