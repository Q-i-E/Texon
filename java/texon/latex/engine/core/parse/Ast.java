package texon.latex.engine.core.parse;
public final class Ast {
	public static final byte GLYPH = 0;
	public static final byte FRAC = 1;
	public static final byte SQRT = 2;
	public static final byte SCRIPT = 3;
	public static final byte SEQ = 4;
	public static final byte TEXT = 5;
	public static final byte OVER = 6;
	public static final byte UNDER = 7;
	public static final byte SPACE = 8;
	public static final byte LEFT = 9;
	public static final byte ACCENT = 10;
	public static final byte CLASS = 11;
	public static final byte DELIM = 12;
	public static final byte FAMILY = 13;
	public static final byte OVERBRACE = 14;
	public static final byte UNDERBRACE = 15;
	public static final byte STACK = 16;
	public static final byte PHANTOM = 17;
	public static final byte NOT = 18;
	public static final byte STYLE = 19;
	public static final byte ENV = 20;
	public static final byte ROW = 21;
	public static final byte CELL = 22;
	public static final byte ERROR = 23;
	public static final byte XARROW = 24;
	public static final byte CLAP = 25;
	public static final byte GENFRAC = 26;
	public static final byte SMASH = 27;
	public static final byte BOXED = 28;
	public static final byte MIDDLE = 29;
	public static final byte CANCEL = 30;
	public static final byte RULE = 31;
	public static final byte RAISE = 32;
	public static final byte DIMSPACE = 33;
	public static final byte XACCENT = 34;
	public static final byte UACCENT = 35;
	public static final byte WACCENT = 36;
	public static final byte ATTR = 37;
	public static final byte TAG = 38;
	public static final byte COLORDECL = 39;
	public static final byte MATHCHOICE = 40;
	public static final byte ACCENTSET = 41;
	public static final byte UNDERACCENT = 42;
	public static final byte PRESCRIPT = 43;
	public static final byte SMASHOP = 44;
	public static final byte SIDESET = 45;
	public static final int S_DISPLAY = 0;
	public static final int S_TEXT = 1;
	public static final int S_SCRIPT = 2;
	public static final int S_SCRIPTSCRIPT = 3;
	public static final int E_MATRIX = 0;
	public static final int E_PMATRIX = 1;
	public static final int E_BMATRIX = 2;
	public static final int E_BMATRIX_B = 3;
	public static final int E_VMATRIX = 4;
	public static final int E_VMATRIX_B = 5;
	public static final int E_SMALLMATRIX = 6;
	public static final int E_MATRIX_S = 7;
	public static final int E_PMATRIX_S = 8;
	public static final int E_BMATRIX_S = 9;
	public static final int E_ARRAY = 10;
	public static final int E_CASES = 11;
	public static final int E_SUBARRAY = 12;
	public static final int E_ALIGN = 13;
	public static final int E_ALIGN_S = 14;
	public static final int E_ALIGNED = 15;
	public static final int E_ALIGNAT = 16;
	public static final int E_ALIGNAT_S = 17;
	public static final int E_ALIGNEDAT = 18;
	public static final int E_FLALIGN = 19;
	public static final int E_FLALIGN_S = 20;
	public static final int E_GATHER = 21;
	public static final int E_GATHER_S = 22;
	public static final int E_GATHERED = 23;
	public static final int E_SPLIT = 24;
	public static final int COLS_ALIGN = -1;
	public static final int COLS_GATHER = -2;
	public static final int NONE = -1;
	public static final int LARGE_OP = 1;
	public static final int LARGE_OP_NL = 2;
	public static final int ERR_UNKNOWN = 0;
	public static final int ERR_STRAY = 1;
	public static final int ERR_ENV = 2;
	public static final int ERR_ARG = 3;
	public static final int ERR_DANGLING = 4;
	public static final int INITIAL = 256;
	private static final int KEEP = 1 << 16;
	public byte[] kind = new byte[INITIAL];
	public int[] a = new int[INITIAL];
	public int[] b = new int[INITIAL];
	public int[] c = new int[INITIAL];
	public int[] d = new int[INITIAL];
	public int[] e = new int[INITIAL];
	public int[] next = new int[INITIAL];
	public int[] at = new int[INITIAL];
	public String[] str = new String[8];
	public int strCount;
	public int size;
	public int root = NONE;
	public int add(byte k, int va, int vb, int vc, int pos) {
		if (size == kind.length) grow();
		int i = size++;
		kind[i] = k;
		a[i] = va;
		b[i] = vb;
		c[i] = vc;
		d[i] = NONE;
		e[i] = NONE;
		next[i] = NONE;
		at[i] = pos;
		return i;
	}
	public int add6(byte k, int va, int vb, int vc, int vd, int ve, int pos) {
		if (size == kind.length) grow();
		int i = size++;
		kind[i] = k;
		a[i] = va;
		b[i] = vb;
		c[i] = vc;
		d[i] = vd;
		e[i] = ve;
		next[i] = NONE;
		at[i] = pos;
		return i;
	}
	private void grow() {
		int cap = kind.length << 1;
		kind = java.util.Arrays.copyOf(kind, cap);
		a = java.util.Arrays.copyOf(a, cap);
		b = java.util.Arrays.copyOf(b, cap);
		c = java.util.Arrays.copyOf(c, cap);
		d = java.util.Arrays.copyOf(d, cap);
		e = java.util.Arrays.copyOf(e, cap);
		next = java.util.Arrays.copyOf(next, cap);
		at = java.util.Arrays.copyOf(at, cap);
	}
	public int glyph(int cp, int pos) {
		return add(GLYPH, cp, NONE, NONE, pos);
	}
	public int largeOp(int cp, int pos) {
		return add(GLYPH, cp, LARGE_OP, NONE, pos);
	}
	public int largeOpNl(int cp, int pos) {
		return add(GLYPH, cp, LARGE_OP_NL, NONE, pos);
	}
	public int space(int mu, int pos) {
		return add(SPACE, mu, NONE, NONE, pos);
	}
	public int seq(int first) {
		return add(SEQ, first, NONE, NONE, NONE);
	}
	public int style(int content, int code, int pos) {
		return add(STYLE, content, code, NONE, pos);
	}
	public int error(int pos) {
		return add(ERROR, NONE, NONE, NONE, pos);
	}
	public int error(int pos, int len, int code) {
		return add(ERROR, NONE, code, len, pos);
	}
	public int env(int code, int rows, int spec, int pos) {
		return add(ENV, code, rows, spec, pos);
	}
	public int row(int cells) {
		return add(ROW, cells, NONE, NONE, NONE);
	}
	public int cell(int content) {
		return add(CELL, content, NONE, NONE, NONE);
	}
	public int xarrow(int cp, int above, int below, int pos) {
		return add(XARROW, cp, above, below, pos);
	}
	public int clap(int content, int mode, int pos) {
		return add(CLAP, content, mode, NONE, pos);
	}
	public int smash(int content, int pos) {
		return add(SMASH, content, NONE, NONE, pos);
	}
	public int boxed(int content, int pos) {
		return add(BOXED, content, NONE, NONE, pos);
	}
	public int genfrac(int num, int den, int style, int left, int right, int pos) {
		return add6(GENFRAC, num, den, style, left, right, pos);
	}
	public int middle(int cp, int pos) {
		return add(MIDDLE, cp, NONE, NONE, pos);
	}
	public int cancel(int content, int mode, int value, int pos) {
		return add(CANCEL, content, mode, value, pos);
	}
	public int rule(int w, int h, int raise, int pos) {
		return add6(RULE, w, h, raise, NONE, NONE, pos);
	}
	public int raise(int content, int amount, int pos) {
		return add(RAISE, content, amount, NONE, pos);
	}
	public int attr(int content, int fg, int bg, int frame, int pad, int pos) {
		return add6(ATTR, content, fg, bg, frame, pad, pos);
	}
	public int tag(int content, int cls, int id, int pos) {
		return add(TAG, content, cls, id, pos);
	}
	public int colorDecl(int content, int fg, int pos) {
		return add(COLORDECL, content, fg, NONE, pos);
	}
	public int mathchoice(int dc, int tc, int sc, int ssc, int pos) {
		return add6(MATHCHOICE, dc, tc, sc, ssc, NONE, pos);
	}
	public int accentSet(int base, int accent, int pos) {
		return add(ACCENTSET, base, accent, NONE, pos);
	}
	public int underAccent(int base, int accent, int pos) {
		return add(UNDERACCENT, base, accent, NONE, pos);
	}
	public int prescript(int sup, int sub, int base, int pos) {
		return add(PRESCRIPT, sup, sub, base, pos);
	}
	public int smashOp(int content, int pos) {
		return add(SMASHOP, content, NONE, NONE, pos);
	}
	public int sideset(int lsub, int lsup, int rsub, int rsup, int op, int pos) {
		return add6(SIDESET, lsub, lsup, rsub, rsup, op, pos);
	}
	public int str(String s) {
		if (strCount == str.length) str = java.util.Arrays.copyOf(str, str.length << 1);
		str[strCount] = s;
		return strCount++;
	}
	public void clear() {
		size = 0;
		strCount = 0;
		root = NONE;
		if (kind.length > KEEP) {
			kind = new byte[INITIAL];
			a = new int[INITIAL];
			b = new int[INITIAL];
			c = new int[INITIAL];
			d = new int[INITIAL];
			e = new int[INITIAL];
			next = new int[INITIAL];
			at = new int[INITIAL];
		}
	}
}