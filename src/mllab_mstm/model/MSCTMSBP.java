package mllab_mstm.model;

import java.io.*;

import mllab_mstm.util.Log;
import mllab_mstm.util.MyUtil;
import mllab_mstm.util.XORShiftRandom;

public class MSCTMSBP {
	XORShiftRandom rand = null;
	int H; //Number of topic sources
	double lambdaMult;
	double alpha, beta, omega, gamma[], delta[][][], eta, lambda[];
	int I, L, D, K, J;
	int[][] T;
	int[] V;
	//precompute
	double alphaK, betaV[], omegaV[], deltaS[][], etaJ, lambdaH, gammaT;

	int[][][][] w;
	int[][][][] z;
	int[][][][] r;
	int[][][][][] x;
	int[][][][][] q;//switch: 0 => z, 1 => epsilon
	int[][][][][] b;
	int[][][][][] y;
	int[][][][][] u;
	int a;	//switch: 0 => phi, 1 => psi


	int[][][][] Ndlsk;
	int[][][][] Mdlsk;
	int[][][] Nlkv;
	int[][] NlkvS;
	int[][][][] Mhlkv;
	int[][][] MhlkvS;
	int[][][][][] Mdll1cs;
	int[][][][] Mdll1csS;
	int[][][][][] Mdll1cj;
	int[][][][] Mdll1cjS;
	int[][][][][] Mdll1ch;
	int[][][][] Mdll1chS;
	int[][][] Ndtk;
	int[][] NdtkS;
	int[][][][] Ndlst;
// 	int[][][] NdlstS;
	int[][][][] Ndlsgt;

	double[][][] theta;
	double[][][][] pi;
	double[][][] phi;
	double[][][] psi;
	double[][][][] rho;
	
	double[] nll;

