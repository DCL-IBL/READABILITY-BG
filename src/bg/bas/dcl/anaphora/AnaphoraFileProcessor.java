package bg.bas.dcl.anaphora;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Scanner;

public class AnaphoraFileProcessor {

	public static final String ANAPHORA_COLUMN = "DCL:ANAPHORA";
	public static final String COREF_COLUMN = "DCL:COREF";

	// external folder with resources, null for the ones in the jar
	public String resourceDir = null;

	MissingSubjectRecogniser msr = new MissingSubjectRecogniser();
	AnaphoraRecogniser ar = new AnaphoraRecogniser();
	PredicateFinder pf = new PredicateFinder();

	int zeroId = 0, anaphorId = 0, chainId = 0;

	public int sentences = 0;
	public HashMap<String,Integer> stats = new LinkedHashMap<String,Integer>();

	public InputStream open(String name) throws Exception {

		if (resourceDir != null) {
			File f = new File(resourceDir, name);
			if (f.exists()) return new FileInputStream(f);
		}

		InputStream in = getClass().getResourceAsStream("/resources/" + name);
		if (in == null) throw new Exception("Missing resource: " + name);

		return in;
	}

	public ArrayList<String[]> readTable(String name){

		ArrayList<String[]> res = new ArrayList<String[]>();

		try{

			Scanner s = new Scanner(open(name), "UTF-8");
			boolean header = true;

			while (s.hasNextLine()){

				String l = s.nextLine();
				if (l.startsWith("\uFEFF")) l = l.substring(1);

				if (l.startsWith("#") || l.trim().equals("")) continue;
				if (header) {
					header = false;
					continue;
				}

				res.add(l.split("\t", -1));
			}

			s.close();
		} catch(Exception e){
			e.printStackTrace();
		}

		return res;
	}

	public void readResources(){

		for (String[] d : readTable("pronouns.tsv")) {
			if (d.length < 5) continue;
			PronounForm p = new PronounForm();
			p.form = d[0].trim();
			p.kind = d[1].trim();
			p.person = d[2].trim();
			p.gender = d[3].trim();
			p.number = d[4].trim();
			if (!ar.pronouns.containsKey(p.form)) ar.pronouns.put(p.form, new ArrayList<PronounForm>());
			ar.pronouns.get(p.form).add(p);
		}

		for (String[] d : readTable("impersonal_verbs.tsv")) {
			if (d.length < 2) continue;
			String kind = d[1].trim();
			if (!msr.impersonal.containsKey(kind)) msr.impersonal.put(kind, new HashSet<String>());
			msr.impersonal.get(kind).add(d[0].trim());
		}

		for (String[] d : readTable("subject_rules.tsv")) {
			SubjectRule r = new SubjectRule();
			if (r.readRuleFromString(String.join("\t", d))) msr.rules.add(r);
		}

		for (String[] d : readTable("anaphora_rules.tsv")) {
			AnaphoraRule r = new AnaphoraRule();
			if (r.readRuleFromString(String.join("\t", d))) ar.rules.add(r);
		}
	}

