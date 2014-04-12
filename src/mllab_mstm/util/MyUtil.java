package mllab_mstm.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import mllab_mstm.util.lang.LangUtil;

import org.jsoup.Jsoup;

public class MyUtil {
	static Pattern pws = Pattern.compile("\\s+");
	static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	
	public static String[][] getLines(File f) throws Exception {
		return getLines(f, null);
	}

	public static String[][] getLines(File f, String sepRegex) throws Exception {
		// read file into array of lines, each line is an array of strings
		// (tokens)
		// tokens are sepRegex-separated; empty lines are skipped
		Pattern psep;
		if (sepRegex == null) {
			psep = pws;
		} else {
			psep = Pattern.compile(sepRegex);
		}
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		String[] toks;
		List<String[]> sents = new ArrayList<String[]>();
		while ((line = br.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			}
			toks = psep.split(line);
			if (toks.length == 0 || toks[0].equals("")) {
				continue;
			}
			sents.add(toks);
		}
		br.close();
		return sents.toArray(new String[0][0]);
	}

	public static String[][] getArrayFromFile(File f, String sepRegex)
			throws Exception {
		// read file into array of lines, each line is an array of strings
		// entries are sepRegex-separated
		Pattern psep;
		if (sepRegex == null) {
			psep = pws;
		} else {
			psep = Pattern.compile(sepRegex);
		}
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		List<String[]> rows = new ArrayList<String[]>();
		while ((line = br.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			}
			rows.add(psep.split(line, -1));//-1 => do not drop trailing empty strings
		}
		br.close();
		return rows.toArray(new String[0][0]);
	}

	public static String[] getDoc(File f) throws Exception {
		return getDoc(f, null, null);
	}

