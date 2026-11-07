package bg.bas.dcl.clausesplitter;

public class Clause {

	public int id = 0;
	public int start = -1;

	// COC, COS or SCOC as opened by the rules
	public String open = "";
	// MAIN, COORD or SUB
	public String kind = "";

	public int head = 0;
	public int coord = 0;
	public String conj = "";

	public boolean hasVerb = false;

	public Clause(int id, String open, String kind, int start){
		this.id = id;
		this.open = open;
		this.kind = kind;
		this.start = start;
	}
}
