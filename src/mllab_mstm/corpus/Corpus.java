package mllab_mstm.corpus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import mllab_mstm.model.MsctmArticle;
import mllab_mstm.util.MyUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class Corpus {
	static Pattern pws = Pattern.compile("\\s+");//whitespace
	public Corpus(String corpdir) throws Exception {

	}
	public static File[] getFileArrayFromList(File corplistFile, int colnum) throws Exception {
		File[] corplistFiles = new File[]{corplistFile};
		return getFileArrayFromList(corplistFiles, colnum);
	}
	public static File[] getFileArrayFromList(File[] corplistFiles, int colnum) throws Exception {
		String[][] filesdl = new String[0][0];
		for (File corplistFile : corplistFiles) {
			filesdl = ArrayUtils.addAll(filesdl, MyUtil.getArrayFromFile(corplistFile, null));
		}
//		String[][] filesdl = MyUtil.getArrayFromFile(filelistfile, null);
		File[] files;
		if (colnum > 0) {//get files in a particular column
			files = new File[filesdl.length];
			colnum--;
			for (int i=0; i<filesdl.length; i++) {
				if (filesdl[i][colnum].equals("")) {
					files[i] = null;
				} else {
					files[i] = new File(filesdl[i][colnum]);
				}
			}
		} else {//get all files
			List<File> files1 = new ArrayList<File>();
			for (String[] fs : filesdl) {
				for (String f : fs) {
					files1.add( f.equals("") ? null : new File(f) );
				}
			}
			files = files1.toArray(new File[0]);
		}
		return files;
	}
	public static void getwordidmapflist(File filelistfile, int colnum, File wmapfile)
		throws Exception {
		getwordidmap(getFileArrayFromList(filelistfile, colnum), wmapfile);
	}
	public static void getwordidmapdir(File corpdir, File wmapfile) throws Exception {
		getwordidmap((corpdir).listFiles(), wmapfile);
	}
	public static void getwordidmap(File[] files, File wmapfile) throws Exception {
		WordIdMap wim = new WordIdMap();
		for (File f : files) {
			if (f==null) continue;
			for (String tok : MyUtil.getDoc(f)) {
				wim.addToken(tok);
			}
		}
		wim.save(wmapfile);
	}
	public static void getwordidmapmsctm(File[] corplistFiles, String lang, File wmapfile)
		throws Exception {
		WordIdMap wim = new WordIdMap();
		for (File f : getFileArrayFromList(corplistFiles, 0)) {
			if (f==null) continue;
			for (String tok : MsctmArticle.read(f).getTokens(lang)) {
				wim.addToken(tok);
			}
		}
		wim.save(wmapfile);
	}
	public static void getwikirel(File corplistFile, File indexDir, File stopFile, 
			int maxHits, File wikirelFile) throws Exception {
		LuceneIndex li = new LuceneIndex(indexDir, maxHits, stopFile);
		wikirelFile.delete();
		for (String[] tuple : MyUtil.getArrayFromFile(corplistFile, null)) {
			for (String fname : tuple) {
				if (!fname.equals("")) {
					//get article contents
					String content = StringUtils.join((MsctmArticle.read(new File(fname))).body, ' ');
					//query index
					List<String> relWiki;
					if (content.equals("")) {
						relWiki = new ArrayList<String>();
					} else {
						relWiki = li.search(content);
					}
					//add results to list
					relWiki.add(0, fname);
					FileUtils.write(wikirelFile, StringUtils.join(relWiki, '\t') + "\n", "UTF-8", true);
				}
			}
		}
	}
	private static void getWikiCorplist(File corplistFile, File wikirelFile, Double wiki2newsRatio,
			String l1, File l1l2IdmapFile, File wikiCorplistFile) throws Exception {
		//read wikirelfile
		int resultListSize = 0;
		File wikiDir, l1Dir = null, l2Dir;
		boolean first = true;
		HashMap<String,String[]> news2wiki = new HashMap<String,String[]>();
		for (String[] row : MyUtil.getArrayFromFile(wikirelFile, null)) {
			if (first) {
				resultListSize = row.length-1;
				l2Dir = new File(row[1]).getParentFile();
				wikiDir = l2Dir.getParentFile();
				l1Dir = new File(wikiDir.getPath(), l1);
				first = false;
			}
			news2wiki.put(row[0], row);
		}
		//read corplist
		String[][] newsTuples = MyUtil.getArrayFromFile(corplistFile, null);
		int nNewsArts = newsTuples.length;
		int nWikiArts = (int) (wiki2newsRatio * nNewsArts);
		HashSet<String> wikiArts = new HashSet<String>();
		//collect wiki articles
		int resultListPos = 1;
		int iNews = 0;
		while(true) {
			//process each row in corplist
			for (String newsArt : newsTuples[iNews]) {
				if (!newsArt.equals("")) {
					String[] wikiRelArt = news2wiki.get(newsArt);
					//if we run out lucene results, ignore this article
					if (resultListPos < wikiRelArt.length) {
						wikiArts.add(wikiRelArt[resultListPos]);
					}
				}
			}
			iNews++;
			//if all rows finished, start again but use next file in result list
			if (iNews == nNewsArts){
				iNews = 0;
				resultListPos++;
			}
			//break if we have enough wiki articles, or if all results have been exhausted
			if (wikiArts.size() == nWikiArts || resultListPos > resultListSize) {
				break;
			}
		}
		//read id map
		HashMap<String, String> l2Tol1Id = new HashMap<String, String>();
		for (String[] l1l2 : MyUtil.getArrayFromFile(l1l2IdmapFile, null)) {
			l2Tol1Id.put(l1l2[1], l1l2[0]);
		}
		//write wiki corplist file
		wikiCorplistFile.delete();
		for (String l2File : wikiArts) {
			String l2Id = l2File.substring(l2File.lastIndexOf('/')+1, l2File.lastIndexOf('.'));
			String l1Id = l2Tol1Id.get(l2Id);
			String l1File = new File(l1Dir, l1Id+".json").getPath();
			FileUtils.write(wikiCorplistFile, l1File+"\t"+l2File+"\n", "UTF-8", true);
		}
		
	}
	public static void main(String[] args) throws Exception {
		String cmd = args[0];
		int i = 1;
		if (cmd.equals("getwordidmapdir")) {
			String corpdir = args[i++];
			String wmapfile = args[i++];
			getwordidmapdir(new File(corpdir), new File(wmapfile));
		} else if (cmd.equals("getwordidmapflist")) {
			String filelistfile = args[i++];
			String colnum = args[i++];
			String wmapfile = args[i++];
			getwordidmapflist(new File(filelistfile), Integer.parseInt(colnum), new File(wmapfile));
		} else if (cmd.equals("getwordidmapmsctm")) {
			String corplistFiles = args[i++];
			String lang = args[i++];
			String wmapfile = args[i++];
			getwordidmapmsctm(MyUtil.commaSepList2FileArray(corplistFiles), lang, new File(wmapfile));
		} else if (cmd.equals("getwikirel")) {
			String corplistfile = args[i++];
			String indexDir = args[i++];
			String stopFile = args[i++];//should be same as one used during index creation
			String maxHits = args[i++];
			String wikirelfile = args[i++];
			getwikirel(new File(corplistfile), new File(indexDir), new File(stopFile), 
					Integer.parseInt(maxHits), new File(wikirelfile));
		} else if (cmd.equals("getwikicorplist")) {
			String corplistFile = args[i++];
			String wikirelFile = args[i++];
			String wiki2newsRatio = args[i++];
			String l1 = args[i++];
			String l1l2IdmapFile = args[i++];
			String wikiCorplistFile = args[i++];
			getWikiCorplist(new File(corplistFile), new File(wikirelFile), 
					Double.parseDouble(wiki2newsRatio), l1, new File(l1l2IdmapFile), 
					new File(wikiCorplistFile));
		} 
	}
}