	public MSCTMSBP(int K, int J, int I,
		double alpha, double beta, double omega, double[] gamma,
		double[][][] delta, double eta, double lambda, double lambdaMult, int a) throws Exception {
		rand = new XORShiftRandom(); //default seed
		this.K = K;
		this.J = J;
		if (J>0) {
			H = 2;
		} else {
			H = 1;
		}
		this.I = I;
		this.alpha = alpha;
		this.beta = beta;
		this.omega = omega;
		this.gamma = gamma;
		this.delta = delta;
		this.eta = eta;
		this.lambdaMult = lambdaMult;
		this.lambda = new double[] {lambdaMult*lambda, lambdaMult*(1-lambda)};
		this.a = a;
	}
	public int sampleq(int d, int l, int l1, int c, int m) {
		if (H==1) {
			return 0; //no psi topics
		} else {
//			return 0;
			return rand.nextDouble()*lambdaMult < lambda[0] ? 0 : 1;
		}
	}
	public int sampleb(int d, int l, int l1, int c, int m) {
		return rand.nextInt(w[d][l].length);
	}
	public int sampley(int d, int l, int l1, int c, int m) {
		int[] zdls = z[d][l][b[d][l][l1][c][m]];
		return zdls[rand.nextInt(zdls.length)];
	}
    public void initLatent() throws Exception {
		//article
		z = new int[D][L][][];
		r = new int[D][L][][];
		//comments
		q = new int[D][L][L][][];
		b = new int[D][L][L][][];
		y = new int[D][L][L][][];
		u = new int[D][L][L][][];
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
						z[d][l][s][n] = rand.nextInt(K);
						r[d][l][s][n] = rand.nextInt(T[d][l]);
					}
				}
				//comments
				for (int l1=0; l1<L; l1++) {
					int C = x[d][l][l1].length;
					q[d][l][l1] = new int[C][];
					b[d][l][l1] = new int[C][];
					y[d][l][l1] = new int[C][];
					u[d][l][l1] = new int[C][];
					for (int c=0; c<C; c++) {
						int M = x[d][l][l1][c].length;
						q[d][l][l1][c] = new int[M];
						b[d][l][l1][c] = new int[M];
						y[d][l][l1][c] = new int[M];
						u[d][l][l1][c] = new int[M];
						for (int m=0; m<M; m++) {
							q[d][l][l1][c][m] = sampleq(d,l,l1,c,m);
							if (q[d][l][l1][c][m] == 0) {
								b[d][l][l1][c][m] = sampleb(d,l,l1,c,m);
								y[d][l][l1][c][m] = sampley(d,l,l1,c,m);//depends on previous step
								u[d][l][l1][c][m] = -1;
							} else {
								b[d][l][l1][c][m] = -1;
								y[d][l][l1][c][m] = -1;
								u[d][l][l1][c][m] = rand.nextInt(J);
							}
						}
					}
				}
			}
		}
    }
    public void initCounts() {
		Ndlsk = new int[D][L][][];
		Mdlsk = new int[D][L][][];
		Nlkv = new int[L][][];
		NlkvS = new int[L][K];
		Mhlkv = new int[H][L][][];
		MhlkvS = new int[H][L][];
		Mdll1cs = new int[D][L][L][][];
		Mdll1csS = new int[D][L][L][];
		Mdll1cj = new int[D][L][L][][];
		Mdll1cjS = new int[D][L][L][];
		Mdll1ch = new int[D][L][L][][];
		Mdll1chS = new int[D][L][L][];
		Ndtk = new int[D][][];
		NdtkS = new int[D][];
		Ndlst = new int[D][L][][];
// 		NdlstS = new int[D][L][];
		Ndlsgt = new int[D][L][][];
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
				int S = w[d][l].length;
				Ndlsk[d][l] = new int[S][K];
				Mdlsk[d][l] = new int[S][K];
				for (int l1=0; l1<L; l1++) {
					int C = x[d][l][l1].length;
					Mdll1cs[d][l][l1] = new int[C][S];
					Mdll1csS[d][l][l1] = new int[C];
					Mdll1cj[d][l][l1] = new int[C][J];
					Mdll1cjS[d][l][l1] = new int[C];
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
			Mhlkv[0][l] = new int[K][V[l]];
			MhlkvS[0][l] = new int[K];
			if (H>1) {
				Mhlkv[1][l] = new int[J][V[l]];
				MhlkvS[1][l] = new int[J];
			}
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
							Mdll1ch[d][l][l1][c][h]++;
							Mdll1chS[d][l][l1][c]++;
							if (h == 0) {
								int k = y[d][l][l1][c][m];
								int s = b[d][l][l1][c][m];
								assert u[d][l][l1][c][m] == -1;
								Mdlsk[d][l][s][k]++;
								Mdll1cs[d][l][l1][c][s]++;
								Mdll1csS[d][l][l1][c]++;
								Mhlkv[0][l1][k][v]++;
								MhlkvS[0][l1][k]++;
							} else {
								int j = u[d][l][l1][c][m];
								assert y[d][l][l1][c][m] == -1;
								assert b[d][l][l1][c][m] == -1;
								Mdll1cj[d][l][l1][c][j]++;
								Mdll1cjS[d][l][l1][c]++;
								Mhlkv[1][l1][j][v]++;
								MhlkvS[1][l1][j]++;
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
				nr *= Math.pow((Ndlsk[d][l][s][zdlsn] + 1)/Ndlsk[d][l][s][zdlsn],
						Mdlsk[d][l][s][zdlsn]);
			}
			nr *= beta + Nlkv[l][zdlsn][wdlsn] + Mhlkv[0][l][zdlsn][wdlsn];
// 			double dr = V[l]*beta + NlkvS[l][zdlsn] + MhlkvS[0][l][zdlsn];
			dr *= betaV[l] + NlkvS[l][zdlsn] + MhlkvS[0][l][zdlsn];
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
			double U = rand.nextDouble();
			for (zdlsn=0; zdlsn<K; zdlsn++) {
				if (U < pz[zdlsn]/pz[K-1]) break;
			}
			if (zdlsn==K) {
				Log.pr("sth wrong");
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
// 			double dr = K*alpha + NdtkS[d][rdlsn];
			double dr = alphaK + NdtkS[d][rdlsn];
			nr *= gamma[0] + Ndlst[d][l][s][rdlsn];
			dr *= gammaT + Ndlst[d][l][s][rdlsn] + Ndlsgt[d][l][s][rdlsn];
			for (int t=0; t<rdlsn; t++) {
				nr *= gamma[1] + Ndlsgt[d][l][s][t];
				dr *= gammaT + Ndlst[d][l][s][t] + Ndlsgt[d][l][s][t];
			}
			if (rdlsn==0) {
				pr[0] = nr/dr;
			} else {
				pr[rdlsn] = pr[rdlsn-1] + (nr/dr);
			}
		}
		//sample from conditional
		double U = rand.nextDouble();
		for (rdlsn=0; rdlsn<T[d][l]; rdlsn++) {
			if (U < pr[rdlsn]/pr[T[d][l]-1]) break;
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
	//Not using this optimized version currently, because 
	//speedup is not that great (probably) --- T^2 to T,
	//but K dominates this anyway.
	public void sampleAndSetr1(int d, int l, int s, int n) {
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
		double prodnr = 1, proddr = 1;
		for (rdlsn=0; rdlsn<T[d][l]; rdlsn++) {
			double nr = alpha + Ndtk[d][rdlsn][zdlsn];
// 			double dr = K*alpha + NdtkS[d][rdlsn];
			double dr = alphaK + NdtkS[d][rdlsn];
			nr *= gamma[0] + Ndlst[d][l][s][rdlsn];
			dr *= gammaT + Ndlst[d][l][s][rdlsn] + Ndlsgt[d][l][s][rdlsn];
			if (rdlsn>0) {
				prodnr *= gamma[1] + Ndlsgt[d][l][s][rdlsn-1];
				proddr *= gammaT + Ndlst[d][l][s][rdlsn-1] + Ndlsgt[d][l][s][rdlsn-1];
			}
			nr *= prodnr;
			dr *= proddr;
			for (int t=0; t<rdlsn; t++) {
				nr *= gamma[1] + Ndlsgt[d][l][s][t];
				dr *= gammaT + Ndlst[d][l][s][t] + Ndlsgt[d][l][s][t];
			}
			if (rdlsn==0) {
				pr[0] = nr/dr;
			} else {
				pr[rdlsn] = pr[rdlsn-1] + (nr/dr);
			}
		}
		//sample from conditional
		double U = rand.nextDouble();
		for (rdlsn=0; rdlsn<T[d][l]; rdlsn++) {
			if (U < pr[rdlsn]/pr[T[d][l]-1]) break;
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
	public int sampleAndSetqbyuJoint(int d, int l, int l1, int c, int m) throws Exception {
		int qdll1cm, bdll1cm, ydll1cm, udll1cm;
		int xdll1cm = x[d][l][l1][c][m];
		int S = w[d][l].length;
		//Precompute common term
		double[] lambdaTermNr = new double[] {lambda[0] + Mdll1ch[d][l][l1][c][0], lambda[1] + Mdll1ch[d][l][l1][c][1]};
		//case 1: topic comes from z
		qdll1cm = 0;
		udll1cm = -1;
		double[][] pby = new double[S][K];
		for (bdll1cm=0; bdll1cm<S; bdll1cm++) {
			for (ydll1cm=0; ydll1cm<K; ydll1cm++) {
				double nr = Ndlsk[d][l][bdll1cm][ydll1cm];
				double dr = 1;
				if (nr > 0) {
					dr = w[d][l][bdll1cm].length;
					nr *= lambdaTermNr[qdll1cm];
					nr *= beta + Nlkv[l1][ydll1cm][xdll1cm] + Mhlkv[0][l1][ydll1cm][xdll1cm];
					dr *= betaV[l1] + NlkvS[l1][ydll1cm] + MhlkvS[0][l1][ydll1cm];
					nr *= delta[d][l][bdll1cm] + Mdll1cs[d][l][l1][c][bdll1cm];
					dr *= deltaS[d][l] + Mdll1csS[d][l][l1][c];
				}
				//add to conditional pmf vector
				if (bdll1cm == 0 && ydll1cm == 0) {//first row, first column
					pby[0][0] = nr/dr;
				} else if (ydll1cm == 0) {//nth row, first column (n>1) 
					pby[bdll1cm][0] = pby[bdll1cm-1][K-1] + nr/dr;
				} else {//other columns on any row
					pby[bdll1cm][ydll1cm] = pby[bdll1cm][ydll1cm-1] + nr/dr;
				}
			}
		}
		//case 2: topic comes from epsilon
		qdll1cm = 1;
		bdll1cm = ydll1cm = -1;
		double[] pu = new double[J];
		for (udll1cm=0; udll1cm<J; udll1cm++) {
			double nr = lambdaTermNr[qdll1cm];
			double dr = 1;
			if (nr > 0) {
				nr *= omega + Mhlkv[1][l1][udll1cm][xdll1cm];
				dr = omegaV[l] + MhlkvS[1][l1][udll1cm];
				nr *= eta + Mdll1cj[d][l][l1][c][udll1cm];
				dr *= etaJ + Mdll1cjS[d][l][l1][c];
			}
			//add to conditional pmf vector
			if (udll1cm == 0) {//carry over cumulative sum from case 1
				pu[0] = pby[S-1][K-1] + nr/dr;
			} else {
				pu[udll1cm] = pu[udll1cm-1] + nr/dr;
			}
		}
		
		//sample from conditional
		double U = rand.nextDouble();
		double tot = pu[J-1];
		boolean found = false;
		qdll1cm = 0;
		udll1cm = -1;
		for (bdll1cm=0; bdll1cm<S; bdll1cm++) {
			for (ydll1cm=0; ydll1cm<K; ydll1cm++) {
				if (U < pby[bdll1cm][ydll1cm]/tot) {
					found = true;
					break;
				}
			}
			if (found) break;
		}
		if (!found) { 
			qdll1cm = 1;
			bdll1cm = ydll1cm = -1;
			for (udll1cm=0; udll1cm<J; udll1cm++) {
				if (U < pu[udll1cm]/tot) break;
			}
		}
		q[d][l][l1][c][m] = qdll1cm;
		b[d][l][l1][c][m] = bdll1cm;
		y[d][l][l1][c][m] = ydll1cm;
		u[d][l][l1][c][m] = udll1cm;
		return qdll1cm;
	}
	public void sampleAndSetqbyu(int d, int l, int l1, int c, int m) throws Exception {
		int qdll1cm = q[d][l][l1][c][m];//note current q
		int v = x[d][l][l1][c][m];
		int k = y[d][l][l1][c][m];
		int s = b[d][l][l1][c][m];
		int j = u[d][l][l1][c][m];
		
		//decrement counts based on current q
		Mdll1ch[d][l][l1][c][qdll1cm]--;
		Mdll1chS[d][l][l1][c]--;
		if (qdll1cm == 0) {
			assert j == -1;
			Mdlsk[d][l][s][k]--;
			Mdll1cs[d][l][l1][c][s]--;
			Mdll1csS[d][l][l1][c]--;
			Mhlkv[0][l1][k][v]--;
			MhlkvS[0][l1][k]--;
		} else {
			assert k == -1;
			assert s == -1;
			Mdll1cj[d][l][l1][c][j]--;
			Mdll1cjS[d][l][l1][c]--;
			Mhlkv[1][l1][j][v]--;
			MhlkvS[1][l1][j]--;
		}
		//sample joint
		qdll1cm = sampleAndSetqbyuJoint(d,l,l1,c,m);
		
//		//sample conditional
//		//sample next q
//		qdll1cm = sampleAndSetq(d,l,l1,c,m);
//		//sample b,y or u, based on next q
//		if (qdll1cm == 0) {
//			sampleAndSetb(d, l, l1, c, m);
//			sampleAndSety(d, l, l1, c, m);
//			u[d][l][l1][c][m] = -1;
//		} else {
//			b[d][l][l1][c][m] = -1;
//			y[d][l][l1][c][m] = -1;
//			sampleAndSetu(d, l, l1, c, m);
//		}
		
		//increment counts
		Mdll1ch[d][l][l1][c][qdll1cm]++;
		Mdll1chS[d][l][l1][c]++;
		s = b[d][l][l1][c][m];
		k = y[d][l][l1][c][m];
		j = u[d][l][l1][c][m];
		if (qdll1cm == 0) {
			assert j == -1;
			Mdlsk[d][l][s][k]++;
			Mdll1cs[d][l][l1][c][s]++;
			Mdll1csS[d][l][l1][c]++;
			Mhlkv[0][l1][k][v]++;
			MhlkvS[0][l1][k]++;
		} else {
			assert s == -1;
			assert k == -1;
			Mdll1cj[d][l][l1][c][j]++;
			Mdll1cjS[d][l][l1][c]++;
			Mhlkv[1][l1][j][v]++;
			MhlkvS[1][l1][j]++;
		}
	}
	public int sampleAndSetq(int d, int l, int l1, int c, int m) throws Exception {
		if (H==1) {//no noise topics
			return 0;
		}
		
		int qdll1cm;
//		//deterministic choice of q
//		if (y[d][l][l1][c][m] == -1) {
//			assert b[d][l][l1][c][m] == -1;
//			qdll1cm = 1;
//		} else {
//			assert u[d][l][l1][c][m] == -1;
//			qdll1cm = 0;
//		}
//		assert q[d][l][l1][c][m] == qdll1cm;
		
		//sample q from fixed distribution
		qdll1cm = rand.nextDouble()*lambdaMult < lambda[0] ? 0 : 1;
		
		q[d][l][l1][c][m] = qdll1cm;
		return qdll1cm;
	}
	public void sampleAndSetb(int d, int l, int l1, int c, int m) throws Exception {
		assert q[d][l][l1][c][m] == 0;
		int bdll1cm;
		int ydll1cm = y[d][l][l1][c][m];
//		assert ydll1cm != -1;
		
		//compute conditional distribution
		int S = w[d][l].length;
		double[] pb = new double[S];
		for (bdll1cm=0; bdll1cm<S; bdll1cm++) {
			double nr;
			if (ydll1cm == -1) {
				// this is a hack! for sampling b when y has an invalid value.
				// in this case, choice of sentence depends only on other 
				// sentence assignments in the comment, and not on the topic
				// of this token (since it is not available).
				nr = 1;         
			} else {
				nr = Ndlsk[d][l][bdll1cm][ydll1cm];
			}
			double dr = w[d][l][bdll1cm].length;
			nr *= delta[d][l][bdll1cm] + Mdll1cs[d][l][l1][c][bdll1cm];
			//Not required since independent of bdll1cm
/*// 			dr *= S*delta + Mdll1csS[d][l][l1][c];
			dr *= deltaS[d][l] + Mdll1csS[d][l][l1][c];*/
			if (bdll1cm==0) {
				pb[0] = nr/dr;
			} else {
				pb[bdll1cm] = pb[bdll1cm-1] + (nr/dr);
			}
		}
		//sample from conditional
		double U = rand.nextDouble();
		for (bdll1cm=0; bdll1cm<S; bdll1cm++) {
			if (U < pb[bdll1cm]/pb[S-1]) break;
		}
		b[d][l][l1][c][m] = bdll1cm;
	}
	public void sampleAndSety(int d, int l, int l1, int c, int m) {
		assert q[d][l][l1][c][m] == 0;
		int bdll1cm = b[d][l][l1][c][m];
		int xdll1cm = x[d][l][l1][c][m];
		int ydll1cm;
		assert bdll1cm != -1;
		//compute conditional distribution
		double[] py = new double[K];
		for (ydll1cm=0; ydll1cm<K; ydll1cm++) {
			double nr = Ndlsk[d][l][bdll1cm][ydll1cm];
			// Not required since independent of ydll1cm
			double dr = 1;//w[d][l][bdll1cm].length;
			nr *= beta + Nlkv[l1][ydll1cm][xdll1cm] + Mhlkv[0][l1][ydll1cm][xdll1cm];
// 			dr *= V[l]*beta + NlkvS[l1][ydll1cm] + MhlkvS[0][l1][ydll1cm];
			dr *= betaV[l1] + NlkvS[l1][ydll1cm] + MhlkvS[0][l1][ydll1cm];
			if (ydll1cm==0) {
				py[0] = nr/dr;
			} else {
				py[ydll1cm] = py[ydll1cm-1] + (nr/dr);
			}
		}
		//sample from conditional
		double U = rand.nextDouble();
		for (ydll1cm=0; ydll1cm<K; ydll1cm++) {
			if (U < py[ydll1cm]/py[K-1]) break;
		}
		y[d][l][l1][c][m] = ydll1cm;
	}
	public void sampleAndSetu(int d, int l, int l1, int c, int m) {
		assert q[d][l][l1][c][m] == 1;
		int xdll1cm = x[d][l][l1][c][m];
		int udll1cm;
		//compute conditional distribution
		double[] pu = new double[J];
		for (udll1cm=0; udll1cm<J; udll1cm++) {
			double nr = omega + Mhlkv[1][l1][udll1cm][xdll1cm];
// 			double dr = V[l]*omega + MhlkvS[1][l][udll1cm];
			double dr = omegaV[l] + MhlkvS[1][l1][udll1cm];
			nr *= eta + Mdll1cj[d][l][l1][c][udll1cm];
			// Not required since independent of udll1cm
/*// 			dr *= J*eta + Mdll1cjS[d][l][l1][c];
			dr *= etaJ + Mdll1cjS[d][l][l1][c];*/
			if (udll1cm==0) {
				pu[0] = nr/dr;
			} else {
				pu[udll1cm] = pu[udll1cm-1] + (nr/dr);
			}
		}
		//sample from conditional
		double U = rand.nextDouble();
		for (udll1cm=0; udll1cm<J; udll1cm++) {
			if (U < pu[udll1cm]/pu[J-1]) break;
		}
		u[d][l][l1][c][m] = udll1cm;
	}
	public void precompute() {
		//precompute
		alphaK = alpha*K;
		etaJ = eta*J;
		lambdaH = lambda[0] + lambda[1];//sum over components of lambda must be 1.
		betaV = new double[L];
		omegaV = new double[L];
		for (int l=0; l<L; l++) {
			betaV[l] = beta * V[l];
			omegaV[l] = omega * V[l];
		}
		gammaT = gamma[0] + gamma[1];
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
								sampleAndSetqbyu(d,l,l1,c,m);
							}
						}
					}
				}
			}
			// compute negative log-likelihood
//			nll[i] = computeNll();
		}
    }
