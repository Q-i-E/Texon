package texon.latex.engine.core.layout;
public final class AccentCompat {
	private static final int[] CP = {0x300, 0x301, 0x302, 0x303, 0x304, 0x306, 0x307, 0x308, 0x30A, 0x30C, 0x20D6, 0x20D7, 0x20DB, 0x20DC, 0x20E1};
	private static final int[] GAP = {-38, -38, -8, 23, 5, -27, 15, 23, -11, -32, 0, -33, 40, 40, 0};
	private AccentCompat() {
	}
	public static int gap(int cp) {
		int lo = 0;
		int hi = CP.length - 1;
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			int c = CP[mid];
			if (c == cp) return GAP[mid];
			if (c < cp) lo = mid + 1; else hi = mid - 1;
		}
		return 0;
	}
	private static final int[] WCP = {0x302, 0x303};
	private static final int[] WGAP = {17, 65};
	public static int wideGap(int cp) {
		int lo = 0;
		int hi = WCP.length - 1;
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			int c = WCP[mid];
			if (c == cp) return WGAP[mid];
			if (c < cp) lo = mid + 1; else hi = mid - 1;
		}
		return gap(cp);
	}
}