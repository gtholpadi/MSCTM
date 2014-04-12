package mllab_mstm.model;

import java.io.*;

import mllab_mstm.util.Log;
import mllab_mstm.util.MyUtil;
import mllab_mstm.util.XORShiftRandom;

public class MsctmSbp1 {
	XORShiftRandom rand = null;
//	double lambdaMult;
	double alpha, beta, omega, gamma[], delta[][][], eta, lambda[][][][][];
	int I, L, D, K, H=3;//H=number of topic sources
	int[][] T;
	int[] V;
	//precompute
	double alphaK, betaV[], omegaV[], deltaS[][], etaK, lambdaS[][][][], gammaS;
	double[][][][][] lambdaCumS123;//for sampling
	double[][][][][] lambdaCumS23;//for sampling

	int[][][][] w;
	int[][][][] z;
	int[][][][] r;
	int[][][][][] x;
	int[][][][][] q;//switch: 0 => z, 1 => epsilon, 2 => noise
	int[][][][][] b;
	int[][][][][] y;


	int[][][][] Ndlsk;
	int[][][] Nlkv;
	int[][] NlkvS;
	int[][][] M12lkv;
	int[][] M12lkvS;
	int[][] M3lv;
	int[] M3lvS;
	int[][][][][] Mdll1cs;
	int[][][][] Mdll1csS;
	int[][][][][] Mdll1ck;
	int[][][][] Mdll1ckS;
	int[][][][][] Mdll1ch;
	int[][][][] Mdll1chS;
	int[][][] Ndtk;
	int[][] NdtkS;
	int[][][][] Ndlst;
// 	int[][][] NdlstS;
	int[][][][] Ndlsgt;
	int[][][][] Mdlsk;

	double[][][] theta;
	double[][][][] pi;
	double[][][] phi;
	double[][] psi;
	double[][][][] rho;
	
	double[] nll;

