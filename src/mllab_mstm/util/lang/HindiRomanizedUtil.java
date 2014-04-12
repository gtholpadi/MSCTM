package mllab_mstm.util.lang;

public class HindiRomanizedUtil extends EnglishUtil {
	private static HindiRomanizedUtil instance;
	protected HindiRomanizedUtil() {
		super();
	}
	public static HindiRomanizedUtil getInstance() {
		if (instance == null) {
			instance = new HindiRomanizedUtil();
		}
		return instance;
	}
}
