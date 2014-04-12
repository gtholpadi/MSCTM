package mllab_mstm.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import mllab_mstm.util.Log;
import mllab_mstm.util.MyUtil;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

// Class for evaluation
public class Eval {
	int D, L;
	int gsal[][][][][];
	int gsde[][][][];

	public Eval(File gsFile) throws JsonSyntaxException,
			UnsupportedEncodingException, FileNotFoundException, IOException {
		gsal = (new Gson()).fromJson(MyUtil.readGzippedTextFile(gsFile),
				int[][][][][].class);
		D = gsal.length;
		L = gsal[0].length;
		// set detection gs
		gsde = new int[D][L][L][];
		for (int d = 0; d < D; d++) {
			for (int l = 0; l < L; l++) {
				for (int l1 = 0; l1 < L; l1++) {
					int C = gsal[d][l][l1].length;
					gsde[d][l][l1] = new int[C];
					for (int c = 0; c < C; c++) {
						int S = gsal[d][l][l1][c].length;
						for (int s = 0; s < S; s++) {
							// If this comment is related to at least one
							// sentence, then it is a specific comment.
							if (gsal[d][l][l1][c][s] == 1) {
								gsde[d][l][l1][c] = 1;
								break;
							}
						}
					}
				}
			}
		}
	}

	private void evaldetect(File cldeFile) throws Exception {
		int[][][][] clde = (new Gson()).fromJson(
				MyUtil.readGzippedTextFile(cldeFile), int[][][][].class);

		// init counts
		int[][] tp = new int[D][L];
		int[][] tn = new int[D][L];
		int[][] fp = new int[D][L];
		int[][] fn = new int[D][L];
		double[][] precdl = new double[D][L];
		double[][] recadl = new double[D][L];
		double[][] revprecdl = new double[D][L];
		double[][] revrecadl = new double[D][L];

		int numDocSpec = 0;//number of docs with specific comments
		int numCom = 0;//number of comments in corpus
		int numDetSpecCom = 0;//number of comments detected as specific
		for (int d = 0; d < D; d++) {
			for (int l = 0; l < L; l++) {
				int numComInArt = 0;
				for (int l1 = 0; l1 < L; l1++) {
					int C = gsal[d][l][l1].length;
					for (int c = 0; c < C; c++) {
						numComInArt++;
						// count tp, fp, tn, fn
						if (gsde[d][l][l1][c] == 1 && clde[d][l][l1][c] == 1) {
							tp[d][l]++;
							numDetSpecCom++;
						} else if (gsde[d][l][l1][c] == 1
								&& clde[d][l][l1][c] != 1) {
							fn[d][l]++;
						} else if (gsde[d][l][l1][c] != 1
								&& clde[d][l][l1][c] == 1) {
							fp[d][l]++;
							numDetSpecCom++;
						} else if (gsde[d][l][l1][c] != 1
								&& clde[d][l][l1][c] != 1) {
							tn[d][l]++;
						}
					}
				}
				// compute document-level precision and recall, 
				// but only if there is at least one specific comment.
				if (! (tp[d][l] == 0 && fn[d][l] == 0) ) {
					precdl[d][l] = tp[d][l] == 0 ? 0 : tp[d][l] / (double) (tp[d][l] + fp[d][l]);
					recadl[d][l] = tp[d][l] / (double)(tp[d][l] + fn[d][l]);
					revprecdl[d][l] = tn[d][l] == 0 ? 0 : tn[d][l] / (double) (tn[d][l] + fn[d][l]);
					revrecadl[d][l] = tn[d][l] == 0 ? 0 : tn[d][l] / (double)(tn[d][l] + fp[d][l]);
					numDocSpec++;
					numCom += numComInArt;
				}
			}
		}
		// average over all documents
		double prec = MyUtil.sumDouble(precdl) / numDocSpec;
		double reca = MyUtil.sumDouble(recadl) / numDocSpec;
		double revprec = MyUtil.sumDouble(revprecdl) / numDocSpec;
		double revreca = MyUtil.sumDouble(revrecadl) / numDocSpec;
		double f1 = (prec == 0 && reca == 0) ? 0 : (2*prec*reca)/(prec+reca);
		double revf1 = (revprec == 0 && revreca == 0) ? 0 : (2*revprec*revreca)/(revprec+revreca);
//		Log.pr(String.format("Precision=%f Recall=%f\n", prec, reca));
		Log.pr(String.format("[%f %f %f] [%f %f %f] (%d/%d) (%d/%d)", prec, reca, f1, revprec, revreca, revf1,
				numDocSpec, D, numDetSpecCom, numCom));
	}

