package bg.bas.dcl.clausesplitter.rules;

import java.util.ArrayList;

import bg.bas.dcl.clausesplitter.ClauseStack;
import bg.bas.dcl.clausesplitter.ClauseVerb;
import bg.bas.dcl.verbforms.Sentence;

public class ArbitraryClauseSplittingRule extends ClauseSplittingRule {

	public boolean findClauseBorders(
			Sentence sentence,
			ArrayList<ClauseVerb> verbs,
			int v, int spos, int epos,
			ClauseStack stack
			){

		if (epos >= sentence.size()-1) return false;
		if (!checkCondition(stack)) return false;

		// boundary right before the next verb form
		closeTop(sentence, epos, stack);

		return true;
	}
}
