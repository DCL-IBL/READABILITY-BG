package bg.bas.dcl.readability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class FeatureExtractor {

	static final String VOWELS = "аеиоуъюяѝ";

	// predominantly abstract nouns (abstract_nouns_bg.tsv)
	public HashSet<String> abstractNouns = new HashSet<String>();

	// conjunction id -> type of subordinate clause (conj_bg.tsv)
	HashMap<String,String> subordinateTypes = new HashMap<String,String>();

	static final String COMPLEX_FORMS = " Pluperfect Future_in_the_Past Future_Perfect Future_Perfect_in_the_Past ";

	public void readConjunctions(ArrayList<String[]> rows){
		for (String[] d : rows) {
			if (d.length < 11) continue;
			String t = d[10].trim();
			String type = "compl";
			if (t.startsWith("определително") || d[6].trim().equals("R")) type = "rel";
			else if (t.startsWith("обстоятелствено")) type = "adv";
			if (!subordinateTypes.containsKey(d[0].trim())) subordinateTypes.put(d[0].trim(), type);
		}
	}

	public TextFeatures extract(TextDocument doc, LinkedHashMap<String,Lexicon> lexicons){

		TextFeatures tf = new TextFeatures();

		surface(doc, tf);
		lexical(doc, tf, lexicons);
		verbs(doc, tf);
		clauses(doc, tf);
		syntax(doc, tf);
		anaphora(doc, tf);

		return tf;
	}

	int syllables(String w){
		int n = 0;
		for (char c : w.toLowerCase().toCharArray()) {
			if (VOWELS.indexOf(c) >= 0) n++;
		}
		return n;
	}

	int letters(String w){
		int n = 0;
		for (char c : w.toCharArray()) {
			if (Character.isLetter(c)) n++;
		}
		return n;
	}

	void surface(TextDocument doc, TextFeatures tf){

		int words = 0, syll = 0, lett = 0, longw = 0, poly4 = 0, poly5 = 0, longs = 0, maxs = 0, commas = 0;

		for (int s=0; s<doc.sentences.size(); s++) {

			int n = 0;

			for (Token t : doc.sentences.get(s)) {

				if (t.form.equals(",")) commas++;
				if (!t.isWord()) continue;

				int sy = syllables(t.form);
				int le = letters(t.form);
				if (le == 0) continue;

				n++;
				syll += sy;
				lett += le;
				if (le >= 8) longw++;
				if (sy >= 4) poly4++;
				if (sy >= 5) poly5++;

				if (sy >= 5) {
					Place p = new Place("place:long_word", s, t.form);
					p.tokens.add(t.sentpos);
					tf.places.add(p);
				}
			}

			words += n;
			if (n > 20) {
				longs++;
				Place p = new Place("place:long_sentence", s, n + " думи");
				tf.places.add(p);
			}
			if (n > maxs) maxs = n;
		}

		int sents = Math.max(1, doc.sentences.size());
		words = Math.max(1, words);

		double asl = (double) words / sents;
		double asw = (double) syll / words;

		tf.count("words", words);
		tf.count("sentences", doc.sentences.size());

		double lw8 = 100.0 * longw / words, pw4 = 100.0 * poly4 / words, pw5 = 100.0 * poly5 / words;

		tf.values.put("asl", asl);
		tf.values.put("asw", asw);
		tf.values.put("awl", (double) lett / words);
		tf.values.put("long_words_pct", lw8);
		tf.values.put("polysyll_pct", pw5);
		tf.values.put("long_sent_pct", 100.0 * longs / sents);
		tf.values.put("max_sent", (double) maxs);
		tf.values.put("commas_per_sent", (double) commas / sents);

		// indices calibrated for Bulgarian
		tf.values.put("fre_bg", 206.835 - 1.3 * asl - 52.5 * asw);
		tf.values.put("lix_bg", asl + lw8);
		tf.values.put("cli_bg", 0.0594 * (100.0 * lett / words) - 0.4035 * (100.0 * sents / words) - 20.33);
		tf.values.put("fog_bg", 0.4 * (asl + pw5));
		tf.values.put("smog_bg", 1.0430 * Math.sqrt(poly5 * 30.0 / sents) + 3.1291);
		tf.values.put("pisarek_bg", 0.94 * asl / 3 + 0.73 * pw4 / 3 + 1);
	}

	void lexical(TextDocument doc, TextFeatures tf, LinkedHashMap<String,Lexicon> lexicons){

		int content = 0, nouns = 0, abstr = 0;

		for (int s=0; s<doc.sentences.size(); s++) {
			for (Token t : doc.sentences.get(s)) {
				if (t.isContent()) content++;
				if (!t.upos.equals("NOUN")) continue;
				nouns++;
				if (abstractNouns.contains(t.baseLemma())) {
					abstr++;
					Place p = new Place("place:abstract_word", s, t.baseLemma());
					p.tokens.add(t.sentpos);
					tf.places.add(p);
				}
			}
		}

		tf.values.put("lex_density", 100.0 * content / Math.max(1, tf.get("words")));
		tf.values.put("abstract_pct", 100.0 * abstr / Math.max(1, nouns));

		for (Lexicon lex : lexicons.values()) {

			int n = 0, direct = 0, extended = 0;
			HashSet<String> listed = new HashSet<String>();

			for (int s=0; s<doc.sentences.size(); s++) {
				for (Token t : doc.sentences.get(s)) {

					if (!t.isContent() || t.deprel.equals("flat") || letters(t.form) == 0) continue;
					n++;

					String l = t.baseLemma();
					if (lex.contains(l)) direct++;
					if (lex.containsRelated(l)) extended++;
					else {
						Place p = new Place("place:unknown_word", s, l);
						p.tokens.add(t.sentpos);
						p.lexicon = lex.name;
						tf.places.add(p);
						listed.add(l);
					}
				}
			}

			// over all words: function words and proper names count as known
			double w = Math.max(1, tf.get("words"));
			double tokens = 100.0 * (w - (n - extended)) / w;

			n = Math.max(1, n);
			tf.coverage.put(lex.name, new double[]{100.0 * direct / n, 100.0 * extended / n, tokens});
		}
	}

	// first word of each form in DCL:VERBFORM
	void verbs(TextDocument doc, TextFeatures tf){

		int col = doc.column("DCL:VERBFORM");
		if (col < 0) return;

		int forms = 0, analytic = 0, ambiguous = 0, passive = 0, evidential = 0;

		for (int s=0; s<doc.sentences.size(); s++) {

			HashMap<String,ArrayList<Integer>> parts = new HashMap<String,ArrayList<Integer>>();
			HashMap<String,String> labels = new HashMap<String,String>();

			for (Token t : doc.sentences.get(s)) {
				String v = t.col(col);
				if (v.equals("_")) continue;
				String id = v.contains(":") ? v.substring(0, v.indexOf(":")) : v;
				if (!parts.containsKey(id)) parts.put(id, new ArrayList<Integer>());
				parts.get(id).add(t.sentpos);
				if (v.contains(":")) labels.put(id, v.substring(v.indexOf(":")+1).split(";")[0]);
			}

			for (String id : labels.keySet()) {

				String label = labels.get(id);
				ArrayList<Integer> p = parts.get(id);
				forms++;

				if (p.size() > 1) analytic++;
				if (label.contains("|")) ambiguous++;

				if (label.contains("Pass")) {
					passive++;
					tf.places.add(place("place:passive_verb", s, label, p));
				}
				if (label.contains("Ren") || label.contains("Concl") || label.contains("Dub")) {
					evidential++;
					tf.places.add(place("place:evidential_verb", s, label, p));
				}

				for (String part : label.split("[|+\\-]")) {
					if (COMPLEX_FORMS.contains(" " + part + " ")) {
						tf.places.add(place("place:complex_verb", s, label, p));
						break;
					}
				}
			}
		}

		double w = Math.max(1, tf.get("words"));

		tf.count("verb_forms", forms);
		tf.values.put("analytic_per_sent", (double) analytic / Math.max(1, doc.sentences.size()));
		tf.values.put("ambiguous_verb_pct", 100.0 * ambiguous / Math.max(1, forms));
		tf.values.put("passive_per100", 100.0 * passive / w);
		tf.values.put("evidential_per100", 100.0 * evidential / w);
	}

	Place place(String key, int s, String detail, ArrayList<Integer> tokens){
		Place p = new Place(key, s, detail);
		p.tokens.addAll(tokens);
		return p;
	}

	void clauses(TextDocument doc, TextFeatures tf){

		int col = doc.column("DCL:CLAUSE");
		if (col < 0) return;

		int clauses = 0, sub = 0, coord = 0, rel = 0, compl = 0, adv = 0;

		for (int s=0; s<doc.sentences.size(); s++) {

			int n = 0;

			for (Token t : doc.sentences.get(s)) {

				String v = t.col(col);
				if (!v.contains(":")) continue;

				n++;
				String kind = v.substring(v.indexOf(":")+1).split(";")[0];

				if (kind.equals("COORD")) coord++;
				if (!kind.equals("SUB")) continue;

				sub++;
				String type = "compl";
				if (v.contains(";conj=")) {
					String c = v.substring(v.indexOf(";conj=")+6);
					c = c.substring(0, c.indexOf(":") > 0 ? c.indexOf(":") : c.length());
					if (subordinateTypes.containsKey(c)) type = subordinateTypes.get(c);
				}
				if (type.equals("rel")) rel++;
				else if (type.equals("adv")) adv++;
				else compl++;
			}

			clauses += n;
			if (n >= 4) tf.places.add(new Place("place:many_clauses", s, n + " прости изречения"));
		}

		double sents = Math.max(1, doc.sentences.size());

		tf.count("clauses", clauses);
		tf.values.put("clauses_per_sent", clauses / sents);
		tf.values.put("sub_per_sent", sub / sents);
		tf.values.put("rel_per_sent", rel / sents);
		tf.values.put("compl_per_sent", compl / sents);
		tf.values.put("adv_per_sent", adv / sents);
		tf.values.put("coord_per_sent", coord / sents);
	}

	// tree depth and dependency distance from the UD analysis
	void syntax(TextDocument doc, TextFeatures tf){

		double depth = 0, dist = 0;
		int links = 0;

		for (ArrayList<Token> sent : doc.sentences) {

			int max = 0;

			for (Token t : sent) {

				int d = 0, h = t.head, steps = 0;
				while (h > 0 && steps++ < sent.size()) {
					d++;
					Token ht = doc.getById(sent, h);
					h = ht == null ? 0 : ht.head;
				}
				if (d > max) max = d;

				if (t.head > 0 && t.isWord()) {
					dist += Math.abs(t.id - t.head);
					links++;
				}
			}

			depth += max;
		}

		tf.values.put("tree_depth", depth / Math.max(1, doc.sentences.size()));
		tf.values.put("dep_distance", dist / Math.max(1, links));
	}

	void anaphora(TextDocument doc, TextFeatures tf){

		int col = doc.column("DCL:ANAPHORA");
		if (col < 0) return;

		// label (A1, Z3) and sentence#ID -> position in the text
		HashMap<String,Integer> positions = new HashMap<String,Integer>();
		for (int s=0; s<doc.sentences.size(); s++) {
			for (Token t : doc.sentences.get(s)) {
				int pos = t.textpos >= 0 ? t.textpos : 0;
				positions.put(doc.sentIds.get(s) + "#" + t.id, pos);
				String v = t.col(col);
				if (v.contains(":")) positions.put(v.substring(0, v.indexOf(":")), pos);
			}
		}

		int anaphora = 0, prodrop = 0, links = 0;
		double dist = 0;

		for (int s=0; s<doc.sentences.size(); s++) {
			for (Token t : doc.sentences.get(s)) {

				String v = t.col(col);
				if (!v.contains(":")) continue;

				String type = v.substring(v.indexOf(":")+1).split(";")[0];
				boolean zero = v.startsWith("Z");

				if (zero && !type.equals("Pro-drop")) continue;
				if (zero) prodrop++;
				else anaphora++;

				if (!v.contains(";ref=")) {
					tf.places.add(place("place:unresolved", s, t.form, single(t.sentpos)));
					continue;
				}

				String ref = v.substring(v.indexOf(";ref=")+5);
				if (ref.contains(";")) ref = ref.substring(0, ref.indexOf(";"));
				Integer p = positions.get(ref);
				if (p == null || t.textpos < 0) continue;

				int d = Math.abs(t.textpos - p);
				dist += d;
				links++;

				if (d > 20) tf.places.add(place("place:far_antecedent", s, t.form + " (" + d + " думи)", single(t.sentpos)));
			}
		}

		tf.values.put("anaphora_per100", 100.0 * anaphora / Math.max(1, tf.get("words")));
		tf.values.put("prodrop_pct", 100.0 * prodrop / Math.max(1, tf.get("clauses")));
		tf.values.put("antecedent_distance", links == 0 ? 0 : dist / links);
	}

	ArrayList<Integer> single(int i){
		ArrayList<Integer> l = new ArrayList<Integer>();
		l.add(i);
		return l;
	}
}
