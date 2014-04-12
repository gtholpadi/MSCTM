package mllab_mstm.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPOutputStream;

import mllab_mstm.corpus.WordIdMap;
import mllab_mstm.util.Log;
import mllab_mstm.util.MyUtil;

public class MsctmDocSbpSparseMultNoise extends MsctmDocSbpSparse {
	int J;
	int Tconst;

	public MsctmDocSbpSparseMultNoise(String[] langs, int sPerT, int Tconst, int K, int J, int I,
			double alpha, double beta, double omega, 
//			double[] nutauk, double[] mu,
			double St, double K1, double[] V1,
			double[] gamma, double delta, double deltaMult, double eta, double xi,
			double[] lambda, double lambdaMult) throws Exception {
		super(langs, sPerT, K, I, alpha, beta, omega, 
//				nutauk, mu,
				St, K1, V1,
				gamma, delta, deltaMult,
				eta, lambda, lambdaMult);
		this.J = J;
		this.Tconst = Tconst;
		msctm = new MsctmSbpSparseMultNoise(K, J, I, alpha, beta, omega, 
//				null, mu,
				null, null,
				gamma, null, eta, null, xi);
	}
	@Override
	public void loadCorpora(File[] corplistFiles, String[] widmapfiles, int minSegSize, int minComSize, 
			double[] minFreq, String[] stopFiles) throws Exception {
		super.loadCorpora(corplistFiles, widmapfiles, minSegSize, minComSize, minFreq, stopFiles);
		//override T computed from sPerT if constant T specified
		if (Tconst != 0) {
			for (int d=0; d<D; d++) {
				for (int l=0; l<L; l++) {
					if (filesdl[d][l] != null) {
						T[d][l] = Tconst;
					}
				}
			}
		}
		//force T=1 if more than 1 article in tuple
		for (int d=0; d<D; d++) {
			int nArts = 0;
			for (int l=0; l<L; l++) {
				if (filesdl[d][l] != null) {
					nArts++;
					if (nArts > 1) {
						break;
					}
				}
			}
			if (nArts > 1) {
				for (int l=0; l<L; l++) {
					if (filesdl[d][l] != null) {
						T[d][l] = 1;
					}
				}
			}
		}
	}
	@Override
	public  void saveHyperParams(BufferedWriter bw) throws IOException {
		super.saveHyperParams(bw);
		MsctmSbpSparseMultNoise msctm = (MsctmSbpSparseMultNoise) this.msctm;
		bw.write(String.format("%s\t%d\n", "Tconst", Tconst));
		bw.write(String.format("%s\t%d\n", "J", J));
		bw.write(String.format("%s\t%f\n", "xi", msctm.xi));
	}
	@Override
	public void savePsi(String paramDir) throws Exception {
		MsctmSbpSparseMultNoise msctm = (MsctmSbpSparseMultNoise) this.msctm;
		for (int l=0; l<L; l++) {
			File psifile = new File(paramDir, id2la[l]+".psi.gz");
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
					new GZIPOutputStream(new FileOutputStream(psifile)), "UTF-8"));
			//first line is: #<J> <V>
			bw.write("#" + String.valueOf(J) + " " + String.valueOf(V[l]));
			bw.newLine();
			for (int v=0; v<V[l]; v++) {
				for (int j=0; j<J; j++) {
					if (j!=0) bw.write(" ");
					bw.write(String.valueOf(msctm.psi[l][j][v]));
				}
				bw.newLine();
			}
			bw.close();
		}
	}
	@Override
    public void printTopWordsPsi(int numtopw, BufferedWriter bw)  throws IOException {
    	MsctmSbpSparseMultNoise msctm = (MsctmSbpSparseMultNoise) this.msctm;
		bw.append("#psi\n");
		for (int j=0; j<J; j++) {
			bw.write("Topic "+j); bw.newLine();
			for (int l=0; l<L; l++) {
				bw.write("Language "+l+": ");
				//get top words
				bw.write(getTopWordsForTopic(msctm.psi[l][j], numtopw, id2wl[l]));
				bw.newLine();
			}
			bw.newLine();
		}
	}
    public static void estimate(
		String[] langs, File[] corplistFiles, String[] widmapfiles, String[] stopFiles, int sPerT, 
		int Tconst, int K, int J, int I, double alpha, double beta, double omega, 
//		double[] nutauk, double[] mu, 
		double St, double K1, double[] V1,
		double[] gamma, double delta, double deltaMult, double eta, double xi, double[] lambda, 
		double lambdaMult, double sumThresh, String modeldir, int numtopw, int minSegSize, 
		int mincomSize,	double[] minFreq) throws Exception {
		long s;
		createModeldirs(modeldir, langs);		
		MsctmDocSbpSparseMultNoise msctmdoc = new MsctmDocSbpSparseMultNoise(langs, sPerT, Tconst, K, J, I, 
				alpha, beta, omega, 
//				nutauk, mu,
				St, K1, V1,
				gamma, delta, deltaMult, eta, xi, lambda, lambdaMult);
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
    // input: phifile and widmap for a language, number of topwords required, target topword file 
    public static void getTopWords(File phiFile, File widmapFile, int numTopw, File topwFile) 
    		throws Exception {
    	String[] id2w = (new WordIdMap(widmapFile)).getIdWordMap();
    	double[][] phil = readPhi(phiFile);
    	int K = phil.length;
    	Log.set(topwFile);
    	for (int k=0; k<K; k++) {
    		Log.prln(String.format("Topic %d : %s", k, getTopWordsForTopic(phil[k], numTopw, id2w)));
    	}
    	Log.reset();
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
			String Tconst = args[i++];
			String K = args[i++];
			String J = args[i++];
			String I = args[i++];
			String alpha = args[i++];
			String beta = args[i++];
			String omega = args[i++];
			String St = args[i++];//strength parameter
			String K1= args[i++];//word specificity parameter
			String V1 = args[i++];//topic sparsity parameter for each language
//			String nutauk = args[i++];//[1.1, #words allowed in a topic] //[1.1 or 2, 0.05 to 0.99]
//			String mutauv = args[i++];//[1.1 or 2, 0.05 to 0.99]
			String gamma = args[i++];
			String delta = args[i++];
			String deltaMult = args[i++];
			String eta = args[i++];
			String xi = args[i++];
			String lambda = args[i++];
			String lambdaMult = args[i++];
			String sumThresh = args[i++];
			String modeldir = args[i++];
			String numtopw = args[i++];
			String minSegSize = args[i++];
			String minComSize = args[i++];
			String minFreq = args[i++];

//			double[] nutauk1 = MyUtil.strToDoubleArr(nutauk, ",");
////			nutauk1[1] = nutauk1[0]/nutauk1[1] - nutauk1[0];
//			double[] mu = MyUtil.strToDoubleArr(mutauv, ",");
//			mu[1] = mu[0]/mu[1] - mu[0];

			estimate(langs.split(","), MyUtil.commaSepList2FileArray(corplistFiles), widmapfiles.split(","), stopFiles.split(","), 
				Integer.parseInt(sPerT), Integer.parseInt(Tconst), Integer.parseInt(K), Integer.parseInt(J), Integer.parseInt(I),
				Double.parseDouble(alpha), Double.parseDouble(beta), Double.parseDouble(omega), 
//				nutauk1, mu,
				Double.parseDouble(St), Double.parseDouble(K1), MyUtil.strToDoubleArr(V1, ","), 
				MyUtil.strToDoubleArr(gamma, ","), Double.parseDouble(delta), Double.parseDouble(deltaMult), 
				Double.parseDouble(eta),Double.parseDouble(xi),	MyUtil.strToDoubleArr(lambda, ","), 
				Double.parseDouble(lambdaMult), Double.parseDouble(sumThresh), modeldir, 
				Integer.parseInt(numtopw), Integer.parseInt(minSegSize), Integer.parseInt(minComSize),
				MyUtil.strToDoubleArr(minFreq, ","));
		} else if (cmd.equals("gettopw")) {
			String phiFile = args[i++];
			String widmapFile = args[i++];
			String numTopw = args[i++];
			String topwFile = args[i++];
			getTopWords(new File(phiFile), new File(widmapFile), Integer.parseInt(numTopw), 
					new File(topwFile));
		} else {
			Log.pr(Math.ceil(((double)5)/99999));
		}
	}
}
