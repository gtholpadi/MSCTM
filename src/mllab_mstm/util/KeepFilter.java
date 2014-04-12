package mllab_mstm.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.regex.Pattern;

public class KeepFilter {
	private HashSet<String> keepwords = new HashSet<String>();
	private Pattern pat = Pattern.compile("\\s+");

	public KeepFilter(HashSet<String> keepwords) {
		this.keepwords = keepwords;
	}
	public KeepFilter(File keepfile) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(keepfile));
		String line;
		while ((line = br.readLine()) != null) {
			keepwords.add(line.trim());
		}
		br.close();
	}
	public String keep(String text) {
		StringBuilder keeptext = new StringBuilder();
		boolean first = true;
		for (String word : pat.split(text)) {
			if (keepwords.contains(word)) {
				if (first) { first = false; }
				else { keeptext.append(" "); }
				keeptext.append(word);
			}
		}
		return keeptext.toString();
	}
	public HashSet<String> getKeepWords() {
		return keepwords;
	}
	public void removeWords(HashSet<String> remWords) {
		keepwords.removeAll(remWords);
	}
}
