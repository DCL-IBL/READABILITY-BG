package bg.bas.dcl.nounphrases;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;

public class NounPhraseRecogniser{

	public static final String VERSION = "1.0";

	public int n = 2;

	ArrayList<NounPhraseModel> nms = new ArrayList<NounPhraseModel>();
	NounPhraseConstraints constraints = new NounPhraseConstraints();

	public ArrayList<NounPhraseModel> readModelsFromFile(InputStream in){

		try{

			Scanner s = new Scanner(in, "UTF-8");
			boolean header = true;

			while (s.hasNextLine()){

				String l = s.nextLine();
				if (l.startsWith("\uFEFF")) l = l.substring(1);

				if (l.trim().equals("") || l.startsWith("#")) continue;
				if (header) {
					header = false;
					continue;
				}

				NounPhraseModel inm = new NounPhraseModel();
				if (!inm.readModelFromString(l, n)) continue;

				// longer models first
				int position = nms.size();
				for (int i=0; i<nms.size(); i++) {
					if (nms.get(i).getLength() < inm.getLength()) {
						position = i;
						break;
					}
				}
				nms.add(position, inm);
			}

//			System.out.println(nms.size());
			s.close();
		} catch(Exception e){
			e.printStackTrace();
		}

		return nms;
	}

	// check column of np_constraints.tsv and active = да
	public void readConstraintsFromFile(InputStream in){

		try{

			Scanner s = new Scanner(in, "UTF-8");
			boolean header = true;

			while (s.hasNextLine()){

				String l = s.nextLine();
				if (l.startsWith("\uFEFF")) l = l.substring(1);

				if (l.trim().equals("") || l.startsWith("#")) continue;
				if (header) {
					header = false;
					continue;
				}

				String[] data = l.split("\t");
				if (data.length < 7) continue;

				if (data[6].trim().equals("да")) constraints.active.add(data[2].trim());
			}

			s.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	public String getContext(Sentence sent, int start, int len){
		String res = "";
		for (int i=start; i<sent.size() && i-start<=len; i++){
			res += sent.get(i).form+" ";
		}
		return res.trim();
	}

	public ArrayList<NounPhrase> findForms(Sentence sent) {

		ArrayList<NounPhrase> nounphrases = new ArrayList<NounPhrase>();

		HashSet<Integer> blocked = new HashSet<Integer>();
		for (Word t : sent) {
			if (t.inVerbForm || t.tag.equals("") && !t.upos.equals("CCONJ")) blocked.add(t.sentpos);
		}

		int start = 0;

main:	while (start<sent.size()) {

//			System.out.println("CONTEXT: " + getContext(sent,start,5));

			if (blocked.contains(start)) {
				start++;
				continue;
			}

			NounPhrase np = null;

			for (NounPhraseModel m : nms){

				if (np != null && m.getLength() < np.modelLength) break;

				NounPhrase res = m.findMatch(sent, start, blocked);
				if (res == null || res.size() == 0) continue;
				if (!constraints.check(sent, res, m)) continue;

				if (np == null) {
					np = res;
					np.rules.clear();
					np.rules.add(m.id);
					np.modelLength = m.getLength();
				} else if (np.equals(res) && np.headPosition == res.headPosition) {
					// same words, another rule
					np.rules.add(m.id);
				}
			}

			if (np == null) {
				start++;
				continue main;
			}

			int first = np.get(0);
			if (first > 0 && sent.get(first-1).upos.equals("ADP")
					&& sent.get(first-1).clause.equals(sent.get(first).clause)){
				np.add(0, first-1);
				np.isPrepositional = true;
				np.prep = sent.get(first-1).form;
			}

			nounphrases.add(np);

			start = np.get(np.size()-1)+1;
		}

		return nounphrases;
	}

	static void usage() {
		System.err.println(
				"NounPhraseRecogniser " + VERSION + "\n"
				+ "Usage: java -jar NounPhraseRecogniser.jar -i <input file or folder> -o <output file or folder>\n"
				+ "           [-resources <folder>] [-gap <n>]\n"
				+ "  -resources  folder with resource files that replace the built-in ones\n"
				+ "  -gap        maximum length of the (n,{...}) slots in the rules (default 2)\n"
				+ "  -list       print the rules in order of application and exit");
	}

	public static void main(String[] args) throws Exception {

		String in = null, out = null, resources = null;
		int gap = 2;
		boolean list = false;

		for (int i=0; i<args.length; i++) {
			String a = args[i];
			if (a.equals("-i")) in = args[++i];
			else if (a.equals("-o")) out = args[++i];
			else if (a.equals("-resources")) resources = args[++i];
			else if (a.equals("-gap")) gap = Integer.parseInt(args[++i]);
			else if (a.equals("-list")) list = true;
			else {
				usage();
				System.exit(1);
			}
		}

		NounPhraseRecogniser npr = new NounPhraseRecogniser();
		npr.n = gap;

		NounPhraseFileProcessor npfp = new NounPhraseFileProcessor(npr);
		npfp.resourceDir = resources;

		npr.readModelsFromFile(npfp.open("np_rules.tsv"));
		npr.readConstraintsFromFile(npfp.open("np_constraints.tsv"));

		if (list) {
			PrintStream ps = new PrintStream(System.out, true, "UTF-8");
			for (NounPhraseModel m : npr.nms) {
				ps.println(m.id + "\t" + m.getLength() + "\t" + m.name + "\t" + m.modelstring + "\t" + m.agreement);
			}
			ps.println("Constraints: " + npr.constraints.active);
			return;
		}

		if (in == null || out == null) {
			usage();
			System.exit(1);
		}

		File fin = new File(in);
		File fout = new File(out);
		ArrayList<File> files = new ArrayList<File>();

		if (fin.isDirectory()) {
			File[] all = fin.listFiles();
			Arrays.sort(all);
			for (File f : all) {
				if (f.getName().endsWith(".conllu") || f.getName().endsWith(".conllup")) files.add(f);
			}
			fout.mkdirs();
		} else files.add(fin);

		for (File f : files) {
			File o = fout;
			if (fin.isDirectory()) o = new File(fout, f.getName().replaceAll("\\.conllup?$", "") + ".conllup");
			npfp.processFile(f, o);
		}

		System.err.println("NounPhraseRecogniser " + VERSION + ", rules: " + npr.nms.size()
				+ ", constraints: " + npr.constraints.active.size());
		System.err.println("Files: " + files.size() + ", sentences: " + npfp.sentences
				+ ", phrases: " + npfp.phrases + " " + npfp.categories);
	}
}
