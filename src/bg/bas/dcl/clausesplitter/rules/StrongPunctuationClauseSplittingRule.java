package bg.bas.dcl.clausesplitter.rules;

import java.util.ArrayList;

import bg.bas.dcl.clausesplitter.ClauseStack;
import bg.bas.dcl.clausesplitter.ClauseVerb;
import bg.bas.dcl.verbforms.Sentence;

public class StrongPunctuationClauseSplittingRule extends ClauseSplittingRule {

	ArrayList<PunctuationException> exceptions = new ArrayList<PunctuationException>();

	public StrongPunctuationClauseSplittingRule(ArrayList<PunctuationException> exceptions){
		this.exceptions = exceptions;
	}

	public boolean findClauseBorders(
			Sentence sentence,
			ArrayList<ClauseVerb> verbs,
			int v, int spos, int epos,
			ClauseStack stack
			){

		if (!checkCondition(stack)) return false;

		boolean found = false;

		for (int i=getStart(spos, stack); i<=epos; i++) {

			if (!strongPunctuation.contains(sentence.get(i).form)
					|| isException(sentence, i)) continue;

			if (stack.size() > 0 && stack.top().hasVerb) {
				while (stack.size() > 0) stack.removeFromStack(i);
			}

			if (i < sentence.size()-1) stack.putInStack("SCOC", "MAIN", i+1);

			found = true;
		}

		return found;
	}

	public boolean isException(Sentence sentence, int tokenpos){

		String token = sentence.get(tokenpos).form;

		for (PunctuationException pe : exceptions){

			if (!pe.sign.equals(token)) continue;

			int k = tokenpos-1;
			if (pe.position.equals("post")) k = tokenpos+1;

			if (k < 0 || k >= sentence.size()) continue;

			String w = sentence.get(k).form;
			char ch = pe.position.equals("pre") ? w.charAt(w.length()-1) : w.charAt(0);

			if (pe.str.equals("<l>") && Character.isLowerCase(ch)) return true;
			if (pe.str.equals("<u>") && Character.isUpperCase(ch)) return true;
			if (!pe.str.startsWith("<") && w.equalsIgnoreCase(pe.str)) return true;
		}

		return false;
	}

	public static class PunctuationException {
		public String str = "";
		public String position = "";
		public String sign = "";
	}
}
