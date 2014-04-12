package mllab_mstm.util.lang;

public class KannadaRomanizedUtil extends EnglishUtil {
	private static KannadaRomanizedUtil instance;
	protected KannadaRomanizedUtil() {
		super();
	}
	public static KannadaRomanizedUtil getInstance() {
		if (instance == null) {
			instance = new KannadaRomanizedUtil();
		}
		return instance;
	}
}
