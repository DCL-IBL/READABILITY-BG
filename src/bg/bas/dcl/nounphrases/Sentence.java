package bg.bas.dcl.nounphrases;

import java.util.ArrayList;

public class Sentence extends ArrayList<Word> {

	private static final long serialVersionUID = 1L;

	// all lines incl. comments and multiword tokens
	public ArrayList<String> lines = new ArrayList<String>();

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
}
