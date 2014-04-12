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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.Pair;

import com.google.gson.Gson;

public class MSTM_old {
	public static String PARAMX = "x";
	public static String PARAMZ = "z";
	public static String PARAMTHETA = "theta";
	public static String PARAMPI = "pi";
	public static String PARAMPHI = "phi";
	RandomGenerator gen = null;
	double alpha, beta, gamma;
	int T, K, I, L, D;
	Integer[] V = null;

	File[][] filesl = null;
	List<HashMap<String,Integer>> w2idl = null;
	String[][] id2wl = null;

	Integer[][][][] w = null;
	Integer[][][][] x = null;
	Integer[][][][] z = null;

	Integer[][][][] Nldtk = null;
	Integer[][][] Nldt = null;

	Integer[][][][] Nldst = null;
	Integer[][][] Nlds = null;

	Integer[][][] Nlkv = null;
	Integer[][] Nlk = null;

	Double[][][] theta = null;
	Double[][][][] pi = null;
	Double[][][] phi = null;

    public MSTM_old(int seed, int T, int K, int I, double alpha, double beta, double gamma) {
		gen = new MersenneTwister(seed);
		this.T = T;
		this.K = K;
		this.I = I;
		this.alpha = alpha;
		this.beta = beta;
		this.gamma = gamma;
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
		for (int l=0; l<L; l++) {
//			w2idl.add(Corpus.loadWidMapFile(widmapfiles[l]));
			id2wl[l] = new String[w2idl.get(l).size()];
			for (Map.Entry<String,Integer> entry : w2idl.get(l).entrySet()) {
				id2wl[l][entry.getValue()] = entry.getKey();
			}
		}
    }
    public void loadCorpora(String corpdoclistfile, String[] widmapfiles) throws Exception {
		setFileList(corpdoclistfile);
		setWidMap(widmapfiles);
		List<Integer[][][]> w = new ArrayList<Integer[][][]>();
		List<Integer> Vs = new ArrayList<Integer>();
		Pattern pws = Pattern.compile("\\s+");//whitespace
		for(int l=0; l<L; l++) {
			int V = -1;
			List<Integer[][]> corp = new ArrayList<Integer[][]>();
			for(int d=0; d<D; d++) {
				List<Integer[]> doc = new ArrayList<Integer[]>();
				BufferedReader br = new BufferedReader(new FileReader(filesl[l][d]));
				String line;
				while ((line=br.readLine()) != null) {
					List<Integer> sent = new ArrayList<Integer>();
					for (String tok : pws.split(line.trim())) {
						if (tok.equals("")) continue;
// 						int wid = Integer.parseInt(tok);
						int wid = w2idl.get(l).get(tok);
						sent.add(wid);
						if (wid > V) { V = wid; }
					}
					doc.add( sent.toArray( new Integer[0] ));
				}
				br.close();
				corp.add( doc.toArray(new Integer[0][0]) );
			}
			Vs.add(V+1);
			w.add(corp.toArray( new Integer[0][0][0] ));
		}
		this.w = w.toArray( new Integer[0][0][0][0] );
		this.V = Vs.toArray( new Integer[0] );
	}
	//load id corpora
    public void loadCorpora1(String corpdoclistfile, String[] widmapfiles) throws Exception {
		setFileList(corpdoclistfile);
		setWidMap(widmapfiles);
		List<Integer[][][]> w = new ArrayList<Integer[][][]>();
		List<Integer> Vs = new ArrayList<Integer>();
		Pattern pws = Pattern.compile("\\s+");//whitespace
		for(int l=0; l<L; l++) {
			int V = -1;
			List<Integer[][]> corp = new ArrayList<Integer[][]>();
			for(int d=0; d<D; d++) {
				List<Integer[]> doc = new ArrayList<Integer[]>();
				BufferedReader br = new BufferedReader(new FileReader(filesl[l][d]));
				String line;
				while ((line=br.readLine()) != null) {
					List<Integer> sent = new ArrayList<Integer>();
					for (String tok : pws.split(line.trim())) {
						if (tok.equals("")) continue;
						int wid = Integer.parseInt(tok);
						sent.add(wid);
						if (wid > V) { V = wid; }
					}
					doc.add( sent.toArray( new Integer[0] ));
				}
				br.close();
				corp.add( doc.toArray(new Integer[0][0]) );
			}
			Vs.add(V+1);
			w.add(corp.toArray( new Integer[0][0][0] ));
		}
		this.w = w.toArray( new Integer[0][0][0][0] );
		this.V = Vs.toArray( new Integer[0] );
	}
//     public void loadCorpora(String[] corpdirs) throws Exception {
// 		List<File[]> filesl = new ArrayList<File[]>();
// 		List<Integer[][][]> w = new ArrayList<Integer[][][]>();
// 		List<Integer> Vs = new ArrayList<Integer>();
// 		Pattern pws = Pattern.compile("\\s+");//whitespace
// 		for(String dir : corpdirs) {
// 			int V = -1;
// 			List<File> files = new ArrayList<File>();
// 			List<Integer[][]> corp = new ArrayList<Integer[][]>();
// 			for(File file : (new File(dir)).listFiles()) {
// 				List<Integer[]> doc = new ArrayList<Integer[]>();
// 				BufferedReader br = new BufferedReader(new FileReader(file));
// 				String line;
// 				while ((line=br.readLine()) != null) {
// 					List<Integer> sent = new ArrayList<Integer>();
// 					for (String tok : pws.split(line.trim())) {
// 						if (tok.equals("")) continue;
// 						int wid = Integer.parseInt(tok);
// 						sent.add(wid);
// 						if (wid > V) { V = wid; }
// 					}
// 					doc.add( sent.toArray( new Integer[0] ));
// 				}
// 				br.close();
// 				corp.add( doc.toArray(new Integer[0][0]) );
// 				files.add(file);
// 			}
// 			Vs.add(V+1);
// 			w.add(corp.toArray( new Integer[0][0][0] ));
// 			filesl.add(files.toArray(new File[0]));
// 		}
// 		this.w = w.toArray( new Integer[0][0][0][0] );
// 		this.V = Vs.toArray( new Integer[0] );
// 		this.filesl = filesl.toArray( new File[0][0] );
// 	}
    private int randx() {
		return gen.nextInt(T);
    }
    private int randz() {
		return gen.nextInt(K);
    }
    public void initLatent() {
		x = new Integer[w.length][][][];
		z = new Integer[w.length][][][];
		for (int l=0; l<w.length; l++) {
			x[l] = new Integer[w[l].length][][];
			z[l] = new Integer[w[l].length][][];
			for (int d=0; d<w[l].length; d++) {
				x[l][d] = new Integer[w[l][d].length][];
				z[l][d] = new Integer[w[l][d].length][];
				for (int s=0; s<w[l][d].length; s++) {
					x[l][d][s] = new Integer[w[l][d][s].length];
					z[l][d][s] = new Integer[w[l][d][s].length];
					for (int n=0; n<w[l][d][s].length; n++) {
						x[l][d][s][n] = randx();
						z[l][d][s][n] = randz();
					}
				}
			}
		}
    }
    public void initCounts() {
		int l,d,s,t,k,v;
		int L = w.length;
		Nldtk = new Integer[L][][][];
		Nldt = new Integer[L][][];
		Nldst = new Integer[L][][][];
		Nlds = new Integer[L][][];
		Nlkv = new Integer[L][][];
		Nlk = new Integer[L][];
		for (l=0; l<L; l++) {
			int D = w[l].length;
			Nldtk[l] = new Integer[D][][];
			Nldt[l] = new Integer[D][];
			Nldst[l] = new Integer[D][][];
			Nlds[l] = new Integer[D][];
			for (d=0; d<D; d++) {
				Nldtk[l][d] = new Integer[T][];
				Nldt[l][d] = new Integer[T];
				for (t=0; t<T; t++) {
					Nldtk[l][d][t] = new Integer[K];
					Nldt[l][d][t] = 0;
					for (k=0; k<K; k++) {
						Nldtk[l][d][t][k] = 0;
					}
				}
				int S = w[l][d].length;
				Nldst[l][d] = new Integer[S][];
				Nlds[l][d] = new Integer[S];
				for (s=0; s<S; s++) {
					Nldst[l][d][s] = new Integer[T];
					Nlds[l][d][s] = 0;
					for (t=0; t<T; t++) {
						Nldst[l][d][s][t] = 0;
					}
				}
			}
			Nlkv[l] = new Integer[K][];
			Nlk[l] = new Integer[K];
			for (k=0; k<K; k++) {
				Nlkv[l][k] = new Integer[V[l]];
				Nlk[l][k] = 0;
				for (v=0; v<V[l]; v++) {
					Nlkv[l][k][v] = 0;
				}
			}
		}
    }
    public void setCounts() {
		int l,d,s,n,t,k,v;
		for (l=0; l<w.length; l++) {
			for (d=0; d<w[l].length; d++) {
				for (s=0; s<w[l][d].length; s++) {
					for (n=0; n<w[l][d][s].length; n++) {
						t = x[l][d][s][n];
						k = z[l][d][s][n];
						v = w[l][d][s][n];
						Nldtk[l][d][t][k]++;
						Nldt[l][d][t]++;
						Nldst[l][d][s][t]++;
						Nlds[l][d][s]++;
						Nlkv[l][k][v]++;
						Nlk[l][k]++;
					}
				}
			}
		}
    }
    public void printCounts() {
		System.out.print("\n**********\nCOUNTS\n");
		int l,d,s,t,k,v;
		int L = w.length;
		for (l=0; l<L; l++) {
			int D = w[l].length;
			for (d=0; d<D; d++) {
				System.out.print("\nNldtk\n");
				for (t=0; t<T; t++) {
					System.out.print(Nldt[l][d][t]+": ");
					for (k=0; k<K; k++) {
						System.out.print(Nldtk[l][d][t][k]+" ");
					}
					System.out.print("\n");
				}
				System.out.print("---");
				int S = w[l][d].length;
				System.out.print("\nNldst\n");
				for (s=0; s<S; s++) {
					System.out.print(Nlds[l][d][s] + ": ");
					for (t=0; t<T; t++) {
						System.out.print(Nldst[l][d][s][t] + " ");
					}
					System.out.print("\n");
				}
				System.out.print("---");
			}
			System.out.print("\nNlkv\n");
			for (k=0; k<K; k++) {
				System.out.print(Nlk[l][k] + ": ");
				for (v=0; v<V[l]; v++) {
					System.out.print(Nlkv[l][k][v] + " ");
				}
				System.out.print("\n");
			}
			System.out.print("---");
		}
    }
    public void sampleAndSetx(int l, int d, int s, int n) {
		int xldsn;
		int xldsn_old = x[l][d][s][n];
		int zldsn = z[l][d][s][n];
		int wldsn = w[l][d][s][n];
		//N... to N...{-ldsn}, i.e. counts without ldsn
		Nldtk[l][d][xldsn_old][zldsn]--;
		Nldt[l][d][xldsn_old]--;
		Nldst[l][d][s][xldsn_old]--;
		Nlds[l][d][s]--;
		Nlkv[l][zldsn][wldsn]--;
		Nlk[l][zldsn]--;
		//compute conditional distribution
		double[] px = new double[T];
		for (xldsn=0; xldsn<T; xldsn++) {
			// first term
			double nr1 = alpha;
			for (int l1=0; l1<w.length;l1++) {
				nr1 += Nldtk[l1][d][xldsn][zldsn];
			}
			double dr1 = K*alpha;
			for (int l1=0; l1<w.length;l1++) {
				dr1 += Nldt[l1][d][xldsn];
			}
			//second term
			double nr2 = gamma + Nldst[l][d][s][xldsn];
			double dr2 = T*gamma + Nlds[l][d][s];
			//p(x)
			px[xldsn] = (nr1*nr2)/(dr1*dr2);
		}
		//sample from conditional
		List<Pair<Integer,Double>> pmf = new ArrayList<Pair<Integer,Double>>();
		for (xldsn=0; xldsn<T; xldsn++) {
			pmf.add(new Pair<Integer,Double>(xldsn, px[xldsn]));
		}
		EnumeratedDistribution<Integer> sampler =
			new EnumeratedDistribution<Integer>(gen, pmf);
		xldsn = sampler.sample();
		//set var
		x[l][d][s][n] = xldsn;
		//N...{-ldsn} to N..., i.e. counts with ldsn
		Nldtk[l][d][xldsn][zldsn]++;
		Nldt[l][d][xldsn]++;
		Nldst[l][d][s][xldsn]++;
		Nlds[l][d][s]++;
		Nlkv[l][zldsn][wldsn]++;
		Nlk[l][zldsn]++;
    }
	public void sampleAndSetz(int l, int d, int s, int n) {
		int zldsn;
		int xldsn = x[l][d][s][n];
		int wldsn = w[l][d][s][n];
		int zldsn_old = z[l][d][s][n];
		//N... to  N...{-ldsn} i.e. counts without ldsn
		Nldtk[l][d][xldsn][zldsn_old]--;
		Nldt[l][d][xldsn]--;
		Nldst[l][d][s][xldsn]--;
		Nlds[l][d][s]--;
		Nlkv[l][zldsn_old][wldsn]--;
		Nlk[l][zldsn_old]--;
		//compute conditional distribution
		double[] pz = new double[K];
		for (zldsn=0; zldsn<K; zldsn++) {
			// first term
			double nr1 = alpha;
			for (int l1=0; l1<w.length;l1++) {
				nr1 += Nldtk[l1][d][xldsn][zldsn];
			}
			double dr1 = K*alpha;
			for (int l1=0; l1<w.length;l1++) {
				dr1 += Nldt[l1][d][xldsn];
			}
			//second term
			double nr2 = beta + Nlkv[l][zldsn][wldsn];
			double dr2 = V[l]*beta + Nlk[l][zldsn];
			//p(z)
			pz[zldsn] = (nr1*nr2)/(dr1*dr2);
		}
		//sample from conditional
		List<Pair<Integer,Double>> pmf = new ArrayList<Pair<Integer,Double>>();
		for (zldsn=0; zldsn<K; zldsn++) {
			pmf.add(new Pair<Integer,Double>(zldsn, pz[zldsn]));
		}
		EnumeratedDistribution<Integer> sampler =
			new EnumeratedDistribution<Integer>(gen, pmf);
		zldsn = sampler.sample();
		//set var
		z[l][d][s][n] = zldsn;
		//N...{-ldsn} to N... i.e. counts with ldsn
		Nldtk[l][d][xldsn][zldsn]++;
		Nldt[l][d][xldsn]++;
		Nldst[l][d][s][xldsn]++;
		Nlds[l][d][s]++;
		Nlkv[l][zldsn][wldsn]++;
		Nlk[l][zldsn]++;
    }
    public void sample() {
		int l,d,s,n;//t,k,v;
		for (int i=0; i < this.I; i++) {
			for (l=0; l<w.length; l++) {
				for (d=0; d<w[l].length; d++) {
					for (s=0; s<w[l][d].length; s++) {
						for (n=0; n<w[l][d][s].length; n++) {
							sampleAndSetx(l,d,s,n);
							sampleAndSetz(l,d,s,n);
						}
					}
				}
			}
		}
    }
    public void inferParams() {
		int l,d,s,t,k,v;
		int L = w.length;
		int D = w[0].length;
		// allocate memory
		theta = new Double[D][T][K];
		pi = new Double[L][D][][];
		phi = new Double[L][K][];
		for (l=0; l<L; l++) {
			for (d=0; d<D; d++) {
				int S = w[l][d].length;
				pi[l][d] = new Double[S][T];
			}
			for(k=0; k<K; k++) {
				phi[l][k] = new Double[V[l]];
			}
		}
/*		// infer theta
		for (d=0; d<D; d++) {
			for (t=0; t<T; t++) {
				List<Pair<Integer,Double> pmf =
					new ArrayList<Pair<Integer,Double>>();
				for (k=0; k<K; k++) {
					double pk = alpha;
					for (l=0; l<L; l++) {
						pk += Nldt[l][d][t][k]
					}
					pmf.add(new Pair<Integer,Double>(k, pk));
				}
				EnumeratedDistribution<Integer> dist =
					new EnumeratedDistribution<Integer>(gen, pmf);
				pmf = dist.getPmf();
				for (k=0; k<K; k++) {
					theta[d][t][k] = pmf.get(k).getValue();
				}
			}
		}	*/
		// infer theta
		for (d=0; d<D; d++) {
			for (t=0; t<T; t++) {
				double tot = 0;
				double[] pk = new double[K];
				for (k=0; k<K; k++) {
					pk[k] = alpha;
					for (l=0; l<L; l++) {
						pk[k] += Nldtk[l][d][t][k];
					}
					tot += pk[k];
				}
				for (k=0; k<K; k++) {
					theta[d][t][k] = pk[k]/tot;
				}
			}
		}
		// infer pi
		for (l=0; l<L; l++) {
			for (d=0; d<D; d++) {
				for (s=0; s<w[l][d].length; s++) {
					double tot = 0;
					double[] pt = new double[T];
					for (t=0; t<T; t++) {
						pt[t] = gamma + Nldst[l][d][s][t];
						tot += pt[t];
					}
					for (t=0; t<T; t++) {
						pi[l][d][s][t] = pt[t]/tot;
					}
				}
			}
		}
		// infer phi
		for (l=0; l<L; l++) {
			for(k=0; k<K; k++) {
				double tot = 0;
				double[] pv = new double[V[l]];
				for (v=0; v<V[l]; v++) {
					pv[v] = beta + Nlkv[l][k][v];
					tot += pv[v];
				}
				for (v=0; v<V[l]; v++) {
					phi[l][k][v] = pv[v]/tot;
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
    public void saveParams(String thetadir, String[] pidirs, String[] phidirs,
		double sumThresh) throws Exception{
		int l, d, s, t;//n, k, t, v;
		int L = w.length;
		int D = w[0].length;
		String line;
		// save theta
		for (d=0; d<D; d++) {
			File thetafile = new File(thetadir, filesl[0][d].getName());
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
				bw.write(getTopEntries(theta[d][t], sortIndexes(theta[d][t]), sumThresh));
				bw.newLine();
			}
			bw.close();
		}
		// save pi
		for (l=0; l<L; l++) {
			for (d=0; d<D; d++) {
				File pifile = new File(pidirs[l], filesl[l][d].getName());
				BufferedWriter bw = new BufferedWriter(new FileWriter(pifile));
				//pi vectors, one per line (sentence)
				for (s=0; s<w[l][d].length; s++) {
					bw.write(getTopEntries(pi[l][d][s], sortIndexes(pi[l][d][s]), sumThresh));
					bw.newLine();
				}
				bw.close();
			}
		}
		// save phi
		Gson gson = new Gson();
		for (l=0; l<L; l++) {
			String json = gson.toJson(phi[l]);
			File phifile = new File(phidirs[l], "phi.gz");
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				new GZIPOutputStream(new FileOutputStream(phifile)), "UTF-8"));
			bw.write(json);
			bw.close();
		}
    }
    private void printVars(Integer[][][][] corpora) {
		for (int l=0; l<corpora.length; l++) {
			for (int d=0; d<corpora[l].length; d++) {
				System.out.println("File: "+filesl[l][d].getAbsolutePath());
				for (int s=0; s<corpora[l][d].length; s++) {
					for (int n=0; n<corpora[l][d][s].length; n++) {
						System.out.print(corpora[l][d][s][n] + "-");
					}
					System.out.println("");
				}
				System.out.println("---");
			}
			System.out.println("\n================\n");
		}
    }
    private void saveVars(Integer[][][][] corpora, String[] corpdirs)
		throws Exception{
		for (int l=0; l<corpora.length; l++) {
			for (int d=0; d<corpora[l].length; d++) {
				BufferedWriter bw = new BufferedWriter(
					new FileWriter(new File(corpdirs[l], filesl[l][d].getName())));
				for (int s=0; s<corpora[l][d].length; s++) {
					String line = StringUtils.join(corpora[l][d][s], " ");
					bw.write(line);
					bw.newLine();
				}
				bw.close();
			}
		}
    }
    public void saveVars(String[] xdirs, String[] zdirs)
		throws Exception {
		saveVars(x, xdirs);
		saveVars(z, zdirs);
    }
    public void printVars() {
 		printVars(w);
		printVars(x);
		printVars(z);
    }
	public void printTopWords(int numtopw, String topwfile, String[] widmapfiles)
		throws Exception {
		int l,k;
		int L = w.length;
		//read widmapfiles
		//TODO instead use w2idl
		String[][] w4id = new String[L][];
		String line;
		Pattern ptab = Pattern.compile("\\t");
		for (l=0; l<L; l++) {
			w4id[l] = new String[V[l]];
			BufferedReader br = new BufferedReader(new FileReader(widmapfiles[l]));
			br.readLine();//skip first line
			while ((line=br.readLine()) != null) {
				String[] rec = ptab.split(line);
				w4id[l][Integer.parseInt(rec[0])] = rec[1];
			}
			br.close();
		}
		//write topwords
		BufferedWriter bw = new BufferedWriter(new FileWriter(topwfile));
		for (k=0; k<K; k++) {
			bw.write("Topic "+k); bw.newLine();
			for (l=0; l<L; l++) {
				bw.write("Language "+l+": ");
				//get top words

				Integer[] wids = sortIndexes(phi[l][k]);
				List<String> topw = new ArrayList<String>();
				Double tot = 0.0;
				for (int i=0; i<numtopw; i++) {
// 					topw.add(w4id[l][wids[i]]);
					topw.add(id2wl[l][wids[i]]);
					tot += phi[l][k][wids[i]];
				}
				bw.write(StringUtils.join(topw, " "));
				bw.write("\t("+String.valueOf(tot)+")");
				bw.newLine();
			}
			bw.newLine();
		}
		bw.close();
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
		String[] langs, String corpdoclistfile, String[] widmapfiles,
		int T, int K, int I, double alpha, double beta, double gamma, double sumThresh,
		String modeldir, int numtopw
		) throws Exception {
		createModeldirs(modeldir, langs);
		MSTM_old mstm = new MSTM_old(2013, T, K, I, alpha, beta, gamma);
		mstm.loadCorpora(corpdoclistfile, widmapfiles);
		mstm.initLatent();
		mstm.initCounts();
		mstm.setCounts();
		mstm.sample();
		mstm.saveVars(getParamPaths(modeldir, PARAMX, langs),
			getParamPaths(modeldir, PARAMZ, langs));
		mstm.inferParams();
		mstm.saveParams(getParamPaths(modeldir, PARAMTHETA, null)[0],
			getParamPaths(modeldir, PARAMPI, langs),
			getParamPaths(modeldir, PARAMPHI, langs),
			sumThresh);
		mstm.printTopWords(numtopw, modeldir+File.separator+"topw.txt",
			widmapfiles);
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
