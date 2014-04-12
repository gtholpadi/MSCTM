package mllab_mstm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import mllab_mstm.corpus.DJArticle;
import mllab_mstm.corpus.DJArticle.DJComment;
import mllab_mstm.corpus.KPArticle;
import mllab_mstm.corpus.KPArticle.KPComment;
import mllab_mstm.corpus.KSArticle;
import mllab_mstm.corpus.KSArticle.KSComment;
import mllab_mstm.corpus.PVArticle;
import mllab_mstm.corpus.PVArticle.PVComment;
import mllab_mstm.util.StopFilter;
import mllab_mstm.util.lang.LangUtil;

import org.apache.commons.io.FileUtils;

public class Adhoc {
	// Preprocess each file in directory corpdir, assuming it is in language lang,
	// and store it in destdir. Preprocessing parameters:
	// lang: remove characters in other language scripts
	// minWordLen: remove words shorter than this length
	// stopfile : list of stopwords to remove
	public static void preprocorp(String lang, String corpdir, String destdir,
		String stopfile, int minWordLen) throws Exception {
		/* for each file in corpdir:
			sentence-segment -> clean -> stop -> save in destdir
		*/
//		SentenceSegmenter ss = new SentenceSegmenter(lang);
		LangUtil lu = LangUtil.getInstance();
//		CleanText ct = new CleanText(lang, minWordLen);
		StopFilter sf = new StopFilter(new File(stopfile));
		Pattern pe = Pattern.compile("^\\s*$");//empty line

		String line;
		for(String file : (new File(corpdir)).list()) {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(destdir, file)));
			BufferedReader br = new BufferedReader(new FileReader(new File(corpdir, file)));
			while((line = br.readLine()) != null) {
//				for (String sent : ss.segment(line)) {
				for (String sent : lu.sentencifyText(lang, line)) {
//					sent = sf.stop(ct.clean(sent));
					sent = sf.stop(lu.cleanText(lang, sent));
					if(! pe.matcher(sent).matches()) {
						bw.write(sent);
						bw.newLine();
					}
				}
			}
			br.close();
			bw.close();
		}
	}
	// Preprocess each file in directory corpdir, assuming it is in language lang,
	// and store it in destdir. Written specifically for the Hansards corpus, which
	// (sometimes) gzips files, and uses ISO8859_15 encoding (instead of Unicode).
	// Preprocessing parameters:
	// lang: remove characters in other language scripts
	// minWordLen: remove words shorter than this length
	// stopfile : list of stopwords to remove
	public static void preprocorphansard(String lang, String corpdir, String destdir,
		String stopfile, int minWordLen) throws Exception {
		/* for each file in corpdir:
			unzip -> decode using ISO-8859-15 -> clean -> stop -> save in destdir
		*/
		LangUtil lu = LangUtil.getInstance();
//		CleanText ct = new CleanText(lang, minWordLen);
		StopFilter sf = new StopFilter(new File(stopfile));
//		Pattern pe = Pattern.compile("^\\s*$");//empty line

		String line;
		for(String file : (new File(corpdir)).list()) {
			//For reading gzipped files
// 			BufferedWriter bw = new BufferedWriter(new FileWriter(
// 				new File(destdir, file.replaceFirst("\\.gz$", "") )));
// 			BufferedReader br = new BufferedReader(new InputStreamReader(
// 				new GZIPInputStream(new FileInputStream(new File(corpdir, file))), "ISO8859_15"));
			//For reading unzipped files from mldascatgath `split' dataset
			BufferedWriter bw = new BufferedWriter(new FileWriter(
				new File(destdir, file) ));
			BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(new File(corpdir, file)), "ISO8859_15"));
			while((line = br.readLine()) != null) {
				String sent = sf.stop(lu.cleanText(lang, line)).trim();
// 				if(! pe.matcher(sent).matches()) { //keep empty lines to preserve sentence alignment
					bw.write(sent);
					bw.newLine();
// 				}
			}
			br.close();
			bw.close();
		}
	}
	// Given a filename-map (which is also an id-map) and the directories where the
	// files reside, create a comparable corpus in the destination directories, with
	// using new corresponding file names, based on the index in the id-map
	public static void makecompcorp(String l1l2map, String srcdir1,
		String dstdir1, String srcdir2, String dstdir2) throws Exception {
		/* for each file1-file2 pair in l1l2mapfile,
			if srcdir1/file1 and srcdir2/file2 both exist
				increment counter
				copy srcdir1/file1 to dstdir1/counter
				copy srcdir2/file2 to dstdir2/counter
		*/
		BufferedReader br = new BufferedReader(new FileReader(l1l2map));
		String line;
		Pattern ptab = Pattern.compile("\\t");
		int lineno = 0;
		while((line=br.readLine()) != null) {
			String[] rec = ptab.split(line);
			File f1 = new File(srcdir1, rec[0]);
			File f2 = new File(srcdir2, rec[1]);
			lineno++;
			if (f1.exists() && f2.exists()) {
// 				lineno++;
				FileUtils.copyFile(f1, new File(dstdir1, String.valueOf(lineno)));
				FileUtils.copyFile(f2, new File(dstdir2, String.valueOf(lineno)));
			}
		}
		br.close();
	}
	// Replace all words in a corpus by their ids.
	public static void corpword2id(String srcdir, String dstdir, String wmfile) throws Exception {
		// Read wmfile
		BufferedReader br = new BufferedReader(new FileReader(wmfile));
		br.readLine(); //skip first line

		Pattern ptab = Pattern.compile("\\t");
		HashMap<String,String> voc = new HashMap<String,String>();
		String line;
		String[] rec;
		while ((line=br.readLine()) != null) {
			rec = ptab.split(line);
			voc.put(rec[1], rec[0]);
		}
		br.close();
		//replace word by id in corpus
		String sent;
		Pattern pws = Pattern.compile("\\s+");//whitespace
		for(String file : (new File(srcdir)).list()) {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(dstdir, file)));
			br = new BufferedReader(new FileReader(new File(srcdir, file)));
			while((line = br.readLine()) != null) {
				sent = "";
				for(String tok : pws.split(line)) {
					if (tok.equals("")) continue;
					sent += voc.get(tok) + " ";
				}
				sent = sent.trim();
				bw.write(sent); bw.newLine();
			}
			br.close();
			bw.close();
		}
	}
	// Parsing to extract dates in the file names of the Telegraph and ABP corpora.
	public static String getDate(String fname) {
		//telegraph: 1071001_nation_story_8381581.utf8
// 		Pattern pcheck = Pattern.compile("1[0-9]+_[a-z]+_story_[0-9]+\\.utf8");
		//abp: 1070101_1desh2.pc.utf8
 		Pattern pcheck = Pattern.compile("1[0-9]+.*utf8$");
		if (pcheck.matcher(fname).lookingAt()) {
		//telegraph and abp
			String date = "20" + fname.substring(1,3) + fname.substring(3,5) + fname.substring(5,7);
// 			System.out.println(fname + " "+ date);
			return date;
		} else {
			System.err.println("no match:"+fname);
			return null;
		}
	}
	// For each Telegraph/ABP document in corpdir, get its date.
	// Then count the number of articles per day in this way.
	public static void newsstats(String corpdir) throws Exception {
		HashMap<String,Integer> date2ct = new HashMap<String,Integer>();
		for (String file : (new File(corpdir)).list()) {
			String date = getDate(file);
			if (date == null) continue;
			if (!date2ct.containsKey(date)) {
				date2ct.put(date,0);
			}
			date2ct.put(date, date2ct.get(date)+1);
		}
		for (Map.Entry<String,Integer> e : date2ct.entrySet()) {
			System.out.println(String.format("%s\t%d", e.getKey(), e.getValue()));
		}
	}
	// Scan a directory containing JSON files of articles. Get all comments.
	// For each date, print number of comments for that date.
	public static void getcommstats(String artDir, String artType) throws Exception {
		DateFormat df = new SimpleDateFormat("yyyyMMdd");
		HashMap<String,Integer> date2ct = new HashMap<String,Integer>();
		String cdate;
		for (File f : (new File(artDir)).listFiles()) {
			if (artType.equals("pv")) {
				PVArticle a = PVArticle.read(f);
				for (PVComment c : a.comments) {
					cdate = df.format(c.date);
					if (!date2ct.containsKey(cdate)) {
						date2ct.put(cdate,0);
					}
					date2ct.put(cdate, date2ct.get(cdate)+1);
				}
			} else if (artType.equals("kp")) {
				KPArticle a = KPArticle.read(f);
				for (KPComment c : a.comments) {
					cdate = df.format(c.date);
					if (!date2ct.containsKey(cdate)) {
						date2ct.put(cdate,0);
					}
					date2ct.put(cdate, date2ct.get(cdate)+1);
				}
			} else if (artType.equals("dj")) {
				DJArticle a = DJArticle.read(f);
				for (DJComment c : a.comments) {
					cdate = df.format(c.date);
					if (!date2ct.containsKey(cdate)) {
						date2ct.put(cdate,0);
					}
					date2ct.put(cdate, date2ct.get(cdate)+1);
				}
			} else if (artType.equals("ks")) {
				KSArticle a = KSArticle.read(f);
				for (KSComment c : a.comments) {
					cdate = df.format(c.date);
					if (!date2ct.containsKey(cdate)) {
						date2ct.put(cdate,0);
					}
					date2ct.put(cdate, date2ct.get(cdate)+1);
				}
			}
		}
		for (Map.Entry<String,Integer> e : date2ct.entrySet()) {
			System.out.println(String.format("%s\t%d", e.getKey(), e.getValue()));
		}
	}
	public static void main(String[] args) throws Exception {
		String cmd = args[0];
		int i = 1;
		if (cmd.equals("preprocorp")) {
			String lang = args[i++];
			String corpdir = args[i++];
			String destdir = args[i++];
			String stopfile = args[i++];
			int minWordLen = Integer.parseInt(args[i++]);
			preprocorp(lang, corpdir, destdir, stopfile, minWordLen);
		} else if (cmd.equals("preprocorphansard")) {
			String lang = args[i++];
			String corpdir = args[i++];
			String destdir = args[i++];
			String stopfile = args[i++];
			int minWordLen = Integer.parseInt(args[i++]);
			preprocorphansard(lang, corpdir, destdir, stopfile, minWordLen);
		} else if (cmd.equals("makecompcorp")) {
			String l1l2map = args[i++];
			String srcdir1 = args[i++];
			String dstdir1 = args[i++];
			String srcdir2 = args[i++];
			String dstdir2 = args[i++];
			makecompcorp(l1l2map, srcdir1, dstdir1, srcdir2, dstdir2);
		} else if (cmd.equals("corpword2id")) {
			String srcdir = args[i++];
			String dstdir = args[i++];
			String wmfile = args[i++];
			corpword2id(srcdir, dstdir, wmfile);
		} else if (cmd.equals("newsstats")) {
			String corpdir = args[i++];
			newsstats(corpdir);
		} else if (cmd.equals("getcommstats")) {
			String artDir = args[i++];
			String artType = args[i++];
			getcommstats(artDir, artType);
		} else if (cmd.equals("testing")) {
			double[] qwe = {1., 2., 3.};
			System.out.println(String.valueOf(qwe[0]));
		} else {
			System.out.println("invalid command");
		}
	}
}