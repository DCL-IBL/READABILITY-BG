package bg.bas.dcl.clausesplitter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Scanner;

import bg.bas.dcl.clausesplitter.rules.ArbitraryClauseSplittingRule;
import bg.bas.dcl.clausesplitter.rules.ClauseSplittingRule;
import bg.bas.dcl.clausesplitter.rules.ClosingPunctuationClauseSplittingRule;
import bg.bas.dcl.clausesplitter.rules.ConjunctionClauseSplittingRule;
import bg.bas.dcl.clausesplitter.rules.PunctuationClauseSplittingRule;
import bg.bas.dcl.clausesplitter.rules.SpecialVerbClauseSplittingRule;
import bg.bas.dcl.clausesplitter.rules.StrongPunctuationClauseSplittingRule;
import bg.bas.dcl.clausesplitter.rules.StrongPunctuationClauseSplittingRule.PunctuationException;

public class ClauseSplittingFileProcessor {

	// external folder with resources, null for the ones in the jar
	public String resourceDir = null;

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

				// first non-comment line is the header
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

	// d[6] is the type, d[7] compulsory (bg), d[8] comma before (bg)
	// the English file has no compulsory column - taken from the Bulgarian entries with the same id
	public ArrayList<Conjunction> readConjunctionsFromFile(String lang){

		HashMap<String,Boolean> bgCompulsory = new HashMap<String,Boolean>();
		HashMap<String,String> bgType = new HashMap<String,String>();

		for (String[] d : readTable("conj_bg.tsv")) {
			boolean c = d[7].trim().equals("да");
			if (bgCompulsory.containsKey(d[0])) c = c && bgCompulsory.get(d[0]);
			bgCompulsory.put(d[0], c);
			if (!bgType.containsKey(d[0])) bgType.put(d[0], d[6].trim());
		}

		LinkedHashMap<String,Conjunction> conj = new LinkedHashMap<String,Conjunction>();

		for (String[] d : readTable("conj_" + lang + ".tsv")) {

			if (d.length < 7 || d[0].trim().equals("")) continue;

			String id = d[0].trim();
			String type = d[6].trim();
			boolean compulsory;
			boolean comma = false;
			String sem = "";

			if (lang.equals("bg")) {
				compulsory = d[7].trim().equals("да");
				comma = d.length > 8 && d[8].trim().equals("да");
				if (d.length > 9) sem = d[9].trim();
			} else {
				if (bgType.containsKey(id) && !bgType.get(id).equals("C") && type.equals("S")) type = bgType.get(id);
				if (bgCompulsory.containsKey(id)) compulsory = bgCompulsory.get(id);
				else compulsory = !type.equals("C");
				if (d.length > 7) sem = d[7].trim();
			}

			ArrayList<String> segments = getSegments(d[1]);

			for (int k=0; k<segments.size(); k++) {

				String lemma = segments.get(k);
				// only the first part of a discontinuous conjunction can be compulsory
				boolean comp = compulsory && k == 0;
				String key = lemma + "\t" + type;

				Conjunction c = conj.get(key);
				if (c == null) {
					c = new Conjunction();
					c.id = id;
					c.lemma = lemma;
					c.parts = lemma.split(" ");
					c.type = type;
					c.semanticType = sem;
					c.isCompulsaryCO = comp;
					c.preceedingCommaCO = comma;
					c.onlyPart = k > 0;
					conj.put(key, c);
				} else if (k == 0 && c.onlyPart) {
					c.id = id;
					c.isCompulsaryCO = comp;
					c.preceedingCommaCO = comma;
					c.onlyPart = false;
				} else if (k == 0) {
					// bg: strong only if strong in all its uses; en: strong if any of its Bulgarian equivalents is
					if (lang.equals("bg")) c.isCompulsaryCO = c.isCompulsaryCO && comp;
					else c.isCompulsaryCO = c.isCompulsaryCO || comp;
					c.preceedingCommaCO = c.preceedingCommaCO || comma;
				}
				if (!c.ids.contains(id)) c.ids.add(id);
			}
		}

		ArrayList<Conjunction> res = new ArrayList<Conjunction>(conj.values());

		// longer conjunctions first
		Collections.sort(res, new Comparator<Conjunction>() {
			public int compare(Conjunction a, Conjunction b) {
				return b.size() - a.size();
			}
		});

		return res;
	}

