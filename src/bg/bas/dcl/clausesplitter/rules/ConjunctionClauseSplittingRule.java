package bg.bas.dcl.clausesplitter.rules;

import java.util.ArrayList;

import bg.bas.dcl.clausesplitter.ClauseStack;
import bg.bas.dcl.clausesplitter.ClauseVerb;
import bg.bas.dcl.clausesplitter.Conjunction;
import bg.bas.dcl.verbforms.Sentence;

public class ConjunctionClauseSplittingRule extends ClauseSplittingRule {

	public ConjunctionClauseSplittingRule(ArrayList<Conjunction> conjList){
		this.conjList = conjList;
	}

	public boolean findClauseBorders(
			Sentence sentence,
			ArrayList<ClauseVerb> verbs,
			int v, int spos, int epos,
			ClauseStack stack
			){

		if (!checkCondition(stack)) return false;

		String compulsory = options.get("compulsory");
		String types = options.get("type");

		spos = getStart(spos, stack);
		boolean found = false;

		if (direction.equals("backward")) {

			for (int i=epos; i>=spos; i--) {
				Conjunction c = findConjunction(sentence, i, epos, compulsory, types);
				if (c == null) continue;

//				System.out.println("CONJ "+c.lemma+"\t"+i);
				openByConj(sentence, c, i, spos, stack);
				return true;
			}

			return false;
		}

		for (int i=spos; i<=epos; i++) {

			Conjunction c = findConjunction(sentence, i, epos, compulsory, types);
			if (c == null) continue;

//			System.out.println("CONJ "+c.lemma+"\t"+i);
			openByConj(sentence, c, i, spos, stack);
			found = true;

			i += c.size()-1;
		}

		return found;
	}
}
