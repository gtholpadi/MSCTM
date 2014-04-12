package mllab_mstm.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import mllab_mstm.util.Log;
import mllab_mstm.util.MyUtil;

import org.apache.commons.lang3.ArrayUtils;

import com.google.gson.Gson;

public class MsctmDocSbpSparse extends MsctmDocSbp1 {
//	double[] nutauk;
	double St, K1, V1[];
	
	public MsctmDocSbpSparse(String[] langs, int sPerT, int K, int I,
			double alpha, double beta, double omega, 
//			double[] nutauk, double[] mu,
			double St, double K1, double[] V1,
			double[] gamma,
			double delta, double deltaMult, double eta, double[] lambda,
			double lambdaMult) throws Exception {
		super(langs, sPerT, K, I, alpha, beta, omega, gamma, delta, deltaMult,
				eta, lambda, lambdaMult);
//		this.nutauk = nutauk;
		this.St = St;
		this.K1 = K1;
		this.V1 = V1;
		msctm = new MsctmSbpSparse(K, I, alpha, beta, omega, 
//				null, mu,
				null, null,
				gamma, null, eta, null);
	}
	@Override
	public void loadCorpora(File[] corplistFiles, String[] widmapfiles, int minSegSize, int minComSize, 
			double[] minFreq, String[] stopFiles) throws Exception {
		super.loadCorpora(corplistFiles, widmapfiles, minSegSize, minComSize, minFreq, stopFiles);
		MsctmSbpSparse msctm = (MsctmSbpSparse) this.msctm;
		//reset K1 and V1
			//first ensure they are in legal range
		K1 = Math.min(K1, K/2);
		for (int l=0; l<L; l++) {
			V1[l] = Math.min(V1[l], V[l]/4);
		}
			//reset to enforce sparsity
		for (int l=0; l<L; l++) {
			V1[l] = Math.min(V1[l], K1/K * V[l]/2.1);
		}
		//set mu
		double[][] mu = new double[L][2];
		for (int l=0; l<L; l++) {
			mu[l][0] = Math.max(1.1, St * K1);
			mu[l][1] = mu[l][0] * (K/K1 - 1);
		}
		msctm.mu = mu;
		//set nu
		double[][] nu = new double[L][2];
		for (int l=0; l<L; l++) {
			nu[l][0] = Math.max(1.1, St * V1[l]);
			nu[l][1] = nu[l][0] * (K1/K * V[l]/V1[l] - 1);
		}
		msctm.nu = nu;
	}
	@Override
	public void saveHyperParams(BufferedWriter bw) throws IOException {
		super.saveHyperParams(bw);
		MsctmSbpSparse msctm = (MsctmSbpSparse) this.msctm;
		bw.write(String.format("%s\t%s\n", "nu", ArrayUtils.toString(msctm.nu)));
		bw.write(String.format("%s\t%s\n", "mu", ArrayUtils.toString(msctm.mu)));
	}
	@Override
	public void saveVars(String varDir) throws Exception {
		super.saveVars(varDir);
		// save vars
		Gson gson = new Gson();
		MsctmSbpSparse msctm = (MsctmSbpSparse) this.msctm;
		MyUtil.saveToGzippedTextFile(gson.toJson(msctm.Valk), new File(varDir, "Valk.gz"));
		MyUtil.saveToGzippedTextFile(gson.toJson(msctm.Vlk), new File(varDir, "Vlk.gz"));
		MyUtil.saveToGzippedTextFile(gson.toJson(msctm.Klv), new File(varDir, "Klv.gz"));
		MyUtil.saveToGzippedTextFile(gson.toJson(msctm.a1), new File(varDir, "a1.gz"));
		MyUtil.saveToGzippedTextFile(gson.toJson(msctm.e1), new File(varDir, "e1.gz"));
		MyUtil.saveToGzippedTextFile(gson.toJson(msctm.f1), new File(varDir, "f1.gz"));
	}
    public static void estimate(
		String[] langs, File[] corplistFiles, String[] widmapfiles, String[] stopFiles, int sPerT, int K, int I,
		double alpha, double beta, double omega, 
//		double[] nutauk, double[] mu,
		double St, double K1, double[] V1l,
		double[] gamma, double delta, double deltaMult, double eta, 
		double[] lambda, double lambdaMult, double sumThresh, String modeldir, int numtopw, int minSegSize, int mincomSize, 
		double[] minFreq) throws Exception {
		long s;
		createModeldirs(modeldir, langs);		
		MsctmDocSbpSparse msctmdoc = new MsctmDocSbpSparse(langs, sPerT, K, I, alpha, beta, omega, 
//				nutauk, mu,
				St, K1, V1l,
				gamma, delta, deltaMult, eta, lambda, lambdaMult);
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
			String St = args[i++];//strength parameter
			String K1= args[i++];//word specificity parameter
			String V1 = args[i++];//topic sparsity parameter for each language
//			String nutauk = args[i++];//[1.1, #words allowed in a topic] //[1.1 or 2, 0.05 to 0.99]
//			String mutauv = args[i++];//[1.1 or 2, 0.05 to 0.99]
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

//			double[] nutauk1 = MyUtil.strToDoubleArr(nutauk, ",");
////			nutauk1[1] = nutauk1[0]/nutauk1[1] - nutauk1[0];
//			double[] mu = MyUtil.strToDoubleArr(mutauv, ",");
//			mu[1] = mu[0]/mu[1] - mu[0];

			estimate(langs.split(","), MyUtil.commaSepList2FileArray(corplistFiles), widmapfiles.split(","), stopFiles.split(","), 
				Integer.parseInt(sPerT), Integer.parseInt(K), Integer.parseInt(I),
				Double.parseDouble(alpha), Double.parseDouble(beta), Double.parseDouble(omega), 
//				nutauk1, mu,
				Double.parseDouble(St), Double.parseDouble(K1), MyUtil.strToDoubleArr(V1, ","), 
				MyUtil.strToDoubleArr(gamma, ","), Double.parseDouble(delta), Double.parseDouble(deltaMult), Double.parseDouble(eta),
				MyUtil.strToDoubleArr(lambda, ","), Double.parseDouble(lambdaMult), Double.parseDouble(sumThresh), modeldir, 
				Integer.parseInt(numtopw), Integer.parseInt(minSegSize), Integer.parseInt(minComSize),
				MyUtil.strToDoubleArr(minFreq, ","));
		} else {
			Log.pr(Math.ceil(((double)5)/99999));
		}
	}
}
