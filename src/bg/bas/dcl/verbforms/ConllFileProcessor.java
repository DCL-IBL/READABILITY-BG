package bg.bas.dcl.verbforms;

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
import java.util.HashMap;

public class ConllFileProcessor {

	public static final String COLUMN = "DCL:VERBFORM";

	VerbFormFormalism vff = null;
	TagMapper mapper = null;

	int formId = 0;

	public int sentences = 0;
	public int forms = 0;
	public int analytic = 0;
	public int ambiguous = 0;

	public ConllFileProcessor(VerbFormFormalism vff, TagMapper mapper) {
		this.vff = vff;
		this.mapper = mapper;
	}

	public void processFile(File in, File out) throws IOException {

		BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(in), StandardCharsets.UTF_8));
		BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8));

		formId = 0;
		boolean header = false;
		Sentence sent = new Sentence();
		String l;

		while ((l = r.readLine()) != null) {

			if (!header) {
				header = true;
				if (l.startsWith("# global.columns")) {
					w.write(l + " " + COLUMN + "\n");
					continue;
				}
				w.write("# global.columns = ID FORM LEMMA UPOS XPOS FEATS HEAD DEPREL DEPS MISC " + COLUMN + "\n");
			}

			if (l.trim().equals("")) {
				if (sent.lines.size() > 0) writeSentence(sent, w);
				sent = new Sentence();
				continue;
			}

			if (l.startsWith("#")) sent.lines.add(l);
			else sent.addLine(l);
		}

		if (sent.lines.size() > 0) writeSentence(sent, w);

		r.close();
		w.close();
	}

	private void writeSentence(Sentence sent, BufferedWriter w) throws IOException {

		sentences++;

		for (Word t : sent) mapper.setTag(t);

		ArrayList<VerbForm> verbs = vff.findForms(sent);
		HashMap<Integer, String> col = new HashMap<Integer, String>();

		for (VerbForm vf : verbs) {

			formId++;
			forms++;
			if (vf.isAnalytic()) analytic++;
			if (vf.tense.size() > 1) ambiguous++;

			String id = "F" + formId;

			for (int i : vf) col.put(i, id);

			col.put(vf.get(0), id + ":" + vf.getLabel()
					+ ";" + vf.getGrammTag(sent)
					+ ";rule=" + String.join(",", vf.rules));
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
}
