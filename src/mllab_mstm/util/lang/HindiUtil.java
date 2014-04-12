package mllab_mstm.util.lang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mllab_mstm.util.lang.LangUtil.PatMap;
import mllab_mstm.util.lang.LangUtil.StrMap;

public class HindiUtil implements CleansText, DetectsLang, SentencifiesText {
	static HindiUtil instance;
	//data structures for normalization
	HashMap<Character, Character> normCharMap;
	List<StrMap> canonStrMap;
	List<PatMap> canonPatMapAnu;
	//data structures for cleaning
	private Pattern pw;
	private Pattern pc;
	private Pattern pdetect;
	private Pattern psentseg;
	private Pattern pnoise;
	private int minWordLen;

	private HindiUtil() {
		//generic canonicalization string map---DO NOT CHANGE the order of application!
		canonStrMap = new ArrayList<StrMap>();
		//marks
		canonStrMap.add(new StrMap("\u093E\u0945", "\u0949"));//AA mark + CHANDRA mark to VOWEL CHANDRA mark
		canonStrMap.add(new StrMap("\u093E\u0946", "\u094A"));//AA mark + E mark to O mark
		canonStrMap.add(new StrMap("\u093E\u0947", "\u094B"));//AA mark + EE mark to OO mark
		canonStrMap.add(new StrMap("\u093E\u0948", "\u094C"));//AA mark + AI mark to AU mark
		//letters
		canonStrMap.add(new StrMap("\u0905\u0946", "\u0904"));//A + E mark to short A
		canonStrMap.add(new StrMap("\u0905\u093E", "\u0906"));//A + AA mark to AA
		canonStrMap.add(new StrMap("\u0932\u0943", "\u090C"));//L + RR mark to LRR
		canonStrMap.add(new StrMap("\u0905\u094B", "\u0913"));//A + O mark to O
		canonStrMap.add(new StrMap("\u090F\u0945", "\u090D"));//EE + CHANDRA mark to CHANDRA EE 
		canonStrMap.add(new StrMap("\u090F\u0946", "\u090E"));//EE + short E mark to short E 
		canonStrMap.add(new StrMap("\u090F\u0947", "\u0910"));//EE + E mark to AI
		canonStrMap.add(new StrMap("\u0905\u0949", "\u0911"));//A + VOWEL CHANDRA mark to VOWEL CHANDRA
		canonStrMap.add(new StrMap("\u0905\u094A", "\u0912"));//A + short O mark to short O 
		canonStrMap.add(new StrMap("\u0905\u094B", "\u0913"));//A + O mark to O 
		canonStrMap.add(new StrMap("\u0905\u094C", "\u0914"));//A + AU mark to AU 
		canonStrMap.add(new StrMap("\u0950", "\u0913\u0902"));//om character to OO + ANUSVARA mark
		
		//normalization char map---using \u200C (zwnj) as placeholder for characters to be removed
		normCharMap = new HashMap<Character, Character>();
		normCharMap.put('\u0900', '\u0902');//INVERTED CHANDRA ANUSVARA to ANUSVARA
		normCharMap.put('\u0901', '\u0902');//CHANDRA ANUSVARA to ANUSVARA
		normCharMap.put('\u090D', '\u090F');
		normCharMap.put('\u0911', '\u0906');
		normCharMap.put('\u0912', '\u0913');
		normCharMap.put('\u0929', '\u0928');
		normCharMap.put('\u0931', '\u0930');
		normCharMap.put('\u0934', '\u0933');
		normCharMap.put('\u093C', '\u200C');
		normCharMap.put('\u0945', '\u200C');
		normCharMap.put('\u0946', '\u0947');
		normCharMap.put('\u0949', '\u093E');
		normCharMap.put('\u094A', '\u094B');
		normCharMap.put('\u094F', '\u094C');
		normCharMap.put('\u0951', '\u200C');
		normCharMap.put('\u0952', '\u200C');
		normCharMap.put('\u0955', '\u200C');
		normCharMap.put('\u0958', '\u0915');
		normCharMap.put('\u0959', '\u0916');
		normCharMap.put('\u095A', '\u0917');
		normCharMap.put('\u095B', '\u091C');
		normCharMap.put('\u095C', '\u0921');
		normCharMap.put('\u095D', '\u0922');
		normCharMap.put('\u095E', '\u092B');
		normCharMap.put('\u095F', '\u092F');
		normCharMap.put('\u0972', '\u0905');
		normCharMap.put('\u0975', '\u0914');
		normCharMap.put('\u0976', '\u0905');
		normCharMap.put('\u0977', '\u0905');
		normCharMap.put('\u0979', '\u091C');
		normCharMap.put('\u097A', '\u092F');
		normCharMap.put('\u097B', '\u0917');
		normCharMap.put('\u097C', '\u091C');
		normCharMap.put('\u097E', '\u0921');
		normCharMap.put('\u097F', '\u092C');
		normCharMap.put('\u00A0', ' ');//replace non-breaking space with space
		normCharMap.put('\u200D', '\u200C');//remove zwj
		
		//canonicalization string map for Anusvara's
		// - replace anusvara followed by vyanjana to corresponding anunasika
		// - assumes generic canonicalization and normalization is already done
		canonPatMapAnu = new ArrayList<PatMap>();
		canonPatMapAnu.add(new PatMap(Pattern.compile("(\u0902)([\u0915\u0916\u0917\u0918])"), "\u0919\u094D$2"));// ka-varga 
		canonPatMapAnu.add(new PatMap(Pattern.compile("(\u0902)([\u091A\u091B\u091C\u091D])"), "\u091E\u094D$2"));// cha-varga 
		canonPatMapAnu.add(new PatMap(Pattern.compile("(\u0902)([\u091F\u0920\u0921\u0922])"), "\u0923\u094D$2"));// Ta-varga 
		canonPatMapAnu.add(new PatMap(Pattern.compile("(\u0902)([\u0924\u0925\u0926\u0927])"), "\u0928\u094D$2"));// ta-varga 
		canonPatMapAnu.add(new PatMap(Pattern.compile("(\u0902)([\u092A\u092B\u092C\u092D])"), "\u092E\u094D$2"));// pa-varga 
		
		//patterns for cleaning text
		pw = Pattern.compile("(\\p{L}\\p{M}*)+");//word
		pc = Pattern.compile("\\P{InDevanagari}");//not Devanagari (change to scripts (instead of block) in java 7)
		minWordLen = 1;
		//pattern for detect
		pdetect = Pattern.compile("\\p{InDevanagari}");
		//pattern for sentence segmentation
		psentseg = Pattern.compile("।|\\.");//kurt cobain in hiwiki uses '.'
		//patterns for noise character sequences that can be removed/replaced immediately
		//zero-width joiner and non-joiner, non-breaking whitespace
		pnoise = Pattern.compile("\u200D|\u200C");
	}
	public static HindiUtil getInstance() {
		if (instance == null) {
			instance = new HindiUtil();
		}
		return instance;
	}
	// equivalence class code point sequences that give the same grapheme
	public String canonicalize(String text) {
		//assumes ZWJ,ZWNJ,non-breaking whitespace are removed
		for (StrMap sm : canonStrMap) {
			text = text.replace(sm.from, sm.to);
		}
		return text;
	}
	// equivalence class characters likely to be mistyped---e.g. pha and fa 
	public String charNormalize(String text) {
		char[] ctext = text.toCharArray();
		for (int i=0; i<ctext.length; i++) {
			if (normCharMap.containsKey(ctext[i])) {
				ctext[i] = normCharMap.get(ctext[i]);
			}
		}
		text = (new String(ctext)).replace("\u200C", "");//remove placeholder		
		return text;
	}
	//replace anusvara by anunasika depending on following vyanjana
	public String canonicalizeAnusvara(String text) {
		//assumes generic canonicalization and normalization is already done
		for (PatMap pm : canonPatMapAnu) {
			text = pm.pat.matcher(text).replaceAll(pm.repl);
		}
		return text;
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
		//canonicalize
		text = canonicalize(text);
		//normalize
		text = charNormalize(text);
		//canonicalize anusvara
		text = canonicalizeAnusvara(text);
		
		return text;
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
	public static void main(String[] args) throws IOException {
		HindiUtil hu = HindiUtil.getInstance();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
		String line;
		while((line=br.readLine()) != null) {
			System.out.println(hu.normalize(line));
		}
		br.close();
	}
}
