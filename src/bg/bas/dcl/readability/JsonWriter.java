package bg.bas.dcl.readability;

import java.util.ArrayList;
import java.util.Locale;

public class JsonWriter {

	public static String str(String s){
		StringBuilder sb = new StringBuilder("\"");
		for (char c : s.toCharArray()) {
			if (c == '"' || c == '\\') sb.append('\\').append(c);
			else if (c == '\n') sb.append("\\n");
			else if (c == '\t') sb.append("\\t");
			else if (c < 32) sb.append(String.format("\\u%04x", (int) c));
			else sb.append(c);
		}
		return sb.append("\"").toString();
	}

	public static String num(double d){
		if (Double.isNaN(d) || Double.isInfinite(d)) return "null";
		return String.format(Locale.ROOT, "%.3f", d);
	}

	public static String toJson(String name, TextDocument doc, TextFeatures tf, Evaluation e, Profile p){

		StringBuilder sb = new StringBuilder("{\n");

		sb.append("\"name\": ").append(str(name)).append(",\n");
		sb.append("\"counts\": {");
		ArrayList<String> items = new ArrayList<String>();
		for (String k : tf.counts.keySet()) items.add(str(k) + ": " + tf.counts.get(k));
		sb.append(String.join(", ", items)).append("},\n");

		sb.append("\"features\": {");
		items.clear();
		for (String k : tf.values.keySet()) items.add(str(k) + ": " + num(tf.values.get(k)));
		sb.append(String.join(", ", items)).append("},\n");

		sb.append("\"coverage\": {");
		items.clear();
		for (String k : tf.coverage.keySet()) items.add(str(k) + ": [" + num(tf.coverage.get(k)[0]) + ", " + num(tf.coverage.get(k)[1]) + ", " + num(tf.coverage.get(k)[2]) + "]");
		sb.append(String.join(", ", items)).append("},\n");

		sb.append("\"places\": [\n");
		items.clear();
		for (Place pl : tf.places) {
			ArrayList<String> t = new ArrayList<String>();
			for (int i : pl.tokens) t.add(String.valueOf(i));
			items.add("  {\"key\": " + str(pl.key) + ", \"s\": " + pl.sentence + ", \"t\": [" + String.join(",", t) + "], \"detail\": "
					+ str(pl.detail) + (pl.lexicon.equals("") ? "" : ", \"lexicon\": " + str(pl.lexicon)) + "}");
		}
		sb.append(String.join(",\n", items)).append("\n],\n");

		// words with spacing, for display
		sb.append("\"sentences\": [\n");
		items.clear();
		for (int s=0; s<doc.sentences.size(); s++) {
			ArrayList<String> toks = new ArrayList<String>();
			for (Token t : doc.sentences.get(s)) {
				boolean space = !t.col(9).contains("SpaceAfter=No");
				toks.add("[" + str(t.form) + "," + (space ? 1 : 0) + "]");
			}
			items.add("  {\"id\": " + str(doc.sentIds.get(s)) + ", \"tokens\": [" + String.join(",", toks) + "]}");
		}
		sb.append(String.join(",\n", items)).append("\n]");

		if (e != null) {
			sb.append(",\n\"profile\": {");
			items.clear();
			for (String k : p.values.keySet()) items.add(str(k) + ": " + str(p.values.get(k)));
			sb.append(String.join(", ", items)).append("},\n");
			sb.append("\"evaluation\": {\"overall\": ").append(str(e.overall))
				.append(", \"fre\": ").append(num(e.fre))
				.append(", \"text_grade\": ").append(num(e.textGrade))
				.append(", \"reader_grade\": ").append(num(e.readerGrade))
				.append(", \"reader_fre\": ").append(num(e.readerFre))
				.append(", \"reference_group\": ").append(str(e.readerGroup))
				.append(", \"lexicon\": ").append(str(e.lexicon))
				.append(", \"categories\": {");
			items.clear();
			for (String k : e.categories.keySet()) items.add(str(k) + ": " + str(e.categories.get(k)));
			sb.append(String.join(", ", items)).append("}}");
		}

		return sb.append("\n}\n").toString();
	}
}