//    private double computeNll() {
//		double ll = 0;
//		for (int l=0; l<L; l++) {
//			for (int k=0; k<K; k++) {
//				for (int v=0; v<V[l]; v++) {
//					ll += MyUtil.logGamma(beta + Nlkv[l][k][v] + Mhlkv[0][l][k][v]);
//				}
//				ll -= MyUtil.logGamma(betaV[l] + NlkvS[l][k] + MhlkvS[0][l][k]);
//			}
//			for (int j=0; j<J; j++) {
//				for (int v=0; v<V[l]; v++) {
//					ll += MyUtil.logGamma(omega + Mhlkv[1][l][j][v]);
//				}
//				ll -= MyUtil.logGamma(omegaV[l] + MhlkvS[1][l][j]);
//			}
//		}
//		for (int d=0; d<D; d++) {
//			for (int t=0; t<MyUtil.maxInt(T[d]); t++) {
//				for (int k=0; k<K; k++) {
//					ll += MyUtil.logGamma(alpha + Ndtk[d][t][k]);
//				}
//				ll -= MyUtil.logGamma(alphaK + NdtkS[d][t]);
//			}
//			for (int l=0; l<L; l++) {
//				int S = w[d][l].length;
//				for (int s=0; s<S; s++) {
//					for (int t=0; t<T[d][l]-1; t++) {
//						ll += MyUtil.logGamma(gamma[0] + Ndlst[d][l][s][t])
//								+ MyUtil.logGamma(gamma[1] + Ndlsgt[d][l][s][t])
//								- MyUtil.logGamma(gammaT + Ndlst[d][l][s][t] + Ndlsgt[d][l][s][t]);
//					}
//				}
//				for (int l1=0; l1<L; l1++) {
//					int C = x[d][l][l1].length;
//					for (int c=0; c<C; c++) {
//						int M = x[d][l][l1][c].length;
//						for (int m=0; m<M; m++) {
//							if (q[d][l][l1][c][m] == 0) {
//								ll += Math.log(Ndlsk[d][l][b[d][l][l1][c][m]][y[d][l][l1][c][m]]);
//							}
//						}
//						for (int s=0; s<S; s++) {
//							ll += MyUtil.logGamma(delta[d][l][s] + Mdll1cs[d][l][l1][c][s]);
//						}
//						ll -= MyUtil.logGamma(deltaS[d][l] + Mdll1csS[d][l][l1][c]);
//						for (int j=0; j<J; j++) {
//							ll += MyUtil.logGamma(eta + Mdll1cj[d][l][l1][c][j]);
//						}
//						ll -= MyUtil.logGamma(etaJ + Mdll1cjS[d][l][l1][c]);
//						for (int h=0; h<H; h++) {
//							ll += MyUtil.logGamma(lambda[h] + Mdll1ch[d][l][l1][c][h]);
//						}
//						ll -= MyUtil.logGamma(lambdaH + Mdll1chS[d][l][l1][c]);
//					}
//				}
//			}
//		}
//		return -ll;
//	}
    
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
		phi = new double[L][K][];
		for (int l=0; l<L; l++) {
			for(int k=0; k<K; k++) {
				double tot = 0;
				phi[l][k] = new double[V[l]];
				for (int v=0; v<V[l]; v++) {
					phi[l][k][v] = beta + Nlkv[l][k][v] + Mhlkv[0][l][k][v];
					tot += phi[l][k][v];
				}
				for (int v=0; v<V[l]; v++) {
					phi[l][k][v] = phi[l][k][v]/tot;
				}
			}
		}
		// infer psi
		psi = new double[L][J][];
		for (int l=0; l<L; l++) {
			for(int j=0; j<J; j++) {
				double tot = 0;
				psi[l][j] = new double[V[l]];
				for (int v=0; v<V[l]; v++) {
					psi[l][j][v] = omega + Mhlkv[1][l][j][v];
					tot += psi[l][j][v];
				}
				for (int v=0; v<V[l]; v++) {
					psi[l][j][v] = psi[l][j][v]/tot;
				}
			}
		}
    }
    public void saveVars(String varDir) throws Exception {
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
				//save this article and comments
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
					varDir+File.separator+String.valueOf(l), String.valueOf(d))));
				bw.append("#w z\n");
				for (int s=0; s<w[d][l].length; s++) {
					for (int n=0; n<w[d][l][s].length; n++) {
						if (n!=0) bw.append(", ");
						bw.append(String.format("%d %d", w[d][l][s][n],z[d][l][s][n]));
					}
					bw.newLine();
				}
				bw.newLine();
				bw.append("#x q-y/u b\n");
				for (int l1=0; l1<L; l1++) {
					bw.append(String.format("#l1=%d\n", l1));
					for (int c=0; c<x[d][l][l1].length; c++) {
						for (int m=0; m<x[d][l][l1][c].length; m++) {
							if (m!=0) bw.append(", ");
							int yu = (q[d][l][l1][c][m] == 0) ? y[d][l][l1][c][m] : u[d][l][l1][c][m];
							bw.append(String.format("%d %d-%d %d",x[d][l][l1][c][m],
								q[d][l][l1][c][m], yu, b[d][l][l1][c][m]));
						}
						bw.newLine();
					}
				}
				bw.close();
			}
		}
    }
	public static void main(String[] args) throws Exception {
//		double lambda=.9, lambdaMult = 30;
//		double[] lambda1;
//		lambda1 = new double[] {lambdaMult*lambda, lambdaMult*(1-lambda)};

	}
}
