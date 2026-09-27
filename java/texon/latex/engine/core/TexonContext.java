package texon.latex.engine.core;
import texon.latex.engine.config.*;
public final class TexonContext {
	public final TexonOptions options;
	public final SymbolTable symbols = new SymbolTable();
	public final SpacingTable spacing = new SpacingTable();
	public final AccentTable accents = new AccentTable();
	public final FamilyTable families = new FamilyTable();
	public final DelimTable delims = new DelimTable();
	public final AccentCompat accentsCompat = new AccentCompat();
	public final ColorTable colors = new ColorTable();
	private int[] slot = new int[0];
	private int slotMask;
	public TexonContext() {
		this(new TexonOptions());
	}
	public TexonContext(TexonOptions options) {
		this.options = options;
	}
	public void installSymbols(String[] name, byte[] kind, int[] value) {
		symbols.name = name;
		symbols.kind = kind;
		symbols.value = value;
		int cap = 16;
		while (cap < name.length * 2) cap <<= 1;
		slot = new int[cap];
		java.util.Arrays.fill(slot, -1);
		slotMask = cap - 1;
		for (int i = 0; i < name.length; i++) {
			int s = hash(name[i]) & slotMask;
			while (slot[s] != -1) s = (s + 1) & slotMask;
			slot[s] = i;
		}
	}
	public void installSpacing(int[] cp, byte[] cls, byte[] pair) {
		spacing.cp = cp;
		spacing.cls = cls;
		spacing.pair = pair;
	}
	public void installAccents(int[] id, int[] cp, short[] bottom) {
		accents.id = id;
		accents.cp = cp;
		accents.bottom = bottom;
	}
	public void installFamilies(int[] up, int[] lo, int[] di, int[] holeCp, int[] holeTo) {
		families.up = up;
		families.lo = lo;
		families.di = di;
		families.holeCp = holeCp;
		families.holeTo = holeTo;
	}
	public int symbolOf(CharSequence s, int from, int to) {
		if (slot.length == 0) return -1;
		int h = 0x811C9DC5;
		for (int i = from; i < to; i++) h = (h ^ s.charAt(i)) * 0x01000193;
		int p = h & slotMask;
		while (true) {
			int e = slot[p];
			if (e < 0) return -1;
			if (compare(s, from, to, symbols.name[e]) == 0) return e;
			p = (p + 1) & slotMask;
		}
	}
	public byte classOf(int cp) {
		int i = find(spacing.cp, cp);
		return i < 0 ? SpacingTable.ORD : spacing.cls[i];
	}
	public int space(byte left, byte right, int mu) {
		return spacing.pair[left * 8 + right] * mu;
	}
	public void setClass(int cp, byte cls) {
		int i = find(spacing.cp, cp);
		if (i >= 0) {
			spacing.cls[i] = cls;
			return;
		}
		i = ~i;
		spacing.cp = insert(spacing.cp, i, cp);
		spacing.cls = insert(spacing.cls, i, cls);
	}
	public void setPair(byte left, byte right, int mu) {
		spacing.pair[left * 8 + right] = (byte) mu;
	}
	public int accentIndex(int id) {
		for (int i = 0; i < accents.id.length; i++) {
			if (accents.id[i] == id) return i;
		}
		return -1;
	}
	public int accentCp(int i) {
		return accents.cp[i];
	}
	public int accentBottom(int cp) {
		for (int i = 0; i < accents.cp.length; i++) {
			if (accents.cp[i] == cp) return accents.bottom[i];
		}
		return 0;
	}
	public void setAccent(int id, int cp, int bottom) {
		accents.id = insert(accents.id, accents.id.length, id);
		accents.cp = insert(accents.cp, accents.cp.length, cp);
		accents.bottom = insert(accents.bottom, accents.bottom.length, (short) bottom);
	}
	public int accentGap(int cp) {
		int i = find(accentsCompat.cp, cp);
		return i < 0 ? 0 : accentsCompat.gap[i];
	}
	public int accentWideGap(int cp) {
		int i = find(accentsCompat.wcp, cp);
		return i < 0 ? accentGap(cp) : accentsCompat.wgap[i];
	}
	public byte familyOf(int id) {
		switch (id) {
			case SymbolTable.ID_MATHBF: return FamilyTable.BOLD;
			case SymbolTable.ID_MATHIT: return FamilyTable.ITALIC;
			case SymbolTable.ID_MATHCAL: return FamilyTable.SCRIPT;
			case SymbolTable.ID_MATHFRAK: return FamilyTable.FRAKTUR;
			case SymbolTable.ID_MATHBB: return FamilyTable.BB;
			case SymbolTable.ID_MATHSF: return FamilyTable.SF;
			case SymbolTable.ID_MATHTT: return FamilyTable.TT;
		}
		return FamilyTable.MATH;
	}
	public int holeOf(int cp) {
		int i = find(families.holeCp, cp);
		return i < 0 ? -1 : families.holeTo[i];
	}
	public void setFamily(int fam, int up, int lo, int di) {
		families.up[fam] = up;
		families.lo[fam] = lo;
		families.di[fam] = di;
	}
	public void setHole(int cp, int to) {
		int i = find(families.holeCp, cp);
		if (i >= 0) {
			families.holeTo[i] = to;
			return;
		}
		i = ~i;
		families.holeCp = insert(families.holeCp, i, cp);
		families.holeTo = insert(families.holeTo, i, to);
	}
	public int delimBase(int cp) {
		int i = find(delims.cp, cp);
		return i < 0 ? 0 : delims.base[i];
	}
	public int delimStretch(int cp) {
		int i = find(delims.cp, cp);
		return i < 0 ? 0 : delims.stretch[i];
	}
	public int delimKern(int cp, boolean stretched) {
		int i = find(delims.cp, cp);
		return i < 0 ? 0 : stretched ? delims.stretch[i] : delims.base[i];
	}
	public void setDelim(int cp, int base, int stretch) {
		int i = find(delims.cp, cp);
		if (i >= 0) {
			delims.base[i] = base;
			delims.stretch[i] = stretch;
			return;
		}
		i = ~i;
		delims.cp = insert(delims.cp, i, cp);
		delims.base = insert(delims.base, i, base);
		delims.stretch = insert(delims.stretch, i, stretch);
	}
	public int parseColor(CharSequence s, int from, int to) {
		while (from < to && s.charAt(from) <= ' ') from++;
		while (to > from && s.charAt(to - 1) <= ' ') to--;
		if (from >= to) return ColorTable.NONE;
		if (s.charAt(from) == '#') return hex(s, from + 1, to);
		for (int i = 0; i < colors.names.length; i++) {
			if (match(s, from, to, colors.names[i])) return colors.values[i];
		}
		return ColorTable.NONE;
	}
	public void setColor(String name, int argb) {
		for (int i = 0; i < colors.names.length; i++) {
			if (colors.names[i].equals(name)) {
				colors.values[i] = argb;
				return;
			}
		}
		colors.names = java.util.Arrays.copyOf(colors.names, colors.names.length + 1);
		colors.values = java.util.Arrays.copyOf(colors.values, colors.values.length + 1);
		colors.names[colors.names.length - 1] = name;
		colors.values[colors.values.length - 1] = argb;
	}
	private static int find(int[] a, int cp) {
		int lo = 0;
		int hi = a.length - 1;
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			int v = a[mid];
			if (v == cp) return mid;
			if (v < cp) lo = mid + 1; else hi = mid - 1;
		}
		return ~lo;
	}
	private static int[] insert(int[] a, int at, int v) {
		int[] n = new int[a.length + 1];
		System.arraycopy(a, 0, n, 0, at);
		n[at] = v;
		System.arraycopy(a, at, n, at + 1, a.length - at);
		return n;
	}
	private static byte[] insert(byte[] a, int at, byte v) {
		byte[] n = new byte[a.length + 1];
		System.arraycopy(a, 0, n, 0, at);
		n[at] = v;
		System.arraycopy(a, at, n, at + 1, a.length - at);
		return n;
	}
	private static short[] insert(short[] a, int at, short v) {
		short[] n = new short[a.length + 1];
		System.arraycopy(a, 0, n, 0, at);
		n[at] = v;
		System.arraycopy(a, at, n, at + 1, a.length - at);
		return n;
	}
	private static int hash(String s) {
		int h = 0x811C9DC5;
		for (int i = 0; i < s.length(); i++) h = (h ^ s.charAt(i)) * 0x01000193;
		return h;
	}
	private static int compare(CharSequence s, int from, int to, String name) {
		int n = to - from;
		int m = name.length();
		int k = n < m ? n : m;
		for (int i = 0; i < k; i++) {
			int d = s.charAt(from + i) - name.charAt(i);
			if (d != 0) return d;
		}
		return n - m;
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
		return ColorTable.NONE;
	}
	private static int digit(char c) {
		if (c >= '0' && c <= '9') return c - '0';
		if (c >= 'a' && c <= 'f') return c - 'a' + 10;
		if (c >= 'A' && c <= 'F') return c - 'A' + 10;
		return -1;
	}
}
