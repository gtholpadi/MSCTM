package mllab_mstm.corpus;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import mllab_mstm.util.Log;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.google.gson.Gson;

public class KSArticle {
	public static class KSComment {
		static SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy hh:mm aa");
		String user;
		public Date date;
		String body;
		public static KSComment[] parseComments(File commentsFile) throws Exception {
			List<KSComment> cs = new ArrayList<KSComment>();
			Document doc = Jsoup.parse(commentsFile, null);
			Elements comments = doc.select("td[class=text1]");
			for (Element e : comments) {
				KSComment c = new KSComment();
				c.body = KSArticle.clean(e.text());

				Element temp = e.parent().nextElementSibling().select("font[color=blue]").first();
				c.user = KSArticle.clean(temp.text());

				c.date = parseDate(KSArticle.clean(temp.parent().ownText().replace("|", " ")));

				cs.add(c);
			}
			return cs.toArray(new KSComment[0]);
		}
		private static Date parseDate(String date1) throws Exception {
			return df.parse(date1);
		}
	}
	static Gson gson = new Gson();
	String aid;
	String author;
	String authid;
	Date date;
	String categ;
	String catid;
	String title;
	String[] body;
	String votes;
	String rating;
	public KSComment[] comments;
	String[] related;
	public void save(File f) throws Exception {
		FileUtils.write(f, gson.toJson(this), "UTF-8");
	}
	public static KSArticle read(File f) throws Exception {
		return gson.fromJson(FileUtils.readFileToString(f, "UTF-8"), KSArticle.class);
	}
	public void print() throws Exception {
		Log.prln("-----------------------");
		Log.prln(aid);
		Log.prln(author);
		Log.prln(authid);
		Log.prln(date);
		Log.prln(categ);
		Log.prln(catid);
		Log.prln(title);
		Log.prln("--body--BEGIN");
		for (String para : body) {
			Log.prln(para);
		}
		Log.prln("--body--END");
		Log.prln(votes);
		Log.prln(rating);
		Log.prln("--comments--BEGIN");
		for (KSComment c : comments) {
			Log.prln(String.format("%s (%s) : %s", c.user, c.date,c.body));
		}
		Log.prln("--comments--END");
		Log.prln(related.length);
		Log.prln("-----------------------");
	}
	public static KSArticle parseArticle(File artFile, File commentsFile) throws Exception {
		KSArticle a = new KSArticle();
		Document doc = Jsoup.parse(artFile, null);

 		Elements aid = doc.select("script[src^=?ajaxagent=js&this_url]");
 		String[] temp = aid.first().attr("src").split("id%3D");
		a.aid = clean(temp[temp.length-1]);

		Element contarea = doc.select("td[width=825]").first();

		Elements categ = contarea.select("a[href^=category.php?catid=]");
		a.categ = clean(categ.first().text());
		temp = categ.first().attr("href").split("catid=");
		a.catid = clean(temp[temp.length-1]);

		Elements title = contarea.select("td[class=articlehead]");
		a.title = clean(title.first().text());

		Elements author = contarea.select("a[href^=writer_profile.php?id=]");
		a.author = clean(author.first().text());
		temp = author.first().attr("href").split("id=");
		a.authid = clean(temp[temp.length-1]);

		Elements date = contarea.select("td[class=left_nav_text_k]");
		a.date = parseDate(clean(date.first().text()));

		List<String> lbody = new ArrayList<String>();
		getBody(lbody, contarea.select("span[class=article]").first());
		a.body = lbody.toArray(new String[0]);

		Element tempe = contarea.select("span[class=vote]").first();
		if (tempe == null) {
			a.votes = "0";
			a.rating = "0";
		} else {
			Elements voterate = tempe.select("b");
			a.votes = clean(voterate.first().text());
			a.rating = clean(voterate.last().text());
		}

		Elements related = contarea.select("a[href^=article.php?id=]");
		List<String> lrelated = new ArrayList<String>();
		for (int i=0, nr=related.size(); i<nr; i++) {
			temp = related.get(i).attr("href").split("id=");
			lrelated.add(clean(temp[temp.length-1]));
		}
		a.related = lrelated.toArray(new String[0]);

		a.comments = KSComment.parseComments(commentsFile);
		return a;
	}
	private static void getBody(List<String> bodySoFar, Node e) {
		for (Node child : e.childNodes()) {
			if (child instanceof TextNode) {
				String text = clean(((TextNode) child).text());
				if (!text.equals("")) {
					bodySoFar.add(text);
				}
			} else {
				getBody(bodySoFar, child);
			}
		}
	}
	private static Date parseDate(String date1) throws Exception {
		String[] temp = date1.split(",")[1].split("\\(");
		String[] date = temp[0].trim().split("\\s+");
		String[] time = temp[1].split("\\s+")[0].split(":");
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
		c.set(Integer.parseInt(date[2]), getMonthKannada(date[1]), Integer.parseInt(date[0]),
			Integer.parseInt(time[0]), Integer.parseInt(time[1]), 0);
		return c.getTime();
	}
	private static int getMonthKannada(String month) throws Exception {
		int m;
		if (month.equals("ಜನವರಿ")) {
			m = 0;
		} else if (month.equals("ಫೆಬ್ರವರಿ")) {
			m = 1;
		} else if (month.equals("ಮಾರ್ಚ್")) {
			m = 2;
		} else if (month.equals("ಏಪ್ರಿಲ್")) {
			m = 3;
		} else if (month.equals("ಮೇ")) {
			m = 4;
		} else if (month.equals("ಜೂನ್")) {
			m = 5;
		} else if (month.equals("ಜುಲೈ")) {
			m = 6;
		} else if (month.equals("ಆಗಸ್ಟ್")) {
			m = 7;
		} else if (month.equals("ಸೆಪ್ಟೆಂಬರ್")) {
			m = 8;
		} else if (month.equals("ಅಕ್ಟೋಬರ್")) {
			m = 9;
		} else if (month.equals("ನವೆಂಬರ್")) {
			m = 10;
		} else if (month.equals("ಡಿಸೆಂಬರ್")) {
			m = 11;
		} else {
			throw new Exception("invalid month: "+month);
		}
		return m;
	}
	public static String clean(String text) {
		//remove ZWNJ, non-breaking space (also remove ZWJ (\u200D) ?)
		return text.replace("\u200C","").replace('\u00A0', ' ').trim();
	}
}
