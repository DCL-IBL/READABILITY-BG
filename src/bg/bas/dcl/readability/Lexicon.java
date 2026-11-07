package bg.bas.dcl.readability;

import java.util.ArrayList;
import java.util.HashSet;

public class Lexicon {

	public String name = "";
	public HashSet<String> lemmas = new HashSet<String>();
	public HashSet<String> roots = new HashSet<String>();

	ArrayList<String> prefixes = new ArrayList<String>();
	ArrayList<String> suffixes = new ArrayList<String>();

	public Lexicon(String name, ArrayList<String[]> rows, ArrayList<String[]> derivation){

		this.name = name;

		for (String[] d : derivation) {
			if (d.length < 2) continue;
			if (d[0].trim().equals("представка")) prefixes.add(d[1].trim());
			else if (!suffixes.contains(d[1].trim())) suffixes.add(d[1].trim());
		}

		// longer morphemes first
		java.util.Collections.sort(suffixes, new java.util.Comparator<String>() {
			public int compare(String a, String b) {
				return b.length() - a.length();
			}
		});
		java.util.Collections.sort(prefixes, new java.util.Comparator<String>() {
			public int compare(String a, String b) {
				return b.length() - a.length();
			}
		});

		for (String[] d : rows) {
			if (d.length < 1 || d[0].trim().equals("")) continue;
			String l = d[0].trim().toLowerCase();
			lemmas.add(l);
			roots.add(getRoot(l));
		}
	}

	public boolean contains(String lemma){
		return lemmas.contains(lemma.toLowerCase());
	}

	public boolean containsRelated(String lemma){

		String l = lemma.toLowerCase();
		if (contains(l) || roots.contains(getRoot(l))) return true;

		// изям -> ям
		for (String p : prefixes) {
			if (l.startsWith(p) && l.length() - p.length() >= 2 && contains(l.substring(p.length()))) return true;
		}

		return false;
	}

	// one prefix, then suffixes while at least 3 letters remain
	public String getRoot(String l){

		for (String p : prefixes) {
			if (l.startsWith(p) && l.length() - p.length() >= 4) {
				l = l.substring(p.length());
				break;
			}
		}

		boolean changed = true;
		while (changed) {
			changed = false;
			for (String s : suffixes) {
				if (l.endsWith(s) && l.length() - s.length() >= 3) {
					l = l.substring(0, l.length() - s.length());
					changed = true;
					break;
				}
			}
		}

		return l;
	}
}
