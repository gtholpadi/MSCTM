package mllab_mstm.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mllab_mstm.corpus.WikiUtil;
import mllab_mstm.util.Log;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;

// Simple article and comments.

public class Article {
	//Static
	public static class Comment {
		String body;
	}
	static Gson gson = new Gson();
//	static Pattern pws = Pattern.compile("\\p{Z}+");//whitespace
	static Pattern pws = Pattern.compile("\\s+");//whitespace
	static Pattern pp = Pattern.compile("\\p{P}");//punctuation
	static String lemmatizerPrefix = "#####"; //prefix for lines with metadata
	static String lemmatizerDelim = "#";
	//Instance attr
	public String aid;
	public String[] body;
	public Comment[] comments;

	public void save(File f) throws Exception {
		FileUtils.write(f, gson.toJson(this), "UTF-8");
	}
	public static Article read(File f) throws Exception {
		return gson.fromJson(FileUtils.readFileToString(f, "UTF-8"), Article.class);
	}
	public static String[] textToTokenArray(String text) {
		List<String> toks = new ArrayList<String>();
		for (String text1 : pws.split(text)) {//split on whitespace
			//need to split on punctuation, but also need to save
			//the punctuation mark (in case stemmer needs it).
			Matcher m = pp.matcher(text1);
			int pos = 0;//start scanning from this position
			while(m.find()) {//find punctuation
				int s = m.start();
				int e = m.end();
				assert e == s+1;
				if (s>pos) {//add text preceding punctuation
					toks.add(text1.substring(pos, s));
				}
				toks.add(m.group());//add punctuation mark
				pos = e;//shift current position
			}
			if (pos < text1.length()) {//add text after last punctuation
				toks.add(text1.substring(pos));
			}
		}
		return toks.toArray(new String[0]);
	}
	public void print4Lemmatizer() throws Exception {
		Log.pr(String.format("%s%s%s%d%s%d\n", lemmatizerPrefix, aid, lemmatizerDelim, 
				body.length, lemmatizerDelim, comments.length));
		for (int s=0; s<body.length; s++) {
			String[] toks = textToTokenArray(body[s]);
			Log.pr(String.format("%s%d\n", lemmatizerPrefix, toks.length));
			for (int n=0; n<toks.length; n++) {
				Log.prln(toks[n]);
			}
		}
		for (int c=0; c<comments.length; c++) {
			String[] toks = textToTokenArray(comments[c].body);
			Log.pr(String.format("%s%d\n", lemmatizerPrefix, toks.length));
			for (int m=0; m<toks.length; m++) {
				Log.prln(toks[m]);
			}
		}
	}
	public static void readLemmatizerOutput(File lemmatizerFile, File outDir, File logDir) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(lemmatizerFile));
		String line; 
