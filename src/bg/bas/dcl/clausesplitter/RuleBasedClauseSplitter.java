package bg.bas.dcl.clausesplitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;

import bg.bas.dcl.clausesplitter.rules.ClauseSplittingRule;
import bg.bas.dcl.verbforms.Sentence;
import bg.bas.dcl.verbforms.TagMapper;
import bg.bas.dcl.verbforms.VerbForm;
import bg.bas.dcl.verbforms.VerbFormFormalism;
import bg.bas.dcl.verbforms.Word;

public class RuleBasedClauseSplitter {

	public static final String VERB_COLUMN = "DCL:VERBFORM";
	public static final String CLAUSE_COLUMN = "DCL:CLAUSE";

	ArrayList<ClauseSplittingRule> rules = new ArrayList<ClauseSplittingRule>();

	VerbFormFormalism vff;
	TagMapper mapper;
	ClauseVerbsVerifier cvv;

	int clauseId = 0;

	public int sentences = 0;
	public int clauses = 0;
	public int forms = 0;

	public RuleBasedClauseSplitter(VerbFormFormalism vff,
			ArrayList<ClauseSplittingRule> rules,
			String lang){

		this.vff = vff;
		this.rules = rules;
		this.mapper = new TagMapper(lang);
		this.cvv = new ClauseVerbsVerifier(lang);
	}

	public ClauseStack processSentence(Sentence sent, ArrayList<ClauseVerb> verbs){

		ClauseStack cls = new ClauseStack();
		cls.putInStack("COC", "MAIN", 0);

		if (verbs.size() == 0) {
			cls.removeFromStack(sent.size()-1);
			return cls;
		}

		int first = verbs.get(0).first();

		if (first > 0) {
			for (ClauseSplittingRule r : rules){
				if (!r.scope.equals("initial")) continue;
				if (r.findClauseBorders(sent, verbs, -1, 0, first-1, cls) && r.isEnding()) break;
			}
		}

		for (int i=0; i<verbs.size(); i++){

			if (cls.size() == 0) cls.putInStack("COC", "MAIN", verbs.get(i).first());
			cls.top().hasVerb = true;

			if (i == verbs.size()-1) break;

			int spos = verbs.get(i).last()+1;
			int epos = verbs.get(i+1).first()-1;

			if (spos > epos) continue;

			for (ClauseSplittingRule r : rules){

				if (!r.scope.equals("gap")) continue;

//				System.out.println(r.order+" "+r.name+" STACK "+cls.stackSize());

				if (r.findClauseBorders(sent, verbs, i, spos, epos, cls) && r.isEnding()) break;
			}
		}

		while (cls.size() > 0) cls.removeFromStack(sent.size()-1);

		return cls;
	}

	// clause of each token: opening borders before the token, closing ones after it
	public int[] assignClauses(Sentence sent, ClauseStack cls){

		ArrayList<ClauseBorder> borders = new ArrayList<ClauseBorder>(cls.clborders);
		Collections.sort(borders, new Comparator<ClauseBorder>() {
			public int compare(ClauseBorder a, ClauseBorder b) {
				return a.position - b.position;
			}
		});

		int[] res = new int[sent.size()];
		ArrayList<Integer> idStack = new ArrayList<Integer>();
		int b = 0;

		for (int j=0; j<sent.size(); j++){

			int k = b;
			while (k < borders.size() && borders.get(k).position == j) {
				if (borders.get(k).type.equals("CO")) idStack.add(borders.get(k).clause);
				k++;
			}

			res[j] = idStack.size() > 0 ? idStack.get(idStack.size()-1) : 0;

			while (b < borders.size() && borders.get(b).position == j) {
				if (borders.get(b).type.equals("CC")) idStack.remove(Integer.valueOf(borders.get(b).clause));
				b++;
			}
		}

		return res;
	}

