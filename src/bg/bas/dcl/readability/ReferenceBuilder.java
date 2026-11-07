package bg.bas.dcl.readability;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Scanner;

public class ReferenceBuilder {

	// reference values per grade group from annotated texts named gradeNN_*.conllup; grades 10 and 12 form one group
	public static void build(File folder, File metadata, File out, FeatureExtractor fe,
			LinkedHashMap<String,Lexicon> lexicons) throws Exception {

		HashSet<String> excluded = new HashSet<String>();
		if (metadata != null) {
			Scanner s = new Scanner(metadata, "UTF-8");
			while (s.hasNextLine()) {
				String[] d = s.nextLine().split("\t");
				if (d.length > 7 && d[7].trim().equals("не")) excluded.add(d[0].trim());
			}
			s.close();
		}

		LinkedHashMap<String,HashMap<String,ArrayList<Double>>> values = new LinkedHashMap<String,HashMap<String,ArrayList<Double>>>();
		HashMap<String,Integer> texts = new HashMap<String,Integer>();

		File[] files = folder.listFiles();
		Arrays.sort(files);

		for (File f : files) {

			String name = f.getName().replaceAll("\\.conllup?$", "");
			if (!name.startsWith("grade") || excluded.contains(name)) continue;

			int grade = Integer.parseInt(name.substring(5, 7));
			String group = grade >= 10 ? "10" : String.valueOf(grade);

			TextDocument doc = new TextDocument();
			doc.read(f);
			TextFeatures tf = fe.extract(doc, lexicons);

			Integer n = texts.get(group);
			texts.put(group, n == null ? 1 : n+1);

			for (String k : tf.values.keySet()) add(values, k, group, tf.values.get(k));
			for (String k : tf.coverage.keySet()) {
				add(values, "coverage_direct_" + k, group, tf.coverage.get(k)[0]);
				add(values, "coverage_extended_" + k, group, tf.coverage.get(k)[1]);
				add(values, "coverage_tokens_" + k, group, tf.coverage.get(k)[2]);
			}
		}

		Writer w = new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8);
		w.write("# Референтни стойности по класове, изчислени върху текстовете от НВО (4., 7. и 10. клас) и ДЗИ (12. клас).\n");
		w.write("# 10. и 12. клас са една група (10). Брой текстове: " + texts + "\n");
		w.write("feature\tgroup\tmean\tp25\tp50\tp75\tn\n");

		for (String k : values.keySet()) {
			for (String g : new String[]{"4", "7", "10"}) {
				ArrayList<Double> v = values.get(k).get(g);
				if (v == null) continue;
				Collections.sort(v);
				double mean = 0;
				for (double x : v) mean += x;
				mean /= v.size();
				w.write(String.format(Locale.ROOT, "%s\t%s\t%.3f\t%.3f\t%.3f\t%.3f\t%d\n",
						k, g, mean, q(v, 0.25), q(v, 0.5), q(v, 0.75), v.size()));
			}
		}

		w.close();
	}

	static void add(LinkedHashMap<String,HashMap<String,ArrayList<Double>>> values, String k, String g, double v){
		if (!values.containsKey(k)) values.put(k, new HashMap<String,ArrayList<Double>>());
		if (!values.get(k).containsKey(g)) values.get(k).put(g, new ArrayList<Double>());
		values.get(k).get(g).add(v);
	}

	static double q(ArrayList<Double> v, double p){
		double pos = p * (v.size() - 1);
		int i = (int) Math.floor(pos);
		if (i + 1 >= v.size()) return v.get(i);
		return v.get(i) + (pos - i) * (v.get(i+1) - v.get(i));
	}
}
