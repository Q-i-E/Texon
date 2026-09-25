package texon.latex.engine.core.lex;
public final class Lexer {
	public static final byte CHAR = 0;
	public static final byte SYMBOL = 1;
	public static final byte COMMAND = 2;
	public static final byte UNKNOWN = 3;
	public static final byte OPEN = 4;
	public static final byte CLOSE = 5;
	public static final byte SUP = 6;
	public static final byte SUB = 7;
	public static final byte PRIME = 8;
	public static final byte EOF = 9;
	public static final byte OP = 10;
	public static final byte OPNL = 11;
	public static final byte ALIGN = 12;
	public static final byte ROW = 13;
	public static final int INITIAL = 256;
	private static final int KEEP = 1 << 16;
	public byte[] kind = new byte[INITIAL];
	public int[] val = new int[INITIAL];
	public int[] at = new int[INITIAL];
	public int[] end = new int[INITIAL];
	public int size;
	private CharSequence src;
	public void tokenize(CharSequence s) {
		src = s;
		size = 0;
		if (kind.length > KEEP) reset();
		int n = s.length();
		ensure(n + 16);
		int i = 0;
		while (i < n) {
			char c = s.charAt(i);
			if (c == '\\') {
				i = control(s, i, n);
			} else if (c == '{') {
				put(OPEN, 0, i, i + 1);
				i++;
			} else if (c == '}') {
				put(CLOSE, 0, i, i + 1);
				i++;
			} else if (c == '^') {
				put(SUP, 0, i, i + 1);
				i++;
			} else if (c == '_') {
				put(SUB, 0, i, i + 1);
				i++;
			} else if (c == '\'') {
				put(PRIME, 0, i, i + 1);
				i++;
			} else if (c == '&') {
				put(ALIGN, 0, i, i + 1);
				i++;
			} else if (c <= ' ') {
				i++;
			} else if (c < 0x80) {
				put(CHAR, c, i, i + 1);
				i++;
			} else {
				int cp = Character.codePointAt(s, i);
				int w = Character.charCount(cp);
				put(CHAR, cp, i, i + w);
				i += w;
			}
		}
		put(EOF, 0, n, n);
		for (int k = 1; k < 16; k++) {
			kind[size + k] = EOF;
			val[size + k] = 0;
			at[size + k] = n;
			end[size + k] = n;
		}
	}
	public CharSequence source() {
		return src;
	}
	private int control(CharSequence s, int i, int n) {
		int start = i;
		int j = i + 1;
		if (j >= n) {
			put(CHAR, '\\', start, j);
			return j;
		}
		if (s.charAt(j) == '\\') {
			put(ROW, 0, start, j + 1);
			return j + 1;
		}
		char c = s.charAt(j);
		if (isLetter(c)) {
			while (j < n && isLetter(s.charAt(j))) j++;
			int idx = SymbolTable.indexOf(s, start + 1, j);
			if (idx < 0) {
				put(UNKNOWN, 0, start, j);
			} else {
				byte k = SymbolTable.KIND[idx];
				if (k == SymbolTable.COMMAND) put(COMMAND, SymbolTable.VALUE[idx], start, j);
				else if (k == SymbolTable.OP) put(OP, SymbolTable.VALUE[idx], start, j);
				else if (k == SymbolTable.OPNL) put(OPNL, SymbolTable.VALUE[idx], start, j);
				else put(SYMBOL, SymbolTable.VALUE[idx], start, j);
			}
			return j;
		}
		int cp = Character.codePointAt(s, j);
		int w = Character.charCount(cp);
		int idx = SymbolTable.indexOf(s, j, j + w);
		if (idx < 0) {
			put(CHAR, cp, start, j + w);
		} else {
			byte k = SymbolTable.KIND[idx];
			if (k == SymbolTable.COMMAND) put(COMMAND, SymbolTable.VALUE[idx], start, j + w);
			else if (k == SymbolTable.OP) put(OP, SymbolTable.VALUE[idx], start, j + w);
			else if (k == SymbolTable.OPNL) put(OPNL, SymbolTable.VALUE[idx], start, j + w);
			else put(SYMBOL, SymbolTable.VALUE[idx], start, j + w);
		}
		return j + w;
	}
	private void put(byte k, int v, int from, int to) {
		kind[size] = k;
		val[size] = v;
		at[size] = from;
		end[size] = to;
		size++;
	}
	private void ensure(int need) {
		if (kind.length >= need) return;
		int cap = kind.length;
		while (cap < need) cap <<= 1;
		kind = java.util.Arrays.copyOf(kind, cap);
		val = java.util.Arrays.copyOf(val, cap);
		at = java.util.Arrays.copyOf(at, cap);
		end = java.util.Arrays.copyOf(end, cap);
	}
	private void reset() {
		kind = new byte[INITIAL];
		val = new int[INITIAL];
		at = new int[INITIAL];
		end = new int[INITIAL];
	}
	private static boolean isLetter(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}
}