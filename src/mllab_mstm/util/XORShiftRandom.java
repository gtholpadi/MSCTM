package mllab_mstm.util;
import java.util.*;

// Copied from http://www.javamex.com/tutorials/random_numbers/java_util_random_subclassing.shtml
@SuppressWarnings("serial")
public class XORShiftRandom extends Random {
//   private long seed = System.nanoTime();
	private static long defaultSeed = 35L;
  private long seed;
  private long initialSeed;
  
  public long getInitialSeed() {
	  return initialSeed;
  }

  public XORShiftRandom() {//default seed
		seed = initialSeed = defaultSeed;
  }
  public XORShiftRandom(long seed) {
		this.seed = initialSeed = seed;
  }
  protected int next(int nbits) {
    // N.B. Not thread-safe!
    long x = this.seed;
    x ^= (x << 21);
    x ^= (x >>> 35);
    x ^= (x << 4);
    this.seed = x;
    x &= ((1L << nbits) -1);
    return (int) x;
  }
}
