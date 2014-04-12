package mllab_mstm.corpus;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mllab_mstm.corpus.DJArticle.DJComment;
import mllab_mstm.util.Log;

public class DainikJagran {
	static Pattern pp = Pattern.compile("[\\s\\.,\\?]+");
	static Pattern phi = Pattern.compile("\\p{InDevanagari}");
	static Pattern pen = Pattern.compile("[\u0000-\u007F\u0080-\u00FF]");
	public static void parseart(File artFile, File tgtFile, File logFile) throws Exception {
		DJArticle a = DJArticle.parseArticle(artFile);
		if (a == null) {
			System.out.println("Nothing saved.");
		} else {
			if (a.comments == null || a.comments.length==0) {
				Log.prln("no comments. skipping...");
			} else {
				a.save(tgtFile);
				Log.set(logFile);
				a.print();
				Log.reset();
			}
		}
	}
	public static void readart(File artFile) throws Exception {
		DJArticle a = DJArticle.read(artFile);
		a.print();
	}
	public static void getstats(String corpdir) throws Exception {
		for (File f : (new File(corpdir)).listFiles()) {
			DJArticle a = DJArticle.read(f);
// 			int numhicomments = countHiComments(a.comments);
			System.out.println(String.format("%s\t%d\t%d\t%d",
				a.aid, a.body.length, a.comments.length,
				countHiComments(a.comments)));
		}
	}
	public static int countHiComments(DJComment[] comments) throws Exception {
		int numhicomments = 0;
		for (DJComment c : comments) {
			int numhitoks = 0;
			String[] toks = pp.split(c.body);
			for (String tok : toks) {
				Matcher m = phi.matcher(tok);
				if (m.find()) {
					numhitoks++;
				}
			}
			if (numhitoks > toks.length-numhitoks) {
				numhicomments++;
			}
		}
		return numhicomments;
	}
	public static void main(String[] args) throws Exception {
		String cmd = args[0];
		int i = 1;
		if (cmd.equals("parseart")) {
			String artFile = args[i++];
			String tgtFile = args[i++];
			String logFile = args[i++];
			parseart(new File(artFile), new File(tgtFile), new File(logFile));
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
