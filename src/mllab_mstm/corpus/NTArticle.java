package mllab_mstm.corpus;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mllab_mstm.util.Log;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import com.google.gson.Gson;

public class NTArticle {
	public static class NTComment {
		static SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy 'at' hh:mm aa");// 22/03/2014 at 05:14 AM
		String user;
		Date date;
		String body;
		String location;
		int nAgree;
		int nDisagree;
		int nRecommend;
		public static List<NTComment> parseComments(File comFile) throws Exception {
			List<NTComment> cs = new ArrayList<NTComment>();
			if (comFile != null) {
				Document doc = Jsoup.parse(comFile, null);
				for (Element e : doc.select("div[class=cmt] > div[id=user-left-panel]")) {
					NTComment c = new NTComment();
					String temp;
					Element tempe = e.select("span[class=name] > a").first();
					if (tempe == null) {
						temp =  e.select("span[class=name]").first().text();
					} else {
						temp = e.select("span[class=name] > a").first().attr("tpointaddress");
						int i = temp.indexOf('@');
						temp = i<0 ? temp : temp.substring(0, i);
					}
					c.user = clean(temp);
					c.date = parseDate(e.select("div[class=user-left-info] > span[style^=color:]").first().ownText());
					c.body = clean(e.select("div[id=bottom-user-panel] > div[class=left-comment-text] > span[style^=color:]").first().text());
					temp = e.select("span[class=location]").first().ownText();
					c.location = clean(temp.substring(0, temp.indexOf("का कहना है")));
					for (Element e1 : e.select("div[id=bottom-user-panel] > div[class=cmtopt] > a")) {
						if (e1.attr("class").equals("agree")) {
							c.nAgree = getNumInBraces(e1.ownText());
						} else if (e1.attr("class").equals("disagree")) {
							c.nDisagree = getNumInBraces(e1.ownText());
						} else if (e1.attr("class").equals("recommend")) {
							c.nRecommend = getNumInBraces(e1.ownText());						
						}
					}
					cs.add(c);
				}
			}
			return cs;
		}
		private static Date parseDate(String date) throws Exception {
			return df.parse(date);
		}
		private static int getNumInBraces(String str) {
			return Integer.valueOf(str.substring(str.indexOf('(')+1, str.indexOf(')')));
		}
	}
	static Gson gson = new Gson();
	static Pattern pws = Pattern.compile("\\s+");
	static SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy, hhmm 'hrs' zzz");//
	static SimpleDateFormat df1 = new SimpleDateFormat("MMM dd, yyyy, hh.mmaa zzz");//Dec 31, 2013, 08.12PM IST
	
