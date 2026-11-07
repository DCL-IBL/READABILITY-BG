package bg.bas.dcl.readability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class TextFeatures {

	public LinkedHashMap<String,Double> values = new LinkedHashMap<String,Double>();

	// lexicon name -> direct and extended coverage of content words, extended coverage of all words
	public LinkedHashMap<String,double[]> coverage = new LinkedHashMap<String,double[]>();

	public ArrayList<Place> places = new ArrayList<Place>();

	public HashMap<String,Integer> counts = new HashMap<String,Integer>();

	public void count(String k, int n){
		Integer c = counts.get(k);
		counts.put(k, c == null ? n : c+n);
	}

	public int get(String k){
		Integer c = counts.get(k);
		return c == null ? 0 : c;
	}
}
