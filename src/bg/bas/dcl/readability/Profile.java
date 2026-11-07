package bg.bas.dcl.readability;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Scanner;

public class Profile {

	public LinkedHashMap<String,String> values = new LinkedHashMap<String,String>();

	public void read(File f) throws Exception {

		Scanner s = new Scanner(new FileInputStream(f), "UTF-8");

		while (s.hasNextLine()) {

			String l = s.nextLine();
			if (l.startsWith("\uFEFF")) l = l.substring(1);
			if (l.contains("#")) l = l.substring(0, l.indexOf("#"));
			if (!l.contains(":")) continue;

			values.put(l.substring(0, l.indexOf(":")).trim().toLowerCase(), l.substring(l.indexOf(":")+1).trim());
		}

		s.close();
	}

	public String get(String k, String def){
		String v = values.get(k);
		return v == null || v.equals("") ? def : v;
	}

	public boolean isL2(){
		return get("language", "L1").equalsIgnoreCase("L2");
	}

	public boolean isDeaf(){
		String n = get("needs", "none").toLowerCase();
		return n.contains("deaf") || n.contains("глух");
	}

	public ArrayList<String> known(){
		ArrayList<String> res = new ArrayList<String>();
		for (String k : get("known", "").split(",")) {
			if (!k.trim().equals("")) res.add(k.trim());
		}
		return res;
	}
}
