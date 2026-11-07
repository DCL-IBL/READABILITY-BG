package bg.bas.dcl.verbforms;

import java.util.ArrayList;
import java.util.HashSet;

public class VerbFormModel extends ArrayList<VerbComponent> {

	private static final long serialVersionUID = 1L;

	public String id = "";
	public String tense = "";
	public String pattern = "";
	public String example = "";

	public int len = 0;

	public boolean readVerbFormModelFromString(String l, int n) {

		String[] data = l.split("\t");

		// data[0] is the id
		// data[1] is the tense
		// data[2] is the struct
		// data[3] example, data[4] frequency
		if (data.length < 3) return false;

		id = data[0].trim();
		tense = data[1].trim();
		pattern = data[2].trim();
		if (data.length > 3) example = data[3].trim();

		for (String d : pattern.split(" ")) {

			if (d.trim().equals("")) continue;

			VerbComponent vc = new VerbComponent();
			vc.readComponentTypeFromString(d.trim(), n);

			if (!vc.optional) len++;

			add(vc);
		}

		return size() > 0;
	}

	public int getFormModelLen() {
		return len;
	}

	// res.get(0) - positions of the fixed components, res.get(1) - positions in the optional slots
	public ArrayList<ArrayList<Integer>> findMatch(Sentence sent, int start, HashSet<Integer> used) {

		ArrayList<Integer> fixed = new ArrayList<Integer>();
		ArrayList<Integer> slots = new ArrayList<Integer>();

		if (!match(sent, 0, start, used, fixed, slots)) return null;

		ArrayList<ArrayList<Integer>> res = new ArrayList<ArrayList<Integer>>();
		res.add(fixed);
		res.add(slots);

		return res;
	}

	private boolean match(Sentence sent, int j, int i, HashSet<Integer> used,
			ArrayList<Integer> fixed, ArrayList<Integer> slots) {

		if (j >= size()) return true;

		VerbComponent vc = get(j);

		if (!vc.optional) {

			if (i >= sent.size() || used.contains(i)) return false;
//			System.out.println("COMPARE "+sent.get(i).lemma+"\t"+sent.get(i).tag+"\t"+vc.lemma+"\t"+vc.pos+vc.gram);

			if (!vc.isMatch(sent.get(i))) return false;

			fixed.add(i);
			if (match(sent, j + 1, i + 1, used, fixed, slots)) return true;
			fixed.remove(fixed.size() - 1);

			return false;
		}

		// greedy, then shorter if the rest does not match
		int k = 0;
		while (k < vc.optionalNum && i + k < sent.size()
				&& !used.contains(i + k) && vc.isMatch(sent.get(i + k))) {
			k++;
		}

		for (; k >= 0; k--) {

			int before = slots.size();
			for (int m=0; m<k; m++) slots.add(i+m);

			if (match(sent, j + 1, i + k, used, fixed, slots)) return true;

			while (slots.size() > before) slots.remove(slots.size() - 1);
		}

		return false;
	}

	// main verb - the last component with a tag only
	public int getMainComponent() {

		int res = -1;
		int f = 0;

		for (VerbComponent vc : this) {
			if (vc.optional) continue;
			if (vc.isMain()) res = f;
			f++;
		}

		if (res >= 0) return res;

		f = 0;
		for (VerbComponent vc : this) {
			if (vc.optional) continue;
			if (vc.pos.startsWith("V")) res = f;
			f++;
		}

		return res;
	}
}
