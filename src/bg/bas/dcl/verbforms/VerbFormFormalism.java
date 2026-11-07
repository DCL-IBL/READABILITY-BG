package bg.bas.dcl.verbforms;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

public class VerbFormFormalism extends ArrayList<VerbFormModel> {

	private static final long serialVersionUID = 1L;

	public int n = 3;

	ArrayList<String> posAndLemmaTriggers = new ArrayList<String>();

	public VerbFormFormalism(int n) {
		this.n = n;
	}

	public void readModels(InputStream in){

		try{

			Scanner s = new Scanner(in, "UTF-8");
			boolean first = true;

			while (s.hasNextLine()){

				String l = s.nextLine();

				if (l.startsWith("\uFEFF")) l = l.substring(1);
				if (first) {
					first = false;
					if (l.startsWith("id\t")) continue;
				}
				if (l.trim().equals("") || l.startsWith("#")) continue;

				VerbFormModel vf = new VerbFormModel();
				if (!vf.readVerbFormModelFromString(l, n)) continue;

				String trigger = vf.get(0).getTrigger();
				if (!posAndLemmaTriggers.contains(trigger)) posAndLemmaTriggers.add(trigger);

				// longer models first
				int position = size();
				for (int i=0; i<size(); i++) {
					if (get(i).getFormModelLen() < vf.getFormModelLen()) {
						position = i;
						break;
					}
				}
				add(position, vf);
			}

//			System.out.println(this.size());
			s.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	private boolean isTrigger(Word t) {
		return posAndLemmaTriggers.contains(t.ruleLemma)
				|| posAndLemmaTriggers.contains(t.lower())
				|| (t.tag.length() > 0 && posAndLemmaTriggers.contains(t.tag.substring(0, 1)));
	}

	public ArrayList<VerbForm> findForms(Sentence sent) {

		ArrayList<VerbForm> verbs = new ArrayList<VerbForm>();
		HashSet<Integer> used = new HashSet<Integer>();

		for (int start=0; start<sent.size(); start++) {

			if (used.contains(start) || !isTrigger(sent.get(start))) continue;

//			System.out.println("Trigger: "+sent.get(start).lemma+"\t"+sent.get(start).tag);

			int modellen = 0;
			VerbForm currentform = null;
			ArrayList<Integer> slots = null;

			for (VerbFormModel vfm : this) {

				if (vfm.getFormModelLen() < modellen) break;

				ArrayList<ArrayList<Integer>> m = vfm.findMatch(sent, start, used);
				if (m == null) continue;

				if (currentform == null) {

					currentform = new VerbForm();
					currentform.fixed = m.get(0);
					slots = m.get(1);

					int mc = vfm.getMainComponent();
					if (mc < 0) mc = m.get(0).size() - 1;
					currentform.mainword = m.get(0).get(mc);

					currentform.addTense(vfm.tense, vfm.id);
					modellen = vfm.getFormModelLen();

				} else if (currentform.fixed.equals(m.get(0))) {
					// same tokens - another reading
					currentform.addTense(vfm.tense, vfm.id);
				}
			}

			if (currentform == null) continue;

			for (int i : currentform.fixed) currentform.addPosition(i);

			// да in optional slots is part of the form (нека да)
			for (int i : slots) {
				if (sent.get(i).lower().equals("да")) currentform.addPosition(i);
			}

			addReflexives(sent, currentform, used);

			used.addAll(currentform);
			if (currentform.getLabel().equals("NVerb")) continue;

			verbs.add(currentform);
		}

		return verbs;
	}

	// reflexive се / си attached to the form
	private void addReflexives(Sentence sent, VerbForm form, HashSet<Integer> used) {

		for (Word w : sent) {

			if (!w.isReflexive() || used.contains(w.sentpos) || form.contains(w.sentpos)) continue;

			Word h = sent.getById(w.head);
			if (h != null && form.fixed.contains(h.sentpos)) form.addPosition(w.sentpos);
		}
	}
}