	private void evalalign(File clalFile, File cldeFile) throws Exception {
		int[][][][][] clal = (new Gson()).fromJson(
				MyUtil.readGzippedTextFile(clalFile), int[][][][][].class);
		int[][][][] clde = (new Gson()).fromJson(
				MyUtil.readGzippedTextFile(cldeFile), int[][][][].class);
		// init counts
		int[][][][] tp = new int[D][L][L][];
		int[][][][] tn = new int[D][L][L][];
		int[][][][] fp = new int[D][L][L][];
		int[][][][] fn = new int[D][L][L][];
		for (int d = 0; d < D; d++) {
			for (int l = 0; l < L; l++) {
				for (int l1 = 0; l1 < L; l1++) {
					int C = gsal[d][l][l1].length;
					tp[d][l][l1] = new int[C];
					fp[d][l][l1] = new int[C];
					tn[d][l][l1] = new int[C];
					fn[d][l][l1] = new int[C];
				}
			}
		}
		// compute precision, recall
		double[][][][] precc = new double[D][L][L][];
		double[][][][] recac = new double[D][L][L][];
		double[][] precl = new double[D][L];
		double[][] recal = new double[D][L];
		int numDocSpec = 0;//number of docs with specific comments
		int totNumSpecCom = 0;//total number of specific comments (in gold standard)
		int totNumDetSpecCom = 0;//total number of specific comments detected (by classifier)
		for (int d = 0; d < D; d++) {
			for (int l = 0; l < L; l++) {
				int numDetSpecCom = 0;//number of specific comments detected in this article
				for (int l1 = 0; l1 < L; l1++) {
					int C = gsal[d][l][l1].length;
					precc[d][l][l1] = new double[C];
					recac[d][l][l1] = new double[C];
					for (int c = 0; c < C; c++) {
						if (gsde[d][l][l1][c] == 1) {
							totNumSpecCom++;
						}
						// for each comment that both gold std and model consider 
						// as specific 
						if (gsde[d][l][l1][c] == 1 && clde[d][l][l1][c] == 1 ) {
							int S = gsal[d][l][l1][c].length;
							for (int s = 0; s < S; s++) {
								if (gsal[d][l][l1][c][s] == 1
										&& clal[d][l][l1][c][s] == 1) {
									tp[d][l][l1][c]++;
								} else if (gsal[d][l][l1][c][s] == 1
										&& clal[d][l][l1][c][s] != 1) {
									fn[d][l][l1][c]++;
								} else if (gsal[d][l][l1][c][s] != 1
										&& clal[d][l][l1][c][s] == 1) {
									fp[d][l][l1][c]++;
								} else if (gsal[d][l][l1][c][s] != 1
										&& clal[d][l][l1][c][s] != 1) {
									tn[d][l][l1][c]++;
								}
							}
							precc[d][l][l1][c] = tp[d][l][l1][c] / (double)(tp[d][l][l1][c] + fp[d][l][l1][c]);
							recac[d][l][l1][c] = tp[d][l][l1][c] / (double)(tp[d][l][l1][c] + fn[d][l][l1][c]);
							numDetSpecCom++;
							totNumDetSpecCom++;
						}
					}
				}
				if (numDetSpecCom > 0) {
					precl[d][l] = MyUtil.sumDouble(precc[d][l]) / numDetSpecCom;
					recal[d][l] = MyUtil.sumDouble(recac[d][l]) / numDetSpecCom;
					numDocSpec++;
				}
			}
		}
		double prec = (numDocSpec == 0) ? 0 : MyUtil.sumDouble(precl) / numDocSpec;
		double reca = (numDocSpec == 0) ? 0 : MyUtil.sumDouble(recal) / numDocSpec;
		double f1 = (prec == 0 && reca == 0) ? 0 : (2*prec*reca)/(prec+reca);
//		Log.pr(String.format("Precision=%f Recall=%f\n", prec, reca));
		Log.pr(String.format("%f %f %f (%d/%d)", prec, reca, f1, 
				totNumDetSpecCom, totNumSpecCom));
	}

	// private void saveCount(int[][][][] ct, File ctFile) throws Exception {
	// Log.set(ctFile);
	// for (int d=0; d<D; d++) {
	// for (int l=0; l<L; l++) {
	// for (int l1=0; l1<L; l1++) {
	// int C = gsal[d][l][l1].length;
	// for (int c=0; c<C; c++) {
	// Log.pr(String.format("%d %d %d %d %d", d, l, l1, c, gsal[d][l][l1][c]));
	// }
	// }
	// }
	// }
	// Log.reset();
	// }

	// private void saveCounts(File outDir) throws Exception {
	// saveCount(tp, new File(outDir, "tp.txt"));
	// saveCount(tn, new File(outDir, "tn.txt"));
	// saveCount(fp, new File(outDir, "fp.txt"));
	// saveCount(fn, new File(outDir, "fn.txt"));
	// }

	private static void evaldetect(File gsFile, File cldeFile) throws Exception {
		Eval e = new Eval(gsFile);
		e.evaldetect(cldeFile);
	}

	private static void evalalign(File gsFile, File clalFile, File cldeFile) throws Exception {
		Eval e = new Eval(gsFile);
		e.evalalign(clalFile, cldeFile);
	}

	private static void evalall(File gsFile, File clalFile, File cldeFile) throws Exception {
		Eval e = new Eval(gsFile);
		e.evaldetect(cldeFile);
		Log.pr(" ");
		e.evalalign(clalFile, cldeFile);
		Log.pr("\n");
	}

	public static void main(String[] args) throws Exception {
		String cmd = args[0];
		int i = 1;
		if (cmd.equals("evaldetect")) {
			String gsFile = args[i++];
			String cldeFile = args[i++];
			evaldetect(new File(gsFile), new File(cldeFile));
		} else if (cmd.equals("evalalign")) {
			String gsFile = args[i++];
			String clalFile = args[i++];
			String cldeFile = args[i++];
			evalalign(new File(gsFile), new File(clalFile), new File(cldeFile));
		} else if (cmd.equals("evalall")) {
			String gsFile = args[i++];
			String clalFile = args[i++];
			String cldeFile = args[i++];
			evalall(new File(gsFile), new File(clalFile), new File(cldeFile));
		} else {
			Log.pr(Math.ceil(((double) 5) / 99999));
		}
	}
}