	String aid;
	String title;
//	String webtitle;
	Date date;
	String[] body;
	public NTComment[] comments;
	String[] relAids; //aid's of related articles
	public void save(File f) throws Exception {
		FileUtils.write(f, gson.toJson(this), "UTF-8");
	}
	public static NTArticle read(File f) throws Exception {
		return gson.fromJson(FileUtils.readFileToString(f, "UTF-8"), NTArticle.class);
	}
	public void print() throws Exception {
		Log.prln("-----------------------");
		Log.prln(aid);
		Log.prln(date);
		Log.prln(title);
//		Log.prln(webtitle);
		Log.prln("--body--BEGIN");
		for (String para : body) {
			Log.prln(para);
		}
		Log.prln("--body--END");
		Log.prln("--comments--BEGIN");
		for (NTComment c : comments) {
// 			Log.println(String.format("%s (%s) : %s", c.user, c.date, makeOneLine(c.body)));
			Log.prln(String.format("%s (%s) [%d,%d,%d]: %s", c.user, c.date, 
					c.nAgree, c.nDisagree, c.nRecommend, c.body));
		}
		Log.prln("--comments--END");
		Log.prln("--related--BEGIN");
		Log.prln("Articles: " + StringUtils.join(relAids, ","));
		Log.prln("--related--END");
		Log.prln("-----------------------");
	}
	public static String getAidFromLink(String link) {
		return link.substring(link.lastIndexOf('/')+1, link.lastIndexOf(".cms"));
	}
	public static NTArticle parseArticle(File artFile, File[] commFiles) throws Exception {
		NTArticle a = new NTArticle();
		Document doc = Jsoup.parse(artFile, null);
		String temps;
		Element tempe, tempe1;
		//aid
		tempe = doc.select("html > head > link[rel=canonical]").first();
		if (tempe == null) {//could not find aid in html, use file name
//			temps = artFile.getName();
//			temps = temps.substring(0, temps.lastIndexOf(".cms"));
			temps = getAidFromLink(artFile.getName());
		} else {
			temps = getAidFromLink(tempe.attr("href"));
		}
		a.aid = clean(temps);
		assert a.aid.length() > 0 ;
		//title
		tempe = doc.select("span[class=artshowhead] > h2").first();
		if (tempe!=null) {
			temps = tempe.ownText();
		} else {
			tempe = doc.select("head > title").first();
			if (tempe!=null) {
				temps =  doc.select("head > title").first().ownText();
				Pattern ptitsuff = Pattern.compile("\\s*-?\\s*Navbharat\\s*Times\\s*$");
				Matcher m = ptitsuff.matcher(temps);
				if (m.find()) {
					temps = temps.substring(0, m.start());
				}
	//			temps = temps.substring(0, temps.lastIndexOf("- Navbharat Times"));
			}
		}
		if (tempe != null) {
			a.title = clean(temps);
		} else {
			System.out.println("No title, skipping " + a.aid);
			return null;
		}
		assert a.title != null;
		//date
		Date tempd = null;
//		tempe = doc.select("div[class=left_container] > span[class=headingnextag]").first();
		tempe = doc.select("div[class^=left] > span[class=headingnextag]").first();
		if (tempe == null) {
//			tempe = doc.select("div[id=storydiv] > div[style] > span[class=byline]").first();
			tempe = doc.select("div[id=storydiv] span[class=byline]").first();
		}
		for (TextNode tn : tempe.textNodes()) {
			try {
				tempd = parseDate(clean(tn.getWholeText()));
				break;
			} catch (ParseException pe) {}//do nothing
		}
		if (tempd == null) {
			throw new Exception("no date");
		}
		a.date = tempd;
		assert a.date != null;
		//body
		List<String> lbody = new ArrayList<String>();
		tempe = doc.select("div[id=storydiv]").first();
		while (true) {//find lowest div[class=Normal] in tree
			tempe1 = tempe.children().select("div[class=Normal]").first();
			if (tempe1 == null) {
				break;
			} else {
				tempe = tempe1;
			}
		}
//		tempe = doc.select("div[id=storydiv] > div[class=Normal]").first();
//		tempe1 = tempe.children().select("div[class=Normal]").first(); 
//		if ( tempe1 != null) {
//			tempe = tempe1;
//		}
		if (tempe.children().size() == 1) {//in case all sentences are under a <p>
			tempe = tempe.child(0);
		}
		Element divClassNormal = tempe;//saving now for use in "related articles"
		
		List<Node> ns = tempe.childNodes();
		for (Node n : ns) {//include children that are textnodes or "strong" nodes
			temps = "";
			if (n instanceof Element && ((Element) n).tagName().equals("strong")) {
				temps = clean( ((Element) n).text() );
			} else if (n instanceof TextNode) {
				temps = clean( ((TextNode) n).text() );
			}
			if (!temps.equals("")) {
				lbody.add(temps);
			}
		}
		a.body = lbody.toArray(new String[0]);
		
		//related articles
		HashSet<String> rel = new LinkedHashSet<String>();
		for (Element e : divClassNormal.select("strong > a")) {
			try {
			rel.add( clean(getAidFromLink(e.attr("href"))) );
			} catch(StringIndexOutOfBoundsException sioobe) {
				//skip this link
			}
		}
		a.relAids = rel.toArray(new String[0]);
		//comments
		if (commFiles == null) {
			a.comments = new NTComment[0];
		} else {
			List<NTComment> lcoms = new ArrayList<NTArticle.NTComment>();
			for (int c=0; c<commFiles.length; c++) {
				lcoms.addAll(NTComment.parseComments(commFiles[c]));
			}
			a.comments = lcoms.toArray(new NTComment[0]);
		}
		//done
		return a;
	}
	private static Date parseDate(String date) throws Exception {
		Date d;
		try {
		d = df.parse(date);
		} catch (ParseException pe) {
			d = df1.parse(date);
		}
		return d;
	}
	public static String clean(String text) {
		return text.replace("\u200C","").replace('\u00A0', ' ').trim();
	}
	public static String makeOneLine(String text) {
		return StringUtils.join(pws.split(text), " ");
	}
}