//		int ln=0;
		while ((line=br.readLine())!=null) {
//			ln++;
			if (line.startsWith(lemmatizerPrefix)) {
				String[] rec = line.split("\t")[0].substring(lemmatizerPrefix.length()).split(lemmatizerDelim);
				Article a = new Article();
				a.aid = rec[0];
				int S = Integer.parseInt(rec[1]);
				int C = Integer.parseInt(rec[2]);
				a.body = new String[S];
				a.comments = new Comment[C];
				for (int s=0; s<S; s++) {
					line = br.readLine();//ln++;
					assert line.startsWith(lemmatizerPrefix);
					int N = Integer.parseInt(line.split("\t")[0].substring(lemmatizerPrefix.length()).split(lemmatizerDelim)[0]);
					StringBuilder sb = new StringBuilder();
					for (int n=0; n<N; n++) {
						line = br.readLine();//ln++;
						if (n!=0) {
							sb.append(' ');
						}
						int lastTabPos = line.lastIndexOf('\t');
						if (lastTabPos >= 0) {
							sb.append( line.substring(lastTabPos+1, line.lastIndexOf('.')) );
						} else{//do not parse, save as-is
							sb.append(line);
						}
					}
					a.body[s] = sb.toString();
				}
				for (int c=0; c<C; c++) {
					line = br.readLine();//ln++;
					assert line.startsWith(lemmatizerPrefix);
					int M = Integer.parseInt(line.split("\t")[0].substring(lemmatizerPrefix.length()).split(lemmatizerDelim)[0]);
					StringBuilder sb = new StringBuilder();
					for (int m=0; m<M; m++) {
						line = br.readLine();
						if (m!=0) {
							sb.append(' ');
						}
						int lastTabPos = line.lastIndexOf('\t');
						if (lastTabPos >= 0) {
							sb.append( line.substring(lastTabPos+1, line.lastIndexOf('.')) );
						} else {//do not parse, save as-is
							sb.append(line);
						}
					}
					a.comments[c] = new Comment();
					a.comments[c].body = sb.toString();
				}
				a.save(new File(outDir, a.aid+".json"));
				Log.set(new File(logDir, a.aid+".log"));
				a.print();
				Log.reset();
			}
		}
		br.close();
	}
	public void print() throws Exception {
		Log.prln("-----------------------");
		Log.prln("--body--BEGIN");
		for (String para : body) {
			Log.prln(para);
		}
		Log.prln("--body--END");
		Log.prln("--comments--BEGIN");
		if (comments != null) {
			for (Comment c : comments) {
				Log.prln(String.format("%s", c.body));
			}
		}
		Log.prln("--comments--END");
		Log.prln("-----------------------");
	}
	public void print(File f) throws Exception {
		Log.set(f);
		print();
		Log.reset();
	}
	private static void printlemma(File jsonDir, File lemmatizerFile) throws Exception {
		Log.set(lemmatizerFile);
		for (File f : jsonDir.listFiles()) {
			read(f).print4Lemmatizer();
		}
		Log.reset();
	}
	public static Article wiki2art(File srcWikiFile) throws IOException {
		// do not "clean"---normalization might affect lemmatizer
		Article a = new Article();
		a.aid = srcWikiFile.getName();
		List<String> abody = new ArrayList<String>();
		for (String seg : WikiUtil.getSegments(srcWikiFile)) {
			if (!seg.equals("")) {
				abody.add(seg);
			}
		}
		a.body = abody.toArray(new String[0]);
		a.comments = new Comment[0];
		return a;
	}
	public static void wiki2art(File wikiFileListFile, File tgtArtDir, File tgtLogDir) throws Exception {
		for (String fname : FileUtils.readLines(wikiFileListFile)) {
			File srcWikiFile = new File(fname);
			Article a = wiki2art(srcWikiFile);
			a.save(new File(tgtArtDir, srcWikiFile.getName() + ".json"));
			a.print(new File(tgtLogDir, srcWikiFile.getName() + ".log"));
		}
	}
	private static void printlemmawiki(File wikiFileListFile, File lemmatizerFile) throws Exception {
		Log.set(lemmatizerFile);
		for (String fname : FileUtils.readLines(wikiFileListFile)) {
			wiki2art(new File(fname)).print4Lemmatizer();
		}
		Log.reset();
	}
	public static void main(String[] args) throws Exception {
		String cmd = args[0];
		int i = 1;
		if (cmd.equals("test")) {
//			String artFile = args[i++];
//			Article a = Article.read(new File(artFile));
//			Log.prln(a.aid);
		} else if (cmd.equals("wiki2art")) {
			String wikiFileListFile = args[i++];
			String tgtArtDir = args[i++];
			String tgtLogDir = args[i++];
			wiki2art(new File(wikiFileListFile), new File(tgtArtDir), new File(tgtLogDir));
		} else if (cmd.equals("printlemma")) {
			String jsonDir = args[i++];
			String lemmatizerFile = args[i++];
			printlemma(new File(jsonDir), new File(lemmatizerFile));
		} else if (cmd.equals("readlemma")) {
			String lemmatizerFile = args[i++];
			String outDir = args[i++];
			String logDir = args[i++];
			readLemmatizerOutput(new File(lemmatizerFile), new File(outDir), new File(logDir));
		} else if (cmd.equals("printlemmawiki")) {
			String wikiFileListFile = args[i++];
			String lemmatizerFile = args[i++];
			printlemmawiki(new File(wikiFileListFile), new File(lemmatizerFile));
		}
	}
}