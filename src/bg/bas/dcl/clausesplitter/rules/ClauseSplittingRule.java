package bg.bas.dcl.clausesplitter.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import bg.bas.dcl.clausesplitter.Clause;
import bg.bas.dcl.clausesplitter.ClauseStack;
import bg.bas.dcl.clausesplitter.ClauseVerb;
import bg.bas.dcl.clausesplitter.Conjunction;
import bg.bas.dcl.verbforms.Sentence;
import bg.bas.dcl.verbforms.Word;

public abstract class ClauseSplittingRule {

	public static HashSet<String> strongPunctuation = new HashSet<String>();
	public static HashSet<String> weakPunctuation = new HashSet<String>();

	static {
		for (String p : new String[]{".", "!", "?", ";", ":", "…", "..."}) strongPunctuation.add(p);
		for (String p : new String[]{",", "-", "–", "—", "(", ")"}) weakPunctuation.add(p);
	}

	public String order = "";
	public String name = "";
	public String scope = "gap";
	public String type = "";
	public HashMap<String,String> options = new HashMap<String,String>();
	public String direction = "forward";
	public String condition = "-";
	public String action = "";
	public boolean isEndingRule = true;

	public ArrayList<Conjunction> conjList = new ArrayList<Conjunction>();

	// v is the verb before the interval, -1 before the first verb
	public abstract boolean findClauseBorders(
			Sentence sentence,
			ArrayList<ClauseVerb> verbs,
			int v, int spos, int epos,
			ClauseStack stack);

	public boolean isEnding(){
		return isEndingRule;
	}

	public void setOptions(String s){

		if (s.equals("-") || s.trim().equals("")) return;

		for (String o : s.split(";")) {
			int i = o.indexOf("=");
			if (i > 0) options.put(o.substring(0,i).trim(), o.substring(i+1).trim());
		}
	}

	public boolean checkCondition(ClauseStack stack){

		if (condition.equals("-") || condition.equals("")) return true;

		Clause top = stack.top();
		if (top == null) return false;

		for (String c : condition.split(",")) {

			c = c.trim();
			boolean neg = c.startsWith("!");
			if (neg) c = c.substring(1);

			boolean res = false;

			if (c.equals("top.hasVerb")) res = top.hasVerb;
			else if (c.startsWith("top.open=")) res = (" "+c.substring(9).replace("|", " ")+" ").contains(" "+top.open+" ");
			else if (c.startsWith("top.kind=")) res = (" "+c.substring(9).replace("|", " ")+" ").contains(" "+top.kind+" ");

			if (res == neg) return false;
		}

		return true;
	}

	// search only after the start of the current clause
	public int getStart(int spos, ClauseStack stack){
		if (stack.size() > 0 && spos < stack.getIndexFromStack(1)) spos = stack.getIndexFromStack(1);
		return spos;
	}

	public boolean isMatch(Word t, String part){
		return part.equals(t.lower()) || part.equals(t.lemma.toLowerCase());
	}

	public Conjunction findConjunction(Sentence sent, int i, int epos, String compulsory, String types){

		for (Conjunction c : conjList) {

			if (compulsory != null && c.isCompulsaryCO != compulsory.equals("true")) continue;
			if (types != null && !(","+types+",").contains(","+c.type+",")) continue;
			if (i+c.size()-1 > epos) continue;

			boolean match = true;
			for (int k=0; k<c.size(); k++) {
				if (!isMatch(sent.get(i+k), c.parts[k])) {
					match = false;
					break;
				}
			}

			if (!match) continue;

			// a preposition, not a conjunction
			if (c.size() == 1 && sent.get(i).upos.equals("ADP")) continue;

			// the clause opened by the conjunction must reach the next verb form
			boolean punct = false;
			for (int k=i+c.size(); k<=epos; k++) {
				if (strongPunctuation.contains(sent.get(k).form) || weakPunctuation.contains(sent.get(k).form)) punct = true;
			}
			if (punct) continue;

			return c;
		}

		return null;
	}

	public void openByConj(Sentence sent, Conjunction c, int i, int spos, ClauseStack stack){

		int pos = i;

		if (c.preceedingCommaCO) {
			int min = Math.max(getStart(spos, stack), i-5);
			for (int p = i-1; p >= min; p--) {
				if (sent.get(p).form.equals(",")) {
					pos = p+1;
					break;
				}
			}
		}

		String conj = c.id + ":" + sent.get(i).lower();
		for (int k=1; k<c.size(); k++) conj += "_" + sent.get(i+k).lower();
		Clause top = stack.top();

		if (!c.isSubordinate()) {

			if (top == null || !top.hasVerb) {
				if (top != null && top.conj.equals("")) top.conj = conj;
				return;
			}

			// coordinated clause on the same level as the closed one
			stack.removeFromStack(pos-1);
			Clause cl = stack.putInStack(top.open, top.kind.equals("SUB") ? "SUB" : "COORD", pos);
			cl.head = top.head;
			cl.coord = top.id;
			cl.conj = conj;

			return;
		}

		Clause cl = stack.putInStack("COS", "SUB", pos);
		cl.conj = conj;
	}

	public void closeTop(Sentence sent, int i, ClauseStack stack){

		stack.removeFromStack(i);

		while (stack.size() > 0 && stack.top().hasVerb) stack.removeFromStack(i);

		if (stack.size() == 0 && i+1 < sent.size()) stack.putInStack("COC", "COORD", i+1);
	}

	public int findPunctuation(Sentence sent, int spos, int epos, HashSet<String> punct){

		if (direction.equals("backward")) {
			for (int i=epos; i>=spos; i--) {
				if (punct.contains(sent.get(i).form)) return i;
			}
			return -1;
		}

		for (int i=spos; i<=epos; i++) {
			if (punct.contains(sent.get(i).form)) return i;
		}
		return -1;
	}

	public boolean isExcepted(String form){
		if (!options.containsKey("except")) return false;
		return (" "+options.get("except")+" ").contains(" "+form+" ");
	}
}
