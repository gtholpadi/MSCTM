package mllab_mstm.util;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class DownloadManager {
	static DownloadManager dm;
	int timeoutMillis = 60 * 1000;
	long waitTimeMillis = 5 * 1000;
	int retryAttempts = 3;
	public DownloadManager() {
		System.getProperties().put("http.proxyHost", "noauthproxy.serc.iisc.ernet.in");
		System.getProperties().put("http.proxyPort", "3128");
	}
	public DownloadManager(int timeoutMillis, long waitTimeMillis) {
		this();
		this.timeoutMillis = timeoutMillis;
		this.waitTimeMillis = waitTimeMillis;
	}
	public static DownloadManager getInstance() {
		if (dm==null) {
			dm = new DownloadManager();
		}
		return dm;
	}
	public static DownloadManager getInstance(int timeoutMillis, long waitTimeMillis) {
		if (dm==null) {
			dm = new DownloadManager(timeoutMillis, waitTimeMillis);
		}
		return dm;
	}
	public Document jsoupParse(URL url, int timeoutMillis, long waitTimeMillis) throws InterruptedException, IOException {
		for (int attempt=0; true; attempt++) {
			try {
				Thread.sleep(waitTimeMillis);
				Document d = Jsoup.parse(url, timeoutMillis);
				System.out.println(String.format("Parsed: %s", url));
				return d;
			} catch(SocketTimeoutException ste) {
				if (attempt==retryAttempts) {
					System.out.println(String.format("Parse timed out: %s (giving up)", url));
					throw ste;
				}
			} catch (HttpStatusException hse) {
				if (attempt==retryAttempts) {
					System.out.println(String.format("Parse HTTP error: %s (giving up)", url));
					throw hse;
				} else {//try again after a longer gap
					Thread.sleep(waitTimeMillis * 6);
				}
			}
		}
	}
	public Document jsoupParse(URL url) throws InterruptedException, IOException {
		return jsoupParse(url, timeoutMillis, waitTimeMillis);
	}
	public void apacheCopyToFile(URL url, File tgtFname) throws InterruptedException, IOException {
		for (int attempt=0; true; attempt++) {
			try {
				Thread.sleep(waitTimeMillis);
				FileUtils.copyURLToFile(url, tgtFname, timeoutMillis, timeoutMillis);
				System.out.println(String.format("Saved: %s to %s", url, tgtFname.getName()));
				break;
			} catch(SocketTimeoutException ste) {
				if (attempt==retryAttempts) {
					System.out.println(String.format("Save timed out: %s (giving up)", url));
					throw ste;
				}
			} catch(IOException ioe) {
				if (attempt==retryAttempts) {
					System.out.println(String.format("Save HTTP error: %s (giving up)", url));
					throw ioe;
				} else {
					Thread.sleep(waitTimeMillis * 6);
				}
			}
		}
	}
}