	public void processFile(File in, File out) throws IOException {

		BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(in), StandardCharsets.UTF_8));
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8));

		clauseId = 0;

		boolean header = false;
		int verbColumn = -1;
		Sentence sent = new Sentence();
		String l;

		while ((l = r.readLine()) != null) {

			if (!header) {
				header = true;
				if (l.startsWith("# global.columns")) {
					String[] cols = l.substring(l.indexOf("=")+1).trim().split("\\s+");
					for (int i=0; i<cols.length; i++) {
						if (cols[i].equals(VERB_COLUMN)) verbColumn = i;
					}
					w.write(l + " " + CLAUSE_COLUMN + "\n");
					continue;
				}
				w.write("# global.columns = ID FORM LEMMA UPOS XPOS FEATS HEAD DEPREL DEPS MISC " + CLAUSE_COLUMN + "\n");
			}

			if (l.trim().equals("")) {
				if (sent.lines.size() > 0) writeSentence(sent, verbColumn, w);
				sent = new Sentence();
				continue;
			}

			if (l.startsWith("#")) sent.lines.add(l);
			else sent.addLine(l);
		}

		if (sent.lines.size() > 0) writeSentence(sent, verbColumn, w);

		r.close();
		w.close();
	}

	// verb forms already marked in the input
	public ArrayList<VerbForm> readVerbForms(Sentence sent, int verbColumn){

		LinkedHashMap<String,VerbForm> forms = new LinkedHashMap<String,VerbForm>();

		for (Word t : sent) {

			if (verbColumn >= t.cols.length) continue;
			String v = t.cols[verbColumn];
			if (v.equals("_")) continue;

			String id = v.contains(":") ? v.substring(0, v.indexOf(":")) : v;

			if (!forms.containsKey(id)) forms.put(id, new VerbForm());
			VerbForm vf = forms.get(id);
			vf.addPosition(t.sentpos);

			if (v.contains(":")) {
				String label = v.substring(v.indexOf(":")+1);
				if (label.contains(";")) label = label.substring(0, label.indexOf(";"));
				vf.addTense(label, "");
			}
		}

		ArrayList<VerbForm> res = new ArrayList<VerbForm>(forms.values());
		Collections.sort(res, new Comparator<VerbForm>() {
			public int compare(VerbForm a, VerbForm b) {
				return a.get(0) - b.get(0);
			}
		});

		return res;
	}

	private void writeSentence(Sentence sent, int verbColumn, BufferedWriter w) throws IOException {

		sentences++;

		for (Word t : sent) mapper.setTag(t);

		// verb forms marked in the input, otherwise found here and not written
		ArrayList<VerbForm> verbs;
		if (verbColumn >= 0) verbs = readVerbForms(sent, verbColumn);
		else verbs = vff.findForms(sent);

		forms += verbs.size();

		ArrayList<ClauseVerb> clVerbs = cvv.reviseVerbs(sent, verbs);
		ClauseStack cls = processSentence(sent, clVerbs);
		int[] assigned = assignClauses(sent, cls);

		// clauses numbered in the order of their first word; clauses without words are dropped
		HashMap<Integer,Integer> ids = new HashMap<Integer,Integer>();
		HashMap<Integer,Integer> firstWord = new HashMap<Integer,Integer>();

		for (int j=0; j<assigned.length; j++) {
			if (assigned[j] == 0 || ids.containsKey(assigned[j])) continue;
			clauseId++;
			clauses++;
			ids.put(assigned[j], clauseId);
			firstWord.put(j, assigned[j]);
		}

		for (String line : sent.lines) {

			if (line.startsWith("#")) {
				w.write(line + "\n");
				continue;
			}

			String id = line.substring(0, line.indexOf("\t"));

			if (!id.matches("\\d+")) {
				w.write(line + "\t_\n");
				continue;
			}

			Word t = sent.getById(Integer.parseInt(id));
			int j = t.sentpos;

			String c = "_";

			if (assigned[j] > 0) {
				c = "C" + ids.get(assigned[j]);
				if (firstWord.containsKey(j)) c += ":" + describe(cls.clauses.get(assigned[j]-1), ids);
			}

			w.write(line + "\t" + c + "\n");
		}

		w.write("\n");
	}

	private String describe(Clause cl, HashMap<Integer,Integer> ids){

		String res = cl.kind;

		if (cl.head > 0 && ids.containsKey(cl.head)) res += ";head=C" + ids.get(cl.head);
		if (cl.coord > 0 && ids.containsKey(cl.coord)) res += ";coord=C" + ids.get(cl.coord);
		if (!cl.conj.equals("")) res += ";conj=" + cl.conj;

		return res;
	}
}
