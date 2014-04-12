package mllab_mstm.corpus;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mllab_mstm.util.Log;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;

public class DJArticle {
	//comment type
	public static class DJComment {
		static SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy, hh:mm:ss aa");
		String user;
		public Date date;
		String body;
		public static DJComment[] parseComments(Document doc) throws Exception {
			List<DJComment> cs = new ArrayList<DJComment>();
			Elements comments = doc.select("div[class=allcomments] > ul > li");
			for (Element e : comments) {
				DJComment c = new DJComment();
				Element userdate = e.select("section[class=title]").first();
				c.user = DJArticle.clean(userdate.ownText());
				c.date = parseDate( DJArticle.clean(userdate.select("span").first().text()) );
				c.body = DJArticle.clean( e.select("li > p").first().text() );
				cs.add(c);
			}
			return cs.toArray(new DJComment[0]);
		}
		private static Date parseDate(String date) throws Exception {
			return df.parse(date);
		}
	}
	//tag type
	static class DJTag {
		String tid;
		String desc;
		public DJTag(String tid, String desc) {
			this.tid = tid;
			this.desc = desc;
		}
	}
	//photo type
	static class DJPhoto {
		String pid;
		String path;
		String title;
		String desc;
		DJComment[] comments;
		String[] related;
		//TODO
	}
	static Gson gson = new Gson();
	static Pattern prp = Pattern.compile("/photos/.*-([0-9]+)\\.html#photodetail");
	static Pattern pra = Pattern.compile("/.*-([0-9]+)\\.html");
	static Pattern pws = Pattern.compile("\\s+");
	static SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm aa (zzz)");
	//data
	String aid;
	String title;
	String webtitle;
	Date date;
	String[] body;
	DJTag[] tags;
	public DJComment[] comments;
	String[] relatedArt;
	String[] relatedPhoto;
	public void save(File f) throws Exception {
		FileUtils.write(f, gson.toJson(this), "UTF-8");
	}
	public static DJArticle read(File f) throws Exception {
		return gson.fromJson(FileUtils.readFileToString(f, "UTF-8"), DJArticle.class);
	}
	public void print() throws Exception {
		Log.prln("-----------------------");
		Log.prln(aid);
		Log.prln(date);
		Log.prln(title);
		Log.prln(webtitle);
		Log.prln("--body--BEGIN");
		for (String para : body) {
			Log.prln(para);
		}
		Log.prln("--body--END");
		Log.prln("--tags--BEGIN");
		if (tags != null) {
			for (DJTag tag : tags) {
				Log.prln(String.format("%s (%s)", tag.tid, tag.desc));
			}
		}
		Log.prln("--tags--END");
		Log.prln("--comments--BEGIN");
		for (DJComment c : comments) {
// 			Log.println(String.format("%s (%s) : %s", c.user, c.date, makeOneLine(c.body)));
			Log.prln(String.format("%s (%s) : %s", c.user, c.date, c.body));
		}
		Log.prln("--comments--END");
		Log.prln("--related--BEGIN");
		Log.prln("Articles: " + StringUtils.join(relatedArt, ","));
		Log.prln("Photos: " + StringUtils.join(relatedPhoto, ","));
		Log.prln("--related--END");
		Log.prln("-----------------------");
	}
	public static DJArticle parseArticle(File artFile) throws Exception {
		String temp;
		DJArticle a = new DJArticle();
		Document doc = Jsoup.parse(artFile, null);

		a.comments = DJComment.parseComments(doc);
		if (a.comments == null || a.comments.length == 0) {
			Log.prln("no comments. skipping...");
			return null;
		}

		Element aid = doc.select("input[id=id01]").select("input[type=hidden]").select("input[name=id01]").first();
		if (aid == null) {
			System.out.println("no id. skipping");
			return null;
		} else {
			a.aid = clean(aid.attr("value"));
		}

		Elements title = doc.select("section[class=title] > h1");
		if (title.first() == null) {
			System.out.println(a.aid + ": no title");
			return null;
		}
		a.title = clean(title.first().text());

		Elements date = doc.select("section[class=title] > section[class=grayrow] > span[class=date]");
		try {
		a.date = parseDate(clean(date.first().text().split(":",2)[1]));
		} catch (java.text.ParseException e) {
			Log.prln("date parse error. ignoring...");
			a.date = null;
		}

		List<String> lbody = new ArrayList<String>();
		Elements body = doc.select("section[class=articaltext] > p");
		for (int i=0, np =body.size(); i<np; i++) {
			Element para = body.get(i);
			temp = clean(para.text());
			if (temp.equals("")
				|| temp.endsWith("m.jagran.com पर")
				|| temp.startsWith("(Hindi news from Dainik Jagran")) {
				continue;
			} else if (temp.startsWith("Tags:")) {
				List<DJTag> ltags = new ArrayList<DJTag>();
				Elements tags = para.select("p > a[href^=/search/]");
				for (int j=0, nt=tags.size(); j<nt; j++) {
					Element tag = tags.get(j);
					String tid = clean(tag.attr("href").substring(8));
					String desc = clean(tag.text());
					ltags.add( new DJTag(tid, desc) );
				}
				a.tags = ltags.toArray(new DJTag[0]);
			} else if (temp.startsWith("Web Title:")) {
				a.webtitle = temp.substring(10).trim();
			} else {
				lbody.add(temp);
			}
		}
		a.body = lbody.toArray(new String[0]);

		Elements related = doc.select("div[class=moreview MT10]").select("li > a[href]");
		List<String> lrelatedArt = new ArrayList<String>();
		List<String> lrelatedPhoto = new ArrayList<String>();
		for (int i=0, nr=related.size(); i<nr; i++) {
			String href = related.get(i).attr("href");
			Matcher m;
			if ( (m=prp.matcher(href)).matches()) {
				lrelatedPhoto.add(clean(m.group(1)));
			} else if ( (m=pra.matcher(href)).matches()) {
				lrelatedArt.add(clean(m.group(1)));
			}
		}
		a.relatedArt = lrelatedArt.toArray(new String[0]);
		a.relatedPhoto = lrelatedPhoto.toArray(new String[0]);

		return a;
	}
	private static Date parseDate(String date) throws Exception {
		return df.parse(date);
	}
	public static String clean(String text) {
		return text.replace("\u200C","").replace('\u00A0', ' ').trim();
	}
	public static String makeOneLine(String text) {
		return StringUtils.join(pws.split(text), " ");
	}
}