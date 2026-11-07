package bg.bas.dcl.clausesplitter;

import java.util.ArrayList;

import bg.bas.dcl.verbforms.Sentence;
import bg.bas.dcl.verbforms.VerbForm;

public class ClauseVerbsVerifier {

	String lang = "bg";

	public ClauseVerbsVerifier(String lang){
		this.lang = lang;
	}

	// verb forms that can be the predicate of a clause
	public ArrayList<ClauseVerb> reviseVerbs(Sentence sentence, ArrayList<VerbForm> verbs){

		ArrayList<ClauseVerb> clVerbs = new ArrayList<ClauseVerb>();

main:	for (VerbForm vf : verbs){

			if (!fullVerb(vf)) continue main;

			int first = vf.get(0);

			// participles after a comma
			if (hasLabel(vf, "DVerb") && first > 0 && sentence.get(first-1).form.equals(",")) continue main;

			ClauseVerb clv = new ClauseVerb();
			clv.addAll(vf);
			clv.tense.addAll(vf.tense);
			clv.mainword = vf.mainword;

			clVerbs.add(clv);
		}

		return clVerbs;
	}

	public boolean hasLabel(VerbForm vf, String label){
		for (String t : vf.tense) {
			if (t.startsWith(label)) return true;
		}
		return false;
	}

	public boolean fullVerb(VerbForm vf){

		for (String t : vf.tense) {
			if (!t.startsWith("NVerb")
					&& !t.endsWith("Participle")
					&& !t.startsWith("Past_Participle")
					&& !t.startsWith("Present_Participle")) return true;
		}

		return false;
	}
}
