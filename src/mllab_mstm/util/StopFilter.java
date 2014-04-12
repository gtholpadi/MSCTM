package mllab_mstm.util;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class StopFilter {
	private HashSet<String> stopwords = new HashSet<String>();
	private Pattern pat = Pattern.compile("\\s+");

	public StopFilter(File stopFile) throws Exception {
		addWords(stopFile);
	}
	public StopFilter(String stopFiles) throws Exception {
		for (String fname : stopFiles.split(":")) {
			addWords(new File(fname));
		}
	}
	public void addWords(File stopFile) throws IOException {
		if (stopFile != null) {
			BufferedReader br = new BufferedReader(new FileReader(stopFile));
			String line;
			while ((line = br.readLine()) != null) {
				stopwords.add(line.trim());
			}
			br.close();
		}
	}
	public String stop(String text) {
		if (stopwords.size()==0) {
			return text;
		}
		StringBuilder stoppedtext = new StringBuilder();
		boolean first = true;
		for (String word : pat.split(text)) {
			if (!stopwords.contains(word)) {
				if (first) { first = false; }
				else { stoppedtext.append(" "); }
				stoppedtext.append(word);
			}
		}
		return stoppedtext.toString();
	}
	public HashSet<String> getStopWords() {
		return stopwords;
	}
	public static void main(String[] args) throws Exception {
		String stopfile = args[0];
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
		StopFilter sf = new StopFilter(new File(stopfile));
		String line;
		while((line=in.readLine()) != null) {
			System.out.println(sf.stop(line));
		}
		in.close();
	}
}