package mllab_mstm.model;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;

import mllab_mstm.corpus.WordIdMap;
import mllab_mstm.util.Log;
import mllab_mstm.util.MyUtil;

import org.apache.commons.io.*;
import org.apache.commons.lang3.*;

public class MSCTMDoc {
	public static String PARAMX = "x";
	public static String PARAMZ = "z";
	public static String PARAMR = "r";
	public static String PARAMQ = "q";
	public static String PARAMB = "b";
	public static String PARAMY = "y";
	public static String PARAMU = "u";
	public static String PARAMTHETA = "theta";
	public static String PARAMPHI = "phi";
	public static String PARAMPSI = "psi";
	public static String PARAMRHO = "rho";
	String[] id2la;
	HashMap<String,Integer> la2id;
	int T[], K, J, L, D, sPerT;
	int[] V;
	File[][] filesdl;
//	List<HashMap<String,Integer>> w2idl;
	List<WordIdMap> w2idl;
	String[][] id2wl = null;
	MSCTM msctm;
	int[][][][] w;
	int[][][][][] x;

	public MSCTMDoc(String[] langs, int sPerT, int K, int J, int I, double alpha, double beta, double omega,
		double gamma, double delta, double eta, double lambda) throws Exception {
		L = langs.length;
		this.id2la = langs;
		la2id = MyUtil.reverseStringArray(id2la);
		this.sPerT = sPerT;
		this.K = K;
		this.J = J;
		msctm = new MSCTM(K, J, I, alpha, beta, omega, gamma, delta, eta, lambda);
	}
	public void setFileList(String corpdoclistfile) throws Exception {
		String[][] filesdl = MyUtil.getArrayFromFile(new File(corpdoclistfile), null);
		D = filesdl.length;
		Log.prln(String.format("D=%d",D));
		this.filesdl = new File[D][L];
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
				if (!filesdl[d][l].equals("")) {
					this.filesdl[d][l] = new File(filesdl[d][l]);
				}
			}
		}
