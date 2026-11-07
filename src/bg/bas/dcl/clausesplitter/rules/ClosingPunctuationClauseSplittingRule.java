package bg.bas.dcl.clausesplitter.rules;

import java.util.ArrayList;

import bg.bas.dcl.clausesplitter.ClauseStack;
import bg.bas.dcl.clausesplitter.ClauseVerb;
import bg.bas.dcl.clausesplitter.Conjunction;
import bg.bas.dcl.verbforms.Sentence;

public class ClosingPunctuationClauseSplittingRule extends ClauseSplittingRule {

	public ClosingPunctuationClauseSplittingRule(ArrayList<Conjunction> conjList){
		this.conjList = conjList;
	}

	public boolean findClauseBorders(
			Sentence sentence,
			ArrayList<ClauseVerb> verbs,
			int v, int spos, int epos,
			ClauseStack stack
			){

		if (!checkCondition(stack)) return false;

		int i = findPunctuation(sentence, getStart(spos, stack), epos, weakPunctuation);
		if (i < 0 || isExcepted(sentence.get(i).form)) return false;

		closeTop(sentence, i, stack);

		// a conjunction right after the sign opens the next clause
		if (i+1 <= epos) {
			Conjunction c = findConjunction(sentence, i+1, epos, null, null);
			if (c != null) openByConj(sentence, c, i+1, i+1, stack);
		}

		return true;
	}
}
