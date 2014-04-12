package mllab_mstm.util.lang;

import java.io.File;
import java.util.*;
import java.util.regex.*;

import mllab_mstm.util.Log;
import mllab_mstm.util.StopFilter;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

public class LangUtil {
	public static class StrMap {
		String from;
		String to;
		public StrMap(String from, String to) {
			this.from = from;
			this.to = to;
		}
	}
	public static class PatMap {
		Pattern pat;
		String repl;
		public PatMap(Pattern pat, String repl) {
			this.pat = pat;
			this.repl = repl;
		}
	}
	private static LangUtil instance;
	public static String EN = "en";
	public static String HI = "hi";
	public static String HIR = "hir";
	public static String KN = "kn";
	public static String KNR = "knr";
	public static Pattern pws = Pattern.compile("\\s+");
	//data
	Pattern pp;
	HashMap<String, CleansText> ct;
	HashMap<String, StopFilter> sf;
	HashMap<String, DetectsLang> dl;
	HashMap<String, SentencifiesText> st;
	HashMap<String,Integer> la2id;
	private LangUtil() {
		pp = Pattern.compile("[\\s\\.,\\?]+");//punctuation
		try {
			DetectorFactory.loadProfile("/home/gtholpadi/Dropbox/work/lib/langdetect-profiles");
		} catch (LangDetectException e) {
			e.printStackTrace();
		}
		sf = new HashMap<String, StopFilter>();
		ct = new HashMap<String, CleansText>();
		dl = new HashMap<String, DetectsLang>();
		st = new HashMap<String, SentencifiesText>();
		
		ct.put(KN, KannadaUtil.getInstance());
		dl.put(KN, KannadaUtil.getInstance());
		st.put(KN, KannadaUtil.getInstance());
		
		ct.put(HI, HindiUtil.getInstance());
		dl.put(HI, HindiUtil.getInstance());
		st.put(HI, HindiUtil.getInstance());
		
		ct.put(EN, EnglishUtil.getInstance());
		dl.put(EN, EnglishUtil.getInstance());
		st.put(EN, EnglishUtil.getInstance());
		
		ct.put(HIR, HindiRomanizedUtil.getInstance());
		dl.put(HIR, HindiRomanizedUtil.getInstance());
		st.put(HIR, HindiRomanizedUtil.getInstance());
		
		ct.put(KNR, KannadaRomanizedUtil.getInstance());
		dl.put(KNR, KannadaRomanizedUtil.getInstance());
		st.put(KNR, KannadaRomanizedUtil.getInstance());
	}
	public static LangUtil getInstance() {
		if (instance == null) {
			instance = new LangUtil();
		}
		return instance;
	}
	public String getLanguage(String text, String[] langs, String backupLang) throws LangDetectException {
		int L = langs.length;
		int[] numtoks = new int[L];
		String[] toks = pp.split(text);
		for (String tok : toks) {
			for (int l=0; l<L; l++) {
				if(dl.get(langs[l]).detectLang(tok)) {
					numtoks[l]++;
				}
			}
		}
		int maxct = 0, maxi = -1;
		for (int l=0; l<L; l++) {
			if (maxct < numtoks[l]) {
				maxct = numtoks[l];
				maxi = l;
			}
		}
		String lang;
		if (maxi == -1) {//unsupported language
			lang = null;
		} else {
			lang = langs[maxi];			
			try {
				if (langs[maxi].equals(LangUtil.EN)) {
					if (!getLanguage1(text).equals(LangUtil.EN)) {
						lang = backupLang;
					} else {
						lang = LangUtil.EN;
					}
				}
			} catch (LangDetectException e) {
				System.out.println("Error in getLanguage1. Ignoring ...");
			}
		}
		return lang;
	}
	public static String getLanguage1(String text) throws LangDetectException {
		Detector detector = DetectorFactory.create();
		detector.append(text.toLowerCase());//sometimes upper case text is wrongly detected as en.
        return detector.detect();
	}
	public String cleanText(String lang, String text) {
		return ct.get(lang).cleanText(text);
	}
	public String stopText(String lang, String text) {
		return sf.get(lang).stop(text);
	}
	public String cleanStopText(String lang, String text) {
		return stopText(lang, cleanText(lang, text));
	}
	public void setStopFilter(String lang, File stopFile) throws Exception {
		if (!sf.containsKey(lang)) {
			sf.put(lang, new StopFilter(stopFile));
		} else {
			sf.get(lang).addWords(stopFile);
		}
	}
	public void setStopFilters(String[] langs, File[] stopFiles) throws Exception {
		for (int i=0; i<langs.length; i++) {
			setStopFilter(langs[i], stopFiles[i]);
		}
	}
	public String[] sentencifyText(String lang, String text) {
		return st.get(lang).sentencifyText(text);
	}
	public static int numTokens(String text) {
		return pws.split(text).length;
	}
	public static void main(String[] args) throws Exception {
		@SuppressWarnings("unused")
		LangUtil lu = LangUtil.getInstance();
		String text="", la="";
		while(true) {
			text = "gujrat ki sarkar gujrat ke laik hai delhi ke liye nahi";
			la = getLanguage1(text);
			Log.prln(la);
		}
	}
}