	public MsctmSbp1(int K, int I,
		double alpha, double beta, double omega, double[] gamma,
		double[][][] delta, double eta, double[][][][][] lambda//,double lambdaMult
		) throws Exception {
		rand = new XORShiftRandom(); //default seed
		this.K = K;
		this.I = I;
		this.alpha = alpha;
		this.beta = beta;
		this.omega = omega;
		this.gamma = gamma;
		this.delta = delta;
		this.eta = eta;
//		this.lambdaMult = lambdaMult;
		this.lambda = lambda;
	}
	public int sampleq(int d, int l, int l1, int c, int m) {
		double U = rand.nextDouble();
		int i;
		//doc non-empty
		if (w[d][l].length > 0) {
			for (i=0; i<H; i++) {
				if (U<lambdaCumS123[d][l][l1][c][i]) {
					break;
				}
			}
		} else { //doc empty
			for (i=1; i<H; i++) {
				if (U<lambdaCumS23[d][l][l1][c][i]) {
					break;
				}
			}
		}
		return i;
	}
	public int sampleb(int d, int l, int l1, int c, int m) throws Exception {
		return rand.nextInt(w[d][l].length);
	}
	public int sampley(int d, int l, int l1, int c, int m, int h) {
		if (h==0) {
			//topic from sentence b
			int[] zdls = z[d][l][b[d][l][l1][c][m]];
			return zdls[rand.nextInt(zdls.length)];
		} else if (h==1) {
			//any topic in corpus
			return samplePhiTopic();
		} else {
			//noise topic
			return samplePsiTopic();
		}
	}
	public int samplePhiTopic() {
		return rand.nextInt(K);
	}
	public int samplePsiTopic() {
		return 0;
	}
    public void initLatent() throws Exception {
    	//init lambda cumulative sum for use in sampling
    	lambdaCumS123 = new double[D][L][L][][];
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
				for (int l1=0; l1<L; l1++) {
					int C = x[d][l][l1].length;
					lambdaCumS123[d][l][l1] = new double[C][];
					for (int c=0; c<C; c++) {
						lambdaCumS123[d][l][l1][c] = MyUtil.getCumProb(lambda[d][l][l1][c]);
					}
				}
			}
		}
    	lambdaCumS23 = new double[D][L][L][][];
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
				for (int l1=0; l1<L; l1++) {
					int C = x[d][l][l1].length;
					lambdaCumS23[d][l][l1] = new double[C][];
					for (int c=0; c<C; c++) {
						lambdaCumS23[d][l][l1][c] = MyUtil.getCumProb(
								new double[] {0, lambda[d][l][l1][c][1], lambda[d][l][l1][c][2]});
					}
				}
			}
		}
		//article
		z = new int[D][L][][];
		r = new int[D][L][][];
		//comments
		q = new int[D][L][L][][];
		b = new int[D][L][L][][];
		y = new int[D][L][L][][];
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
				//article
				int S = w[d][l].length;
				z[d][l] = new int[S][];
				r[d][l] = new int[S][];
				for (int s=0; s<S; s++) {
					int N = w[d][l][s].length;
					z[d][l][s] = new int[N];
					r[d][l][s] = new int[N];
					for (int n=0; n<N; n++) {
						z[d][l][s][n] = samplePhiTopic();
						//TODO change sampling for r to use SBP prior
						r[d][l][s][n] = rand.nextInt(T[d][l]); 
					}
				}
				//comments
				for (int l1=0; l1<L; l1++) {
					int C = x[d][l][l1].length;
					q[d][l][l1] = new int[C][];
					b[d][l][l1] = new int[C][];
					y[d][l][l1] = new int[C][];
					for (int c=0; c<C; c++) {
						int M = x[d][l][l1][c].length;
						q[d][l][l1][c] = new int[M];
						b[d][l][l1][c] = new int[M];
						y[d][l][l1][c] = new int[M];
						for (int m=0; m<M; m++) {
							q[d][l][l1][c][m] = sampleq(d,l,l1,c,m);
							if (q[d][l][l1][c][m] == 0) {
								b[d][l][l1][c][m] = sampleb(d,l,l1,c,m);
							} else {
								b[d][l][l1][c][m] = -1;
							}
							y[d][l][l1][c][m] = sampley(d,l,l1,c,m, q[d][l][l1][c][m]);
						}
					}
				}
			}
		}
    }
    public void initCounts() {
		Ndlsk = new int[D][L][][];
		Nlkv = new int[L][][];
		NlkvS = new int[L][K];
		M12lkv = new int[L][][];
		M12lkvS = new int[L][K];
		M3lv = new int[L][];
		M3lvS = new int[L];
		Mdll1cs = new int[D][L][L][][];
		Mdll1csS = new int[D][L][L][];
		Mdll1ck = new int[D][L][L][][];
		Mdll1ckS = new int[D][L][L][];
		Mdll1ch = new int[D][L][L][][];
		Mdll1chS = new int[D][L][L][];
		Ndtk = new int[D][][];
		NdtkS = new int[D][];
		Ndlst = new int[D][L][][];
// 		NdlstS = new int[D][L][];
		Ndlsgt = new int[D][L][][];
		Mdlsk = new int[D][L][][];
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
				int S = w[d][l].length;
				Ndlsk[d][l] = new int[S][K];
				Mdlsk[d][l] = new int[S][K];
				for (int l1=0; l1<L; l1++) {
					int C = x[d][l][l1].length;
					Mdll1cs[d][l][l1] = new int[C][S];
					Mdll1csS[d][l][l1] = new int[C];
					Mdll1ck[d][l][l1] = new int[C][K];
					Mdll1ckS[d][l][l1] = new int[C];
					Mdll1ch[d][l][l1] = new int[C][H];
					Mdll1chS[d][l][l1] = new int[C];
				}
				Ndlst[d][l] = new int[S][T[d][l]];
// 				NdlstS[d][l] = new int[S];
				Ndlsgt[d][l] = new int[S][T[d][l]];
			}
			Ndtk[d] = new int[MyUtil.maxInt(T[d])][K];
			NdtkS[d] = new int[MyUtil.maxInt(T[d])];
		}
		for (int l=0; l<L; l++) {
			Nlkv[l] = new int[K][V[l]];
			M12lkv[l] = new int[K][V[l]];
			M3lv[l] = new int[V[l]];
		}
    }
    public void setCounts() throws Exception {
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
				int S = w[d][l].length;
				for (int s=0; s<S; s++) {
					int N = w[d][l][s].length;
					for (int n=0; n<N; n++) {
						int k = z[d][l][s][n];
						int t = r[d][l][s][n];
						int v = w[d][l][s][n];
						Ndlsk[d][l][s][k]++;
						Nlkv[l][k][v]++;
						NlkvS[l][k]++;
						Ndtk[d][t][k]++;
						NdtkS[d][t]++;
						Ndlst[d][l][s][t]++;
// 						NdlstS[d][l][s]++;
						for (int t1=0; t1<t; t1++) {
							Ndlsgt[d][l][s][t1]++;
						}
					}
				}
				for (int l1=0; l1<L; l1++) {
					int C = x[d][l][l1].length;
					for (int c=0; c<C; c++) {
						int M = x[d][l][l1][c].length;
						for (int m=0; m<M; m++) {
							int v = x[d][l][l1][c][m];
							int h = q[d][l][l1][c][m];
							int k = y[d][l][l1][c][m];
							Mdll1ch[d][l][l1][c][h]++;
							Mdll1chS[d][l][l1][c]++;
							if (h == 0 || h == 1) {
								M12lkv[l1][k][v]++;
								M12lkvS[l1][k]++;
								if (h==0) {
									int s = b[d][l][l1][c][m];
									Mdlsk[d][l][s][k]++;
									Mdll1cs[d][l][l1][c][s]++;
									Mdll1csS[d][l][l1][c]++;
								} else {//h=1
									assert b[d][l][l1][c][m] == -1;
									Mdll1ck[d][l][l1][c][k]++;
									Mdll1ckS[d][l][l1][c]++;
								}
							} else {//h==2
								assert b[d][l][l1][c][m] == -1;
								M3lv[l1][v]++;
								M3lvS[l1]++;
							}
						}
					}
				}
			}
		}
    }
    public void sampleAndSetz(int d, int l, int s, int n) throws Exception {
		int zdlsn = z[d][l][s][n];
		int wdlsn = w[d][l][s][n];
		int rdlsn = r[d][l][s][n];
		//decrement counts
		Ndlsk[d][l][s][zdlsn]--;
		Nlkv[l][zdlsn][wdlsn]--;
		NlkvS[l][zdlsn]--;
		Ndtk[d][rdlsn][zdlsn]--;
// 		NdtkS[d][rdlsn]--;
		//compute conditional distribution
		boolean foundInf = false;//found Infinity
		double[] pz = new double[K];
		for (zdlsn=0; zdlsn<K; zdlsn++) {
			double nr = 1, dr = 1;
			if (Ndlsk[d][l][s][zdlsn] == 0) {
				if (Mdlsk[d][l][s][zdlsn] == 0) {
					nr *= 1;
				} else {
					foundInf = true;
					break;
				}
			} else {
				nr *= Math.pow((Ndlsk[d][l][s][zdlsn] + 1)/Ndlsk[d][l][s][zdlsn],Mdlsk[d][l][s][zdlsn]);
			}
			nr *= beta + Nlkv[l][zdlsn][wdlsn] + M12lkv[l][zdlsn][wdlsn];
			dr *= betaV[l] + NlkvS[l][zdlsn] + M12lkvS[l][zdlsn];
			nr *= alpha + Ndtk[d][rdlsn][zdlsn];
			// Not required since independent of zdlsn
// 			dr *= alphaK + NdtkS[d][rdlsn];
			if (zdlsn==0) {
				pz[0] = nr/dr;
			} else {
				pz[zdlsn] = pz[zdlsn-1] + (nr/dr);
			}
		}
		//sample from conditional
		if (!foundInf) {
			double U = rand.nextDouble() * pz[K-1];
			for (zdlsn=0; zdlsn<K; zdlsn++) {
				if (U < pz[zdlsn]) break;
			}
		}
		z[d][l][s][n] = zdlsn;
		//increment counts
		Ndlsk[d][l][s][zdlsn]++;
		Nlkv[l][zdlsn][wdlsn]++;
		NlkvS[l][zdlsn]++;
		Ndtk[d][rdlsn][zdlsn]++;
// 		NdtkS[d][rdlsn]++;
    }
	public void sampleAndSetr(int d, int l, int s, int n) {
		int zdlsn = z[d][l][s][n];
		int rdlsn = r[d][l][s][n];
		//decrement counts
		Ndtk[d][rdlsn][zdlsn]--;
		NdtkS[d][rdlsn]--;
		Ndlst[d][l][s][rdlsn]--;
// 		NdlstS[d][l][s]--;
		for (int t=0; t<rdlsn; t++) {
			Ndlsgt[d][l][s][t]--;
		}
		//compute conditional distribution
		double[] pr = new double[T[d][l]];
		for (rdlsn=0; rdlsn<T[d][l]; rdlsn++) {
			double nr = alpha + Ndtk[d][rdlsn][zdlsn];
			double dr = alphaK + NdtkS[d][rdlsn];
			nr *= gamma[0] + Ndlst[d][l][s][rdlsn];
			dr *= gammaS + Ndlst[d][l][s][rdlsn] + Ndlsgt[d][l][s][rdlsn];
			for (int t=0; t<rdlsn; t++) {
//				nr *= gamma[1] + Ndlsgt[d][l][s][t];
//				dr *= gammaS + Ndlst[d][l][s][t] + Ndlsgt[d][l][s][t];
				
//				computing fraction to avoid overflow problem
				nr *= (gamma[1] + Ndlsgt[d][l][s][t])/(gammaS + Ndlst[d][l][s][t] + Ndlsgt[d][l][s][t]);
			}
			if (rdlsn==0) {
				pr[0] = nr/dr;
			} else {
				pr[rdlsn] = pr[rdlsn-1] + (nr/dr);
			}
		}
		//sample from conditional
		double U = rand.nextDouble() * pr[T[d][l]-1];
		for (rdlsn=0; rdlsn<T[d][l]; rdlsn++) {
			if (U < pr[rdlsn]) break;
		}
		r[d][l][s][n] = rdlsn;
		//increment counts
		Ndtk[d][rdlsn][zdlsn]++;
		NdtkS[d][rdlsn]++;
		Ndlst[d][l][s][rdlsn]++;
// 		NdlstS[d][l][s]++;
		for (int t=0; t<rdlsn; t++) {
			Ndlsgt[d][l][s][t]++;
		}
    }
	public void sampleAndSetqby(int d, int l, int l1, int c, int m) throws Exception {
		int h = q[d][l][l1][c][m];
		int v = x[d][l][l1][c][m];
		int k = y[d][l][l1][c][m];
		//decrement counts
		Mdll1ch[d][l][l1][c][h]--;
		Mdll1chS[d][l][l1][c]--;
		if (h == 0 || h == 1) {
			M12lkv[l1][k][v]--;
			M12lkvS[l1][k]--;
			if (h==0) {
				int s = b[d][l][l1][c][m];
				Mdlsk[d][l][s][k]--;
				Mdll1cs[d][l][l1][c][s]--;
				Mdll1csS[d][l][l1][c]--;
			} else {//h=1
				assert b[d][l][l1][c][m] == -1;
				Mdll1ck[d][l][l1][c][k]--;
				Mdll1ckS[d][l][l1][c]--;
			}
		} else {//h==2
			assert b[d][l][l1][c][m] == -1;
			M3lv[l1][v]--;
			M3lvS[l1]--;
		}
		//sample joint
		sampleAndSetqbyJoint(d,l,l1,c,m);		
		//increment counts
		h = q[d][l][l1][c][m];
		k = y[d][l][l1][c][m];
		Mdll1ch[d][l][l1][c][h]++;
		Mdll1chS[d][l][l1][c]++;
		if (h == 0 || h == 1) {
			M12lkv[l1][k][v]++;
			M12lkvS[l1][k]++;
			if (h==0) {
				int s = b[d][l][l1][c][m];
				Mdlsk[d][l][s][k]++;
				Mdll1cs[d][l][l1][c][s]++;
				Mdll1csS[d][l][l1][c]++;
			} else {//h=1
				assert b[d][l][l1][c][m] == -1;
				Mdll1ck[d][l][l1][c][k]++;
				Mdll1ckS[d][l][l1][c]++;
			}
		} else {//h==2
			assert b[d][l][l1][c][m] == -1;
			M3lv[l1][v]++;
			M3lvS[l1]++;
		}
	}
	public void sampleAndSetqbyJoint(int d, int l, int l1, int c, int m) throws Exception {
		int qdll1cm, bdll1cm, ydll1cm;
		int xdll1cm = x[d][l][l1][c][m];
		int S = w[d][l].length; 
		//Precompute common term
		double[] lambdaTermNr = new double[H];
		for (int i=0; i<H; i++) {
			lambdaTermNr[i] = lambda[d][l][l1][c][i] + Mdll1ch[d][l][l1][c][i];
		}
		//case 1: topic comes from z
		qdll1cm = 0;
		double[][] pby = new double[S][K];
		for (bdll1cm=0; bdll1cm<S; bdll1cm++) {
			for (ydll1cm=0; ydll1cm<K; ydll1cm++) {
				double nr = Ndlsk[d][l][bdll1cm][ydll1cm];
				double dr = 1;
				if (nr > 0) {
					dr = w[d][l][bdll1cm].length;
					nr *= beta + Nlkv[l1][ydll1cm][xdll1cm] + M12lkv[l1][ydll1cm][xdll1cm];
					dr *= betaV[l1] + NlkvS[l1][ydll1cm] + M12lkvS[l1][ydll1cm];
					nr *= delta[d][l][bdll1cm] + Mdll1cs[d][l][l1][c][bdll1cm];
					dr *= deltaS[d][l] + Mdll1csS[d][l][l1][c];
					nr *= lambdaTermNr[qdll1cm];
				}
				//add to conditional pmf vector
				if (bdll1cm == 0 && ydll1cm == 0) {//first row, first column
					pby[0][0] = nr/dr;
				} else if (ydll1cm == 0) {//nth row (n>1), first column 
					pby[bdll1cm][0] = pby[bdll1cm-1][K-1] + nr/dr;
				} else {//other columns on any row
					pby[bdll1cm][ydll1cm] = pby[bdll1cm][ydll1cm-1] + nr/dr;
				}
			}
		}
		//case 2: topic comes from epsilon
		qdll1cm = 1;
		bdll1cm = -1;
		double[] py = new double[K];
		for (ydll1cm=0; ydll1cm<K; ydll1cm++) {
			double nr = lambdaTermNr[qdll1cm];
			double dr = 1;
			if (nr > 0) {
				nr *= beta + Nlkv[l1][ydll1cm][xdll1cm] + M12lkv[l1][ydll1cm][xdll1cm];
				dr *= betaV[l1] + NlkvS[l1][ydll1cm] + M12lkvS[l1][ydll1cm];
//				nr *= omega + Mhlkv[1][l1][udll1cm][xdll1cm];
//				dr = omegaV[l] + MhlkvS[1][l1][udll1cm];
				nr *= eta + Mdll1ck[d][l][l1][c][ydll1cm];
				dr *= etaK + Mdll1ckS[d][l][l1][c];
			}
			//add to conditional pmf vector
			if (ydll1cm == 0) {//carry over cumulative sum from case 1
				py[0] = (S==0 ? 0 : pby[S-1][K-1]) + nr/dr;
			} else {
				py[ydll1cm] = py[ydll1cm-1] + nr/dr;
			}
		}
		//case 3: topic comes from noise
		qdll1cm = 2;
		bdll1cm = -1;
		ydll1cm = 0;
		double pnoise = py[K-1] + (lambdaTermNr[qdll1cm] * (omega + M3lv[l1][xdll1cm])) / (omegaV[l1] + M3lvS[l1]); 
		//sample from conditional
		double U = rand.nextDouble() * pnoise;
		boolean found = false;
		qdll1cm = 0;
		for (bdll1cm=0; bdll1cm<S; bdll1cm++) {
			for (ydll1cm=0; ydll1cm<K; ydll1cm++) {
				if (U < pby[bdll1cm][ydll1cm]) {
					found = true;
					break;
				}
			}
			if (found) break;
		}
		if (!found) { 
			qdll1cm = 1;
			bdll1cm = -1;
			for (ydll1cm=0; ydll1cm<K; ydll1cm++) {
				if (U < py[ydll1cm]) {
					found = true;
					break;
				}
			}
		}
		if (!found) {
			qdll1cm = 2;
			bdll1cm = -1;
			ydll1cm = 0;
		}
		q[d][l][l1][c][m] = qdll1cm;
		b[d][l][l1][c][m] = bdll1cm;
		y[d][l][l1][c][m] = ydll1cm;
	}
	public void precompute() {
		//precompute
		alphaK = alpha*K;
		etaK = eta*K;
		lambdaS = new double[D][L][L][];
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
				for (int l1=0; l1<L; l1++) {
					int C = x[d][l][l1].length;
					lambdaS[d][l][l1] = new double[C];
					for (int c=0; c<C; c++) {
						for (int i=0; i<H; i++) {
							lambdaS[d][l][l1][c] += lambda[d][l][l1][c][i];
						}
					}
				}
			}
		}
		betaV = new double[L];
		omegaV = new double[L];
		for (int l=0; l<L; l++) {
			betaV[l] = beta * V[l];
			omegaV[l] = omega * V[l];
		}
		gammaS = gamma[0] + gamma[1];
		deltaS = new double[D][L];
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
				deltaS[d][l] = MyUtil.sumDouble(delta[d][l]);
			}
		}
	}
    public void sample() throws Exception {
		precompute();
		nll = new double[I];
		for (int i=0; i<I; i++) {
			sampleI();
			if (i%100==0) {Log.pr(String.format("%d iterations done\n", i+1));}
			// compute negative log-likelihood
//			if (i>=400 && 
//					i%20==0) {
//				nll[i] = computeNll();
//			}
		}
    }
    public void sampleI() throws Exception {
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
				int S = w[d][l].length;
				for (int s=0; s<S; s++) {
					int N = w[d][l][s].length;
					for (int n=0; n<N; n++) {
						sampleAndSetz(d,l,s,n);
						sampleAndSetr(d,l,s,n);
					}
				}
				for (int l1=0; l1<L; l1++) {
					int C = x[d][l][l1].length;
					for (int c=0; c<C; c++) {
						int M = x[d][l][l1][c].length;
						for (int m=0; m<M; m++) {
							sampleAndSetqby(d,l,l1,c,m);
						}
					}
				}
			}
		}
    }
    //compute Negative log likelihood
    public double computeNll() {
		double ll = 0;
		for (int l=0; l<L; l++) {
			for (int k=0; k<K; k++) {
				for (int v=0; v<V[l]; v++) {
					ll += MyUtil.logGamma(beta + Nlkv[l][k][v] + M12lkv[l][k][v]);
				}
				ll -= MyUtil.logGamma(betaV[l] + NlkvS[l][k] + M12lkvS[l][k]);
			}
			for (int v=0; v<V[l]; v++) {
				ll += MyUtil.logGamma(omega + M3lv[l][v]);
			}
			ll -= MyUtil.logGamma(omegaV[l] + M3lvS[l]);
		}
		for (int d=0; d<D; d++) {
			for (int t=0; t<MyUtil.maxInt(T[d]); t++) {
				for (int k=0; k<K; k++) {
					ll += MyUtil.logGamma(alpha + Ndtk[d][t][k]);
				}
				ll -= MyUtil.logGamma(alphaK + NdtkS[d][t]);
			}
			for (int l=0; l<L; l++) {
				int S = w[d][l].length;
				for (int s=0; s<S; s++) {
					for (int t=0; t<T[d][l]-1; t++) {
						ll += MyUtil.logGamma(gamma[0] + Ndlst[d][l][s][t])
								+ MyUtil.logGamma(gamma[1] + Ndlsgt[d][l][s][t])
								- MyUtil.logGamma(gammaS + Ndlst[d][l][s][t] + Ndlsgt[d][l][s][t]);
					}
				}
				for (int l1=0; l1<L; l1++) {
					int C = x[d][l][l1].length;
					for (int c=0; c<C; c++) {
						int M = x[d][l][l1][c].length;
						for (int m=0; m<M; m++) {
							if (q[d][l][l1][c][m] == 0) {
								ll += Math.log(Ndlsk[d][l][b[d][l][l1][c][m]][y[d][l][l1][c][m]])
										- Math.log(w[d][l][b[d][l][l1][c][m]].length);
							}
						}
						for (int s=0; s<S; s++) {
							ll += MyUtil.logGamma(delta[d][l][s] + Mdll1cs[d][l][l1][c][s]);
						}
						ll -= MyUtil.logGamma(deltaS[d][l] + Mdll1csS[d][l][l1][c]);
						for (int k=0; k<K; k++) {
							ll += MyUtil.logGamma(eta + Mdll1ck[d][l][l1][c][k]);
						}
						ll -= MyUtil.logGamma(etaK + Mdll1ckS[d][l][l1][c]);
						for (int h=0; h<H; h++) {
							ll += MyUtil.logGamma(lambda[d][l][l1][c][h] + Mdll1ch[d][l][l1][c][h]);
						}
						ll -= MyUtil.logGamma(lambdaS[d][l][l1][c] + Mdll1chS[d][l][l1][c]);
					}
				}
			}
		}
		return -ll;
	}
    
	public void inferParams() {
/*		int l,d,s,n,t,k,v;
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
		}*/
		// infer phi
		inferPhi();
		// infer psi
		inferPsi();
    }
	public void inferPsi() {
		psi = new double[L][];
		for (int l=0; l<L; l++) {
			double tot = 0;
			psi[l] = new double[V[l]];
			for (int v=0; v<V[l]; v++) {
				psi[l][v] = omega + M3lv[l][v];
				tot += psi[l][v];
			}
			for (int v=0; v<V[l]; v++) {
				psi[l][v] = psi[l][v]/tot;
			}
		}
	}
	public void inferPhi() {
		phi = new double[L][K][];
		for (int l=0; l<L; l++) {
			for(int k=0; k<K; k++) {
				double tot = 0;
				phi[l][k] = new double[V[l]];
				for (int v=0; v<V[l]; v++) {
					phi[l][k][v] = beta + Nlkv[l][k][v] + M12lkv[l][k][v];
					tot += phi[l][k][v];
				}
				for (int v=0; v<V[l]; v++) {
					phi[l][k][v] = phi[l][k][v]/tot;
				}
			}
		}
	}
    public void saveVars(String varDir) throws Exception {
		// print out for qualitative analysis
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
				//save this article and comments
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
					varDir+File.separator+String.valueOf(l), String.valueOf(d))));
				bw.append("#w-z r\n");
				for (int s=0; s<w[d][l].length; s++) {
					for (int n=0; n<w[d][l][s].length; n++) {
						if (n!=0) bw.append(", ");
						bw.append(String.format("%d-%d %d", 
								w[d][l][s][n], z[d][l][s][n], r[d][l][s][n]));
					}
					bw.newLine();
				}
				bw.newLine();
				bw.append("#x q-y b\n");
				for (int l1=0; l1<L; l1++) {
					for (int c=0; c<x[d][l][l1].length; c++) {
						for (int m=0; m<x[d][l][l1][c].length; m++) {
							if (m!=0) bw.append(", ");
							bw.append(String.format("%d %d-%d %d",x[d][l][l1][c][m],
								q[d][l][l1][c][m], y[d][l][l1][c][m], b[d][l][l1][c][m]));
						}
						bw.newLine();
					}
				}
				bw.close();
			}
		}		
    }
}
