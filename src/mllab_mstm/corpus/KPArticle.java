package mllab_mstm.corpus;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import mllab_mstm.util.Log;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class KPArticle {
	public static class KPComment {
		static JsonParser parser = new JsonParser();
		static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss");
		String user;
		public Date date;
		String body;
		int uvote;
		int dvote;
		public static KPComment[] parseComments(File commentsFile) throws Exception {
			List<KPComment> cs = new ArrayList<KPComment>();
			Document doc = Jsoup.parse(commentsFile, null);
			Element commentsElem = doc.select("script[id=disqus-threadData]").first();
			if (commentsElem == null) {
				System.out.println("no commentsElem.");
				return null;
			}
			String commentsJson = commentsElem.data();
			JsonObject co;
			try {
			co = parser.parse(commentsJson).getAsJsonObject();
			} catch (com.google.gson.JsonSyntaxException e) {
				Log.prln("error parsing comment json.");
				return null;
			}
			if (co.get("code").getAsInt() != 0) {
				System.out.println("comments rc != 0.");
				return null;
			}
			JsonArray comments = co.getAsJsonObject("response").getAsJsonArray("posts");
			for (int i=0, nc=comments.size(); i<nc; i++) {
				KPComment c = new KPComment();
				JsonObject o = comments.get(i).getAsJsonObject();
				c.user = KPArticle.clean( o.getAsJsonObject("author").get("name").getAsString() );
				c.body = KPArticle.clean( o.get("raw_message").getAsString() );
				c.date = parseDate( KPArticle.clean( o.get("createdAt").getAsString() ) );
				c.uvote = o.get("likes").getAsInt();
				c.dvote = o.get("dislikes").getAsInt();
				cs.add(c);
			}
			return cs.toArray(new KPComment[0]);
		}
		private static Date parseDate(String date) throws Exception {
			return df.parse(date);
		}
	}
	static Gson gson = new Gson();
	static Pattern pws = Pattern.compile("\\s+");
	static SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy hh:mm:ss aa zzz");
	String aid;
	String title;
	Date date;
	String[] body;
	public KPComment[] comments;
	public void save(File f) throws Exception {
		FileUtils.write(f, gson.toJson(this), "UTF-8");
	}
	public static KPArticle read(File f) throws Exception {
		return gson.fromJson(FileUtils.readFileToString(f, "UTF-8"), KPArticle.class);
	}
	public void print() throws Exception {
		Log.prln("-----------------------");
		Log.prln(aid);
		Log.prln(title);
		Log.prln(date);
		Log.prln("--body--BEGIN");
		for (String para : body) {
			Log.prln(para);
		}
		Log.prln("--body--END");
		Log.prln("--comments--BEGIN");
		if (comments != null) {
			for (KPComment c : comments) {
				Log.prln(String.format("%s (%s) [%d,%d]: %s", c.user, c.date, c.uvote, c.dvote, makeOneLine(c.body)));
			}
		}
		Log.prln("--comments--END");
		Log.prln("-----------------------");
	}
	public static KPArticle parseArticle(File artFile, File commentsFile) throws Exception {
		KPArticle a = new KPArticle();
		a.comments = KPComment.parseComments(commentsFile);
		if (a.comments == null || a.comments.length == 0) {
			System.out.println("zero comments.");
			return null;
		}

		Document doc = Jsoup.parse(artFile, null);

 		String aid = doc.select("body > form[action^=/edition/printkp.aspx?artid=]").first().attr("action");
		a.aid = clean( aid.substring( aid.lastIndexOf('=')+1 ) );
		Element title = doc.select("span[id=lblHeadline]").first();
		if (title == null) {
			System.out.println("no title. skipping");
			return null;
		} else {
			a.title = clean( title.text() );
		}
		Element date = doc.select("span[id=lblPublishedDate]").first();
		if (date == null) {
/*			System.out.println(a.aid + ": no date");
			return null;*/
			a.date = null;
		} else {
			a.date = parseDate(clean( date.text() ));
		}
		List<String> lbody = new ArrayList<String>();
		List<TextNode> body = doc.select("span[id=lblDescription]").first().textNodes();
		for (int i=0, np=body.size(); i<np; i++) {
			String para = clean( body.get(i).text() );
			if (!para.equals("")) {
				lbody.add( para );
			}
		}
		a.body = lbody.toArray(new String[0]);

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
