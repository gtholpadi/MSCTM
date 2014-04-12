package mllab_mstm.corpus;

import java.io.File;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mllab_mstm.corpus.KPArticle.KPComment;
import mllab_mstm.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


public class KannadaPrabha {
	static Pattern pp = Pattern.compile("[\\s\\.,\\?]+");
	static Pattern pkn = Pattern.compile("\\p{InKannada}");
	static Pattern pen = Pattern.compile("[\u0000-\u007F\u0080-\u00FF]");
	public static void parseart(File artFile, File commentsFile, File tgtFile, File logFile)
		throws Exception {
		KPArticle a = KPArticle.parseArticle(artFile, commentsFile);
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
		KPArticle a = KPArticle.read(artFile);
		a.print();
	}
	public static void getstats(String corpdir) throws Exception {
		for (File f : (new File(corpdir)).listFiles()) {
			KPArticle a = KPArticle.read(f);
			System.out.println(String.format("%s\t%d\t%d\t%d",
				a.aid, a.body.length, a.comments.length,
				countKnComments(a.comments)));
		}
	}
	public static int countKnComments(KPComment[] comments) throws Exception {
		int numkncomments = 0;
		for (KPComment c : comments) {
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
	public static void getcommenturl(File artFile) throws Exception {
		Document doc = Jsoup.parse(artFile, null);
		String title = doc.select("span[id=lblHeadline]").first().text();
		title = title.replaceAll("[-.,#!$%&;:{}=_`~()]","").replace(' ','-');
		String aid = doc.select("body > form[action^=/edition/printkp.aspx]").first().attr("action").split("artid=")[1];
		String url = "http://www.kannadaprabha.com/top-news/"
			+ URLEncoder.encode(URLEncoder.encode(title, "UTF-8"), "UTF-8")
			+ "/"
			+ aid
			+ ".html";
		String commurl = "http://disqus.com/embed/comments/?disqus_version=2b152e4d&base=default&f=kannadaprabha"
			+ "&t_u=" + url
			+ "&s_o=default#2";
		System.out.print(commurl);//this can be wget-ed.
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
 		} else if (cmd.equals("getcommenturl")) {
			String artFile = args[i++];
			getcommenturl(new File(artFile));
		} else if (cmd.equals("test")) {
			Pattern p = Pattern.compile("\\?");
			String[] q = p.split("qwe?asd");
			for (String r : q) {
				System.out.println("ZXC:"+r);
			}
		}
	}
}
