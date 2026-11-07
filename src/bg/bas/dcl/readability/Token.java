package bg.bas.dcl.readability;

public class Token {

	public int id = 0;
	public String form = "";
	public String lemma = "";
	public String upos = "";
	public String xpos = "";
	public String feats = "";
	public int head = 0;
	public String deprel = "";

	public String[] cols;

	// position in the sentence and in the text (words only)
	public int sentpos = 0;
	public int textpos = -1;

	public Token(String line, int sentpos){

		cols = line.split("\t", -1);

		id = Integer.parseInt(cols[0]);
		form = cols[1];
		lemma = cols[2];
		upos = cols[3];
		xpos = cols[4];
		feats = cols[5];
		deprel = cols[7];

		try {
			head = Integer.parseInt(cols[6]);
		} catch (NumberFormatException e){
			head = -1;
		}

		this.sentpos = sentpos;
	}

	public boolean isWord(){
		return !upos.equals("PUNCT") && !upos.equals("SYM");
	}

	public boolean isContent(){
		return upos.equals("NOUN") || upos.equals("VERB") || upos.equals("ADJ") || upos.equals("ADV");
	}

	public String lower(){
		return form.toLowerCase();
	}

	// lemma without -(се), -(си)
	public String baseLemma(){
		String l = lemma.toLowerCase();
		if (l.contains("-(")) l = l.substring(0, l.indexOf("-("));
		return l;
	}

	public String getFeat(String name){

		if (feats.equals("_") || feats.equals("")) return null;

		for (String f : feats.split("\\|")) {
			int i = f.indexOf("=");
			if (i > 0 && f.substring(0, i).equals(name)) return f.substring(i+1);
		}

		return null;
	}

	public String col(int i){
		if (i < 0 || i >= cols.length) return "_";
		return cols[i];
	}
}
