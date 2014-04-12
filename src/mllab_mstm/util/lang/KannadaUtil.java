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

public class KannadaUtil implements CleansText, DetectsLang, SentencifiesText {
	static KannadaUtil instance;
	
	//data structures for canonicalization and normalization
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

	private KannadaUtil() {
		//generic canonicalization string map
		canonStrMap = new ArrayList<StrMap>();
		//marks
		canonStrMap.add(new StrMap("\u0CC6\u0CC2", "\u0CCA"));//E mark + UU mark to O mark
		canonStrMap.add(new StrMap("\u0CCA\u0CD5", "\u0CCB"));//O mark + length mark to OO mark
		canonStrMap.add(new StrMap("\u0CC6\u0CD5", "\u0CC7"));//E mark + length mark to EE mark
		canonStrMap.add(new StrMap("\u0CC6\u0CD6", "\u0CC8"));//E mark + AI length mark to AI mark
		canonStrMap.add(new StrMap("\u0CBF\u0CD5", "\u0CC0"));//I mark + length mark to II mark
		//letters
		canonStrMap.add(new StrMap("\u0C89\u0CBE", "\u0C8A"));//U + AA mark to UU
		
		//normalization char map---using \u200C (zwnj) as placeholder for characters to be deleted
		normCharMap = new HashMap<Character, Character>();
		normCharMap.put('\u200D', '\u200C');//remove zwj characters
		normCharMap.put('\u00A0', ' ');//replace non-breaking space with space
		normCharMap.put('\u0CBC', '\u200C');//remove nukta
		
		//canonicalization string map for Anusvara's
		// - replace anusvara followed by vyanjana to corresponding anunasika
		// - assumes generic canonicalization and normalization is already done
		canonPatMapAnu = new ArrayList<PatMap>();
		canonPatMapAnu.add(new PatMap(Pattern.compile("\u0C82([\u0C95\u0C96\u0C97\u0C98])"), "\u0C99\u0CCD$1"));// ka-varga 
		canonPatMapAnu.add(new PatMap(Pattern.compile("\u0C82([\u0C9A\u0C9B\u0C9C\u0C9D])"), "\u0C9E\u0CCD$1"));// cha-varga 
		canonPatMapAnu.add(new PatMap(Pattern.compile("\u0C82([\u0C9F\u0CA0\u0CA1\u0CA2])"), "\u0CA3\u0CCD$1"));// Ta-varga 
		canonPatMapAnu.add(new PatMap(Pattern.compile("\u0C82([\u0CA4\u0CA5\u0CA6\u0CA7])"), "\u0CA8\u0CCD$1"));// ta-varga 
		canonPatMapAnu.add(new PatMap(Pattern.compile("\u0C82([\u0CAA\u0CAB\u0CAC\u0CAD])"), "\u0CAE\u0CCD$1"));// pa-varga 

		//patterns for cleaning text
		pw = Pattern.compile("(\\p{L}(\\p{M}|\u0CE6)*)+");//allow use of 0 instead of anusvara
		pc = Pattern.compile("\\P{InKannada}");//not Kannada (change to scripts (instead of block) in java 7)
		minWordLen = 1;
		//pattern for detect
		pdetect = Pattern.compile("\\p{InKannada}");
		//pattern for sentence segmentation
		psentseg = Pattern.compile("\\.");
		//patterns for noise character sequences that can be removed/replaced immediately
		//zero-width joiner and non-joiner, non-breaking whitespace
		pnoise = Pattern.compile("\u200D|\u200C");
	}
	public static KannadaUtil getInstance() {
		if (instance == null) {
			instance = new KannadaUtil();
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
		//replace 0 with anusvara
		text = text.replace('\u0CE6', '\u0C82');
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
		KannadaUtil ku = KannadaUtil.getInstance();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
		String line;
		while((line=br.readLine()) != null) {
			System.out.println(ku.normalize(line));
		}
		br.close();
	}
}
