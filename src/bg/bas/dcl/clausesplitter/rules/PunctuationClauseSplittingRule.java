package bg.bas.dcl.clausesplitter.rules;

import java.util.ArrayList;

import bg.bas.dcl.clausesplitter.ClauseStack;
import bg.bas.dcl.clausesplitter.ClauseVerb;
import bg.bas.dcl.verbforms.Sentence;

public class PunctuationClauseSplittingRule extends ClauseSplittingRule {

	public boolean findClauseBorders(
			Sentence sentence,
			ArrayList<ClauseVerb> verbs,
			int v, int spos, int epos,
			ClauseStack stack
			){

		if (!checkCondition(stack)) return false;

		int i = findPunctuation(sentence, getStart(spos, stack), epos,
				"strong".equals(options.get("class")) ? strongPunctuation : weakPunctuation);
		if (i < 0 || isExcepted(sentence.get(i).form)) return false;

		closeTop(sentence, i, stack);

		return true;
	}
}
