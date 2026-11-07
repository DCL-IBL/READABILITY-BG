package bg.bas.dcl.clausesplitter;

import java.util.ArrayList;

public class ClauseVerb extends ArrayList<Integer> {

	private static final long serialVersionUID = 1L;

	public ArrayList<String> tense = new ArrayList<String>();
	public int mainword = -1;

	public int first() {
		return get(0);
	}

	public int last() {
		return get(size()-1);
	}
}
