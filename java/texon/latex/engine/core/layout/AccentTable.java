package texon.latex.engine.core.layout;
public final class AccentTable {
	public static final int GAP = 0;
	public static final int COUNT = 8;
	private static int[] ID = new int[0];
	private static int[] CP = new int[0];
	private static short[] BOTTOM = new short[0];
	public static void install(int[] id, int[] cp, short[] bottom) {
		ID = id;
		CP = cp;
		BOTTOM = bottom;
	}
	public static int indexOf(int id) {
		for (int i = 0; i < ID.length; i++) {
			if (ID[i] == id) return i;
		}
		return -1;
	}
	public static int cp(int i) {
		return CP[i];
	}
	public static int bottomOf(int cp) {
		for (int i = 0; i < CP.length; i++) {
			if (CP[i] == cp) return BOTTOM[i];
		}
		return 0;
	}
}
