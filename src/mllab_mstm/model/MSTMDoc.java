package mllab_mstm.model;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;
import org.apache.commons.io.*;
import org.apache.commons.lang3.*;

public class MSTMDoc {
	public static String PARAMX = "x";
	public static String PARAMZ = "z";
	public static String PARAMTHETA = "theta";
	public static String PARAMPI = "pi";
	public static String PARAMPHI = "phi";
	int T, K, L, D;
	Integer[] V;
	File[][] filesl;
	List<HashMap<String,Integer>> w2idl;
	String[][] id2wl = null;
	Integer[][][][] w;
	MSTM mstm;

	public MSTMDoc(int seed, int T, int K, int I, double alpha, double beta, double gamma) {
		this.T = T;
		this.K = K;
		mstm = new MSTM(2013, T, K, I, alpha, beta, gamma);
	}
	public void setFileList(String corpdoclistfile) throws Exception {
		Pattern pws = Pattern.compile("\\s+");//whitespace
		List<List<File>> filesl = new ArrayList<List<File>>();
		String line = null;
		BufferedReader br = new BufferedReader(new FileReader(corpdoclistfile));
		boolean isFirstLine = true;
		D = 0;
		while ( (line=br.readLine()) != null) {
			String[] rec = pws.split(line);
			if (isFirstLine) {
				L = rec.length; //set number of languages
				for (int l=0; l<L; l++) {
					filesl.add( new ArrayList<File>() );
				}
				isFirstLine = false;
			}
			for (int l=0; l<L; l++) {
				filesl.get(l).add(new File(rec[l]));
			}
			D++; //set number of documents
		}
		br.close();
		this.filesl = new File[L][];
		for (int l=0; l<L; l++) {
			//set file list for each language
			this.filesl[l] = filesl.get(l).toArray(new File[0]);
		}
    }
	public void setWidMap(String[] widmapfiles) throws Exception {
		w2idl = new ArrayList<HashMap<String,Integer>>();
		id2wl = new String[L][];
		V = new Integer[L];
		for (int l=0; l<L; l++) {
//			w2idl.add(Corpus.loadWidMapFile(widmapfiles[l]));
			V[l] = w2idl.get(l).size();
			id2wl[l] = new String[V[l]];
			for (Map.Entry<String,Integer> entry : w2idl.get(l).entrySet()) {
				id2wl[l][entry.getValue()] = entry.getKey();
			}
		}
    }
	public void loadCorpora(String corpdoclistfile, String[] widmapfiles) throws Exception {
		setFileList(corpdoclistfile);
		setWidMap(widmapfiles);
		List<Integer[][][]> w = new ArrayList<Integer[][][]>();
// 		List<Integer> Vs = new ArrayList<Integer>();
		Pattern pws = Pattern.compile("\\s+");//whitespace
		for(int l=0; l<L; l++) {
// 			int V = -1;
			List<Integer[][]> corp = new ArrayList<Integer[][]>();
			for(int d=0; d<D; d++) {
				List<Integer[]> doc = new ArrayList<Integer[]>();
				BufferedReader br = new BufferedReader(new FileReader(filesl[l][d]));
				String line;
				while ((line=br.readLine()) != null) {
					List<Integer> sent = new ArrayList<Integer>();
					for (String tok : pws.split(line.trim())) {
						if (tok.equals("")) continue;
						int wid = w2idl.get(l).get(tok);
						sent.add(wid);
// 						if (wid > V) { V = wid; }
					}
					doc.add( sent.toArray( new Integer[0] ));
				}
				br.close();
				corp.add( doc.toArray(new Integer[0][0]) );
			}
// 			Vs.add(V+1);
			w.add(corp.toArray( new Integer[0][0][0] ));
		}
		this.w = w.toArray( new Integer[0][0][0][0] );
// 		this.V = Vs.toArray( new Integer[0] );
	}
	public static class ArrayComparator implements Comparator<Integer>{
		Double[] arr = null;
		public ArrayComparator(Double[] arr) {
			this.arr = arr;
		}
		@Override
		public int compare(Integer i1, Integer i2) {
			if (arr[i2] < arr[i1])
				return -1;
			else if (arr[i2] > arr[i1])
				return 1;
			else
				return 0;
		}
	}
    public static Integer[] sortIndexes(Double[] arr) {
		Integer[] idx = new Integer[arr.length];
		for (int i=0; i<idx.length; i++) { idx[i] = i; }
		Arrays.sort(idx, new ArrayComparator(arr));
		return idx;
    }
    public static String getTopEntries(Double[] arr, Integer[] idx, double sumThresh) {
		int i;
		double tot = 0;
		for (i=0; i<idx.length; i++) {
			tot += arr[idx[i]];
			if (tot >= sumThresh){
				i++;
				break;
			}
		}
		int tillidx = i; //stop before this
		List<String> vec = new ArrayList<String>();
		for (i=0; i<tillidx; i++) {
			vec.add(idx[i] + " " + arr[idx[i]]);
		}
		return StringUtils.join(vec, "\t");
    }
    public Double[] getPerLineTheta(int l, int d, int s) {
		Double[] theta = new Double[K];
		int t,k;
		for (k=0; k<K; k++) {
				theta[k] = 0.0;
			for (t=0; t<T; t++) {
				// theta_ldsk = \sum_t theta_dtk * pi_ldst
				theta[k] += mstm.theta[d][t][k] * mstm.pi[l][d][s][t];
			}
		}
		return theta;
    }
    public void saveParams(String thetadir, String[] pidirs, String[] phidirs,
		double sumThresh) throws Exception{
		int l, d, s, t;//n, k, t, v;
		String line;
		// save theta
		for (d=0; d<D; d++) {
// 			File thetafile = new File(thetadir, filesl[0][d].getName());
			File thetafile = new File(thetadir, String.valueOf(d+1));
			BufferedWriter bw = new BufferedWriter(new FileWriter(thetafile));
			//filenames
			List<String> fnames = new ArrayList<String>();
			for (l=0; l<L; l++) {
				fnames.add(filesl[l][d].getAbsolutePath());
			}
			line = "# " + StringUtils.join(fnames, ", ");
			bw.write(line); bw.newLine();
			//topic vectors, one per line
			for (t=0; t<T; t++) {
				line = t + " : ";
				bw.write(line);
				bw.write(getTopEntries(mstm.theta[d][t], sortIndexes(mstm.theta[d][t]), sumThresh));
				bw.newLine();
			}
			bw.close();
		}
		// save pi
		for (l=0; l<L; l++) {
			for (d=0; d<D; d++) {
// 				File pifile = new File(pidirs[l], filesl[l][d].getName());
				File pifile = new File(pidirs[l], String.valueOf(d+1));
				BufferedWriter bw = new BufferedWriter(new FileWriter(pifile));
				//pi vectors, one per line (sentence)
				for (s=0; s<w[l][d].length; s++) {
// 					bw.write(getTopEntries(mstm.pi[l][d][s], sortIndexes(mstm.pi[l][d][s]), sumThresh));

					//instead of pi vectors, directly save the per-line theta
					// per-line theta = convex combination of doc-thetas based on per-line pi
					Double[] pltheta = getPerLineTheta(l,d,s);
					bw.write(getTopEntries(pltheta, sortIndexes(pltheta), sumThresh));
					bw.newLine();
				}
				bw.close();
			}
		}
		// save phi
		for (l=0; l<L; l++) {
			savePhi(mstm.phi[l], new File(phidirs[l], "phi.gz"));
		}
    }
	public static void savePhi(Double[][] phil, File phifile) throws Exception {
		int k,v;
		int K = phil.length;
		int V = phil[0].length;
// 		Gson gson = new Gson();
// 		String json = gson.toJson(phil);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
			new GZIPOutputStream(new FileOutputStream(phifile)), "UTF-8"));
