package texon.latex.engine.core;
public final class ColorTable {
	public static final int NONE = 0;
	private static final String[] NAMES = {
		"black", "white", "red", "green", "blue", "cyan", "aqua", "magenta", "fuchsia", "yellow",
		"gray", "grey", "silver", "maroon", "olive", "lime", "teal", "navy", "purple", "orange",
		"pink", "brown", "gold", "darkred", "darkgreen", "darkblue", "lightgray", "lightgrey", "transparent"
	};
	private static final int[] VALUES = {
		0xFF000000, 0xFFFFFFFF, 0xFFFF0000, 0xFF008000, 0xFF0000FF, 0xFF00FFFF, 0xFF00FFFF, 0xFFFF00FF, 0xFFFF00FF, 0xFFFFFF00,
		0xFF808080, 0xFF808080, 0xFFC0C0C0, 0xFF800000, 0xFF808000, 0xFF00FF00, 0xFF008080, 0xFF000080, 0xFF800080, 0xFFFFA500,
		0xFFFFC0CB, 0xFFA52A2A, 0xFFFFD700, 0xFF8B0000, 0xFF006400, 0xFF00008B, 0xFFD3D3D3, 0xFFD3D3D3, 0x00000000
	};
	private ColorTable() {
	}
	public static int parse(CharSequence s, int from, int to) {
		while (from < to && s.charAt(from) <= ' ') from++;
		while (to > from && s.charAt(to - 1) <= ' ') to--;
		if (from >= to) return NONE;
		if (s.charAt(from) == '#') return hex(s, from + 1, to);
		for (int i = 0; i < NAMES.length; i++) {
			if (match(s, from, to, NAMES[i])) return VALUES[i];
		}
		return NONE;
	}
	private static boolean match(CharSequence s, int from, int to, String name) {
		if (to - from != name.length()) return false;
		for (int i = 0; i < name.length(); i++) {
			char c = s.charAt(from + i);
			if (c >= 'A' && c <= 'Z') c += 32;
			if (c != name.charAt(i)) return false;
		}
		return true;
	}
	private static int hex(CharSequence s, int from, int to) {
		int v = 0;
		int n = 0;
		for (int i = from; i < to; i++) {
			int d = digit(s.charAt(i));
			if (d < 0) break;
			v = (v << 4) | d;
			n++;
		}
		if (n == 3) {
			int r = (v >> 8) & 15;
			int g = (v >> 4) & 15;
			int b = v & 15;
			return 0xFF000000 | (r * 17) << 16 | (g * 17) << 8 | (b * 17);
		}
		if (n == 6) return 0xFF000000 | v;
		if (n == 8) return v;
		return NONE;
	}
	private static int digit(char c) {
		if (c >= '0' && c <= '9') return c - '0';
		if (c >= 'a' && c <= 'f') return c - 'a' + 10;
		if (c >= 'A' && c <= 'F') return c - 'A' + 10;
		return -1;
	}
}