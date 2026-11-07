package bg.bas.dcl.clausesplitter;

public class ClauseBorder {

	public int position = -1;
	// CO opens a clause at position, CC closes it after position
	public String type = "";
	public int clause = 0;

	public ClauseBorder(int p, String t, int c){
		position = p;
		type = t;
		clause = c;
	}
}
