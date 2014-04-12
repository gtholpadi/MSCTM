package mllab_mstm.model;

public class MsctmSbpSparseMultNoise extends MsctmSbpSparse {
	int J;
	double xi, xiJ;
	int[][][][][] M3dll1cj;
	int[][][][] M3dll1cjS;
	int[][][] M3ljv;
	int[][] M3ljvS;
	double[][][] psi;

	public MsctmSbpSparseMultNoise(int K, int J, int I, double alpha, double beta,
			double omega, double[][] nu, 
//			double[] mu,
			double[][] mu,
			double[] gamma,
			double[][][] delta, double eta, double[][][][][] lambda, double xi)
			throws Exception {
		super(K, I, alpha, beta, omega, nu, mu, gamma, delta, eta, lambda);
		this.J = J;
		this.xi = xi;
	}
	public int samplePsiTopic() {
		return rand.nextInt(J);
	}
	public void initCounts() {
		super.initCounts();
		M3dll1cj = new int[D][L][L][][];
		M3dll1cjS = new int[D][L][L][];
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
				for (int l1=0; l1<L; l1++) {
					int C = x[d][l][l1].length;
					M3dll1cj[d][l][l1] = new int[C][J];
					M3dll1cjS[d][l][l1] = new int[C];
				}
			}
		}
		M3ljv = new int[L][][];
		M3ljvS = new int[L][J];
		for (int l=0; l<L; l++) {
			M3ljv[l] = new int[J][V[l]];
		}
	}
	public void setCounts() {
		super.setCounts();
		for (int d=0; d<D; d++) {
			for (int l=0; l<L; l++) {
				for (int l1=0; l1<L; l1++) {
					int C = x[d][l][l1].length;
					for (int c=0; c<C; c++) {
						int M = x[d][l][l1][c].length;
						for (int m=0; m<M; m++) {
							int v = x[d][l][l1][c][m];
							int h = q[d][l][l1][c][m];
							int j = y[d][l][l1][c][m];
							if (h == 2) {
								M3dll1cj[d][l][l1][c][j]++;
								M3dll1cjS[d][l][l1][c]++;
								M3ljv[l1][j][v]++;
								M3ljvS[l1][j]++;
							}
						}
					}
				}
			}
		}
	}
	public void precompute() {
		super.precompute();
		xiJ = xi*J;
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
//			M3lv[l1][v]--;
//			M3lvS[l1]--;
			M3dll1cj[d][l][l1][c][k]--;
			M3dll1cjS[d][l][l1][c]--;
			M3ljv[l1][k][v]--;
			M3ljvS[l1][k]--;
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
//			M3lv[l1][v]++;
//			M3lvS[l1]++;
			M3dll1cj[d][l][l1][c][k]++;
			M3dll1cjS[d][l][l1][c]++;
			M3ljv[l1][k][v]++;
			M3ljvS[l1][k]++;
		}
	}
	public void sampleAndSetqbyJoint(int d, int l, int l1, int c, int m) throws Exception {
		int qdll1cm, bdll1cm, ydll1cm;
		int xdll1cm = x[d][l][l1][c][m];
		int S = w[d][l].length; 
		//Precompute common term
		double[] lambdaTermNr = new double[H];
		for (int h=0; h<H; h++) {
			lambdaTermNr[h] = lambda[d][l][l1][c][h] + Mdll1ch[d][l][l1][c][h];
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
		//case 3: topic comes from chi
		qdll1cm = 2;
		bdll1cm = -1;		
		double[] py1 = new double[J];
		for (ydll1cm=0; ydll1cm<J; ydll1cm++) {
			double nr = 0;
			double dr = 1;
			if (lambdaTermNr[qdll1cm] > 0) {
				nr = lambdaTermNr[qdll1cm];
				nr *= xi + M3dll1cj[d][l][l1][c][ydll1cm];
				dr *= xiJ + M3dll1cjS[d][l][l1][c];
				nr *= omega + M3ljv[l1][ydll1cm][xdll1cm];
				dr *= omegaV[l1] + M3ljvS[l1][ydll1cm];
			}
			//add to conditional pmf vector
			if (ydll1cm == 0) {//carry over cumulative sum from case 2
				py1[0] = py[K-1] + nr/dr;
			} else {
				py1[ydll1cm] = py1[ydll1cm-1] + nr/dr;
			}
		}
		//sample from conditional
		double U = rand.nextDouble() * py1[J-1];
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
			for (ydll1cm=0; ydll1cm<J; ydll1cm++) {
				if (U < py1[ydll1cm]) {
					found = true;
					break;
				}
			}
		}
		q[d][l][l1][c][m] = qdll1cm;
		b[d][l][l1][c][m] = bdll1cm;
		y[d][l][l1][c][m] = ydll1cm;
	}
	public void inferPsi() {
		psi = new double[L][J][];
		for (int l=0; l<L; l++) {
			for (int j=0; j<J; j++) {
				double tot = 0;
				psi[l][j] = new double[V[l]];
				for (int v=0; v<V[l]; v++) {
					psi[l][j][v] = omega + M3ljv[l][j][v];
					tot += psi[l][j][v];
				}
				for (int v=0; v<V[l]; v++) {
					psi[l][j][v] = psi[l][j][v]/tot;
				}				
			}
		}
	}
}
