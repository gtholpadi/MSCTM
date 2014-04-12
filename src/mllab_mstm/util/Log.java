package mllab_mstm.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class Log {
	static BufferedWriter bw;

	public static void set(File logFile) throws Exception {
		bw = new BufferedWriter(new FileWriter(logFile));
	}

	public static void reset() throws Exception {
		if (bw != null) {
			bw.close();
			bw = null;
		}
	}

	public static void prln(Object o) throws Exception {
		if (o == null) {
			o = "null";
		}
		if (bw == null) {
			System.out.println(o.toString());
		} else {
			bw.write(o.toString());
			bw.newLine();
		}
	}

	public static void pr(Object o) throws Exception {
		if (o == null) {
			o = "null";
		}
		String line = String.format("%s", o.toString());
		if (bw == null) {
			System.out.print(line);
		} else {
			bw.append(line);
		}
	}

	public static void av(String a, Object v) throws Exception {
		if (v == null) {
			v = "null";
		}
		String line = String.format("%s:%s\n", a, v.toString());
		if (bw == null) {
			System.out.print(line);
		} else {
			bw.append(line);
		}
	}

	public static void wt() {// throws Exception {
		System.console().readLine();
	}
}