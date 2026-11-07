package bg.bas.dcl.readability;

import java.util.LinkedHashMap;

public class Evaluation {

	public double readerGrade = 0;
	public String readerGroup = "";
	public boolean strict = false;

	public double fre = 0;
	public double textGrade = 0;
	public double readerFre = 0;

	public String lexicon = "";
	public double coverageDirect = 0;
	public double coverageExtended = 0;
	public double coverageTokens = 0;

	// feature -> лесно, приемливо, трудно, сведение
	public LinkedHashMap<String,String> categories = new LinkedHashMap<String,String>();

	public String overall = "";
}
