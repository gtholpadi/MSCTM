package mllab_mstm.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import mllab_mstm.util.MyUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;

public class MSTMGroup {
	public static String PARAMX = "x";
	public static String PARAMZ = "z";
	public static String PARAMTHETA = "theta";
	public static String PARAMPI = "pi";
	public static String PARAMPHI = "phi";
	int T, K, L, D, G;
	Integer[] V;
	File[][] filesl;
	int[][][] gdmapl;
	List<HashMap<String,Integer>> w2idl;
	String[][] id2wl = null;
	Integer[][][][] w;
	MSTM mstm;

	public MSTMGroup(int seed, int T, int K, int I, double alpha, double beta, double gamma) {
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
    public void setGroupMap(String groupdocmapfile) throws Exception {
		int g,l,d;
		Pattern psp = Pattern.compile(" ");//space
		String[][] lines = MyUtil.getLines(new File(groupdocmapfile), "\\t");
		G = lines.length;
		gdmapl = new int[L][G][];
		for (g=0; g<G; g++) {
			for (l=0; l<L; l++) {
				String[] docs = psp.split(lines[g][l]);
				gdmapl[l][g] = new int[docs.length];
				for (d=0; d<docs.length; d++) {
					gdmapl[l][g][d] = Integer.parseInt(docs[d]);
				}
			}
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
	public void loadCorpora(String corpdoclistfile, String groupdocmapfile,
		String[] widmapfiles) throws Exception {
		int l,g,d,n,D,N;
		setFileList(corpdoclistfile);
		setGroupMap(groupdocmapfile);
		setWidMap(widmapfiles);
		w = new Integer[L][G][][];
		String[] doc;
		HashMap<String,Integer> w2id;
		for (l=0; l<L; l++) {
			w2id = w2idl.get(l);
			for (g=0; g<G; g++) {
				D = gdmapl[l][g].length;
				w[l][g] = new Integer[D][];
				for (d=0; d<D; d++) {
					doc = MyUtil.getDoc(filesl[l][gdmapl[l][g][d]]);
					N = doc.length;
					w[l][g][d] = new Integer[N];
					for (n=0; n<N; n++) {
						w[l][g][d][n] = w2id.get(doc[n]);
					}
				}
			}
		}
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
    public Double[] getPerLineTheta(int l, int g, int d) {
		Double[] theta = new Double[K];
		int t,k;
		for (k=0; k<K; k++) {
				theta[k] = 0.0;
			for (t=0; t<T; t++) {
				// theta_ldsk = \sum_t theta_dtk * pi_ldst
				theta[k] += mstm.theta[g][t][k] * mstm.pi[l][g][d][t];
			}
		}
		return theta;
    }
    public void saveParams(String thetadir, String[] pidirs, String[] phidirs,
		double sumThresh) throws Exception{
		int l, g, d, t;//n, k, t, v;
		String line;
		// save theta
		for (g=0; g<G; g++) {
			File thetafile = new File(thetadir, String.valueOf(g));
			BufferedWriter bw = new BufferedWriter(new FileWriter(thetafile));
			//filenames
			for (l=0; l<L; l++) {
				StringBuilder fnames = new StringBuilder("# Language "+l+" : ");
				for (d=0; d<gdmapl[l][g].length; d++) {
					if (d!=0) fnames.append(",");
					fnames.append(filesl[l][gdmapl[l][g][d]].getAbsolutePath());
				}
				bw.write(fnames.toString()); bw.newLine();
			}
			//topic vectors, one per line
			for (t=0; t<T; t++) {
				line = t + " : ";
				bw.write(line);
				bw.write(getTopEntries(mstm.theta[g][t], sortIndexes(mstm.theta[g][t]), sumThresh));
				bw.newLine();
			}
			bw.close();
		}
		// save pi
		for (l=0; l<L; l++) {
			for (g=0; g<G; g++) {
				File pifile = new File(pidirs[l], String.valueOf(g));
				BufferedWriter bw = new BufferedWriter(new FileWriter(pifile));
				//pi vectors, one per line (doc)
				for (d=0; d<gdmapl[l][g].length; d++) {
					bw.write(filesl[l][gdmapl[l][g][d]].getAbsolutePath() + " : ");
// 					bw.write(getTopEntries(mstm.pi[l][g][d], sortIndexes(mstm.pi[l][g][d]), sumThresh));

					//instead of pi vectors, directly save the per-line theta
					// per-line theta = convex combination of group-thetas based on per-line pi
					Double[] pltheta = getPerLineTheta(l,g,d);
					bw.write(getTopEntries(pltheta, sortIndexes(pltheta), sumThresh));
					bw.newLine();
				}
				bw.close();
			}
		}
		// save phi
		Gson gson = new Gson();
		for (l=0; l<L; l++) {
			String json = gson.toJson(mstm.phi[l]);
			File phifile = new File(phidirs[l], "phi.gz");
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				new GZIPOutputStream(new FileOutputStream(phifile)), "UTF-8"));
			bw.write(json);
			bw.close();
		}
    }
    private void saveVars(Integer[][][][] corpora, String[] corpdirs)
		throws Exception{
		for (int l=0; l<corpora.length; l++) {
			for (int g=0; g<corpora[l].length; g++) {
				BufferedWriter bw = new BufferedWriter(
					new FileWriter(new File(corpdirs[l], String.valueOf(g))));
				for (int d=0; d<corpora[l][d].length; d++) {
					bw.write(filesl[l][gdmapl[l][g][d]].getAbsolutePath() + " : ");
					String line = StringUtils.join(corpora[l][g][d], " ");
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
		mstm.D = G;//treat each group as a doc
		mstm.w = w;
		mstm.V = V;
		mstm.initLatent();
		mstm.initCounts();
		mstm.setCounts();
		mstm.sample();
		mstm.inferParams();
	}
	public static String[] getParamPaths(String rootdir, String param, String[] langs) {
		String[] paths = null;
		if (param.equals(PARAMTHETA)) {
			paths = new String[1];
			paths[0] = rootdir+File.separator+PARAMTHETA;
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
		String[] langs, String corpdoclistfile, String groupdocmapfile, String[] widmapfiles,
		int T, int K, int I, double alpha, double beta, double gamma, double sumThresh,
		String modeldir, int numtopw
		) throws Exception {
		createModeldirs(modeldir, langs);
		MSTMGroup mstmdoc = new MSTMGroup(2013, T, K, I, alpha, beta, gamma);
		System.out.print("Loading...");
		mstmdoc.loadCorpora(corpdoclistfile, groupdocmapfile, widmapfiles);
		System.out.println("done");
		System.out.print("Estimating...");
		mstmdoc.estimate();
		System.out.println("done");
		System.out.print("Saving...");
		mstmdoc.saveVars(getParamPaths(modeldir, PARAMX, langs),
			getParamPaths(modeldir, PARAMZ, langs));
		mstmdoc.saveParams(getParamPaths(modeldir, PARAMTHETA, null)[0],
			getParamPaths(modeldir, PARAMPI, langs),
			getParamPaths(modeldir, PARAMPHI, langs),
			sumThresh);
		mstmdoc.printTopWords(numtopw, modeldir+File.separator+"topw.txt");
		System.out.println("done");
	}
	public static void main(String[] args) throws Exception {
		String cmd = args[0];
		int i = 1;
		if (cmd.equals("estimate")) {
			String langs = args[i++];
			String corpdoclistfile = args[i++];
			String groupdocmapfile = args[i++];
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
			estimate(langs.split(","), corpdoclistfile, groupdocmapfile, widmapfiles.split(","),
				Integer.parseInt(T), Integer.parseInt(K), Integer.parseInt(I),
				Double.parseDouble(alpha), Double.parseDouble(beta),
				Double.parseDouble(gamma), Double.parseDouble(sumThresh),
				modeldir, Integer.parseInt(numtopw) );
		}
	}
}
