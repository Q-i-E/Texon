package texon.latex.engine.core.layout;
public final class SpacingTable {
	public static final byte ORD = 0;
	public static final byte OP = 1;
	public static final byte BIN = 2;
	public static final byte REL = 3;
	public static final byte OPEN = 4;
	public static final byte CLOSE = 5;
	public static final byte PUNCT = 6;
	public static final byte INNER = 7;
	public static final int MU_DIV = 18;
	public static final int DELIMITER_PAD = 100;
	public static final int DELIMITER_SPACE = 0;
	private static byte[] PAIR = new byte[64];
	private static int[] CP = new int[0];
	private static byte[] CLS = new byte[0];
	public static void install(int[] cp, byte[] cls, byte[] pair) {
		CP = cp;
		CLS = cls;
		PAIR = pair;
	}
	public static byte of(int cp) {
		int lo = 0;
		int hi = CP.length - 1;
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			int v = CP[mid];
			if (v == cp) return CLS[mid];
			if (v < cp) lo = mid + 1; else hi = mid - 1;
		}
		return ORD;
	}
	public static int space(byte left, byte right, int mu) {
		return PAIR[left * 8 + right] * mu;
	}
}
