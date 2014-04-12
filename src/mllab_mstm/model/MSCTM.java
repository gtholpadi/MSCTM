package mllab_mstm.model;

import java.io.*;

import mllab_mstm.util.Log;
import mllab_mstm.util.XORShiftRandom;

public class MSCTM {
// 	RandomGenerator gen = null;
	XORShiftRandom rand = null;
	int H; //Number of topic sources
	double lambdaMult = 25;
	double alpha, beta, omega, gamma, delta, eta, lambda[];
/*	//precompute before sampling
	double alphaK, betaV[], omegaV[], gammaT[], deltaS[][], etaJ, lambdaH;*/
// 	int[] Ks = new int[H];
	int I, L, D, K, J;
	int[] T;
	int[] V;
	//precompute
	double alphaK, betaV[], omegaV[], gammaT[], deltaS[][], etaJ, lambdaH;

	int[][][][] w;
	int[][][][] z;
	int[][][][] r;
	int[][][][][] x;
	int[][][][][] q;
	int[][][][][] b;
	int[][][][][] y;
	int[][][][][] u;

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
	int[][][] NdlstS;

	double[][][] theta;
	double[][][][] pi;
	double[][][] phi;
	double[][][] psi;
	double[][][][] rho;

	public MSCTM(int K, int J, int I,
		double alpha, double beta, double omega, double gamma,
		double delta, double eta, double lambda) throws Exception {
		rand = new XORShiftRandom(); //default seed
// 		this.Ks[0] = K;
		this.K = K;
// 		this.Ks[1] = J;
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
		this.lambda = new double[] {lambdaMult*lambda, lambdaMult*(1-lambda)};
		Log.av("lambda", this.lambda[0]);
	}
	public int sampleq(int d, int l, int l1, int c, int m) {
		if (H==1) {
			return 0; //no noise topics
		} else {
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
						r[d][l][s][n] = rand.nextInt(T[d]);
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
							} else {
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
		NdlstS = new int[D][L][];
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
				Ndlst[d][l] = new int[S][T[d]];
				NdlstS[d][l] = new int[S];
			}
			Ndtk[d] = new int[T[d]][K];
			NdtkS[d] = new int[T[d]];
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
    public void setCounts() {
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
						NdlstS[d][l][s]++;
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
								Mdlsk[d][l][s][k]++;
								Mdll1cs[d][l][l1][c][s]++;
								Mdll1csS[d][l][l1][c]++;
								Mhlkv[0][l1][k][v]++;
								MhlkvS[0][l1][k]++;
							} else {
								int j = u[d][l][l1][c][m];
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
		boolean foundInf = false;
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
//			nr *= Math.pow(Ndlsk[d][l][s][zdlsn] + 1, Mdlsk[d][l][s][zdlsn]);
//			for (int k=0; k<K; k++) {
//				if (k != zdlsn) {
//					nr *= Math.pow(Ndlsk[d][l][s][k], Mdlsk[d][l][s][k]);
//				}
//			}
			nr *= beta + Nlkv[l][zdlsn][wdlsn] + Mhlkv[0][l][zdlsn][wdlsn];
// 			double dr = V[l]*beta + NlkvS[l][zdlsn] + MhlkvS[0][l][zdlsn];
			dr *= betaV[l] + NlkvS[l][zdlsn] + MhlkvS[0][l][zdlsn];
			nr *= alpha + Ndtk[d][rdlsn][zdlsn];
			// Not required since independent of zdlsn
// // 			dr *= K*alpha + NdtkS[d][rdlsn];
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
		//compute conditional distribution
		double[] pr = new double[T[d]];
		for (rdlsn=0; rdlsn<T[d]; rdlsn++) {
			double nr = alpha + Ndtk[d][rdlsn][zdlsn];
// 			double dr = K*alpha + NdtkS[d][rdlsn];
			double dr = alphaK + NdtkS[d][rdlsn];
			nr *= gamma + Ndlst[d][l][s][rdlsn];
			// Not required since independent of rdlsn
// // 			dr *= T[d]*gamma + NdlstS[d][l][s];
// 			dr *= gammaT[d] + NdlstS[d][l][s];
			if (rdlsn==0) {
				pr[0] = nr/dr;
			} else {
				pr[rdlsn] = pr[rdlsn-1] + (nr/dr);
			}
		}
		//sample from conditional
		double U = rand.nextDouble();
		for (rdlsn=0; rdlsn<T[d]; rdlsn++) {
			if (U < pr[rdlsn]/pr[T[d]-1]) break;
		}
		r[d][l][s][n] = rdlsn;
		//increment counts
		Ndtk[d][rdlsn][zdlsn]++;
		NdtkS[d][rdlsn]++;
		Ndlst[d][l][s][rdlsn]++;
// 		NdlstS[d][l][s]++;
    }
	public void sampleAndSetqbyu(int d, int l, int l1, int c, int m) throws Exception {
		int h = q[d][l][l1][c][m];//note current q
		int v = x[d][l][l1][c][m];
		//decrement counts based on current q
		Mdll1ch[d][l][l1][c][h]--;
		Mdll1chS[d][l][l1][c]--;
		if (h == 0) {
			int k = y[d][l][l1][c][m];
			int s = b[d][l][l1][c][m];
			Mdlsk[d][l][s][k]--;
			Mdll1cs[d][l][l1][c][s]--;
//			Mdll1csS[d][l][l1][c]--;
			Mhlkv[0][l1][k][v]--;
			MhlkvS[0][l1][k]--;
		} else {
			int j = u[d][l][l1][c][m];
			Mdll1cj[d][l][l1][c][j]--;
//			Mdll1cjS[d][l][l1][c]--;
			Mhlkv[1][l1][j][v]--;
			MhlkvS[1][l1][j]--;
		}
		//sample next q
		int qdll1cm = sampleAndSetq(d,l,l1,c,m);
		//sample b,y or u, based on next q
		if (qdll1cm == 0) {
			sampleAndSetb(d, l, l1, c, m, h);
			sampleAndSety(d, l, l1, c, m);
		} else {
			sampleAndSetu(d, l, l1, c, m);
		}
		//increment counts
		Mdll1ch[d][l][l1][c][qdll1cm]++;
		Mdll1chS[d][l][l1][c]++;
		if (qdll1cm == 0) {
			int s = b[d][l][l1][c][m];
			int k = y[d][l][l1][c][m];
			Mdlsk[d][l][s][k]++;
			Mdll1cs[d][l][l1][c][s]++;
//			Mdll1csS[d][l][l1][c]++;
			Mhlkv[0][l1][k][v]++;
			MhlkvS[0][l1][k]++;
		} else {
			int j = u[d][l][l1][c][m];
			Mdll1cj[d][l][l1][c][j]++;
//			Mdll1cjS[d][l][l1][c]++;
			Mhlkv[1][l1][j][v]--;
			MhlkvS[1][l1][j]--;
		}
	}
	public int sampleAndSetq(int d, int l, int l1, int c, int m) throws Exception {
		if (H==1) {//no noise topics
			return 0;
		}
		int h = q[d][l][l1][c][m];
		int qdll1cm;
		//compute conditional distribution
		double[] pq = new double[H];
		if (h == 0) {//for h=1
			int bdll1cm = b[d][l][l1][c][m];
			int ydll1cm = y[d][l][l1][c][m];
			for (qdll1cm=0; qdll1cm<H; qdll1cm++) {
				double nr = lambda[qdll1cm] + Mdll1ch[d][l][l1][c][qdll1cm];
				//Not required since independent of qdll1cm
				double dr = 1; //lambdaH + Mdll1chS[d][l][l1][c];
				if (qdll1cm == 0) {
					nr *= Ndlsk[d][l][bdll1cm][ydll1cm];
					dr *= w[d][l][bdll1cm].length;
				}
				if (qdll1cm==0) {
					pq[0] = nr/dr;
				} else {
					pq[qdll1cm] = pq[qdll1cm-1] + (nr/dr);
				}
			}
		} else {//for h=2
			for (qdll1cm=0; qdll1cm<H; qdll1cm++) {
				double nr = lambda[qdll1cm] + Mdll1ch[d][l][l1][c][qdll1cm];
				//Not required since independent of qdll1cm
// 				double dr = 1; //lambdaH + Mdll1chS[d][l][l1][c];
				if (qdll1cm==0) {
					pq[0] = nr;
				} else {
					pq[qdll1cm] = pq[qdll1cm-1] + (nr);
				}
			}
		}
		//sample from conditional
		double U = rand.nextDouble();
		for (qdll1cm=0; qdll1cm<H; qdll1cm++) {
			if (U < pq[qdll1cm]/pq[H-1]) break;
		}
		q[d][l][l1][c][m] = qdll1cm;
		return qdll1cm;
	}
	public void sampleAndSetb(int d, int l, int l1, int c, int m, int h) throws Exception {
		int bdll1cm;
		//compute conditional distribution
		int S = w[d][l].length;
		double[] pb = new double[S];
		if (h==0) {
			int ydll1cm = y[d][l][l1][c][m];
			for (bdll1cm=0; bdll1cm<S; bdll1cm++) {
				double nr = Ndlsk[d][l][bdll1cm][ydll1cm];
				double dr = w[d][l][bdll1cm].length;
				nr *= delta + Mdll1cs[d][l][l1][c][bdll1cm];
				//Not required since independent of bdll1cm
	/*// 			dr *= S*delta + Mdll1csS[d][l][l1][c];
				dr *= deltaS[d][l] + Mdll1csS[d][l][l1][c];*/
				if (bdll1cm==0) {
					pb[0] = nr/dr;
				} else {
					pb[bdll1cm] = pb[bdll1cm-1] + (nr/dr);
				}
			}
		} else {
			for (bdll1cm=0; bdll1cm<S; bdll1cm++) {
				double nr = delta + Mdll1cs[d][l][l1][c][bdll1cm];
				//Not required since independent of bdll1cm
	// 			dr *= S*delta + Mdll1csS[d][l][l1][c];
				double dr = 1; //deltaS[d][l] + Mdll1csS[d][l][l1][c];
				if (bdll1cm==0) {
					pb[0] = nr/dr;
				} else {
					pb[bdll1cm] = pb[bdll1cm-1] + (nr/dr);
				}
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
		int bdll1cm = b[d][l][l1][c][m];
		int xdll1cm = x[d][l][l1][c][m];
		int ydll1cm;
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
		lambdaH = lambdaMult;//sum over components of lambda must be 1.
		betaV = new double[L];
		omegaV = new double[L];
		for (int l=0; l<L; l++) {
			betaV[l] = beta * V[l];
			omegaV[l] = omega * V[l];
		}
		gammaT = new double[D];
		for (int d=0; d<D; d++) {
			gammaT[d] = gamma * T[d];
		}
		deltaS = new double[D][L];
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
				deltaS[d][l] = delta * w[d][l].length;
			}
		}
	}
    public void sample() throws Exception {
		precompute();
		Log.prln(String.format("I=%d D=%d L=%d",I,D,L));
		for (int i=0; i<I; i++) {
//			saveVars("./temp");
//			Log.av("Before iter",i);
//			Log.wt();
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
		}
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
		try {
		System.out.println(Math.pow(1/0, 0));
		} catch (java.lang.ArithmeticException ae) {
			System.out.println(Double.POSITIVE_INFINITY == Double.POSITIVE_INFINITY);
		}
	}
}
