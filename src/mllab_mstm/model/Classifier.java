package mllab_mstm.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import mllab_mstm.util.MyUtil;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class Classifier {
	int K, D, L;
	int[][][][][] q;
	int[][][][][] y;
	double[][][] delta;
	int[][][][] Ndlsk;
	int[][][][][] Mdll1cs;
	MSCTMDocSBP.HyperParam hp;

	double[][][][][] vy;
	double[][][][] vz;
	double[][][][][] prs;
	double tcos, tpr, tNg = 6, tSlenFrac = 0.6;
	int[][][][] clde;
	int[][][][][] clal;

	public Classifier(String modelDir, double tcos, double tpr)
			throws JsonSyntaxException, UnsupportedEncodingException,
			FileNotFoundException, IOException {
		// load model output
		Gson gson = new Gson();
		q = gson.fromJson(
				MyUtil.readGzippedTextFile(new File(modelDir, "var/q.gz")),
				int[][][][][].class);
		y = gson.fromJson(
				MyUtil.readGzippedTextFile(new File(modelDir, "var/y.gz")),
				int[][][][][].class);
		delta = gson.fromJson(
				MyUtil.readGzippedTextFile(new File(modelDir, "var/delta.gz")),
				double[][][].class);
		Ndlsk = gson.fromJson(
				MyUtil.readGzippedTextFile(new File(modelDir, "var/Ndlsk.gz")),
				int[][][][].class);
		Mdll1cs = gson.fromJson(MyUtil.readGzippedTextFile(new File(modelDir,
				"var/Mdll1cs.gz")), int[][][][][].class);
		hp = gson.fromJson(
				MyUtil.readGzippedTextFile(new File(modelDir, "par/hp.gz")),
				MSCTMDocSBP.HyperParam.class);
		D = delta.length;
		L = delta[0].length;
		K = hp.K;
		this.tcos = tcos;
		this.tpr = tpr;
	}

	// for any m, get p(b_dll1cm = s | current assignments of b)
	private double[] getPrS(int d, int l, int l1, int c) {
		int S = Ndlsk[d][l].length;
		double[] prs = new double[S];
		for (int s = 0; s < S; s++) {
			prs[s] = Mdll1cs[d][l][l1][c][s];
		}
		MyUtil.l1normalize(prs);
		return prs;
	}

	// l2-normalized K-vectors for sentences
	private double[][] makeZVectors(int d, int l) {
		int S = Ndlsk[d][l].length;
		double[][] vz = new double[S][K];
		for (int s = 0; s < S; s++) {
			for (int k = 0; k < K; k++) {
				vz[s][k] = Ndlsk[d][l][s][k];
			}
			MyUtil.l2normalize(vz[s]);
		}
		return vz;
	}

	// l2-normalized K-vectors for comments
	private double[] makeYVector(int d, int l, int l1, int c) {
		int M = q[d][l][l1][c].length;
		double[] vy = new double[K];
		for (int m = 0; m < M; m++) {
			if (q[d][l][l1][c][m] == 0) {
				vy[y[d][l][l1][c][m]]++;
			}
		}
		MyUtil.l2normalize(vy);
		return vy;
	}

	// classify all comments
	private void classifyAll() {
		clde = new int[D][L][L][];
		clal = new int[D][L][L][][];
		for (int d = 0; d < D; d++) {
			for (int l = 0; l < L; l++) {
				// for each article
				double[][] vz = makeZVectors(d, l);
				int S = Ndlsk[d][l].length;
				for (int l1 = 0; l1 < L; l1++) {
					int C = q[d][l][l1].length;
					clde[d][l][l1] = new int[C];
					clal[d][l][l1] = new int[C][S];
					for (int c = 0; c < C; c++) {
						// for each comment
						double[] vy = makeYVector(d, l, l1, c);
						double[] prs = getPrS(d, l, l1, c);
						for (int s = 0; s < S; s++) {
							// classify pair for alignment
							// 0 = unrelated, 1 = related
							if (MyUtil.dotProduct(vz[s], vy) >= tcos
									&& prs[s] >= tpr) {
								clal[d][l][l1][c][s] = 1;
							} else {
								clal[d][l][l1][c][s] = 0;
							}
						}
						// classify comment for specificity
						// 0 = irrelevant, 1 = specific, 2 = general
						
						int nrel = 0;//number of related sentences
						for (int related : clal[d][l][l1][c]) {
							if (related == 1) {
								nrel++;
							}
						}
						if (nrel == 0) {
							clde[d][l][l1][c] = 0;
						} else if (nrel < Math.min(tNg, tSlenFrac * S)) {
							clde[d][l][l1][c] = 1;
						} else {
							clde[d][l][l1][c] = 2;
						}
					}
				}
			}
		}
	}

	// save classification (alignment and specificity) to gzipped files
	private void saveClsfn(File outDir) throws Exception {
		if (!outDir.exists()) {
			outDir.mkdir();
		}
		Gson gson = new Gson();
		MyUtil.saveToGzippedTextFile(gson.toJson(clde), new File(outDir, "clde.gz"));
		MyUtil.saveToGzippedTextFile(gson.toJson(clal), new File(outDir, "clal.gz"));
	}

	// classify comments based on output of model
	private static void classify(String modelDir, File outDir, double tcos,
			double tpr) throws Exception {
		Classifier cl = new Classifier(modelDir, tcos, tpr);
		cl.classifyAll();
		cl.saveClsfn(outDir);
	}

	// main
	public static void main(String[] args) throws NumberFormatException,
			Exception {
		String cmd = args[0];
		int i = 1;
		if (cmd.equals("classify")) {
			String modelDir = args[i++];
			String outDir = args[i++];
			String tcos = args[i++];
			String tpr = args[i++];
			classify(modelDir, new File(outDir), Double.parseDouble(tcos),
					Double.parseDouble(tpr));
		} else {
			;
		}
	}
}