/*		Pattern pws = Pattern.compile("\\s+");//whitespace
		List<File[]> filesdl = new ArrayList<File[]>();
		String line = null;
		BufferedReader br = new BufferedReader(new FileReader(corpdoclistfile));
		D = 0;
		while ( (line=br.readLine()) != null) {
			String[] rec = pws.split(line);
			File[] fs = new File[L];
			for (int l=0; l<L; l++) {
				fs[l] = new File(rec[l]);
			}
			filesdl.add(fs);
			D++;
		}
		br.close();
		this.filesdl = filesdl.toArray(new File[0][0]);*/
    }
	public void setWidMap(String[] widmapfiles) throws Exception {
//		w2idl = new ArrayList<HashMap<String,Integer>>();
		w2idl = new ArrayList<WordIdMap>();
		id2wl = new String[L][];
		V = new int[L];
		for (int l=0; l<L; l++) {
//			w2idl.add(Corpus.loadWidMapFile(widmapfiles[l]));
			w2idl.add(new WordIdMap(new File(widmapfiles[l])));
			V[l] = w2idl.get(l).size();
//			id2wl[l] = new String[V[l]];
//			for (Map.Entry<String,Integer> entry : w2idl.get(l).entrySet()) {
//				id2wl[l][entry.getValue()] = entry.getKey();
//			}
			id2wl[l] = w2idl.get(l).getIdWordMap();
		}
    }
	public void loadCorpora(String corpdoclistfile, String[] widmapfiles) throws Exception {
		setFileList(corpdoclistfile);
		setWidMap(widmapfiles);
		Pattern pws = Pattern.compile("\\s+");
		T = new int[D];
		w = new int[D][L][][];
		x = new int[D][L][][][];
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
// 				Log.println(String.format("File %d %d(%s)",d,l,filesdl))
				if (filesdl[d][l] == null) {
					w[d][l] = new int[0][];
					x[d][l] = new int[L][0][];
					continue;
				}
				MsctmArticle a = MsctmArticle.read(filesdl[d][l]);
				// set w
				int S = a.body.length;
				T[d] = (int) Math.ceil(((double)S)/sPerT);
				w[d][l] = new int[S][];
				for (int s=0; s<S; s++) {
					String[] toks = pws.split(a.body[s]);
					int N = toks.length;
					w[d][l][s] = new int[N];
					for(int n=0; n<N; n++) {
						try {
						w[d][l][s][n] = w2idl.get(l).getId(toks[n]);
						} catch (Exception e) {
							Log.prln(String.format("%d %d %d %d = %s", d,l,s,n,toks[n]));
							throw e;
						}
					}
				}
				// set x
				x[d][l] = new int[L][][];
				for (int l1=0; l1<L; l1++) {
					int C = a.comments[l1].length;
					x[d][l][l1] = new int[C][];
					for (int c=0; c<C; c++) {
						String[] toks = pws.split(a.comments[l1][c].body);
						int M = toks.length;
						x[d][l][l1][c] = new int[M];
						for (int m=0; m<M; m++) {
// 							Log.println(String.format("%d %d %d %d %d", d,l,l1,c,m));
// 							Log.println(w2idl.get(l1));
// 							Log.println(String.format("tok=%s, id=%d", toks[m], w2idl.get(l1).get(toks[m])));
// 							Log.wt();
							x[d][l][l1][c][m] = w2idl.get(l1).getId(toks[m]);
						}
					}
				}
			}
		}
	}
	public static class ArrayComparator implements Comparator<Integer>{
		double[] arr = null;
		public ArrayComparator(double[] arr) {
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
    public static Integer[] sortIndexes(double[] arr) {
		Integer[] idx = new Integer[arr.length];
		for (int i=0; i<idx.length; i++) { idx[i] = i; }
		Arrays.sort(idx, new ArrayComparator(arr));
		return idx;
    }
//     public static String getTopEntries(Double[] arr, Integer[] idx, double sumThresh) {
// 		int i;
// 		double tot = 0;
// 		for (i=0; i<idx.length; i++) {
// 			tot += arr[idx[i]];
// 			if (tot >= sumThresh){
// 				i++;
// 				break;
// 			}
// 		}
// 		int tillidx = i; //stop before this
// 		List<String> vec = new ArrayList<String>();
// 		for (i=0; i<tillidx; i++) {
// 			vec.add(idx[i] + " " + arr[idx[i]]);
// 		}
// 		return StringUtils.join(vec, "\t");
//     }
//     public Double[] getPerLineTheta(int l, int d, int s) {
// 		Double[] theta = new Double[K];
// 		int t,k;
// 		for (k=0; k<K; k++) {
// 				theta[k] = 0.0;
// 			for (t=0; t<T; t++) {
// 				// theta_ldsk = \sum_t theta_dtk * pi_ldst
// 				theta[k] += msctm.theta[d][t][k] * msctm.pi[l][d][s][t];
// 			}
// 		}
// 		return theta;
//     }
    public void saveParams(String paramDir) throws Exception {
		int l;//, d, s, n, k, t, v;
//		String line;
/*		// save theta
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
				bw.write(getTopEntries(msctm.theta[d][t], sortIndexes(msctm.theta[d][t]), sumThresh));
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
// 					bw.write(getTopEntries(msctm.pi[l][d][s], sortIndexes(msctm.pi[l][d][s]), sumThresh));

					//instead of pi vectors, directly save the per-line theta
					// per-line theta = convex combination of doc-thetas based on per-line pi
					Double[] pltheta = getPerLineTheta(l,d,s);
					bw.write(getTopEntries(pltheta, sortIndexes(pltheta), sumThresh));
					bw.newLine();
				}
				bw.close();
			}
		}*/
		// save phi
		for (l=0; l<L; l++) {
			savePhi(msctm.phi[l], new File(paramDir, id2la[l]+".phi.gz"));
			if (msctm.H > 1) {
				savePhi(msctm.psi[l], new File(paramDir, id2la[l]+".psi.gz"));
			}
		}
    }
	public static void savePhi(double[][] phil, File phifile) throws Exception {
		int k,v;
		int K = phil.length;
		int V = phil[0].length;
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
			new GZIPOutputStream(new FileOutputStream(phifile)), "UTF-8"));
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
	public static double[][] readPhi(File phifile) throws Exception {
		String[] rec;
		BufferedReader br = new BufferedReader(new InputStreamReader(
			new GZIPInputStream(new FileInputStream(phifile)), "UTF-8"));
		//first line is : #<K> <V>
		rec = br.readLine().substring(1).split(" ");
		int K = Integer.parseInt(rec[0]), V = Integer.parseInt(rec[1]);
		double phil[][] = new double[K][V];
		for (int v=0; v<V; v++) {
			rec = br.readLine().split(" ");
			for (int k=0; k<K; k++) {
				phil[k][v] = Double.parseDouble(rec[k]);
			}
		}
		br.close();
		return phil;
	}
/*    private void saveVars(Integer[][][][] corpora, String[] corpdirs)
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
		saveVars(msctm.x, xdirs);
		saveVars(msctm.z, zdirs);
    }*/
    public void saveVars(String varDir) throws Exception {
		int[][][][] w = msctm.w;
		int[][][][] z = msctm.z;
		int[][][][][] x = msctm.x;
		int[][][][][] q = msctm.q;
		int[][][][][] b = msctm.b;
		int[][][][][] y = msctm.y;
		int[][][][][] u = msctm.u;
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
				//save this article and comments
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
					varDir+File.separator+id2la[l], String.valueOf(d))));
				bw.append("#w z\n");
				for (int s=0; s<w[d][l].length; s++) {
					for (int n=0; n<w[d][l][s].length; n++) {
						if (n!=0) bw.append(", ");
						bw.append(String.format("%s %d", id2wl[l][w[d][l][s][n]],z[d][l][s][n]));
					}
					bw.newLine();
				}
				bw.newLine();
				bw.append("#x q-y/u b\n");
				for (int l1=0; l1<L; l1++) {
					for (int c=0; c<x[d][l][l1].length; c++) {
						for (int m=0; m<x[d][l][l1][c].length; m++) {
							if (m!=0) bw.append(", ");
							int yu = (q[d][l][l1][c][m] == 0) ? y[d][l][l1][c][m] : u[d][l][l1][c][m];
							bw.append(String.format("%s %d-%d %d",id2wl[l1][x[d][l][l1][c][m]],
								q[d][l][l1][c][m], yu, b[d][l][l1][c][m]));
						}
						bw.newLine();
					}
				}
				bw.close();
			}
		}
    }
	public void printTopWords(int numtopw, String topwfile) throws Exception {
		int l,k,j;
		//phi
		BufferedWriter bw = new BufferedWriter(new FileWriter(topwfile));
		bw.append("#phi\n");
		for (k=0; k<K; k++) {
			bw.write("Topic "+k); bw.newLine();
			for (l=0; l<L; l++) {
				bw.write("Language "+l+": ");
				//get top words
				Integer[] wids = sortIndexes(msctm.phi[l][k]);
				List<String> topw = new ArrayList<String>();
				double tot = 0.0;
				for (int i=0; i<numtopw; i++) {
					topw.add(id2wl[l][wids[i]]);
					tot += msctm.phi[l][k][wids[i]];
				}
				bw.write(StringUtils.join(topw, " "));
				bw.write("\t("+String.valueOf(tot)+")");
				bw.newLine();
			}
			bw.newLine();
		}
		//psi
		if (msctm.H > 1) {
			bw.newLine();
			bw.append("#psi\n");
			for (j=0; j<J; j++) {
				bw.write("Topic "+j); bw.newLine();
				for (l=0; l<L; l++) {
					bw.write("Language "+l+": ");
					//get top words
					Integer[] wids = sortIndexes(msctm.psi[l][j]);
					List<String> topw = new ArrayList<String>();
					double tot = 0.0;
					for (int i=0; i<numtopw; i++) {
						topw.add(id2wl[l][wids[i]]);
						tot += msctm.psi[l][j][wids[i]];
					}
					bw.write(StringUtils.join(topw, " "));
					bw.write("\t("+String.valueOf(tot)+")");
					bw.newLine();
				}
				bw.newLine();
			}
		}
		bw.close();
	}
	public void estimate() throws Exception {
		msctm.L = L;
		msctm.D = D;
		msctm.V = V;
		msctm.T = T;
		msctm.w = w;
		msctm.x = x;
		msctm.initLatent();
		msctm.initCounts();
		msctm.setCounts();
		long s = System.currentTimeMillis();
		msctm.sample();
		System.out.println("Sampling time: " + (System.currentTimeMillis()-s));
		msctm.inferParams();
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
/*	public static void createModeldirs(String modeldir, String[] langs) throws Exception {
		File rootdir = new File(modeldir);
		if (rootdir.exists()) {
			FileUtils.deleteDirectory(rootdir);
			rootdir.mkdir();
		}
		String[] params = {PARAMZ, PARAMQ, PARAMB, PARAMY, PARAMU, PARAMPHI, PARAMPSI};
		for (String pa : params) {
			for (String path : getParamPaths(modeldir, pa, langs)) {
				(new File(path)).mkdirs();
			}
		}
	}*/
	public static void createModeldirs(String modeldir, String[] langs) throws Exception {
		File rootdir = new File(modeldir);
		if (rootdir.exists()) {
			FileUtils.deleteDirectory(rootdir);
			rootdir.mkdir();
		}
		String varDir = modeldir+File.separator+"var";
		for (String la : langs) {
			new File(varDir+File.separator+la).mkdirs();
		}
		String parDir = modeldir+File.separator+"par";
		new File(parDir).mkdirs();
	}
    public static void estimate(
		String[] langs, String corpdoclistfile, String[] widmapfiles,
		int sPerT, int K, int J, int I,
		double alpha, double beta, double omega, double gamma, double delta, double eta, double lambda,
		double sumThresh, String modeldir, int numtopw
		) throws Exception {
		long s;
		createModeldirs(modeldir, langs);
		MSCTMDoc msctmdoc = new MSCTMDoc(langs, sPerT, K, J, I, alpha, beta, omega, gamma, delta, eta, lambda);
		System.out.print("Loading ...");
		s = System.currentTimeMillis();
		msctmdoc.loadCorpora(corpdoclistfile, widmapfiles);
		System.out.println("done. " + (System.currentTimeMillis()-s));
		System.out.print("Estimating ...");
		s = System.currentTimeMillis();
		msctmdoc.estimate();
		System.out.println("done. " + (System.currentTimeMillis()-s));
		System.out.print("Saving ...");
		s = System.currentTimeMillis();
		msctmdoc.saveVars(modeldir+File.separator+"var");
		msctmdoc.saveParams(modeldir+File.separator+"par");
		msctmdoc.printTopWords(numtopw, modeldir+File.separator+"topw.txt");
		System.out.println("done. " + (System.currentTimeMillis()-s));
	}
	public static void main(String[] args) throws Exception {
		String cmd = args[0];
		int i = 1;
		if (cmd.equals("estimate")) {
			String langs = args[i++];
			String corpdoclistfile = args[i++];
			String widmapfiles = args[i++];
			String sPerT = args[i++];// #sentences/segments per topic vector
			String K = args[i++];
			String J = args[i++];
			String I = args[i++];
			String alpha = args[i++];
			String beta = args[i++];
			String omega = args[i++];
			String gamma = args[i++];
			String delta = args[i++];
			String eta = args[i++];
			String lambda = args[i++];
			String sumThresh = args[i++];
			String modeldir = args[i++];
			String numtopw = args[i++];
			estimate(langs.split(","), corpdoclistfile, widmapfiles.split(","),
				Integer.parseInt(sPerT), Integer.parseInt(K), Integer.parseInt(J), Integer.parseInt(I),
				Double.parseDouble(alpha), Double.parseDouble(beta), Double.parseDouble(omega),
				Double.parseDouble(gamma), Double.parseDouble(delta), Double.parseDouble(eta),
				Double.parseDouble(lambda), Double.parseDouble(sumThresh),
				modeldir, Integer.parseInt(numtopw) );
		}
	}
}
