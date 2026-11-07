package bg.bas.dcl.nounphrases;

import java.util.ArrayList;
import java.util.Collections;

public class NounPhrase extends ArrayList<Integer> {

	private static final long serialVersionUID = 1L;

	public String category = "";
	public ArrayList<String> rules = new ArrayList<String>();

	public int headPosition = -1;
	public int modelLength = 0;

	public boolean isPrepositional = false;
	public String prep = "";

	public void addPosition(int i) {
		if (!contains(i)) add(i);
		Collections.sort(this);
	}

	// positions without the preposition
	public ArrayList<Integer> getComponents() {
		ArrayList<Integer> res = new ArrayList<Integer>(this);
		if (isPrepositional) res.remove(0);
		return res;
	}
}
