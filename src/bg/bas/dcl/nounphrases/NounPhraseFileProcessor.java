package bg.bas.dcl.nounphrases;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class NounPhraseFileProcessor {

	public static final String COLUMN = "DCL:NP";

	// external folder with resources, null for the ones in the jar
	public String resourceDir = null;

	NounPhraseRecogniser npr;
	NPTagMapper mapper = new NPTagMapper();

	int phraseId = 0;

	public int sentences = 0;
	public int phrases = 0;
	public HashMap<String,Integer> categories = new HashMap<String,Integer>();

	public NounPhraseFileProcessor(NounPhraseRecogniser npr){
		this.npr = npr;
	}

	public InputStream open(String name) throws Exception {

		if (resourceDir != null) {
			File f = new File(resourceDir, name);
			if (f.exists()) return new FileInputStream(f);
		}

		InputStream in = getClass().getResourceAsStream("/resources/" + name);
		if (in == null) throw new Exception("Missing resource: " + name);

		return in;
	}

	public void processFile(File in, File out) throws IOException {

		BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(in), StandardCharsets.UTF_8));
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8));

		phraseId = 0;

		boolean header = false;
		int verbColumn = -1, clauseColumn = -1;
		Sentence sent = new Sentence();
		String l;

		while ((l = r.readLine()) != null) {

			if (!header) {
				header = true;
				if (l.startsWith("# global.columns")) {
					String[] cols = l.substring(l.indexOf("=")+1).trim().split("\\s+");
					for (int i=0; i<cols.length; i++) {
						if (cols[i].equals("DCL:VERBFORM")) verbColumn = i;
						if (cols[i].equals("DCL:CLAUSE")) clauseColumn = i;
					}
					w.write(l + " " + COLUMN + "\n");
					continue;
				}
				w.write("# global.columns = ID FORM LEMMA UPOS XPOS FEATS HEAD DEPREL DEPS MISC " + COLUMN + "\n");
			}

			if (l.trim().equals("")) {
				if (sent.lines.size() > 0) writeSentence(sent, verbColumn, clauseColumn, w);
				sent = new Sentence();
				continue;
			}

			if (l.startsWith("#")) sent.lines.add(l);
			else sent.addLine(l);
		}

		if (sent.lines.size() > 0) writeSentence(sent, verbColumn, clauseColumn, w);

		r.close();
		w.close();
	}

	private void writeSentence(Sentence sent, int verbColumn, int clauseColumn, BufferedWriter w) throws IOException {

		sentences++;

		for (Word t : sent) {
			mapper.setTag(t);
			if (verbColumn >= 0 && verbColumn < t.cols.length) t.inVerbForm = !t.cols[verbColumn].equals("_");
			if (clauseColumn >= 0 && clauseColumn < t.cols.length) {
				String c = t.cols[clauseColumn];
				t.clause = c.contains(":") ? c.substring(0, c.indexOf(":")) : c;
			}
		}

		ArrayList<NounPhrase> nps = npr.findForms(sent);
		HashMap<Integer,String> col = new HashMap<Integer,String>();

		for (NounPhrase np : nps) {

			phraseId++;
			phrases++;
			Integer c = categories.get(np.category);
			categories.put(np.category, c == null ? 1 : c+1);

			String id = "P" + phraseId;
			for (int i : np) col.put(i, id);

			col.put(np.get(0), id + ":" + np.category
					+ (np.isPrepositional ? ";prep=" + np.prep : "")
					+ ";" + getGrammTag(sent, np)
					+ ";head=" + sent.get(np.headPosition).id
					+ ";rule=" + String.join(",", np.rules));
		}

		for (String line : sent.lines) {

			if (line.startsWith("#")) {
				w.write(line + "\n");
				continue;
			}

			String id = line.substring(0, line.indexOf("\t"));
			String value = "_";

			if (id.matches("\\d+")) {
				Word t = sent.getById(Integer.parseInt(id));
				if (col.containsKey(t.sentpos)) value = col.get(t.sentpos);
			}

			w.write(line + "\t" + value + "\n");
		}

		w.write("\n");
	}

	// definiteness of the phrase (articled word or demonstrative), gender and number of the head
	public String getGrammTag(Sentence sent, NounPhrase np){

		TreeMap<String,String> f = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);

		boolean def = false;
		for (int i : np.getComponents()) {
			Word t = sent.get(i);
			if (t.isArticled() || t.getPos().equals("PD")) def = true;
		}
		f.put("Definite", def ? "Def" : "Ind");

		Word h = sent.get(np.headPosition);
		if (h.getFeat("Gender") != null) f.put("Gender", h.getFeat("Gender"));
		if (h.getFeat("Number") != null) f.put("Number", h.getFeat("Number"));

		ArrayList<String> res = new ArrayList<String>();
		for (String k : f.keySet()) res.add(k + "=" + f.get(k));

		return String.join("|", res);
	}
}
