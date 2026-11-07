package bg.bas.dcl.nounphrases;

import java.util.ArrayList;
import java.util.HashSet;

public class NounPhraseModel extends ArrayList<NounPhraseComponent>{

	private static final long serialVersionUID = 1L;

	public String id = "";
	public String name = "";
	public String modelstring = "";
	public String example = "";
	public String agreement = "";

	public boolean readModelFromString(String l, int n){

		String[] data = l.split("\t");

		// data[0] is the id
		// data[1] is the category (NPI, NPD, AP)
		// data[2] is the model
		// data[3] example, data[4] agreement
		if (data.length < 3) return false;

		id = data[0].trim();
		name = data[1].trim();
		modelstring = data[2].trim();
		if (data.length > 3) example = data[3].trim();
		if (data.length > 4) agreement = data[4].trim();

		for (String c : modelstring.split(" ")){

			if (c.trim().equals("")) continue;

			NounPhraseComponent npc = new NounPhraseComponent();
			npc.readComponentFromString(c.trim(), n);

//			System.out.println("COMPONENT READ: "+ npc.lemma+","+npc.pos+npc.gram + " o:"+npc.optional+ " h:"+npc.isHead);

			add(npc);
		}

		return size() > 0;
	}

	public int getLength (){
		int len = 0;
		for (NounPhraseComponent c :this){
			if (!c.optional) len++;
		}
		return len;
	}

	public NounPhrase findMatch(Sentence sent, int start, HashSet<Integer> blocked) {

		if (start + getLength() > sent.size()) return null;

		NounPhrase res = new NounPhrase();
		if (!match(sent, 0, start, blocked, sent.get(start).clause, res)) return null;

		res.category = name;
		res.rules.add(id);

		return res;
	}

	private boolean match(Sentence sent, int j, int i, HashSet<Integer> blocked, String clause, NounPhrase res){

		if (j >= size()) return true;

		NounPhraseComponent npc = get(j);

		if (!npc.optional) {

			if (!isFree(sent, i, blocked, clause) || !npc.isMatch(sent.get(i))) return false;

			res.add(i);
			if (npc.isHead) res.headPosition = i;

			if (match(sent, j+1, i+1, blocked, clause, res)) return true;

			res.remove(res.size()-1);
			return false;
		}

		// greedy, then shorter if the rest does not match
		int k = 0;
		while (k < npc.optionalNum && isFree(sent, i+k, blocked, clause) && npc.isMatch(sent.get(i+k))) k++;

		for (; k>=0; k--) {

			int before = res.size();
			for (int m=0; m<k; m++) res.add(i+m);

			if (match(sent, j+1, i+k, blocked, clause, res)) return true;

			while (res.size() > before) res.remove(res.size()-1);
		}

		return false;
	}

	// not part of a verb form, in the same clause
	private boolean isFree(Sentence sent, int i, HashSet<Integer> blocked, String clause){
		if (i >= sent.size() || blocked.contains(i)) return false;
		return sent.get(i).clause.equals(clause);
	}
}
