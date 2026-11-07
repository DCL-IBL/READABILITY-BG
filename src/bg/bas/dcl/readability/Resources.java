package bg.bas.dcl.readability;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;

public class Resources {

	// external folder with resources, null for the ones in the jar
	public static String resourceDir = null;

	public static InputStream open(String name) throws Exception {

		if (resourceDir != null) {
			File f = new File(resourceDir, name);
			if (f.exists()) return new FileInputStream(f);
		}

		File f = new File(name);
		if (f.isAbsolute() || (name.contains("/") && f.exists())) return new FileInputStream(f);

		InputStream in = Resources.class.getResourceAsStream("/resources/readability/" + name);
		if (in == null) in = Resources.class.getResourceAsStream("/resources/" + name);
		if (in == null) throw new Exception("Missing resource: " + name);

		return in;
	}

	// tab-separated, # comments, first line is the header
	public static ArrayList<String[]> readTable(String name){

		ArrayList<String[]> res = new ArrayList<String[]>();

		try{

			Scanner s = new Scanner(open(name), "UTF-8");
			boolean header = true;

			while (s.hasNextLine()){

				String l = s.nextLine();
				if (l.startsWith("\uFEFF")) l = l.substring(1);
				if (l.endsWith("\r")) l = l.substring(0, l.length()-1);

				if (l.startsWith("#") || l.trim().equals("")) continue;
				if (header) {
					header = false;
					continue;
				}

				res.add(l.split("\t", -1));
			}

			s.close();
		} catch(Exception e){
			System.err.println(e.getMessage());
		}

		return res;
	}
}
