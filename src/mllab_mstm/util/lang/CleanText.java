package mllab_mstm.util.lang;

import java.io.*;
import java.util.regex.*;


// DEPRECATED!!!!
public class CleanText {
	private String lang = null;
	private Pattern pw = null;
	private Pattern pc = null;
	private int minWordLen = 1;
	public CleanText(String lang, int minWordLen) {
		this.lang = lang;
		this.minWordLen = minWordLen;
		pw = Pattern.compile("[\\p{L}\\p{M}]+");//word
		/*using blocks instead of scripts since currently on java 6.
		change to scripts when shifting to java 7.*/
		if (lang.equals(LangUtil.EN)) {
// 			pc = Pattern.compile("\\P{InBasic_Latin}|\\P{InLatin-1_Supplement}");//not Latin
			pc = Pattern.compile("[^\u0000-\u007F\u0080-\u00FF]");//not Latin
		} else if (lang.equals(LangUtil.HIR) || lang.equals(LangUtil.KNR)) {//romanized Hindi or Kannada
			pc = Pattern.compile("[^\u0000-\u007F\u0080-\u00FF]");//not Latin
		} else if (lang.equals(LangUtil.HI)) {
			pc = Pattern.compile("\\P{InDevanagari}");//not Devanagari
//		} else if (lang.equals(LangUtils.BN)) {
//			pc = Pattern.compile("\\P{InBengali}");//not Bengali
		} else if (lang.equals(LangUtil.KN)) {
			pc = Pattern.compile("\\P{InKannada}");//not Kannada
			pw = Pattern.compile("(\\p{L}(\\p{M}|\u0CE6)*)+");//allow use of 0 instead of anusvara
		}
	}
	public String clean(String text) {
		StringBuilder cleantext = new StringBuilder();
		String word = "";
		Matcher m = pw.matcher(text);
		boolean first = true;
		while(m.find()) {
			word = text.substring(m.start(),m.end());
			if (word.length() >= minWordLen
				&& !pc.matcher(word).find() //skip if word contains foreign chars
				) {
				if(first) { first = false; }
				else { cleantext.append(" "); }
				cleantext.append(word);
			}
		}
		if (this.lang.equals(LangUtil.EN) || this.lang.equals(LangUtil.HIR) 
				|| this.lang.equals(LangUtil.KNR)) {
			return cleantext.toString().toLowerCase();
		} else if (this.lang.equals(LangUtil.KN)){
			//replace kannada digit 0 with anusvara
			return cleantext.toString().replace('\u0CE6', '\u0C82');
		} else if (this.lang.equals(LangUtil.HI)){
			//normalize
			return HindiUtil.getInstance().normalize(cleantext.toString());
		} else {
			return cleantext.toString();
		}
	}
	public static void main(String[] args) throws Exception {
		String cmd = args[0];
		int i=1;
		if (cmd.equals("clean")) {
			String lang = args[i++];
			int minWordLen = Integer.parseInt(args[i++]);
			//process stdin
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
			CleanText ct = new CleanText(lang, minWordLen);
			String line;
			while((line=in.readLine()) != null) {
				System.out.println(ct.clean(line));
			}
		} else if (cmd.equals("test")){
			String lang = args[i++];
			CleanText ct = new CleanText(lang, 1);
			String text = "";
			while (true) {
				text = ct.clean(text);
			}
		}
	}
}