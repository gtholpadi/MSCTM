package mllab_mstm.corpus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Pattern;

public class WordIdMap {
	private HashMap<String,Integer[]> voc;
	private int wid;
	public WordIdMap(){
		voc = new HashMap<String,Integer[]>();
		wid = 0;
	}
	public WordIdMap(Collection<String> toks) {
		this();
		addTokens(toks);
	}
	public WordIdMap(File widmapFile) throws IOException {
		Pattern ptab = Pattern.compile("\\t");
		voc = new HashMap<String,Integer[]>();
		BufferedReader br = new BufferedReader(new FileReader(widmapFile));
		String line;
		while ( (line=br.readLine()) != null) {
			String[] rec = ptab.split(line);
			voc.put(rec[0], new Integer[] {Integer.parseInt(rec[1]), Integer.parseInt(rec[2])});
		}
		br.close();
		wid = voc.size();
	}
	public int getId(String tok) {
		return voc.get(tok)[0];
	}
	public void addToken(String tok) {
		if (!voc.containsKey(tok)) {
			voc.put(tok, new Integer[]{wid, 0});//word id, #occurrences
			wid++;
		}
		voc.get(tok)[1]++;
	}
	public void addTokens(Collection<String> toks) {
		for(String tok : toks) {
			addToken(tok);
		}
	}
	public void save(File widmapFile) throws IOException {
		// write w-id-map file in format: #NUM_WORDS=<vocab size>
        // word <tab> id <tab> #occurrences
        // ...
		BufferedWriter bw = new BufferedWriter(new FileWriter(widmapFile));
//		bw.append(String.format("#NUM_WORDS=%d",voc.size())).append('\n');
		for (Map.Entry<String,Integer[]> entry : voc.entrySet()) {
			bw.append(String.format("%s\t%d\t%d\n", entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
		}
		bw.close();
	}
	public HashSet<String> getFreqWords(int minFreq) {
		HashSet<String> topw = new HashSet<String>();
		for (Map.Entry<String,Integer[]> entry : voc.entrySet()) {
			if (entry.getValue()[1] >= minFreq) {
				topw.add(entry.getKey());
			}
		}
		return topw;
	}
	public int size() {
		return voc.size();
	}
	public String[] getIdWordMap() {
		String[] id2w = new String[voc.size()];
		for (Map.Entry<String,Integer[]> entry : voc.entrySet()) {
			id2w[entry.getValue()[0]] = entry.getKey();
		}
		return id2w;
	}
}
