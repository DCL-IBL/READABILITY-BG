package bg.bas.dcl.anaphora;

public class PronounForm {

	public String form = "";
	public String kind = "";
	public String person = "";
	public String gender = "";
	public String number = "";

	public String getType() {
		if (kind.startsWith("лично")) return "Personal";
		if (kind.equals("възвратно")) return "Reflexive";
		if (kind.equals("възвратно_притежателно")) return "ReflexivePossessive";
		if (kind.startsWith("притежателно")) return "Possessive";
		if (kind.equals("показателно")) return "Demonstrative";
		if (kind.startsWith("относително")) return "Relative";
		return kind;
	}
}
