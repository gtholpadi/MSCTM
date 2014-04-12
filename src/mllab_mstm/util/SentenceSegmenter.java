package mllab_mstm.util;

import java.io.*;
import java.util.regex.*;

// DEPRECATED!!!
public class SentenceSegmenter {
	private Pattern pd = null; //delimiter
//	private Pattern pe = Pattern.compile("^\\s*$");//empty line

	public SentenceSegmenter(String lang) {
		if (lang.equals("en") || lang.equals("fr")) {
 			pd = Pattern.compile("\\.");
		} else if (lang.equals("hi")) {
			pd = Pattern.compile("।|\\.");//kurt cobain in hi uses '.'
		} else if (lang.equals("bn")) {
			pd = Pattern.compile("।");
		} else if (lang.equals("kn")) {
			pd = Pattern.compile("\\.");
		} else {
			pd = Pattern.compile("\\.");
		}
	}
	public String[] segment(String text) {
// 	public ArrayList<String> segment(String text) {
		return pd.split(text);
/*		String[] sents = pd.split(text);
		ArrayList<String> nesents = new ArrayList<String>();//non-empty sentences
		for (String sent : sents) {
			if (! pe.matcher(sent).matches()) {
				nesents.add(sent);
			}
		}
		return nesents;*/
	}
	public static void main(String[] args) throws Exception {
		String lang = args[0];
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
		SentenceSegmenter ss = new SentenceSegmenter(lang);
		String line;
		while((line=in.readLine()) != null) {
			for (String s : ss.segment(line)) {
				System.out.println(s);
			}
		}
	}
}