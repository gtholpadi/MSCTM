package mllab_mstm.model;

import org.apache.commons.math3.random.*;

public class MSTM {
	RandomGenerator gen = null;
	double alpha, beta, gamma;
	int T, K, I, L, D;
	Integer[] V = null;

	Integer[][][][] w = null;
	Integer[][][][] x = null;
	Integer[][][][] z = null;

	Integer[][][][] Nldtk = null;
	Integer[][][] Nldt = null;

	Integer[][][][] Nldst = null;
	Integer[][][] Nlds = null;

	Integer[][][] Nlkv = null;
	Integer[][] Nlk = null;

	Double[][][] theta = null;
	Double[][][][] pi = null;
	Double[][][] phi = null;

	public MSTM(int seed, int T, int K, int I, double alpha, double beta, double gamma) {
		gen = new MersenneTwister(seed); //8999999999999090999
		this.T = T;
		this.K = K;
		this.I = I;
		this.alpha = alpha;
		this.beta = beta;
		this.gamma = gamma;
	}
    private int randx() {
		return gen.nextInt(T);
    }
    private int randz() {
		return gen.nextInt(K);
    }
    public void initLatent() {
		x = new Integer[w.length][][][];
		z = new Integer[w.length][][][];
		for (int l=0; l<w.length; l++) {
			x[l] = new Integer[w[l].length][][];
			z[l] = new Integer[w[l].length][][];
			for (int d=0; d<w[l].length; d++) {
				x[l][d] = new Integer[w[l][d].length][];
				z[l][d] = new Integer[w[l][d].length][];
				for (int s=0; s<w[l][d].length; s++) {
					x[l][d][s] = new Integer[w[l][d][s].length];
					z[l][d][s] = new Integer[w[l][d][s].length];
					for (int n=0; n<w[l][d][s].length; n++) {
						x[l][d][s][n] = randx();
						z[l][d][s][n] = randz();
					}
				}
			}
		}
    }
    public void initCounts() {
		int l,d,s,t,k,v;//n
		int L = w.length;
		Nldtk = new Integer[L][][][];
		Nldt = new Integer[L][][];
		Nldst = new Integer[L][][][];
		Nlds = new Integer[L][][];
		Nlkv = new Integer[L][][];
		Nlk = new Integer[L][];
		for (l=0; l<L; l++) {
			int D = w[l].length;
			Nldtk[l] = new Integer[D][][];
			Nldt[l] = new Integer[D][];
			Nldst[l] = new Integer[D][][];
			Nlds[l] = new Integer[D][];
			for (d=0; d<D; d++) {
				Nldtk[l][d] = new Integer[T][];
				Nldt[l][d] = new Integer[T];
				for (t=0; t<T; t++) {
					Nldtk[l][d][t] = new Integer[K];
					Nldt[l][d][t] = 0;
					for (k=0; k<K; k++) {
						Nldtk[l][d][t][k] = 0;
					}
				}
				int S = w[l][d].length;
				Nldst[l][d] = new Integer[S][];
				Nlds[l][d] = new Integer[S];
				for (s=0; s<S; s++) {
					Nldst[l][d][s] = new Integer[T];
					Nlds[l][d][s] = 0;
					for (t=0; t<T; t++) {
						Nldst[l][d][s][t] = 0;
					}
				}
			}
			Nlkv[l] = new Integer[K][];
			Nlk[l] = new Integer[K];
			for (k=0; k<K; k++) {
				Nlkv[l][k] = new Integer[V[l]];
				Nlk[l][k] = 0;
				for (v=0; v<V[l]; v++) {
					Nlkv[l][k][v] = 0;
				}
			}
		}
    }
    public void setCounts() {
		int l,d,s,n,t,k,v;
		for (l=0; l<w.length; l++) {
			for (d=0; d<w[l].length; d++) {
				for (s=0; s<w[l][d].length; s++) {
					for (n=0; n<w[l][d][s].length; n++) {
						t = x[l][d][s][n];
						k = z[l][d][s][n];
						v = w[l][d][s][n];
						Nldtk[l][d][t][k]++;
						Nldt[l][d][t]++;
						Nldst[l][d][s][t]++;
						Nlds[l][d][s]++;
						Nlkv[l][k][v]++;
						Nlk[l][k]++;
					}
				}
			}
		}
    }
    public void printCounts() {
		System.out.print("\n**********\nCOUNTS\n");
		int l,d,s,t,k,v;//n
		int L = w.length;
		for (l=0; l<L; l++) {
			int D = w[l].length;
			for (d=0; d<D; d++) {
				System.out.print("\nNldtk\n");
				for (t=0; t<T; t++) {
					System.out.print(Nldt[l][d][t]+": ");
					for (k=0; k<K; k++) {
						System.out.print(Nldtk[l][d][t][k]+" ");
					}
					System.out.print("\n");
				}
				System.out.print("---");
				int S = w[l][d].length;
				System.out.print("\nNldst\n");
				for (s=0; s<S; s++) {
					System.out.print(Nlds[l][d][s] + ": ");
					for (t=0; t<T; t++) {
						System.out.print(Nldst[l][d][s][t] + " ");
					}
					System.out.print("\n");
				}
				System.out.print("---");
			}
			System.out.print("\nNlkv\n");
			for (k=0; k<K; k++) {
				System.out.print(Nlk[l][k] + ": ");
				for (v=0; v<V[l]; v++) {
					System.out.print(Nlkv[l][k][v] + " ");
				}
				System.out.print("\n");
			}
			System.out.print("---");
		}
    }
    public void sampleAndSetx(int l, int d, int s, int n) {
		int xldsn;
		int xldsn_old = x[l][d][s][n];
		int zldsn = z[l][d][s][n];
		//N... to N...{-ldsn}, i.e. counts without ldsn
		Nldtk[l][d][xldsn_old][zldsn]--;
		Nldt[l][d][xldsn_old]--;
		Nldst[l][d][s][xldsn_old]--;
		Nlds[l][d][s]--;
		//these counts are not used; hence not changing them
// 		Nlkv[l][zldsn][wldsn]--;
// 		Nlk[l][zldsn]--;
		//compute conditional distribution
		double[] px = new double[T];
		for (xldsn=0; xldsn<T; xldsn++) {
			// first term
			double nr1 = alpha;
			for (int l1=0; l1<w.length;l1++) {
				nr1 += Nldtk[l1][d][xldsn][zldsn];
			}
			double dr1 = K*alpha;
			for (int l1=0; l1<w.length;l1++) {
				dr1 += Nldt[l1][d][xldsn];
			}
			//second term
			double nr2 = gamma + Nldst[l][d][s][xldsn];
			double dr2 = T*gamma + Nlds[l][d][s];
			//p(x)
			if (xldsn==0) {
				px[xldsn] = (nr1*nr2)/(dr1*dr2);
			} else {
				px[xldsn] = px[xldsn-1] + (nr1*nr2)/(dr1*dr2);
			}
		}
		//sample from conditional
		double u = gen.nextDouble();
		for (xldsn=0; xldsn<T; xldsn++) {
			if (u < px[xldsn]/px[T-1]) break;
		}
		//set var
		x[l][d][s][n] = xldsn;
		//N...{-ldsn} to N..., i.e. counts with ldsn
		Nldtk[l][d][xldsn][zldsn]++;
		Nldt[l][d][xldsn]++;
		Nldst[l][d][s][xldsn]++;
		Nlds[l][d][s]++;
		//these counts are not used; hence not changing them
// 		Nlkv[l][zldsn][wldsn]++;
// 		Nlk[l][zldsn]++;
    }
	public void sampleAndSetz(int l, int d, int s, int n) {
		int zldsn;
		int xldsn = x[l][d][s][n];
		int wldsn = w[l][d][s][n];
		int zldsn_old = z[l][d][s][n];
		//N... to  N...{-ldsn} i.e. counts without ldsn
		Nldtk[l][d][xldsn][zldsn_old]--;
		Nldt[l][d][xldsn]--;
		//these counts are not used; hence not changing them
// 		Nldst[l][d][s][xldsn]--;
// 		Nlds[l][d][s]--;
		Nlkv[l][zldsn_old][wldsn]--;
		Nlk[l][zldsn_old]--;
		//compute conditional distribution
		double[] pz = new double[K];
		for (zldsn=0; zldsn<K; zldsn++) {
			// first term
			double nr1 = alpha;
			for (int l1=0; l1<w.length;l1++) {
				nr1 += Nldtk[l1][d][xldsn][zldsn];
			}
			double dr1 = K*alpha;
			for (int l1=0; l1<w.length;l1++) {
				dr1 += Nldt[l1][d][xldsn];
			}
			//second term
			double nr2 = beta + Nlkv[l][zldsn][wldsn];
			double dr2 = V[l]*beta + Nlk[l][zldsn];
			//p(z)
			if (zldsn==0) {
				pz[zldsn] = (nr1*nr2)/(dr1*dr2);
			} else {
				pz[zldsn] = pz[zldsn-1] + (nr1*nr2)/(dr1*dr2);
			}
		}
		//sample from conditional
		double u = gen.nextDouble();
		for (zldsn=0; zldsn<K; zldsn++) {
			if (u < pz[zldsn]/pz[K-1]) break;
		}
		//set var
		z[l][d][s][n] = zldsn;
		//N...{-ldsn} to N... i.e. counts with ldsn
		Nldtk[l][d][xldsn][zldsn]++;
		Nldt[l][d][xldsn]++;
		//these counts are not used; hence not changing them
// 		Nldst[l][d][s][xldsn]++;
// 		Nlds[l][d][s]++;
		Nlkv[l][zldsn][wldsn]++;
		Nlk[l][zldsn]++;
    }
    public void sample() {
		int l,d,s,n;//,t,k,v;
		for (int i=0; i < this.I; i++) {
			for (l=0; l<w.length; l++) {
				for (d=0; d<w[l].length; d++) {
					for (s=0; s<w[l][d].length; s++) {
						for (n=0; n<w[l][d][s].length; n++) {
							sampleAndSetx(l,d,s,n);
							sampleAndSetz(l,d,s,n);
						}
					}
				}
			}
		}
    }
    public void inferParams() {
		int l,d,s,t,k,v;//n
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
		}
		// infer phi
		for (l=0; l<L; l++) {
			for(k=0; k<K; k++) {
				double tot = 0;
				double[] pv = new double[V[l]];
				for (v=0; v<V[l]; v++) {
					pv[v] = beta + Nlkv[l][k][v];
					tot += pv[v];
				}
				for (v=0; v<V[l]; v++) {
					phi[l][k][v] = pv[v]/tot;
				}
			}
		}
    }
    private void printVars(Integer[][][][] corpora) {
		for (int l=0; l<corpora.length; l++) {
			for (int d=0; d<corpora[l].length; d++) {
// 				System.out.println("File: "+filesl[l][d].getAbsolutePath());
				System.out.println("Language: "+l+", Document: "+d);
				for (int s=0; s<corpora[l][d].length; s++) {
					for (int n=0; n<corpora[l][d][s].length; n++) {
						System.out.print(corpora[l][d][s][n] + "-");
					}
					System.out.println("");
				}
				System.out.println("---");
			}
			System.out.println("\n================\n");
		}
    }
    public void printVars() {
 		printVars(w);
		printVars(x);
		printVars(z);
    }
}
