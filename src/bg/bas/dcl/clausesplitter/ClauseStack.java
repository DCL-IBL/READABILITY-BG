package bg.bas.dcl.clausesplitter;

import java.util.ArrayList;

public class ClauseStack extends ArrayList<Clause> {

	private static final long serialVersionUID = 1L;

	public ArrayList<ClauseBorder> clborders = new ArrayList<ClauseBorder>();
	public ArrayList<Clause> clauses = new ArrayList<Clause>();

	// the clause closed last, for coordination
	public int lastClosed = 0;

	public Clause top(){
		if (size() == 0) return null;
		return get(size()-1);
	}

	public int getIndexFromStack(int i){
		return get(size()-i).start;
	}

	public String getTypeFromStack(int i){
		return get(size()-i).open;
	}

	public int stackSize(){
		return size();
	}

	public Clause putInStack(String type, String kind, int sentIndex){

		Clause c = new Clause(clauses.size()+1, type, kind, sentIndex);

		if (kind.equals("SUB") && size() > 0) c.head = top().id;
		if (kind.equals("COORD")) c.coord = lastClosed;

		clauses.add(c);
		clborders.add(new ClauseBorder(sentIndex, "CO", c.id));
		add(c);

		return c;
	}

	public void removeFromStack(int sentIndex){

		Clause c = top();
		clborders.add(new ClauseBorder(sentIndex, "CC", c.id));
		lastClosed = c.id;

		remove(size()-1);
	}
}
