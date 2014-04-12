package mllab_mstm.model;

import mllab_mstm.util.MyUtil;

public class MsctmSbpSparse extends MsctmSbp1 {
	double[][] nu;
//	double[] mu;
	double[][] mu;
	int a1[][][];
	int e1[][][];
	int f1[][][];
	int Vlk[][];
	int Klv[][];
	int Valk[][];
	public MsctmSbpSparse(int K, int I, double alpha, double beta,
			double omega, double[][] nu, 
//			double[] mu, 
			double[][] mu, 
			double[] gamma, double[][][] delta, double eta,
			double[][][][][] lambda) throws Exception {
		super(K, I, alpha, beta, omega, gamma, delta, eta, lambda);
		this.nu = nu;
		this.mu = mu;
	}
	public void initLatent() throws Exception {
		super.initLatent();
		//init counts now itself since needed for initializing sparsity variables
		super.initCounts();
		super.setCounts();
		//sparsity variables
		a1 = new int[L][][];
		e1 = new int[L][][];
		f1 = new int[L][][];
		for (int l=0; l<L; l++) {
			a1[l] = new int[K][V[l]+1];
			e1[l] = new int[K][V[l]];
			f1[l] = new int[K][V[l]];
			for (int k=0; k<K; k++) {
				boolean allZeros = true;
				for (int v=0; v<V[l]; v++) {
//					a1[l][k][v] = (Nlkv[l][k][v] > 0 || M12lkv[l][k][v] > 0) ? 1 : rand.nextInt(2);
					if (Nlkv[l][k][v] > 0 || M12lkv[l][k][v] > 0) {
						a1[l][k][v] = e1[l][k][v] = f1[l][k][v] = 1;
					} else {
						e1[l][k][v] = rand.nextInt(2);
						f1[l][k][v] = rand.nextInt(2);
						a1[l][k][v] = e1[l][k][v] & f1[l][k][v];//faster than mult?
					}
					if(a1[l][k][v]==1) {
						allZeros = false;
					}
				}
				if (allZeros) {
					a1[l][k][V[l]] = 1;
				}
			}
		}
	}
	public void initCounts() {
		//super method already called in initLatent
		//sparsity variable counts
		Vlk = new int[L][K];
		Valk = new int[L][K];
		Klv = new int[L][];
		for (int l=0; l<L; l++) {
			Klv[l] = new int[V[l]];
		}
	}
	public void setCounts() {
		//super method already called in initLatent
		//sparsity variable counts
		for (int l=0; l<L; l++) {
			for (int k=0; k<K; k++) {
				Vlk[l][k] = MyUtil.sumInt(e1[l][k]);
				Valk[l][k] = MyUtil.sumInt(a1[l][k]) - a1[l][k][V[l]];
			}
			for (int v=0; v<V[l]; v++) {
				for (int k=0; k<K; k++) {
					if (f1[l][k][v]==1) {
						Klv[l][v]++;
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
			//check indicator functions
			if (a1[l][zdlsn][wdlsn]==0) {
				nr = 0;
			} else {
				if (Ndlsk[d][l][s][zdlsn] == 0) {//fraction=infinity
					if (Mdlsk[d][l][s][zdlsn] == 0) {//(infinity)^0=1
						nr *= 1;
					} else {
						foundInf = true;
						break;
					}
				} else {
					nr *= Math.pow((Ndlsk[d][l][s][zdlsn] + 1)/Ndlsk[d][l][s][zdlsn],Mdlsk[d][l][s][zdlsn]);
				}
				//compute other terms
				nr *= beta + Nlkv[l][zdlsn][wdlsn] + M12lkv[l][zdlsn][wdlsn];
				dr *= beta*Valk[l][zdlsn] + NlkvS[l][zdlsn] + M12lkvS[l][zdlsn];
				nr *= alpha + Ndtk[d][rdlsn][zdlsn];
				// Not required since independent of zdlsn
	// 			dr *= alphaK + NdtkS[d][rdlsn];
			}
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
				double nr=0;
				double dr=1;
				//check indicator functions
				if (Ndlsk[d][l][bdll1cm][ydll1cm] > 0 && a1[l1][ydll1cm][xdll1cm] == 1) {
					nr = Ndlsk[d][l][bdll1cm][ydll1cm];
					dr = w[d][l][bdll1cm].length;
					nr *= beta + Nlkv[l1][ydll1cm][xdll1cm] + M12lkv[l1][ydll1cm][xdll1cm];
					dr *= beta*Valk[l1][ydll1cm] + NlkvS[l1][ydll1cm] + M12lkvS[l1][ydll1cm];
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
			double nr = 0;
			double dr = 1;
			if (lambdaTermNr[qdll1cm] > 0 && a1[l1][ydll1cm][xdll1cm] == 1) {
				nr = lambdaTermNr[qdll1cm];
				nr *= beta + Nlkv[l1][ydll1cm][xdll1cm] + M12lkv[l1][ydll1cm][xdll1cm];
				dr *= beta * Valk[l1][ydll1cm] + NlkvS[l1][ydll1cm] + M12lkvS[l1][ydll1cm];
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
	public void sampleAndSetaefJoint(int l, int k, int v) {
		int alkv = a1[l][k][v]; 
		int elkv = e1[l][k][v]; 
		int flkv = f1[l][k][v]; 
		//decrement counts
		if (alkv==1) {//decrement count if a was on
			Valk[l][k]--;
			assert Valk[l][k]>=0;
			if (Valk[l][k]==0) {
				a1[l][k][V[l]] = 1;
			}
		}
		if (elkv==1) {
			Vlk[l][k]--;
		}
		if (flkv==1) {
			Klv[l][v]--;
		}
		//compute conditional
		if (Nlkv[l][k][v]>0 || M12lkv[l][k][v]>0) {//check is sampling is required
			alkv = elkv = flkv = 1;
		} else {
			double[][] paef = new double[2][2];
			double[] betaterm =
					new double[]{MyUtil.logGamma(beta*Valk[l][k]) - MyUtil.logGamma(beta*Valk[l][k] + NlkvS[l][k] + M12lkvS[l][k]),
					MyUtil.logGamma(beta*Valk[l][k] + beta) - MyUtil.logGamma(beta*Valk[l][k] + NlkvS[l][k] + M12lkvS[l][k] + beta)};
			double[] nuterm = new double[]{Math.log(nu[l][1] + V[l] - Vlk[l][k] - 1), Math.log(nu[l][0] + Vlk[l][k])};
//			double[] muterm = new double[]{Math.log(mu[1] + K - Klv[l][v] - 1), Math.log(mu[0] + Klv[l][v])};
			double[] muterm = new double[]{Math.log(mu[l][1] + K - Klv[l][v] - 1), Math.log(mu[l][0] + Klv[l][v])};
			//first compute log pi
			double maxlogp = Double.NEGATIVE_INFINITY;
			for(elkv=0; elkv<2; elkv++) {
				for(flkv=0; flkv<2; flkv++) {
					alkv = elkv & flkv;
					paef[elkv][flkv] = betaterm[alkv] + nuterm[elkv] + muterm[flkv]; //log pi
					if (paef[elkv][flkv] > maxlogp) {
						maxlogp = paef[elkv][flkv];
					}
				}
			}
			//next normalize all log p values by max
			for(elkv=0; elkv<2; elkv++) {
 				for(flkv=0; flkv<2; flkv++) {
					paef[elkv][flkv] = paef[elkv][flkv] - maxlogp; //log (pi/pmax)
				}
			}
			//now build vector for sampling
			for(elkv=0; elkv<2; elkv++) {
				for(flkv=0; flkv<2; flkv++) {
					double nr = Math.exp(paef[elkv][flkv]);//log p -> p
					if (elkv==0 && flkv==0) {
						paef[0][0] = nr;
					} else if(flkv==0) {
						paef[elkv][0] = paef[elkv-1][1] + nr;
					} else {
						paef[elkv][flkv] = paef[elkv][flkv-1] + nr;
					}
				}
			}
			//sample from conditional
			double U = rand.nextDouble() * paef[1][1];
			boolean found = false;
			for (elkv=0; elkv<2; elkv++) {
				for (flkv=0; flkv<2; flkv++) {
					if (U<paef[elkv][flkv]) {
						found = true;
						break;
					}
				}
				if (found) break;
			}
			assert found;
			assert elkv<2;
			assert flkv<2;
			alkv = elkv & flkv;
		}
		a1[l][k][v] = alkv;
		e1[l][k][v] = elkv;
		f1[l][k][v] = flkv;
		//increment counts
		if (alkv==1) {
			Valk[l][k]++;
			a1[l][k][V[l]] = 0;
		}
		if (elkv==1) {
			Vlk[l][k]++;
		}
		if (flkv==1) {
			Klv[l][v]++;
		}
	}
    public void sampleI() throws Exception {
    	super.sampleI();
    	for (int l=0; l<L; l++) {
    		for (int k=0; k<K; k++) {
    			for (int v=0; v<V[l]; v++) {
    				sampleAndSetaefJoint(l, k, v);
    			}
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
					if (a1[l][k][v]==1) {
						phi[l][k][v] = beta + Nlkv[l][k][v] + M12lkv[l][k][v];
					} else {
						phi[l][k][v] = 0;
					}
					tot += phi[l][k][v];
				}
				for (int v=0; v<V[l]; v++) {
					phi[l][k][v] = phi[l][k][v]/tot;
				}
			}
		}
	}
}
