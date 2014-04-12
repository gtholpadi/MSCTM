package mllab_mstm.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import mllab_mstm.corpus.WordIdMap;
import mllab_mstm.util.KeepFilter;
import mllab_mstm.util.Log;
import mllab_mstm.util.MyUtil;
import mllab_mstm.util.StopFilter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;

public class MsctmDocSbp1 {
	String[] id2la;
	HashMap<String,Integer> la2id;
	int T[][], K, L, D, sPerT;
	double[][][] delta;
	double[][][][][] lambda;
	double[] lambdaTemp; //Template for lambda
	double deltaUnif, deltaMult, lambdaMult;
	int[] V;
	File[][] filesdl;
	WordIdMap[] w2idl;
	String[][] id2wl;
	KeepFilter[] kfl;
	MsctmSbp1 msctm;
	int[][][][] w;
	int[][][][][] x;
	private int minSegSize;
	private int minComSize;
	private double[] minFreq;
	private String[] stopFiles;
	private File[] corplistFiles;

	public MsctmDocSbp1(String[] langs, int sPerT, int K, int I, double alpha, double beta, double omega,
		double[] gamma, double delta, double deltaMult, double eta, double[] lambda, double lambdaMult) throws Exception {
		L = langs.length;
		this.id2la = langs;
		la2id = MyUtil.reverseStringArray(id2la);
		this.sPerT = sPerT;
		this.K = K;
		this.deltaUnif = delta;
		this.deltaMult = deltaMult;
		this.lambdaTemp = lambda;
		this.lambdaMult = lambdaMult;
		msctm = new MsctmSbp1(K, I, alpha, beta, omega, gamma, null, eta, null);//, lambdaMult);
	}
	public void setFileList(File[] corplistFiles) throws Exception {
		String[][] filesdl = new String[0][0];
		for (File corplistFile : corplistFiles) {
			filesdl = ArrayUtils.addAll(filesdl, MyUtil.getArrayFromFile(corplistFile, null));
		}
//		String[][] filesdl = MyUtil.getArrayFromFile(new File(corpdoclistfile), null);
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
    }
	public void setWidMap(String[] widmapfiles, String[] stopFiles, double[] minFreq) throws Exception {
		w2idl = new WordIdMap[L];
		id2wl = new String[L][];
		V = new int[L];
		kfl = new KeepFilter[L];
		for (int l=0; l<L; l++) {
			//read widmap 
			w2idl[l] = new WordIdMap(new File(widmapfiles[l]));
			//init keep filter (removing stop words, rare words) 
			kfl[l] = new KeepFilter(w2idl[l].getFreqWords((int)minFreq[l]));
			kfl[l].removeWords(new StopFilter(stopFiles[l]).getStopWords());
			//re-initialize widmap with only keep-words 
			w2idl[l] = new WordIdMap(kfl[l].getKeepWords());
			V[l] = w2idl[l].size();
			id2wl[l] = w2idl[l].getIdWordMap();
		}
    }
	public void loadCorpora(File[] corplistFiles, String[] widmapfiles, int minSegSize, int minComSize, 
			double[] minFreq, String[] stopFiles) throws Exception {
		//save params
		this.minSegSize = minSegSize;
		this.minComSize = minComSize;
		this.minFreq = minFreq;
		this.stopFiles = stopFiles;
		this.corplistFiles = corplistFiles;
		//load corpora
		setFileList(corplistFiles);
		setWidMap(widmapfiles, stopFiles, minFreq);
//		setKeepFilters(stopFiles, minFreq);
		Pattern pws = Pattern.compile("\\s+");
		T = new int[D][L];
		delta = new double[D][L][];
		lambda = new double[D][L][L][][];
		w = new int[D][L][][];
		x = new int[D][L][][][];
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
				if (filesdl[d][l] == null) {
					w[d][l] = new int[0][];
					x[d][l] = new int[L][0][];
					T[d][l] = 0;
					delta[d][l] = new double[0];
					continue;
				}
				MsctmArticle a = MsctmArticle.read(filesdl[d][l]);
				//preprocess article to keep only "good" words
				//do this before hand to avoid empty segments
				//(to avoid the case where all segments are empty;
				// in that case, we want S to be 0.)
				List<String> a1 = new ArrayList<String>();
				for (int s=0; s<a.body.length; s++) {
					String keeptext = kfl[l].keep(a.body[s]);
					if (!keeptext.equals("")) {
						String[] toks = pws.split( kfl[l].keep(a.body[s]) );
						if (toks.length >= minSegSize) {
							a1.add(StringUtils.join(toks, " "));
						}
					}
				}
				a.body = a1.toArray(new String[0]);
				// set w
				int S = a.body.length;
				T[d][l] = (int) Math.ceil(((double)S)/sPerT);
				delta[d][l] = new double[S];
				w[d][l] = new int[S][];
				for (int s=0; s<S; s++) {
					String[] toks = pws.split( a.body[s] );
					int N = toks.length;
					delta[d][l][s] = deltaMult * (deltaUnif>0 ? deltaUnif : N);
					w[d][l][s] = new int[N];
					for(int n=0; n<N; n++) {
						w[d][l][s][n] = w2idl[l].getId(toks[n]);
					}
				}
				// set x
				x[d][l] = new int[L][][];
				for (int l1=0; l1<L; l1++) {
					int C = a.comments[l1].length;
					lambda[d][l][l1] = new double[C][];
					x[d][l][l1] = new int[C][];
					for (int c=0; c<C; c++) {
						String[] toks = pws.split(kfl[l1].keep(a.comments[l1][c].body));
						int M = toks.length;
						if (M<minComSize) { M=0; }
						lambda[d][l][l1][c] = MyUtil.multVectorByScalar(lambdaTemp, 
							lambdaMult != 0 ? lambdaMult : M);
						x[d][l][l1][c] = new int[M];
						for (int m=0; m<M; m++) {
							x[d][l][l1][c][m] = w2idl[l1].getId(toks[m]);
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
    public void saveParams(String paramDir) throws Exception {
    	BufferedWriter bw = new BufferedWriter(new FileWriter(new File(paramDir, "hp.txt")));
    	saveHyperParams(bw);
    	bw.close();
		// save phi
    	savePhi(paramDir);
    	savePsi(paramDir);
		//save widmap = orig widmap - (stopwords + rare words)
		for (int l=0; l<L; l++) {
			w2idl[l].save(new File(paramDir, id2la[l]+".widmap"));
		}
    }
	public  void saveHyperParams(BufferedWriter bw) throws IOException {
		bw.write(String.format("%s\t%d\n", "randSeed", msctm.rand.getInitialSeed()));
		bw.write(String.format("%s\t%f\n", "alpha", msctm.alpha));
		bw.write(String.format("%s\t%f\n", "beta", msctm.beta));
		bw.write(String.format("%s\t%f\n", "omega", msctm.omega));
		bw.write(String.format("%s\t%s\n", "gamma", ArrayUtils.toString(msctm.gamma)));
		bw.write(String.format("%s\t%f\n", "deltaUnif", deltaUnif));
		bw.write(String.format("%s\t%f\n", "deltaMult", deltaMult));
		bw.write(String.format("%s\t%f\n", "eta", msctm.eta));
		bw.write(String.format("%s\t%s\n", "lambdaTemp", ArrayUtils.toString(lambdaTemp)));
		bw.write(String.format("%s\t%f\n", "lambdaMult", lambdaMult));
		bw.write(String.format("%s\t%d\n", "H", msctm.H));
		bw.write(String.format("%s\t%d\n", "I", msctm.I));
		bw.write(String.format("%s\t%d\n", "L", L));
		bw.write(String.format("%s\t%d\n", "D", D));
		bw.write(String.format("%s\t%d\n", "K", K));
		bw.write(String.format("%s\t%d\n", "sPerT", sPerT));	
		bw.write(String.format("%s\t%d\n", "minSegSize", minSegSize));	
		bw.write(String.format("%s\t%d\n", "minComSize", minComSize));	
		bw.write(String.format("%s\t%s\n", "minFreq", ArrayUtils.toString(minFreq)));	
		bw.write(String.format("%s\t%s\n", "corplistFiles", ArrayUtils.toString(corplistFiles)));
		bw.write(String.format("%s\t%s\n", "stopFiles", ArrayUtils.toString(stopFiles)));
		bw.write(String.format("%s\t%s\n", "langs", ArrayUtils.toString(id2la)));
	}
	public void savePhi(String paramDir) throws Exception {
		for (int l=0; l<L; l++) {
			File phifile = new File(paramDir, id2la[l]+".phi.gz");
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
					new GZIPOutputStream(new FileOutputStream(phifile)), "UTF-8"));
			//first line is: #<K> <V>
			bw.write("#" + String.valueOf(K) + " " + String.valueOf(V[l]));
			bw.newLine();
			//each column is a topic
			for (int v=0; v<V[l]; v++) {
				for (int k=0; k<K; k++) {
					if (k!=0) bw.write(" ");
					bw.write(String.valueOf(msctm.phi[l][k][v]));
				}
				bw.newLine();
			}
			bw.close();
		}
	}
	public void savePsi(String paramDir) throws Exception {
		if (msctm.H > 1) {
			for (int l=0; l<L; l++) {
				File psifile = new File(paramDir, id2la[l]+".psi.gz");
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
						new GZIPOutputStream(new FileOutputStream(psifile)), "UTF-8"));
				//first line is: #<V>
				bw.write("#" + String.valueOf(V[l]));
				bw.newLine();
				for (int v=0; v<V[l]; v++) {
					bw.write(String.valueOf(msctm.psi[l][v]));
					bw.newLine();
				}
				bw.close();
			}
		}
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
    public void saveVars(String varDir) throws Exception {
		int[][][][] w = msctm.w;
		int[][][][] z = msctm.z;
		int[][][][] r = msctm.r;
		int[][][][][] x = msctm.x;
		int[][][][][] q = msctm.q;
		int[][][][][] b = msctm.b;
		int[][][][][] y = msctm.y;
		// save vars
		Gson gson = new Gson();
		MyUtil.saveToGzippedTextFile(gson.toJson(q), new File(varDir, "q.gz"));
		MyUtil.saveToGzippedTextFile(gson.toJson(y), new File(varDir, "y.gz"));
		MyUtil.saveToGzippedTextFile(gson.toJson(delta), new File(varDir, "delta.gz"));
		MyUtil.saveToGzippedTextFile(gson.toJson(msctm.Ndlsk), new File(varDir, "Ndlsk.gz"));
		MyUtil.saveToGzippedTextFile(gson.toJson(msctm.Mdll1cs), new File(varDir, "Mdll1cs.gz"));
		MyUtil.saveToGzippedTextFile(gson.toJson(msctm.nll), new File(varDir, "Nll.gz"));
		// print out for qualitative analysis
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
				//save this article and comments
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
					varDir+File.separator+id2la[l], String.valueOf(d+1))));
				String fname = filesdl[d][l] == null ? "" : filesdl[d][l].getPath();
				bw.append('#').append(fname).append('\n');
				bw.append("#w-z r\n");
				for (int s=0; s<w[d][l].length; s++) {
					bw.append(String.format("%d: ", s));
					for (int n=0; n<w[d][l][s].length; n++) {
						if (n!=0) bw.append(", ");
						bw.append(String.format("%s-%d %d", 
								id2wl[l][w[d][l][s][n]], z[d][l][s][n], r[d][l][s][n]));
					}
					bw.newLine();
				}
				bw.newLine();
				bw.append("#x q-y b\n");
				for (int l1=0; l1<L; l1++) {
					for (int c=0; c<x[d][l][l1].length; c++) {
						for (int m=0; m<x[d][l][l1][c].length; m++) {
							if (m!=0) bw.append(", ");
							bw.append(String.format("%s %d-%d %d",id2wl[l1][x[d][l][l1][c][m]],
								q[d][l][l1][c][m], y[d][l][l1][c][m], b[d][l][l1][c][m]));
						}
						bw.newLine();
					}
				}
				bw.close();
			}
		}
    }
    public static String getTopWordsForTopic(double[] topic, int numtopw, String[] id2w) {
    	if (topic.length == 0) {//no words in this language in the corpus
    		return "";
    	}
		Integer[] wids = sortIndexes(topic);
		List<String> topw = new ArrayList<String>();
		double tot = 0.0;
		for (int i=0; i<numtopw && topic[wids[i]]>0; i++) {
			topw.add(id2w[wids[i]]);
			tot += topic[wids[i]];
		}
		return StringUtils.join(topw, " ") + "\t(" + String.valueOf(tot) + ")";
    }
    public void printTopWordsPhi(int numtopw, BufferedWriter bw) throws IOException {
		bw.append("#phi\n");
		for (int k=0; k<K; k++) {
			bw.write("Topic "+k); bw.newLine();
			for (int l=0; l<L; l++) {
				bw.write("Language "+l+": ");
				//get top words
				bw.write(getTopWordsForTopic(msctm.phi[l][k], numtopw, id2wl[l]));
				bw.newLine();
			}
			bw.newLine();
		}
    }
    public void printTopWordsPsi(int numtopw, BufferedWriter bw)  throws IOException {
		if (msctm.H > 1) {
			bw.append("#psi\n");
			for (int l=0; l<L; l++) {
				bw.write("Language "+l+": ");
				//get top words
				bw.write(getTopWordsForTopic(msctm.psi[l], numtopw, id2wl[l]));
				bw.newLine();
			}
		}
    }
	public void printTopWords(int numtopw, File topwfile) throws Exception {
		BufferedWriter bw = new BufferedWriter(new FileWriter(topwfile));
		printTopWordsPhi(numtopw, bw);
		bw.newLine();
		printTopWordsPsi(numtopw, bw);
		bw.close();
	}
	public void estimate() throws Exception {
		Log.prln(String.format("I=%d D=%d L=%d",msctm.I,D,L));
		msctm.delta = delta;
		msctm.lambda = lambda;
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
		String[] langs, File[] corplistFiles, String[] widmapfiles, String[] stopFiles, int sPerT, int K, int I,
		double alpha, double beta, double omega, double[] gamma, double delta, double deltaMult, double eta, 
		double[] lambda, double lambdaMult, double sumThresh, String modeldir, int numtopw, int minSegSize, int mincomSize, 
		double[] minFreq) throws Exception {
		long s;
		createModeldirs(modeldir, langs);		
		MsctmDocSbp1 msctmdoc = new MsctmDocSbp1(langs, sPerT, K, I, alpha, beta, omega, gamma, delta, deltaMult, eta, lambda, lambdaMult);
		System.out.print("Loading ...");
		s = System.currentTimeMillis();
		msctmdoc.loadCorpora(corplistFiles, widmapfiles, minSegSize, mincomSize, minFreq, stopFiles);
		System.out.println("done. " + (System.currentTimeMillis()-s));
		System.out.print("Estimating ...");
		s = System.currentTimeMillis();
		msctmdoc.estimate();
		System.out.println("done. " + (System.currentTimeMillis()-s));
		System.out.print("Saving ...");
		s = System.currentTimeMillis();
		msctmdoc.saveVars(modeldir+File.separator+"var");
		msctmdoc.saveParams(modeldir+File.separator+"par");
		msctmdoc.printTopWords(numtopw, new File(modeldir+File.separator+"topw.txt"));
		System.out.println("done. " + (System.currentTimeMillis()-s));
	}
	public static void main(String[] args) throws Exception {
		String cmd = args[0];
		int i = 1;
		if (cmd.equals("estimate")) {
			String langs = args[i++];
			String corplistFiles = args[i++];
			String widmapfiles = args[i++];
			String stopFiles = args[i++];
			String sPerT = args[i++];// #sentences/segments per topic vector
			String K = args[i++];
			String I = args[i++];
			String alpha = args[i++];
			String beta = args[i++];
			String omega = args[i++];
			String gamma = args[i++];
			String delta = args[i++];
			String deltaMult = args[i++];
			String eta = args[i++];
			String lambda = args[i++];
			String lambdaMult = args[i++];
			String sumThresh = args[i++];
			String modeldir = args[i++];
			String numtopw = args[i++];
			String minSegSize = args[i++];
			String minComSize = args[i++];
			String minFreq = args[i++];

			String[] gamma1 = gamma.split(",");
			double[] gamma2 = new double[]{Double.parseDouble(gamma1[0]), Double.parseDouble(gamma1[1])};
			String[] lambda1 = lambda.split(",");
			double[] lambda2 = new double[]{Double.parseDouble(lambda1[0]), Double.parseDouble(lambda1[1]), Double.parseDouble(lambda1[1])};

			estimate(langs.split(","), MyUtil.commaSepList2FileArray(corplistFiles), widmapfiles.split(","), stopFiles.split(","), 
				Integer.parseInt(sPerT), Integer.parseInt(K), Integer.parseInt(I),
				Double.parseDouble(alpha), Double.parseDouble(beta), Double.parseDouble(omega),
				gamma2, Double.parseDouble(delta), Double.parseDouble(deltaMult), Double.parseDouble(eta),
				lambda2, Double.parseDouble(lambdaMult), Double.parseDouble(sumThresh), modeldir, 
				Integer.parseInt(numtopw), Integer.parseInt(minSegSize), Integer.parseInt(minComSize),
				MyUtil.strToDoubleArr(minFreq, ","));
		} else {
			double[][] nu = new double[][]{ {1., 2., 5.}, {3., 4., 6.} };
//			File[] files = (new File(".")).listFiles();
//			System.out.println(ArrayUtils.toString(files));
			System.out.println(ArrayUtils.toString(nu));
		}
	}
}