// 		bw.write(json);
		//first line is: #<K> <V>
		bw.write("#" + String.valueOf(K) + " " + String.valueOf(V));
		bw.newLine();
		//each column is a topic
		for (v=0; v<V; v++) {
			for (k=0; k<K; k++) {
				if (k!=0) bw.write(" ");
				bw.write(String.valueOf(phil[k][v]));
			}
			bw.newLine();
		}
		bw.close();
	}
	public static Double[][] readPhi(File phifile) throws Exception {
		String[] rec;
		BufferedReader br = new BufferedReader(new InputStreamReader(
			new GZIPInputStream(new FileInputStream(phifile)), "UTF-8"));
		//first line is : #<K> <V>
		rec = br.readLine().substring(1).split(" ");
		int K = Integer.parseInt(rec[0]), V = Integer.parseInt(rec[1]);
		Double phil[][] = new Double[K][V];
		for (int v=0; v<V; v++) {
			rec = br.readLine().split(" ");
			for (int k=0; k<K; k++) {
				phil[k][v] = Double.parseDouble(rec[k]);
			}
		}
		br.close();
		return phil;
	}
    private void saveVars(Integer[][][][] corpora, String[] corpdirs)
		throws Exception{
		for (int l=0; l<corpora.length; l++) {
			for (int d=0; d<corpora[l].length; d++) {
				BufferedWriter bw = new BufferedWriter(
// 					new FileWriter(new File(corpdirs[l], filesl[l][d].getName())));
					new FileWriter(new File(corpdirs[l], String.valueOf(d+1))));
				for (int s=0; s<corpora[l][d].length; s++) {
					String line = StringUtils.join(corpora[l][d][s], " ");
					bw.write(line);
					bw.newLine();
				}
				bw.close();
			}
		}
    }
    public void saveVars(String[] xdirs, String[] zdirs) throws Exception {
		saveVars(mstm.x, xdirs);
		saveVars(mstm.z, zdirs);
    }
	public void printTopWords(int numtopw, String topwfile) throws Exception {
		int l,k;
		//write topwords
		BufferedWriter bw = new BufferedWriter(new FileWriter(topwfile));
		for (k=0; k<K; k++) {
			bw.write("Topic "+k); bw.newLine();
			for (l=0; l<L; l++) {
				bw.write("Language "+l+": ");
				//get top words

				Integer[] wids = sortIndexes(mstm.phi[l][k]);
				List<String> topw = new ArrayList<String>();
				double tot = 0.0;
				for (int i=0; i<numtopw; i++) {
					topw.add(id2wl[l][wids[i]]);
					tot += mstm.phi[l][k][wids[i]];
				}
				bw.write(StringUtils.join(topw, " "));
				bw.write("\t("+String.valueOf(tot)+")");
				bw.newLine();
			}
			bw.newLine();
		}
		bw.close();
	}
	public void estimate() throws Exception {
		mstm.L = L;
		mstm.D = D;
		mstm.w = w;
		mstm.V = V;
		mstm.w = w;
		mstm.initLatent();
		mstm.initCounts();
		mstm.setCounts();
		long s = System.currentTimeMillis();
		mstm.sample();
		System.out.println("Sampling time: " + (System.currentTimeMillis()-s));
		mstm.inferParams();
	}
	public static String[] getParamPaths(String rootdir, String param, String[] langs) {
		String[] paths = null;
		if (param.equals(PARAMTHETA)) {
			paths = new String[1];
			paths[0] = rootdir+File.separator+param;
		} else {
			paths = new String[langs.length];
			for (int i=0; i<langs.length; i++) {
				paths[i] = rootdir+File.separator+param+File.separator+langs[i];
			}
		}
		return paths;
	}
	public static void createModeldirs(String modeldir, String[] langs) throws Exception {
		File rootdir = new File(modeldir);
		if (rootdir.exists()) {
			FileUtils.deleteDirectory(rootdir);
			rootdir.mkdir();
		}
		String[] params = {PARAMX, PARAMZ, PARAMTHETA, PARAMPI, PARAMPHI};
		for (String pa : params) {
			for (String path : getParamPaths(modeldir, pa, langs)) {
				(new File(path)).mkdirs();
			}
		}
	}
    public static void estimate(
		String[] langs, String corpdoclistfile, String[] widmapfiles,
		int T, int K, int I, double alpha, double beta, double gamma, double sumThresh,
		String modeldir, int numtopw
		) throws Exception {
		long s;
		createModeldirs(modeldir, langs);
		MSTMDoc mstmdoc = new MSTMDoc(2013, T, K, I, alpha, beta, gamma);
		System.out.print("Loading ...");
		s = System.currentTimeMillis();
		mstmdoc.loadCorpora(corpdoclistfile, widmapfiles);
		System.out.println("done. " + (System.currentTimeMillis()-s));
		System.out.print("Estimating ...");
		s = System.currentTimeMillis();
		mstmdoc.estimate();
		System.out.println("done. " + (System.currentTimeMillis()-s));
		System.out.print("Saving ...");
		s = System.currentTimeMillis();
		mstmdoc.saveVars(getParamPaths(modeldir, PARAMX, langs),
			getParamPaths(modeldir, PARAMZ, langs));
		mstmdoc.saveParams(getParamPaths(modeldir, PARAMTHETA, null)[0],
			getParamPaths(modeldir, PARAMPI, langs),
			getParamPaths(modeldir, PARAMPHI, langs),
			sumThresh);
		mstmdoc.printTopWords(numtopw, modeldir+File.separator+"topw.txt");
		System.out.println("done. " + (System.currentTimeMillis()-s));
	}
	public static void main(String[] args) throws Exception {
		String cmd = args[0];
		int i = 1;
		if (cmd.equals("estimate")) {
			String langs = args[i++];
			String corpdoclistfile = args[i++];
			String widmapfiles = args[i++];
			String T = args[i++];
			String K = args[i++];
			String I = args[i++];
			String alpha = args[i++];
			String beta = args[i++];
			String gamma = args[i++];
			String sumThresh = args[i++];
			String modeldir = args[i++];
			String numtopw = args[i++];
			estimate(langs.split(","), corpdoclistfile, widmapfiles.split(","),
				Integer.parseInt(T), Integer.parseInt(K), Integer.parseInt(I),
				Double.parseDouble(alpha), Double.parseDouble(beta),
				Double.parseDouble(gamma), Double.parseDouble(sumThresh),
				modeldir, Integer.parseInt(numtopw) );
		}
	}
}
