package mllab_mstm.corpus;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mllab_mstm.corpus.KSArticle.KSComment;
import mllab_mstm.util.Log;


public class KendaSampige {
	static Pattern pp = Pattern.compile("[\\s\\.,\\?]+");
	static Pattern pkn = Pattern.compile("\\p{InKannada}");
	static Pattern pen = Pattern.compile("[\u0000-\u007F\u0080-\u00FF]");
	public static void parseart(File artFile, File commentsFile, File tgtFile) throws Exception {
		KSArticle a = KSArticle.parseArticle(artFile, commentsFile);
		a.save(tgtFile);
// 		a.print();
	}
	public static void readart(File artFile, File logFile) throws Exception {
		KSArticle a = KSArticle.read(artFile);
		if (a == null) {
// 			System.out.println("Nothing saved.");
		} else {
			if (a.comments == null || a.comments.length==0) {
				//dont save
			} else {
				Log.set(logFile);
				a.print();
				Log.reset();
			}
		}
	}
	public static void getstats(String corpdir) throws Exception {
		for (File f : (new File(corpdir)).listFiles()) {
			KSArticle a = KSArticle.read(f);
			System.out.println(String.format("%s\t%s\t%s\t%d\t%d\t%d",
				a.aid, a.authid, a.catid, a.body.length, a.comments.length,
				countKnComments(a.comments)));
		}
	}
	public static int countKnComments(KSComment[] comments) throws Exception {
		int numkncomments = 0;
		for (KSComment c : comments) {
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
			parseart(new File(artFile), new File(commentsFile), new File(tgtFile));
		} else if (cmd.equals("readart")) {
			String artFile = args[i++];
			String logFile = args[i++];
			readart(new File(artFile), new File(logFile));
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
