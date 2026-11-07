package bg.bas.dcl.readability;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class TextDocument {

	public ArrayList<ArrayList<Token>> sentences = new ArrayList<ArrayList<Token>>();
	public ArrayList<String> sentIds = new ArrayList<String>();
	public ArrayList<String> sentTexts = new ArrayList<String>();

	public HashMap<String,Integer> columns = new HashMap<String,Integer>();

	public void read(File in) throws IOException {

		BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(in), StandardCharsets.UTF_8));

		ArrayList<Token> sent = new ArrayList<Token>();
		String id = "", text = "";
		int textpos = 0;
		boolean first = true;
		String l;

		while ((l = r.readLine()) != null) {

			if (first) {
				first = false;
				if (l.startsWith("# global.columns")) {
					String[] cols = l.substring(l.indexOf("=")+1).trim().split("\\s+");
					for (int i=0; i<cols.length; i++) columns.put(cols[i], i);
					continue;
				}
			}

			if (l.trim().equals("")) {
				if (sent.size() > 0) {
					sentences.add(sent);
					sentIds.add(id.equals("") ? String.valueOf(sentences.size()) : id);
					sentTexts.add(text);
				}
				sent = new ArrayList<Token>();
				id = "";
				text = "";
				continue;
			}

			if (l.startsWith("# sent_id")) id = l.substring(l.indexOf("=")+1).trim();
			if (l.startsWith("# text =")) text = l.substring(l.indexOf("=")+1).trim();
			if (l.startsWith("#")) continue;

			String tid = l.substring(0, l.indexOf("\t"));
			if (!tid.matches("\\d+")) continue;

			Token t = new Token(l, sent.size());
			if (t.isWord()) t.textpos = textpos++;
			sent.add(t);
		}

		if (sent.size() > 0) {
			sentences.add(sent);
			sentIds.add(id.equals("") ? String.valueOf(sentences.size()) : id);
			sentTexts.add(text);
		}

		r.close();
	}

	public int column(String name){
		Integer i = columns.get(name);
		return i == null ? -1 : i;
	}

	public Token getById(ArrayList<Token> sent, int id){
		for (Token t : sent) {
			if (t.id == id) return t;
		}
		return null;
	}
}
