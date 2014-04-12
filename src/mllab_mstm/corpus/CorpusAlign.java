package mllab_mstm.corpus;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import mllab_mstm.util.MyUtil;

import org.apache.commons.math3.random.*;
import org.apache.commons.lang3.*;

public class CorpusAlign {
	static RandomGenerator gen;
	public static void getgroups(String corplistfile, Integer groupSize, Double eta,
		String gdmapfile, Integer randSeed) throws Exception {
		String[][] corp = MyUtil.getLines(new File(corplistfile));
		int D = corp.length;
		int L = corp[0].length;
		int etaG = (int) (eta*groupSize);
		int G = D/groupSize; //number of groups
		gen = new MersenneTwister(randSeed);
		int[][][] gdmap = new int[L][G][groupSize+etaG];
		int l,g,i,d;
		// generate mapping
		for (g=0, d=0; g<G; g++) {
			//docs from the group
			for (i=0; i<groupSize; i++, d++) {
				for (l=0; l<L; l++) {
					gdmap[l][g][i] = d;
				}
			}
			//random noise doc
			for (i=0; i<etaG; i++) {
				for (l=0; l<L; l++) {
					gdmap[l][g][groupSize+i] = gen.nextInt(D);
				}
			}
			//permute doc ordering in-place
			for (l=0; l<L; l++) {
				shuffleIntArr(gdmap[l][g]);
			}
		}
		// save mapping
		BufferedWriter bw = new BufferedWriter(new FileWriter(gdmapfile));
// 		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out));
		for (g=0; g<G; g++) {
			for (l=0; l<L; l++) {
				// print <doc-ids for l0> <tab> <docids for l1>
				if (l!=0) bw.write("\t");
				bw.write(joinIntArr(gdmap[l][g], " "));
			}
			bw.newLine();
		}
		bw.close();
	}
	public static String getDate(String fname, int method) {
		if (method==1) {
			//telegraph: 1071001_nation_story_8381581.utf8
			Pattern pcheck = Pattern.compile("1[0-9]+_[a-z]+_story_[0-9]+\\.utf8");
			if (pcheck.matcher(fname).lookingAt()) {
				String date = "20" + fname.substring(1,3) + fname.substring(3,5) + fname.substring(5,7);
	// 			System.out.println(fname + " "+ date);
				return date;
			} else {
				System.err.println("no match:"+fname);
				return null;
			}
		} else if (method==2) {
			//abp: 1070101_1desh2.pc.utf8
			Pattern pcheck = Pattern.compile("1[0-9]+.*utf8$");
			if (pcheck.matcher(fname).lookingAt()) {
				String date = "20" + fname.substring(1,3) + fname.substring(3,5) + fname.substring(5,7);
	// 			System.out.println(fname + " "+ date);
				return date;
			} else {
				System.err.println("no match:"+fname);
				return null;
			}
		}
		return null;
	}
	public static void getgroupsfire(String dir1, String dir2, String gdmapfile) throws Exception {
		HashMap<String,List<String>> date2flist1 = new HashMap<String,List<String>>();
		HashMap<String,List<String>> date2flist2 = new HashMap<String,List<String>>();
		for (File file : (new File(dir1)).listFiles()) {
			String date = getDate(file.getName(), 1);
			if (date==null) continue;
			if (!date2flist1.containsKey(date)) {
				date2flist1.put(date, new ArrayList<String>());
			}
			date2flist1.get(date).add(file.getAbsolutePath());
		}
		for (File file : (new File(dir2)).listFiles()) {
			String date = getDate(file.getName(), 2);
			if (date==null) continue;
			if (!date2flist2.containsKey(date)) {
				date2flist2.put(date, new ArrayList<String>());
			}
			date2flist2.get(date).add(file.getAbsolutePath());
		}
		BufferedWriter bw = new BufferedWriter(new FileWriter(gdmapfile));
		for (Map.Entry<String,List<String>> e : date2flist1.entrySet()) {
			String date = e.getKey();
			List<String> flist1 = e.getValue();
			if (date2flist2.containsKey(date)) {
				List<String> flist2 = date2flist2.get(date);
				//write line with files from both langs
				bw.write(StringUtils.join(flist1, ","));
				bw.write("\t");
				bw.write(StringUtils.join(flist2, ","));
				bw.newLine();
			} else {
				//write line with files from lang 1
				//skipping this for now
			}
		}
		//write lines with files from lang 2
		//skipping this for now
		bw.close();
	}
	public static String joinIntArr(int[] arr, String sep) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<arr.length; i++) {
			if (i!=0) sb.append(sep);
			sb.append(String.valueOf(arr[i]));
		}
		return sb.toString();
	}
	//shuffle int array in-place
	public static void shuffleIntArr(int[] arr) {
		int i, j, temp, L = arr.length;
		for (i=0; i<L; i++) {
			j = i + gen.nextInt(L-i);
			//swap
			temp = arr[i];
			arr[i] = arr[j];
			arr[j] = temp;
		}
	}
	public static void main(String[] args) throws Exception {
		String cmd = args[0];
		int i = 1;
		if (cmd.equals("testing")) {
			int[] qwe = {1,2,3,4};
// 			String asd = StringUtils.join(qwe, ' ');
			System.out.println("QWE"+StringUtils.join(qwe, " ")+"ASD");
		} else if (cmd.equals("getgroups")) {
			String corplistfile = args[i++];
			String groupSize = args[i++]; //number of good docs per group
			String eta = args[i++];//fraction of groupSize to be added as noise docs
			String gdmapfile = args[i++];//group-doc mapping
			String randSeed = "2013";//args[i++];
			getgroups(corplistfile, Integer.parseInt(groupSize), Double.parseDouble(eta),
				gdmapfile, Integer.parseInt(randSeed));
		} else if (cmd.equals("getgroupsfire")) {
			String dir1 = args[i++];
			String dir2 = args[i++];
			String gdmapfile = args[i++];//group-doc mapping
			getgroupsfire(dir1, dir2, gdmapfile);
		}
	}
}