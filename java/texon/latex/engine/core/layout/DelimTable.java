package texon.latex.engine.core.layout;
public final class DelimTable {
	private static final int[] CP = {
		0x28, 0x29, 0x2F, 0x5B, 0x5C, 0x5D, 0x7B, 0x7C, 0x7D,
		0x2016, 0x2191, 0x2193, 0x2195,
		0x2308, 0x2309, 0x230A, 0x230B, 0x27E8, 0x27E9
	};
	private static final int[] BASE = {
		-7, -7, 0, -5, -5, -5, -7, -5, -7,
		16, -7, -7, -7,
		-11, -11, -11, -11, -5, -5
	};
	private static final int[] STRETCH = {
		82, 82, 98, 97, 98, 97, 120, 137, 120,
		186, 190, 190, 153,
		90, 90, 90, 90, 133, 133
	};
	private DelimTable() {
	}
	private static int at(int cp, int[] v) {
		int lo = 0;
		int hi = CP.length - 1;
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			int c = CP[mid];
			if (c == cp) return v[mid];
			if (c < cp) lo = mid + 1; else hi = mid - 1;
		}
		return 0;
	}
	public static int base(int cp) {
		return at(cp, BASE);
	}
	public static int stretch(int cp) {
		return at(cp, STRETCH);
	}
	public static int kern(int cp, boolean stretched) {
		return at(cp, stretched ? STRETCH : BASE);
	}
}