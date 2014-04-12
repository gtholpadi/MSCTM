package mllab_mstm.model;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;

import mllab_mstm.util.MyUtil;

import org.apache.commons.math3.distribution.*;
import org.apache.commons.math3.util.*;
import org.apache.commons.math3.random.*;
import org.apache.commons.lang3.*;

public class Inferencer {
	RandomGenerator gen;
	double alpha, gamma;
	int T, K, I, V;
	int S;
	Double[][] phi;
	HashMap<String,Integer> w2id;

	Integer[][] w;
	Integer[][] x;
	Integer[][] z;

	Integer[][] Ntk;
	Integer[] Nt;
	Integer[][] Nst;
	Integer[] Ns;
	Integer[][] Nkv;
	Integer[] Nk;

	Double[][] theta;
	Double[][] pi;
public Inferencer(int seed, int T, int I, double alpha, double gamma,
		String phiFile, String widMapFile) throws Exception {
		gen = new MersenneTwister(seed);
		this.T = T;
		this.I = I;
		this.alpha = alpha;
		this.gamma = gamma;
//		w2id = Corpus.loadWidMapFile(widMapFile);
		V = w2id.size();
		loadPhi(phiFile);
	}
	void loadPhi(String phiFile) throws Exception {
		int k,v;
		Pattern pws = Pattern.compile("\\s+");
		BufferedReader br = new BufferedReader(new InputStreamReader(
			new GZIPInputStream(new FileInputStream(phiFile)), "UTF-8"));
		String line;
		boolean firstLine = true;
		v = 0;
		while ( (line=br.readLine()) !=  null) {
			String[] rec = pws.split(line);
			if(firstLine) {
				firstLine = false;
				K=rec.length;
				phi = new Double[K][V];
			}
// 			sb.append(line);
			for(k=0; k<K; k++) {
				phi[k][v] = Double.parseDouble(rec[k]);
			}
		}
		br.close();
/*		Gson gson = new Gson();
		phi = gson.fromJson(sb.toString(), Double[][].class);*/
/*		K = phi.length;
		V = phi[0].length;*/
	}
	public void setDoc(String[] lines) {
		Pattern pws = Pattern.compile("\\s+");//whitespace
		List<Integer[]> doc = new ArrayList<Integer[]>();
		for (String line : lines) {
			List<Integer> sent = new ArrayList<Integer>();
			for (String tok : pws.split(line.trim())) {
				if (tok.equals("")) continue;
				int wid = w2id.get(tok);
				sent.add(wid);
			}
			doc.add( sent.toArray(new Integer[0]) );
		}
		w = doc.toArray(new Integer[0][0]);
		S = w.length;
	}
	public void setDoc(String docFile) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(docFile));
		String line = null;
		List<String> lines = new ArrayList<String>();
		while ((line=br.readLine()) != null) {
			lines.add(line);
		}
		br.close();
		setDoc(lines.toArray(new String[0]));
	}
    public void initLatent() {
		x = new Integer[S][];
		z = new Integer[S][];
		for (int s=0; s<S; s++) {
			int N = w[s].length;
			x[s] = new Integer[N];
			z[s] = new Integer[N];
			for (int n=0; n<N; n++) {
				x[s][n] = gen.nextInt(T);
				z[s][n] = gen.nextInt(K);
			}
		}
	}
	public void initCounts() {
//		int t, k, s, v;
		Ntk = new Integer[T][K];
		Nt = new Integer[T];
		Nst = new Integer[S][T];
		Ns = new Integer[S];
		Nkv = new Integer[K][V];
		Nk = new Integer[K];
// 		for (t=0; t<T; t++) {
// 			for (k=0; k<K; k++) {
// 				Ntk[t][k] = 0;
// 			}
// 			Nt[t] = 0;
// 		}
// 		for (s=0; s<S; s++) {
// 			for (t=0; t<T; t++) {
// 				Nst[s][t] = 0;
// 			}
// 			Ns[s] = 0;
// 		}
// 		for (k=0; k<K; k++) {
// 			for (v=0; v<V; v++) {
// 				Nkv[k][v] = 0;
// 			}
// 			Nk[k] = 0;
// 		}
	}
	public void setCounts() {
		int s,n,t,k,v;
		for (s=0; s<S; s++) {
			for (n=0; n<w[s].length; n++) {
				t = x[s][n];
				k = z[s][n];
				v = w[s][n];
				Ntk[t][k]++;
				Nt[t]++;
				Nst[s][t]++;
				Ns[s]++;
				Nkv[k][v]++;
				Nk[k]++;
			}
		}
	}
	public void sampleAndSetx(int s, int n) {
		int xsn;
		int xsn_old = x[s][n];
		int zsn = z[s][n];
		int wsn = w[s][n];
		//N... to N...{-sn}, i.e. counts without sn
		Ntk[xsn_old][zsn]--;
		Nt[xsn_old]--;
		Nst[s][xsn_old]--;
		Ns[s]--;
		Nkv[zsn][wsn]--;
		Nk[zsn]--;
		//compute conditional distribution
		List<Pair<Integer,Double>> pmf = new ArrayList<Pair<Integer,Double>>();
		for (xsn=0; xsn<T; xsn++) {
			// first term
			double nr1 = alpha + Ntk[xsn][zsn];
			double dr1 = K*alpha + Nt[xsn];
			//second term
			double nr2 = gamma + Nst[s][xsn];
			double dr2 = T*gamma + Ns[s];
			//p(x)
			pmf.add(new Pair<Integer,Double>(xsn, (nr1*nr2)/(dr1*dr2) ));
		}
		//sample from conditional
		EnumeratedDistribution<Integer> sampler =
			new EnumeratedDistribution<Integer>(gen, pmf);
		xsn = sampler.sample();
		//set var
		x[s][n] = xsn;
		//N...{-sn} to N..., i.e. counts with sn
		Ntk[xsn][zsn]++;
		Nt[xsn]++;
		Nst[s][xsn]++;
		Ns[s]++;
		Nkv[zsn][wsn]++;
		Nk[zsn]++;
	}
	public void sampleAndSetz(int s, int n) {
		int zsn;
		int zsn_old = z[s][n];
		int xsn = x[s][n];
		int wsn = w[s][n];
		//N... to N...{-sn}, i.e. counts without sn
		Ntk[xsn][zsn_old]--;
		Nt[xsn]--;
		Nst[s][xsn]--;
		Ns[s]--;
		Nkv[zsn_old][wsn]--;
		Nk[zsn_old]--;
		//compute conditional distribution
		List<Pair<Integer,Double>> pmf = new ArrayList<Pair<Integer,Double>>();
		for (zsn=0; zsn<K; zsn++) {
			// first term
			double nr1 = alpha + Ntk[xsn][zsn];
			double dr1 = K*alpha + Nt[xsn];
			//second term
			double nr2 = phi[zsn][wsn];
			//p(z)
			pmf.add(new Pair<Integer,Double>(zsn, (nr1*nr2)/dr1 ));
		}
		//sample from conditional
		EnumeratedDistribution<Integer> sampler =
			new EnumeratedDistribution<Integer>(gen, pmf);
		zsn = sampler.sample();
		//set var
		z[s][n] = zsn;
		//N...{-sn} to N... i.e. counts with sn
		Ntk[xsn][zsn]++;
		Nt[xsn]++;
		Nst[s][xsn]++;
		Ns[s]++;
		Nkv[zsn][wsn]++;
		Nk[zsn]++;
    }
	public void sample() {
		int s,n;//,t,k,v;
		for (int i=0; i < I; i++) {
			for (s=0; s<S; s++) {
				for (n=0; n<w[s].length; n++) {
					sampleAndSetx(s,n);
					sampleAndSetz(s,n);
				}
			}
		}
	}
    public void inferParams() {
		int s,t,k;
		double tot;
		// allocate memory
		theta = new Double[T][K];
		pi = new Double[S][T];
		// infer theta
		for (t=0; t<T; t++) {
			tot = 0;
			double[] pk = new double[K];
			for (k=0; k<K; k++) {
				pk[k] = alpha + Ntk[t][k];
				tot += pk[k];
			}
			for (k=0; k<K; k++) {
				theta[t][k] = pk[k]/tot;
			}
		}
		// infer pi
		for (s=0; s<S; s++) {
			tot = 0;
			double[] pt = new double[T];
			for (t=0; t<T; t++) {
				pt[t] = gamma + Nst[s][t];
				tot += pt[t];
			}
			for (t=0; t<T; t++) {
				pi[s][t] = pt[t]/tot;
			}
		}
    }
    public void saveParams(String thetaFile, String piFile, double sumThresh)
		throws Exception{
		int s, t; //n, k, t, v;
		// save theta
		BufferedWriter bw = new BufferedWriter(new FileWriter(thetaFile));
		//topic vectors, one per line
		for (t=0; t<T; t++) {
			bw.write(String.valueOf(t) + " : ");
			bw.write(MyUtil.getTopEntries(theta[t], MyUtil.sortIndexes(theta[t]), sumThresh, 20, false));
			bw.newLine();
		}
		bw.close();
		// save pi
		bw = new BufferedWriter(new FileWriter(piFile));
		//pi vectors, one per line (sentence)
		for (s=0; s<S; s++) {
			bw.write(MyUtil.getTopEntries(pi[s], MyUtil.sortIndexes(pi[s]), sumThresh, 10, false));
			bw.newLine();
		}
		bw.close();
    }
    private void saveVars(Integer[][] doc, String docFile) throws Exception{
		BufferedWriter bw = new BufferedWriter(new FileWriter(docFile));
		for (int s=0; s<S; s++) {
			bw.write(StringUtils.join(doc[s], " "));
			bw.newLine();
		}
		bw.close();
    }
    public void saveVars(String xFile, String zFile) throws Exception {
		saveVars(x, xFile);
		saveVars(z, zFile);
    }
    public static String getParamPath(String outdir, String docFile, String param) {
		docFile = (new File(docFile)).getName();
		return outdir+File.separator+docFile+"."+param;
    }
    public static void infer(String phiFile, String widMapFile, int T, int I,
		double alpha, double gamma, double sumThresh,
		String docFile, String outdir) throws Exception {
		Inferencer inf = new Inferencer(2013, T, I, alpha, gamma, phiFile,
			widMapFile);
		inf.setDoc(docFile);
		inf.initLatent();
		inf.initCounts();
		inf.setCounts();
		inf.sample();
		inf.saveVars(getParamPath(outdir, docFile, MSTMDoc.PARAMX),
			getParamPath(outdir, docFile, MSTMDoc.PARAMZ));
		inf.inferParams();
		inf.saveParams(getParamPath(outdir, docFile, MSTMDoc.PARAMTHETA),
			getParamPath(outdir, docFile, MSTMDoc.PARAMPI), sumThresh);
    }
	public static void main(String[] args) throws Exception {
		String cmd = args[0];
		int i = 1;
		if (cmd.equals("infer")) {
			String phiFile = args[i++];
			String widmapFile = args[i++];
			String T = args[i++];
			String I = args[i++];
			String alpha = args[i++];
			String gamma = args[i++];
			String sumThresh = args[i++];
			String docFile = args[i++];
			String outdir = args[i++];
			infer(phiFile, widmapFile, Integer.parseInt(T), Integer.parseInt(I),
				Double.parseDouble(alpha), Double.parseDouble(gamma),
				Double.parseDouble(sumThresh), docFile, outdir);
		}
	}
}