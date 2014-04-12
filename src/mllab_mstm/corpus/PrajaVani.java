package mllab_mstm.corpus;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mllab_mstm.corpus.PVArticle.PVComment;
import mllab_mstm.util.Log;


public class PrajaVani {
	static Pattern pp = Pattern.compile("[\\s\\.,\\?]+");
	static Pattern pkn = Pattern.compile("\\p{InKannada}");
	static Pattern pen = Pattern.compile("[\u0000-\u007F\u0080-\u00FF]");
	public static void parseart(File artFile, File commentsFile, File tgtFile, File logFile)
		throws Exception {
		PVArticle a = PVArticle.parseArticle(artFile, commentsFile);
		if (a == null) {
			System.out.println("Nothing saved.");
		} else {
			if (a.comments == null || a.comments.length==0) {
				//dont save
			} else {
				a.save(tgtFile);
				Log.set(logFile);
				a.print();
				Log.reset();
			}
		}
	}
	public static void readart(File artFile) throws Exception {
		PVArticle a = PVArticle.read(artFile);
		a.print();
	}
	public static void getstats(String corpdir) throws Exception {
		for (File f : (new File(corpdir)).listFiles()) {
			PVArticle a = PVArticle.read(f);
			System.out.println(String.format("%s\t%s\t%d\t%d\t%d",
				a.aid, a.categ, a.body.length, a.comments.length,
				countKnComments(a.comments)));
		}
	}
	public static int countKnComments(PVComment[] comments) throws Exception {
		int numkncomments = 0;
		for (PVComment c : comments) {
			int numkntoks = 0;
			String[] toks = pp.split(c.body);
			for (String tok : toks) {
				Matcher m = pkn.matcher(tok);
				if (m.find()) {
					numkntoks++;
				}
			}
			if (numkntoks > toks.length-numkntoks) {
				numkncomments++;
			}
		}
		return numkncomments;
	}
	public static void main(String[] args) throws Exception {
		String cmd = args[0];
		int i = 1;
		if (cmd.equals("parseart")) {
			String artFile = args[i++];
			String commentsFile = args[i++];
			String tgtFile = args[i++];
			String logFile = args[i++];
			parseart(new File(artFile), new File(commentsFile), new File(tgtFile), new File(logFile));
		} else if (cmd.equals("readart")) {
			String artFile = args[i++];
			readart(new File(artFile));
		} else if (cmd.equals("getstats")) {
			String corpdir = args[i++];
			getstats(corpdir);
		} else if (cmd.equals("test")) {
			Pattern p = Pattern.compile("\\?");
			String[] q = p.split("qwe?asd");
			for (String r : q) {
				System.out.println("ZXC:"+r);
			}
		}
	}
}
