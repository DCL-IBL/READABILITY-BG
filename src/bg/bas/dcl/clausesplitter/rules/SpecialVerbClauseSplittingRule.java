package bg.bas.dcl.clausesplitter.rules;

import java.util.ArrayList;
import java.util.HashMap;

import bg.bas.dcl.clausesplitter.ClauseStack;
import bg.bas.dcl.clausesplitter.ClauseVerb;
import bg.bas.dcl.verbforms.Sentence;
import bg.bas.dcl.verbforms.Word;

public class SpecialVerbClauseSplittingRule extends ClauseSplittingRule {

	HashMap<String,ArrayList<String>> specialVerbs = new HashMap<String,ArrayList<String>>();

	String particle = "да";
	String clitics = " ли се си не not ";

	public SpecialVerbClauseSplittingRule(HashMap<String,ArrayList<String>> specialVerbs, String lang){
		this.specialVerbs = specialVerbs;
		if (lang.equals("en")) particle = "to";
	}

	public boolean findClauseBorders(
			Sentence sentence,
			ArrayList<ClauseVerb> verbs,
			int v, int spos, int epos,
			ClauseStack stack
			){

		if (v < 0 || !checkCondition(stack)) return false;

		boolean special = false;
		for (String t : options.get("type").split(",")) {
			if (hasType(sentence, verbs.get(v), t.trim())) special = true;
		}
		if (!special) return false;

		// only да and clitics between the two verb forms
		boolean hasParticle = false;

		for (int i=spos; i<=epos; i++) {

			Word t = sentence.get(i);

			if (t.lower().equals(particle)) hasParticle = true;
			else if (!clitics.contains(" "+t.lower()+" ") && !t.tag.startsWith("P")) return false;
		}

		return hasParticle;
	}

	public boolean hasType(Sentence sentence, ClauseVerb vf, String type){

		if (!specialVerbs.containsKey(type)) return false;

		for (String w : specialVerbs.get(type)){
			for (int i : vf) {
				if (sentence.get(i).lemma.equalsIgnoreCase(w) || sentence.get(i).lower().equals(w)) return true;
			}
		}

		return false;
	}
}
