package bg.bas.dcl.anaphora;

public class SubjectRule {

	public String id = "";
	public String type = "";
	public String person = "-";
	public String number = "-";
	public String[] conditions = new String[0];

	public boolean readRuleFromString(String l){

		String[] data = l.split("\t");

		// data[0] is the id
		// data[1] is the type
		// data[2] person, data[3] number, data[4] conditions
		if (data.length < 5) return false;

		id = data[0].trim();
		type = data[1].trim();
		person = data[2].trim();
		number = data[3].trim();
		if (!data[4].trim().equals("-")) conditions = data[4].trim().split(",");

		return true;
	}

	public boolean hasLemmaCondition(){
		for (String c : conditions) {
			if (c.startsWith("LEMMA=")) return true;
		}
		return false;
	}
}