	// read file into array of tokens,
	public static String[] getDoc(File f, String type, String stopFile)
			throws Exception {
		if (type == null) {
			// tokens are whitespace-separated
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line;
			String[] toks;
			List<String> doc = new ArrayList<String>();
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}
				toks = pws.split(line);
				if (toks.length == 0 || toks[0].equals("")) {
					continue;
				}
				for (String tok : toks) {
					doc.add(tok);
				}
			}
			br.close();
			return doc.toArray(new String[0]);
		} else if (type.equals("tg")) {
			LangUtil lu = LangUtil.getInstance();
//			CleanText ct = new CleanText("en", 3);
			StopFilter sf = new StopFilter(new File(stopFile));
//			return pws.split(sf.stop(ct.clean(Jsoup.parse(f, "UTF-8")
//					.select("text").text())));
			return pws.split(sf.stop(lu.cleanStopText("en",Jsoup.parse(f, "UTF-8")
					.select("text").text())));
		} else if (type.equals("abp")) {
			LangUtil lu = LangUtil.getInstance();
//			CleanText ct = new CleanText("bn", 1);
			StopFilter sf = new StopFilter(new File(stopFile));
//			return pws.split(sf.stop(ct.clean(Jsoup.parse(f, "UTF-8")
//					.select("text").text())));
			return pws.split(sf.stop(lu.cleanStopText("bn",Jsoup.parse(f, "UTF-8")
					.select("text").text())));
		} else {
			return null;
		}
	}

	public static Set<String> getWordListSet(String wordListFile)
			throws Exception {
		Set<String> words = new HashSet<String>();
		BufferedReader br = new BufferedReader(new FileReader(wordListFile));
		String line;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			}
			words.add(line.trim());
		}
		br.close();
		return words;
	}

	public static List<String> getWordListList(String wordListFile)
			throws Exception {
		List<String> words = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(wordListFile));
		String line;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			}
			words.add(line.trim());
		}
		br.close();
		return words;
	}

	public static class DoubleArrayComparator implements Comparator<Integer> {
		Double[] arr = null;

		public DoubleArrayComparator(Double[] arr) {
			this.arr = arr;
		}

		@Override
		public int compare(Integer i1, Integer i2) {
			if (arr[i2] < arr[i1])
				return -1;
			else if (arr[i2] > arr[i1])
				return 1;
			else
				return 0;
		}
	}

	public static Integer[] sortIndexes(Double[] arr) {
		Integer[] idx = new Integer[arr.length];
		for (int i = 0; i < idx.length; i++) {
			idx[i] = i;
		}
		Arrays.sort(idx, new DoubleArrayComparator(arr));
		return idx;
	}

	public static int getTopEntries(Double[] arr, Integer[] idx,
			double sumThresh, int maxEntries, boolean doNormalization) {

		int i;
		double tot;
		// normalize in-place!!
		if (doNormalization) {
			tot = 0;
			for (i = 0; i < arr.length; i++) {
				tot += arr[i];
			}
			for (i = 0; i < arr.length; i++) {
				arr[i] /= tot;
			}
		}
		// get top entries
		if (maxEntries > idx.length)
			maxEntries = idx.length;
		tot = 0;
		for (i = 0; i < maxEntries; i++) {
			tot += arr[idx[i]];
			if (tot >= sumThresh) {
				i++;
				break;
			}
		}
		return i; // stop before this
		// int tillidx = i;
		// List<String> vec = new ArrayList<String>();
		// for (i=0; i<tillidx; i++) {
		// vec.add(idx[i] + " " + arr[idx[i]]);
		// }
		// return StringUtils.join(vec, "\t");
	}

	public static HashMap<String, Integer> reverseStringArray(String[] arr) {
		HashMap<String, Integer> s2id = new HashMap<String, Integer>(arr.length);
		for (int i = 0; i < arr.length; i++) {
			s2id.put(arr[i], i);
		}
		return s2id;
	}

	public static int maxInt(int[] a) {
		int max = a[0];
		for (int i = 1; i < a.length; i++) {
			if (a[i] > max) {
				max = a[i];
			}
		}
		return max;
	}

	public static int sumInt(int[] a) {
		int tot = 0;
		for (int i = 0; i < a.length; i++) {
			tot += a[i];
		}
		return tot;
	}
	public static int sumInt(int[][] a) {
		int tot = 0;
		for (int i = 0; i < a.length; i++) {
			tot += sumInt(a[i]);
		}
		return tot;
	}

	public static void l2normalize(double[] a) {
		double sumOfSq = 0;
		for (int i = 0; i < a.length; i++) {
			sumOfSq += a[i] * a[i];
		}
		if (sumOfSq != 0) {
			for (int i = 0; i < a.length; i++) {
				a[i] /= sumOfSq;
			}
		}
	}

	public static void l1normalize(double[] a) {
		double tot = sumDouble(a);
		if (tot != 0) {
			for (int i = 0; i < a.length; i++) {
				a[i] /= tot;
			}
		}
	}

	public static double dotProduct(double[] v1, double[] v2) {
		double dp = 0;
		for (int i = 0; i < v1.length; i++) {
			dp += v1[i] * v2[i];
		}
		return dp;
	}

	public static double avgInt(int[] arr) {
		return arr.length==0 ? 0 : sumInt(arr)/arr.length;
	}
	public static void saveToGzippedTextFile(String s, File f) throws Exception {
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				new GZIPOutputStream(new FileOutputStream(f)), "UTF-8"));
		bw.write(s);
		bw.close();
	}

	public static String readGzippedTextFile(File f)
			throws UnsupportedEncodingException, FileNotFoundException,
			IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new GZIPInputStream(new FileInputStream(f)), "UTF-8"));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line).append("\n");
		}
		br.close();
		return sb.toString();
	}

	public static double sumDouble(double[] ds) {
		double tot = 0;
		for (int i = 0; i < ds.length; i++) {
			tot += ds[i];
		}
		return tot;
	}
	public static double sumDouble(double[][] ds) {
		double tot = 0;
		for (int i = 0; i < ds.length; i++) {
			tot += sumDouble(ds[i]);
		}
		return tot;
	}
    public static double logGamma(double x) {
    	return Gamma.logGamma(x);
    }
    public static double gamma(double x) {
    	return Gamma.gamma(x);
    }
	// Input  : Comma-separated filename list
	// Output : Array of File objects (possibly null)
	public static File[] commaSepList2FileArray(String fileNames) {
		String[] fileNames1 = fileNames.split(",");
		int L = fileNames1.length;
		File[] files = new File[L];
		for (int l=0; l<L; l++) {
			if (fileNames1[l].equals("NONE")) {
				files[l] = null;
			} else {
				files[l] = new File(fileNames1[l]);
			}
		}
		return files;
	}
	public static double[] getCumProb(double[] p) {
		int N = p.length;
		double[] cp = new double[N];
		cp[0] = p[0];
		for (int i=1; i<N; i++) {
			cp[i] = cp[i-1] + p[i];
		}
		double tot=cp[N-1];
		for (int i=0; i<N; i++) {
			cp[i] /= tot;
		}		
		return cp;
	}
	public static double[] multVectorByScalar(double[] v, double s) {
		int N = v.length;
		double[] vs = new double[N];
		for (int i=0; i<N; i++) {
			vs[i] = v[i] * s;
		}
		return vs;
	}
	public static Date getDate(String date) throws ParseException {
		return df.parse(date);
	}
	public static double[] strToDoubleArr(String str, String regex) {
		String[] sarr = str.split(regex);
		double[] darr = new double[sarr.length];
		for (int i=0; i<sarr.length; i++) {
			darr[i] = Double.parseDouble(sarr[i]);
		}
		return darr;
	}
	public static String joinInt(int[] arr, String delim) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<arr.length; i++) {
			if (i!=0) sb.append(delim);
			sb.append(String.valueOf(arr[i]));
		}
		return sb.toString();
	}
	public static void main(String[] args) throws Exception {
		String cmd = args[0];
		if (cmd.equals("test")) {
			//
		}
	}
}