	// "и [cl1], и [cl2]" -> "и", ", и"; "(all) the more so as" -> "all the more so as", "the more so as"
	public ArrayList<String> getSegments(String s){

		ArrayList<String> res = new ArrayList<String>();

		s = s.trim().toLowerCase();
		if (s.endsWith(".") && !s.equals("i.e.")) s = s.substring(0, s.length()-1);

		if (s.contains("(") && s.contains(")")) {
			res.addAll(getSegments(s.replaceAll("[()]", "")));
			res.addAll(getSegments(s.replaceAll("\\([^)]*\\)", "")));
			return res;
		}

		for (String seg : s.split("\\[[^\\]]*\\]")) {

			seg = seg.replace(",", " , ").replaceAll("\\s+", " ").trim();
			while (seg.startsWith(", ")) seg = seg.substring(2);
			if (seg.equals(",") || seg.equals("")) continue;

			if (!res.contains(seg)) res.add(seg);
		}

		return res;
	}

	// type -> lemmas; English types are given the Bulgarian names used in the rules
	public HashMap<String,ArrayList<String>> readSpecialVerbsFromFile(String lang){

		HashMap<String,ArrayList<String>> res = new HashMap<String,ArrayList<String>>();
		HashMap<String,String> names = new HashMap<String,String>();
		names.put("modal", "модален");
		names.put("phase", "фазов");
		names.put("auxiliary", "спомагателен");

		for (String[] d : readTable("special_verbs_" + lang + ".tsv")) {

			if (d.length < 3) continue;

			String verb = d[1].trim();
			String type = lang.equals("bg") ? d[3].trim() : d[2].trim();
			if (names.containsKey(type)) type = names.get(type);

			if (!res.containsKey(type)) res.put(type, new ArrayList<String>());
			res.get(type).add(verb);
		}

		return res;
	}

	public ArrayList<PunctuationException> readExceptionsFromFile(){

		ArrayList<PunctuationException> res = new ArrayList<PunctuationException>();

		for (String[] d : readTable("strong_punct_exceptions.tsv")) {

			if (d.length < 4) continue;

			for (String str : d[1].trim().split(" ")) {
				PunctuationException pe = new PunctuationException();
				pe.str = str;
				pe.position = d[2].trim();
				pe.sign = d[3].trim();
				res.add(pe);
			}
		}

		return res;
	}

	// order, name, scope, type, options, direction, condition, action, after
	public ArrayList<ClauseSplittingRule> readRulesFromFile(String lang,
			ArrayList<Conjunction> conj,
			HashMap<String,ArrayList<String>> specialVerbs,
			ArrayList<PunctuationException> exceptions){

		ArrayList<ClauseSplittingRule> rules = new ArrayList<ClauseSplittingRule>();

		for (String[] d : readTable("clause_rules.tsv")) {

			if (d.length < 9) continue;

			String type = d[3].trim();
			ClauseSplittingRule r = null;

			if (type.equals("CONJ")) r = new ConjunctionClauseSplittingRule(conj);
			else if (type.equals("SPECIAL_VERB_DA")) r = new SpecialVerbClauseSplittingRule(specialVerbs, lang);
			else if (type.equals("STRONG_PUNCT")) r = new StrongPunctuationClauseSplittingRule(exceptions);
			else if (type.equals("CLOSING_PUNCT")) r = new ClosingPunctuationClauseSplittingRule(conj);
			else if (type.equals("PUNCT")) r = new PunctuationClauseSplittingRule();
			else if (type.equals("GAP_END")) r = new ArbitraryClauseSplittingRule();
			else {
				System.err.println("Unknown rule type: " + type);
				continue;
			}

			r.order = d[0].trim();
			r.name = d[1].trim();
			r.scope = d[2].trim();
			r.type = type;
			r.setOptions(d[4].trim());
			r.direction = d[5].trim();
			r.condition = d[6].trim();
			r.action = d[7].trim();
			r.isEndingRule = d[8].trim().equals("stop");

			rules.add(r);
		}

		Collections.sort(rules, new Comparator<ClauseSplittingRule>() {
			public int compare(ClauseSplittingRule a, ClauseSplittingRule b) {
				return Integer.valueOf(a.order) - Integer.valueOf(b.order);
			}
		});

		return rules;
	}
}
