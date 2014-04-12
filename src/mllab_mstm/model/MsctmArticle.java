package mllab_mstm.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import mllab_mstm.corpus.WikiUtil;
import mllab_mstm.util.Log;
import mllab_mstm.util.MyUtil;
import mllab_mstm.util.lang.LangUtil;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;

// Simple article and comments.

class MsctmComment {
	String body;
	public MsctmComment(String body) {
		this.body = body;
	}
}
public class MsctmArticle {
	static Pattern pws = Pattern.compile("\\s+");//whitespace
	static Gson gson = new Gson();
	String artLang;
	public String[] body;
	String[] commLangs;
	MsctmComment[][] comments;

	public void save(File f) throws Exception {
		FileUtils.write(f, gson.toJson(this), "UTF-8");
	}
	public static MsctmArticle read(File f) throws Exception {
		return gson.fromJson(FileUtils.readFileToString(f, "UTF-8"), MsctmArticle.class);
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
			for (MsctmComment[] cs : comments) {
				for (MsctmComment c : cs) {
					Log.prln(String.format("%s", c.body));
				}
			}
		}
		Log.prln("--comments--END");
		Log.prln("-----------------------");
	}
	public String[] getTokens(String lang) throws Exception {
		StringBuilder sb = new StringBuilder();
		if (artLang.equals(lang)) {
			for (int s=0; s<body.length; s++) {
				sb.append(body[s]).append(" ");
			}
		}
		for (int l=0; l<commLangs.length; l++) {
			if (lang.equals(commLangs[l])) {
				for (int c=0; c<comments[l].length; c++) {
					sb.append(comments[l][c].body).append(" ");
				}
				break;
			}
		}
		String[] toks = pws.split(sb.toString().trim());
		if (toks.length == 1 && toks[0].equals("")) {
			toks = new String[0];
		}
		return toks;
	}
	public static void arstech2msctm(String artLang, List<String> srcArt,
		List<String> srcComms, File tgtArtFile, File tgtLogFile) throws Exception {
		MsctmArticle ma = new MsctmArticle();
		ma.artLang = artLang;
		ma.body = srcArt.toArray(new String[0]);
		ma.commLangs = new String[]{ artLang };
		ma.comments = new MsctmComment[1][srcComms.size()];
		for (int c=0; c < srcComms.size(); c++) {
			ma.comments[0][c] = new MsctmComment(srcComms.get(c));
		}
		ma.save(tgtArtFile);
		Log.set(tgtLogFile);
		ma.print();
		Log.reset();
	}
	public static void getarstechartcomms(String artLang, File srcArtFile, File srcCommFile,
		File tgtDir, File logDir) throws Exception {
		BufferedReader bra = new BufferedReader(new FileReader(srcArtFile));
		BufferedReader brc = new BufferedReader(new FileReader(srcCommFile));
		int doci = 1;
		bra.readLine();bra.readLine();//skip first two lines
		brc.readLine();brc.readLine();//skip first two lines
		String line;
		List<String> doc = new ArrayList<String>();
		while ( (line=bra.readLine())!=null ) {
			if (line.equals("")) {//save
				//get comments
				String comm;
				List<String> comms = new ArrayList<String>();
				while ( (comm=brc.readLine())!= null) {
					if (comm.equals("")) {
						break;
					} else {
						comms.add(comm);
					}
				}
				//save article + comments
				arstech2msctm(artLang, doc, comms,
					new File(tgtDir, String.valueOf(doci)+".json"),
					new File(logDir, String.valueOf(doci)+".log"));
				// prepare for next article
				doci++;
				doc = new ArrayList<String>();
				bra.readLine();//skip one line
				brc.readLine();//skip one line
			} else {
				doc.add(line);
			}
		}
		brc.close();
		bra.close();
	}
	public static void wiki2msctm(String artLang, String[] commLangs, File[] stopFiles,
		File srcArtFile, File tgtArtFile, File tgtLogFile) throws Exception {
		int L = commLangs.length;
		LangUtil lu = LangUtil.getInstance();
//		HashMap<String,Integer> la2id = MyUtil.reverseStringArray(commLangs);
		lu.setStopFilters(commLangs, stopFiles);

		MsctmArticle ma = new MsctmArticle();
		ma.artLang = artLang;
		List<String> abody = new ArrayList<String>();
//		String doc = FileUtils.readFileToString(srcArtFile, "UTF-8");
//		Pattern pseg = Pattern.compile("\\n\\s*\\n");
//		for (String seg : pseg.split(doc)) {
		for (String seg : WikiUtil.getSegments(srcArtFile)) {
			seg = lu.cleanStopText(artLang, seg);
			if (!seg.equals("")) {
				abody.add(seg);
			}
		}
		ma.body = abody.toArray(new String[0]);
		ma.commLangs = commLangs;
		ma.comments = new MsctmComment[L][0];

		ma.save(tgtArtFile);
		Log.set(tgtLogFile);
		ma.print();
		Log.reset();
	}
	public static void art2msctmfile(String artLang, String[] commLangs, String backupLang, File[] stopFiles,
			File srcArtFile, File tgtArtFile, File tgtLogFile, int minComments) throws Exception {
		int L = commLangs.length;
		LangUtil lu = LangUtil.getInstance();
		HashMap<String,Integer> la2id = MyUtil.reverseStringArray(commLangs);
		lu.setStopFilters(commLangs, stopFiles);
		art2msctm(artLang, commLangs, backupLang, srcArtFile, tgtArtFile, 
				tgtLogFile, minComments, L, lu, la2id);
	}
	public static void art2msctm(String artLang, String[] commLangs, String backupLang,
			File srcArtFile, File tgtArtFile, File tgtLogFile, int minComments,
			int L, LangUtil lu, HashMap<String,Integer> la2id) throws Exception {

			Article a = Article.read(srcArtFile);
			if (a.comments.length < minComments) {
				System.err.println("too few comments; skipping.");
				return;
			}

			MsctmArticle ma = new MsctmArticle();
			ma.artLang = artLang;
			List<String> abody = new ArrayList<String>();
			for (int s=0; s<a.body.length; s++) {
				String sent = lu.cleanStopText(artLang, a.body[s]);
				if (!sent.equals("")) {
					abody.add(sent);
				}
			}
			ma.body = abody.toArray(new String[0]);

			ma.commLangs = commLangs;
			List<List<MsctmComment>> acomm = new ArrayList<List<MsctmComment>>(L);
			for (int l=0; l<L; l++) {
				acomm.add(new ArrayList<MsctmComment>());
			}
			for (int c=0; c<a.comments.length; c++) {
				String body;
				String la = lu.getLanguage(a.comments[c].body, commLangs, backupLang);
				if (la != null) {//supported language
					body = lu.cleanStopText(la, a.comments[c].body);
					acomm.get(la2id.get(la)).add(new MsctmComment(body));
				}
			}
			List<MsctmComment[]> acomm1 = new ArrayList<MsctmComment[]>(L);
			for (List<MsctmComment> comml : acomm) {
				acomm1.add(comml.toArray(new MsctmComment[0]));
			}
			ma.comments = acomm1.toArray(new MsctmComment[0][0]);

			ma.save(tgtArtFile);
			Log.set(tgtLogFile);
			ma.print();
			Log.reset();
		}
	public static void art2msctmdir(String artLang, String[] commLangs, String backupLang, File[] stopFiles,
			File srcArtDir, File tgtArtDir, File tgtLogDir, int minComments) throws Exception {
		int L = commLangs.length;
		LangUtil lu = LangUtil.getInstance();
		HashMap<String,Integer> la2id = MyUtil.reverseStringArray(commLangs);
		lu.setStopFilters(commLangs, stopFiles);
		int extlen = ".json".length();//assuming .json extension for all files
		for (File f : srcArtDir.listFiles()) {
			String fname = f.getName(); 
			String aid = fname.substring(0, fname.length()-extlen);
			art2msctm(artLang, commLangs, backupLang, f, new File(tgtArtDir, aid+".json"), 
					new File(tgtLogDir, aid+".log"), minComments, L, lu, la2id);
		}
	}
	//article statistics (assuming input corplist has files in only one column)
	//art stats: for each article, the number of segments, and the number of tokens per segment.
	//com stats: for each article, for each language, the number of comments, and the average number of
	//           tokens per comment.
	public static void getArtStats(File corplistFile, File artStatsFile) throws Exception{
		String[][] filesdl = MyUtil.getArrayFromFile(corplistFile, null);
		int D = filesdl.length;
		int L = filesdl[0].length;
		int[][][] as = new int[D][L][];
		int[][][][] cs = new int[D][L][L][];
		artStatsFile.delete();
		FileUtils.writeStringToFile(artStatsFile, "#article\tno. of segments\taverage segment size" +
		"\tl1:no. of comments\taverage comment size" + "\tl2:no. of comments\taverage comment size" +
		"\tl3:no. of comments\taverage comment size\n", "UTF-8", true);
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
				if (!filesdl[d][l].equals("")) {
					StringBuilder sb = new StringBuilder();
					MsctmArticle ma = MsctmArticle.read(new File(filesdl[d][l]));
					//article stats
					int S = ma.body.length;
					as[d][l] = new int[S];
					for (int s=0; s<S; s++) {
						as[d][l][s] = LangUtil.numTokens(ma.body[s]);
					}
					sb.append(String.format("%s\t%d\t%f", filesdl[d][l], S, MyUtil.avgInt(as[d][l])));
					//comment stats
//					String[] nCom = new String[L];
//					for (int l1=0; l1<L; l1++) {
//						nCom[l1] = String.valueOf(ma.comments[l1].length);
//					}
//					Log.prln(String.format("%s\t%s", filesdl[d][l], StringUtils.join(nCom, '\t')));
					for (int l1=0; l1<L; l1++) {
						int C = ma.comments[l1].length;
						cs[d][l][l1] = new int[C];
						for (int c=0; c<C; c++) {
							cs[d][l][l1][c] = LangUtil.numTokens(ma.comments[l1][c].body);
						}
						sb.append(String.format("\t%d\t%f", C, MyUtil.avgInt(cs[d][l][l1])));
					}
					sb.append('\n');
					FileUtils.writeStringToFile(artStatsFile, sb.toString(), "UTF-8", true);
				}
			}
		}
	}
	public static void main(String[] args) throws Exception {
		String cmd = args[0];
		int i = 1;
		if (cmd.equals("art2msctm")) {
			String artLang = args[i++];
			String commLangs = args[i++];
			String backupLang = args[i++];
			String stopFiles = args[i++];
			String srcArtFile = args[i++];
			String tgtArtFile = args[i++];
			String tgtLogFile = args[i++];
			String minComments = args[i++];
			art2msctmfile(artLang, commLangs.split(","), backupLang, MyUtil.commaSepList2FileArray(stopFiles),
				new File(srcArtFile), new File(tgtArtFile), new File(tgtLogFile),
				Integer.parseInt(minComments));
		} else if (cmd.equals("art2msctmdir")) {
			String artLang = args[i++];
			String commLangs = args[i++];
			String backupLang = args[i++];
			String stopFiles = args[i++];
			String srcArtDir = args[i++];
			String tgtArtDir = args[i++];
			String tgtLogDir = args[i++];
			String minComments = args[i++];
			art2msctmdir(artLang, commLangs.split(","), backupLang, MyUtil.commaSepList2FileArray(stopFiles),
				new File(srcArtDir), new File(tgtArtDir), new File(tgtLogDir),
				Integer.parseInt(minComments));
		} else if (cmd.equals("wiki2msctm")) {
			String artLang = args[i++];
			String commLangs = args[i++];
			String stopFiles = args[i++];
			String srcArtFile = args[i++];
			String tgtArtFile = args[i++];
			String tgtLogFile = args[i++];
			wiki2msctm(artLang, commLangs.split(","), MyUtil.commaSepList2FileArray(stopFiles),
				new File(srcArtFile), new File(tgtArtFile), new File(tgtLogFile));
		} else if (cmd.equals("getarstechartcomms")) {
			String artLang = args[i++];
			String srcArtFile = args[i++];
			String srcCommFile = args[i++];
			String tgtDir = args[i++];
			String logDir = args[i++];
			getarstechartcomms(artLang, new File(srcArtFile), new File(srcCommFile),
				new File(tgtDir), new File(logDir));
		} else if (cmd.equals("getartstats")) {
			String corplistFile = args[i++];
			String artStatsFile = args[i++];
			getArtStats(new File(corplistFile), new File(artStatsFile));
		} else if (cmd.equals("test")) {
			Log.prln(pws.split("b").length);
			Log.prln(pws.split(" b".trim()).length);
			Log.prln(pws.split(" b c".trim()).length);
			Log.prln(pws.split("b c \n").length);
		}
	}
}