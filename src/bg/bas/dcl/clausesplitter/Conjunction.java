package bg.bas.dcl.clausesplitter;

import java.util.ArrayList;

public class Conjunction {

	public String id = "";
	public String lemma = "";
	public String[] parts = new String[0];

	// C coordinating, S subordinating, R relative, Q interrogative
	public String type = "";
	public String semanticType = "";

	public boolean isCompulsaryCO = false;
	public boolean preceedingCommaCO = false;
	// found only as a later part of a discontinuous conjunction
	public boolean onlyPart = false;

	public ArrayList<String> ids = new ArrayList<String>();

	public int size() {
		return parts.length;
	}

	public boolean isSubordinate() {
		return !type.startsWith("C");
	}
}
