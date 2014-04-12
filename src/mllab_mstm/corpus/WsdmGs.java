package mllab_mstm.corpus;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import mllab_mstm.util.Log;
import mllab_mstm.util.MyUtil;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;

//Class for handling gold set given by Trapit
public class WsdmGs {

	// convert WSDM GS to my GS
	// 0 = irrelevant/general, 1 = specific
	public static int[][][][][] getWSDMgs(File gsFile, File lnFile,
			File NdlskFile) throws Exception {
		Pattern pc = Pattern.compile("\\n");
		Pattern ps = Pattern.compile(" ");

		// read all files
		int[][][][] Ndlsk = (new Gson()).fromJson(
				MyUtil.readGzippedTextFile(NdlskFile), int[][][][].class);
		int[][] lnmap = getLnFile(lnFile);
		String gsall = FileUtils.readFileToString(gsFile, "UTF-8");
		// gs per document
		String[] gsd = gsall.split("\\n\\n");
		int D = gsd.length;

		int[][][][][] gs = new int[D][1][1][][];
		for (int d = 0; d < D; d++) {
			int S = Ndlsk[d][0].length;
			// gs per comment
			String[] gsdc = pc.split(gsd[d]);
			int C = lnmap[d].length;
			gs[d][0][0] = new int[C][S];
			for (int c = 0; c < C; c++) {
				int c1 = lnmap[d][c];
				if (!gsdc[c1 + 1].startsWith("NONE")) {
					// relevant sentence ids
					String[] gsdcs = ps.split(gsdc[c1 + 1]);
					for (int i = 0; i < gsdcs.length; i++) {
						gs[d][0][0][c][Integer.parseInt(gsdcs[i])] = 1;
					}
				}
			}
		}
		return gs;
	}

	public static int[][] getLnFile(File lnFile) throws IOException {
		String lnAll = FileUtils.readFileToString(lnFile, "UTF-8");
		String[] lnd = lnAll.split("\\n");
		int[][] lnmap = new int[lnd.length][];
		for (int d = 0; d < lnd.length; d++) {
			String[] maps = lnd[d].split(" ");
			lnmap[d] = new int[maps.length];
			for (int c = 0; c < maps.length; c++) {
				String[] rec = maps[c].split(":");
				lnmap[d][Integer.parseInt(rec[0])] = Integer.parseInt(rec[1]);
			}
		}
		return lnmap;
	}

	public static void printGs(int[][][][][] gs) throws Exception {
		for (int d = 0; d < gs.length; d++) {
			for (int l = 0; l < gs[d].length; l++) {
				for (int l1 = 0; l1 < gs[d][l].length; l1++) {
					for (int c = 0; c < gs[d][l][l1].length; c++) {
						Log.pr(String.format("(%d, %d) -->", d, c));
						for (int s = 0; s < gs[d][l][l1][c].length; s++) {
							if (gs[d][l][l1][c][s] == 1) {
								Log.pr(String.format(" %d", s));
							}
						}
						Log.prln("");
					}
				}
			}
		}
	}

	public static void wsdmgs2mygs(File gsFile, File lnFile, File NdlskFile,
			File mygsFile) throws Exception {
		int[][][][][] gs = WsdmGs.getWSDMgs(gsFile, lnFile, NdlskFile);
		MyUtil.saveToGzippedTextFile((new Gson()).toJson(gs), mygsFile);
		printGs(gs);
	}

	public static void main(String[] args) throws Exception {
		String cmd = args[0];
		int i = 1;
		if (cmd.equals("wsdmgs2mygs")) {
			String gsFile = args[i++];
			String lnFile = args[i++];
			String NdlskFile = args[i++];
			String mygsFile = args[i++];
			wsdmgs2mygs(new File(gsFile), new File(lnFile),
					new File(NdlskFile), new File(mygsFile));
		} else {
			Log.pr(Math.ceil(((double) 5) / 99999));
		}
	}
}
