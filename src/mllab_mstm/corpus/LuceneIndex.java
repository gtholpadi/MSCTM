package mllab_mstm.corpus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import mllab_lucene.TestDemo;
import mllab_mstm.model.MsctmArticle;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class LuceneIndex {
	public static String FLDTYPE_PATH = "path"; 
	public static String FLDTYPE_CONTENTS = "contents";
	static Pattern pws = Pattern.compile("\\s+");
	IndexReader reader;
	IndexSearcher searcher;
	Analyzer analyzer;
	QueryParser contentParser;
	int maxHits;
	public LuceneIndex(File indexDir, int maxHits, File stopFile) throws Exception {
		reader = DirectoryReader.open(FSDirectory.open(indexDir));
		searcher = new IndexSearcher(reader);
		analyzer = mllab_lucene.MyUtil.getAnalyzer(stopFile);
		contentParser = new QueryParser(Version.LUCENE_44, TestDemo.FLDTYPE_CONTENTS, analyzer);
		this.maxHits = maxHits;
	}
	public List<String> search(String sQuery) throws ParseException, IOException {
		List<String> results = new ArrayList<String>();
		if (!sQuery.equals("")) {//return empty list for empty query
			Query query = null;
			try {
				query = contentParser.parse(QueryParser.escape(sQuery));
			} catch(ParseException pe) {
				if (pe.getCause() instanceof BooleanQuery.TooManyClauses) {//truncate long queries
					sQuery = StringUtils.join(pws.split(sQuery), ' ', 0, BooleanQuery.getMaxClauseCount()-1);
					query = contentParser.parse(QueryParser.escape(sQuery));
				} else {
					throw pe;
				}
			}
			TopDocs docs = searcher.search(query, maxHits);
			//assemble list of wiki articles
			for (ScoreDoc hit : docs.scoreDocs) {
				results.add(searcher.doc(hit.doc).get(TestDemo.FLDTYPE_PATH));
			}
		}
		return results;
	}
	public static void indexArticles(File artDir, File indexDir, File stopFile) 
			throws Exception {
		//init index
		Directory dir = FSDirectory.open(indexDir);
		Analyzer analyzer;
		analyzer = new StandardAnalyzer(Version.LUCENE_44, 
				new BufferedReader(new InputStreamReader(new FileInputStream(stopFile))));
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_44, analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		IndexWriter writer = new IndexWriter(dir, iwc);
		//write to index
		for (File f : artDir.listFiles()) {
			//get doc
			Document doc = new Document();
			doc.add(new StringField(FLDTYPE_PATH, f.getPath(), Field.Store.YES));
			doc.add(new TextField(FLDTYPE_CONTENTS, StringUtils.join(MsctmArticle.read(f).body, ' '), 
					Field.Store.NO));
			//add to index
			writer.addDocument(doc);
			System.out.println("Added: " + f.getName());
		}
		writer.close();
	}
	public static void main(String[] args) throws Exception {
		String cmd = args[0];
		int i = 1;
	    if (cmd.equals("indexart")) {
			String artDir = args[i++];
			String indexDir = args[i++];
			String stopFile = args[i++];
			indexArticles(new File(artDir), new File(indexDir), new File(stopFile));
	    }
	}
}
