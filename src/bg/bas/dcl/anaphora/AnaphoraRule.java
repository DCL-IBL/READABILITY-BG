package bg.bas.dcl.anaphora;

public class AnaphoraRule {

	public String id = "";
	public String kinds = "";
	public String person = "-";
	public String strategy = "";
	public String scope = "";
	public String conditions = "";

	public boolean readRuleFromString(String l){

		String[] data = l.split("\t");

		// data[0] is the id
		// data[1] the kinds of anaphora, data[2] person
		// data[3] strategy, data[4] scope, data[5] conditions
		if (data.length < 6) return false;

		id = data[0].trim();
		kinds = "," + data[1].trim() + ",";
		person = data[2].trim();
		strategy = data[3].trim();
		scope = data[4].trim();
		conditions = data[5].trim();

		return true;
	}

	public boolean appliesTo(Mention m){

		String k = m.kind.equals("ZERO") ? "pro-drop" : m.pronounKind;

		if (!kinds.equals(",*,") && !kinds.contains("," + k + ",")) return false;
		if (!person.equals("-") && !person.equals(m.person)) return false;

		return true;
	}

	public boolean has(String c){
		return ("," + conditions + ",").contains("," + c + ",");
	}
}
