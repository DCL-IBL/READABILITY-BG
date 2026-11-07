package bg.bas.dcl.readability;

import java.util.ArrayList;

public class Place {

	public String key = "";
	public int sentence = 0;
	public ArrayList<Integer> tokens = new ArrayList<Integer>();
	public String detail = "";

	// lexicon the place depends on (unknown words), empty otherwise
	public String lexicon = "";

	public Place(String key, int sentence, String detail){
		this.key = key;
		this.sentence = sentence;
		this.detail = detail;
	}
}