	public void processFile(File in, File out) throws IOException {

		BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(in), StandardCharsets.UTF_8));
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8));

		zeroId = 0;
		anaphorId = 0;
		chainId = 0;

		String header = null;
		int npColumn = -1;
		ArrayList<Document> docs = new ArrayList<Document>();
		Document doc = new Document();
		Sentence sent = new Sentence();
		int paragraph = 0;
		String l;

		while ((l = r.readLine()) != null) {

			if (header == null) {
				header = "# global.columns = ID FORM LEMMA UPOS XPOS FEATS HEAD DEPREL DEPS MISC";
				if (l.startsWith("# global.columns")) {
					header = l;
					String[] cols = l.substring(l.indexOf("=")+1).trim().split("\\s+");
					for (int i=0; i<cols.length; i++) {
						if (cols[i].equals("DCL:NP")) npColumn = i;
					}
					continue;
				}
			}

			if (l.trim().equals("")) {
				if (sent.lines.size() > 0) {
					sent.index = doc.sentences.size();
					sent.paragraph = paragraph;
					if (sent.sentId.equals("")) sent.sentId = String.valueOf(sentences + 1);
					doc.sentences.add(sent);
					sentences++;
				}
				sent = new Sentence();
				continue;
			}

			if (l.startsWith("# newdoc")) {
				if (doc.sentences.size() > 0) docs.add(doc);
				doc = new Document();
			}
			if (l.startsWith("# newpar")) paragraph++;
			if (l.startsWith("# sent_id")) sent.sentId = l.substring(l.indexOf("=")+1).trim();

			if (l.startsWith("#")) sent.lines.add(l);
			else sent.addLine(l);
		}

		if (sent.lines.size() > 0) {
			sent.index = doc.sentences.size();
			sent.paragraph = paragraph;
			if (sent.sentId.equals("")) sent.sentId = String.valueOf(sentences + 1);
			doc.sentences.add(sent);
			sentences++;
		}
		if (doc.sentences.size() > 0) docs.add(doc);

		w.write(header + " " + ANAPHORA_COLUMN + " " + COREF_COLUMN + "\n");

		for (Document d : docs) {
			processDocument(d, npColumn);
			writeDocument(d, w);
		}

		r.close();
		w.close();
	}

	public void processDocument(Document doc, int npColumn){

		for (Sentence sent : doc.sentences) {

			pf.findPredicates(sent);

			if (npColumn >= 0) {
				for (Word t : sent) {
					String v = npColumn < t.cols.length ? t.cols[npColumn] : "_";
					// noun phrases only, not adjective phrases
					if (!v.contains(";head=") || !v.contains(":NP")) continue;
					String h = v.substring(v.indexOf(";head=")+6);
					if (h.contains(";")) h = h.substring(0, h.indexOf(";"));
					Word head = sent.getById(Integer.parseInt(h));
					if (head != null) head.npHead = true;
				}
			}

			ArrayList<Mention> current = new ArrayList<Mention>();

			for (Word t : sent) {

				PronounForm p = ar.getPronoun(t);

				if (p != null) current.add(ar.newPronoun(sent, t.sentpos, p));
				else if (npColumn >= 0 ? t.npHead : (t.upos.equals("NOUN") || t.upos.equals("PROPN"))
						&& !t.deprel.equals("flat") && !t.deprel.equals("fixed")) current.add(ar.newNP(sent, t.sentpos));
			}

			doc.mentions.addAll(current);

			for (Predicate p : sent.predicates) {
				Mention z = msr.findMissingSubject(doc, sent, p);
				if (z != null) {
					current.add(z);
					doc.mentions.add(z);
				}
			}

			Collections.sort(current, new Comparator<Mention>() {
				public int compare(Mention a, Mention b) {
					return a.pos - b.pos;
				}
			});

			// missing subjects first, then pronouns
			for (Mention m : current) {
				if (m.kind.equals("ZERO")) ar.resolve(doc, m);
			}
			for (Mention m : current) {
				if (m.kind.equals("PRONOUN")) ar.resolve(doc, m);
			}
		}
	}

	private void writeDocument(Document doc, BufferedWriter w) throws IOException {

		ArrayList<Mention> ms = new ArrayList<Mention>(doc.mentions);
		Collections.sort(ms, new Comparator<Mention>() {
			public int compare(Mention a, Mention b) {
				return a.before(b) ? -1 : b.before(a) ? 1 : 0;
			}
		});

		HashMap<Mention,Integer> size = new HashMap<Mention,Integer>();
		for (Mention m : ms) {
			if (m.kind.equals("ZERO")) m.label = "Z" + (++zeroId);
			if (m.kind.equals("PRONOUN")) m.label = "A" + (++anaphorId);
			Integer s = size.get(m.root());
			size.put(m.root(), s == null ? 1 : s+1);
		}

		HashMap<Mention,String> chains = new HashMap<Mention,String>();
		HashMap<Sentence,HashMap<Integer,String>> anaphora = new HashMap<Sentence,HashMap<Integer,String>>();
		HashMap<Sentence,HashMap<Integer,String>> coref = new HashMap<Sentence,HashMap<Integer,String>>();

		for (Mention m : ms) {

			if (size.get(m.root()) > 1) {
				if (!chains.containsKey(m.root())) chains.put(m.root(), "E" + (++chainId));
				put(coref, m.sent, m.pos, chains.get(m.root()));
			}

			if (m.label.equals("")) continue;

			String v = m.label + ":" + m.type + ";" + m.getFeats();
			if (m.rules.size() > 0) v += ";rule=" + String.join(",", m.rules);
			if (m.antecedent != null) v += ";ref=" + ref(m.antecedent);

			put(anaphora, m.sent, m.pos, v);
			count(m.kind.equals("ZERO") ? m.type : "Anaphora", m.antecedent != null);
		}

		for (Sentence sent : doc.sentences) {
			for (String line : sent.lines) {

				if (line.startsWith("#")) {
					w.write(line + "\n");
					continue;
				}

				String id = line.substring(0, line.indexOf("\t"));
				String a = "_", c = "_";

				if (id.matches("\\d+")) {
					int pos = sent.getById(Integer.parseInt(id)).sentpos;
					if (anaphora.containsKey(sent) && anaphora.get(sent).containsKey(pos)) a = anaphora.get(sent).get(pos);
					if (coref.containsKey(sent) && coref.get(sent).containsKey(pos)) c = coref.get(sent).get(pos);
				}

				w.write(line + "\t" + a + "\t" + c + "\n");
			}
			w.write("\n");
		}
	}

	// another anaphora or missing subject by its label, a noun phrase by sentence and word ID
	String ref(Mention a){
		if (!a.label.equals("")) return a.label;
		return a.sent.sentId + "#" + a.sent.get(a.pos).id;
	}

	void put(HashMap<Sentence,HashMap<Integer,String>> col, Sentence s, int pos, String v){
		if (!col.containsKey(s)) col.put(s, new HashMap<Integer,String>());
		String old = col.get(s).get(pos);
		if (old != null && !old.equals(v) && !("," + old + ",").contains("," + v + ",")) v = old + "," + v;
		col.get(s).put(pos, v);
	}

	void count(String type, boolean resolved){
		String k = type + (type.equals("Pro-drop") || type.equals("Anaphora") ? (resolved ? " resolved" : " unresolved") : "");
		Integer c = stats.get(k);
		stats.put(k, c == null ? 1 : c+1);
	}
}
