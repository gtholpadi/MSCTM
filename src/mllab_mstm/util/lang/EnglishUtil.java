package mllab_mstm.util.lang;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnglishUtil implements CleansText, DetectsLang, SentencifiesText {
	private static EnglishUtil instance;
	//data structures for cleaning
	protected Pattern pw;
	protected Pattern pc;
	protected Pattern pdetect;
	protected Pattern psentseg;
	private Pattern pnoise;
	protected int minWordLen;

	protected EnglishUtil() {
		//patterns for cleaning text
		pw = Pattern.compile("(\\p{L}\\p{M}*)+");//word
		pc = Pattern.compile("[^\u0000-\u007F\u0080-\u00FF]");//not Latin (change to scripts (instead of block) in java 7)
		minWordLen = 1;
		//pattern for detect
		pdetect = Pattern.compile("[\u0000-\u007F\u0080-\u00FF]");
		//pattern for sentence segmentation
		psentseg = Pattern.compile("\\.");
		//patterns for noise character sequences that can be removed/replaced immediately
		//zero-width joiner and non-joiner, non-breaking whitespace
		pnoise = Pattern.compile("\u200D|\u200C");
	}
	public static EnglishUtil getInstance() {
		if (instance == null) {
			instance = new EnglishUtil();
		}
		return instance;
	}
	public String removeNoise(String text) {
		//remove noise characters listed in pnoise
		text = pnoise.matcher(text).replaceAll("");
		//replace non-breaking space with normal space
		text = text.replace('\u00A0', ' ');
		return text;
	}
	public String normalize(String text) {
		//remove noise
		text = removeNoise(text);
		//lowercase
		return text.toLowerCase();
	}
	@Override
	public String cleanText(String text) {
		//normalize
		text = normalize(text);
		//extract tokens/words
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
		return cleantext.toString();
	}
	@Override
	public boolean detectLang(String text) {
		return pdetect.matcher(text).find();
	}
	@Override
	public String[] sentencifyText(String text) {
		return psentseg.split(text);
	}
}
