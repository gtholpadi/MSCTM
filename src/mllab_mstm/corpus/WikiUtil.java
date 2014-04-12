package mllab_mstm.corpus;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

public class WikiUtil {
	static Pattern pseg = Pattern.compile("\\n\\s*\\n");
	public static String[] getSegments(File wikiArtFile) throws IOException {
		return pseg.split(FileUtils.readFileToString(wikiArtFile, "UTF-8"));
	}
}
