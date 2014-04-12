package mllab_mstm.corpus;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.regex.Pattern;

import mllab_mstm.util.DownloadManager;
import mllab_mstm.util.Log;
import mllab_mstm.util.MyUtil;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class NavbharatTimes {
//	static long waitTimeMillis = 3 * 1000;
	static int archid20131231 = 41639;
	static String archurlformat = "http://navbharattimes.indiatimes.com/archivelist/starttime-%d.cms";
	static String arturlformat = "http://navbharattimes.indiatimes.com%s";
	static String comurlformat = "http://navbharattimes.indiatimes.com/articleshow_othcmtofart/%s?curpg=%d&s=1";
//	static int timeoutMillis = 90 * 1000;
	static Pattern pComExists = Pattern.compile("\\s*\\(\\s*[0-9]+\\s*\\)\\s*");//"(123)", with possible whitespace
	public static Document dloadParse(URL url, int timeoutMillis, long waitTimeMillis) throws InterruptedException, IOException {
		Thread.sleep(waitTimeMillis);
		return Jsoup.parse(url, timeoutMillis);
	}
	public static void crawl(File tgtDir, int fromOffset, int skipArts, int tillOffset, File logFile) 
			throws IOException, InterruptedException {
		DownloadManager dm = DownloadManager.getInstance();
		for (int d=fromOffset; d<=tillOffset; d++) {
			FileUtils.write(logFile, String.format("--------Day %d\n", d), "UTF-8", true);
			URL url = new URL(String.format(archurlformat, archid20131231-d));
			Document archd = dm.jsoupParse(url);
			Elements es = archd.select("span[class=normtxt] > a[href^=/articleshow/]");
			for (int artCount=0;  artCount < es.size() ;artCount++) {
				if (d==fromOffset && artCount<=skipArts) {//these are already processed
					continue;
				}
				Element e = es.get(artCount);
				String attrval = e.attr("href");
				String artFname = attrval.substring(attrval.lastIndexOf('/')+1);
				//get comments
				boolean moreComments = true, commentsExist = false;
				int pg=1;
				try { 
					for (pg=1; moreComments; pg++) {
						url = new URL(String.format(comurlformat, artFname, pg));
						Document comd = dm.jsoupParse(url);
						Element e1 = comd.select("div[id=cmtcount]").first();
						if (pComExists.matcher(e1.ownText()).matches()) {
							commentsExist = true;
							String comFname = String.format("com.%d.%s", pg, artFname);
							FileUtils.write(new File(comFname), comd.outerHtml(),"UTF-8");
						} else {
							moreComments = false;
						}
					}
				} catch (SocketTimeoutException ste) {//do nothing
				}
				//get article only if comments exist
				if (commentsExist) {
					try {
						url = new URL(String.format(arturlformat,attrval));
						dm.apacheCopyToFile(url, new File(tgtDir, artFname));
						FileUtils.write(logFile, String.format("Article %d with %d comment pages saved.\n", artCount, pg-1), "UTF-8", true);
					} catch (SocketTimeoutException ste) {
						FileUtils.write(logFile, String.format("Article %d timed-out (and skipped).\n", artCount), "UTF-8", true);
					} catch (IOException ioe) {
						FileUtils.write(logFile, String.format("Article %d had HTTP error (and was skipped).\n", artCount), "UTF-8", true);
					}
				} else {
					FileUtils.write(logFile, String.format("Article %d skipped.\n", artCount), "UTF-8", true);
				}
			}
		}
	}
	public static void parseart(File artHtmlFile, File[]comHtmlFiles, File tgtJsonFile, 
			File tgtLogFile) throws Exception {
		NTArticle a = null;
		try {
			a = NTArticle.parseArticle(artHtmlFile, comHtmlFiles);
		} catch (Exception e) {
			System.err.println("ERROR IN " + artHtmlFile);
			throw e;
		}
		if (a != null) {
			a.save(tgtJsonFile);
			Log.set(tgtLogFile);
			a.print();
			Log.reset();
		} else {
//			System.out.println("");
		}
	}
	public static void parseartlist(File artListFile, String startFromFile) throws Exception {
		String[][] paramList = MyUtil.getArrayFromFile(artListFile, " ");
		int startFromIndex;
		if (startFromFile.equals("NONE")) {
			startFromIndex = 0;
		} else {
			for (startFromIndex=0; ! paramList[startFromIndex][0].equals(startFromFile); startFromIndex++) { }
		}
		for (int i=startFromIndex; i<paramList.length; i++) {
			String[] params = paramList[i];
			parseart(new File(params[0]), MyUtil.commaSepList2FileArray(params[1]), 
					new File(params[2]), new File(params[3]));
		}
	}
	public static void main(String[] args) throws Exception {
		String cmd = args[0];
		int i = 1;
		if (cmd.equals("crawl")) {
			String tgtDir = args[i++];
			String fromOffset = args[i++];
			String skipArts = args[i++];
			String tillOffset = args[i++];
			String logFile = args[i++];
			crawl(new File(tgtDir), Integer.valueOf(fromOffset), Integer.valueOf(skipArts), 
					Integer.valueOf(tillOffset), new File(logFile));
		} else if (cmd.equals("parseart")) {
			String artHtmlFile = args[i++];
			String comHtmlFiles = args[i++];
			String tgtJsonFile = args[i++];
			String tgtLogFile = args[i++];
			parseart(new File(artHtmlFile), MyUtil.commaSepList2FileArray(comHtmlFiles), 
					new File(tgtJsonFile), new File(tgtLogFile));
		} else if (cmd.equals("parseartlist")) {
			String artListFile = args[i++];
			String startFromFile = args[i++];
			parseartlist(new File(artListFile), startFromFile);
		} else if (cmd.equals("test")) {
		}
	}
}
