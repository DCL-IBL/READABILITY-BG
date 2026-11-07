package bg.bas.dcl.anaphora;

import java.util.ArrayList;

public class Predicate {

	public int pos = -1;
	public int finite = -1;

	public String person = "";
	public String number = "";
	public String gender = "";
	public String tense = "";

	public boolean imperative = false;
	public boolean copula = false;
	public boolean se = false;
	public boolean daClause = false;
	public boolean hasDaClause = false;

	public ArrayList<Integer> subjects = new ArrayList<Integer>();

	// governing predicate, -1 for the main clause
	public int governor = -1;
}
