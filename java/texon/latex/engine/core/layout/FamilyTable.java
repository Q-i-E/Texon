package texon.latex.engine.core.layout;
import texon.latex.engine.core.lex.*;
public final class FamilyTable {
	public static final byte MATH = 0;
	public static final byte BOLD = 1;
	public static final byte ITALIC = 2;
	public static final byte SCRIPT = 3;
	public static final byte FRAKTUR = 4;
	public static final byte BB = 5;
	public static final byte SF = 6;
	public static final byte TT = 7;
	public static final int COUNT = 8;
	static int[] UP = new int[COUNT];
	static int[] LO = new int[COUNT];
	static int[] DI = new int[COUNT];
	private static int[] HOLE_CP = new int[0];
	private static int[] HOLE_TO = new int[0];
	public static void install(int[] up, int[] lo, int[] di, int[] holeCp, int[] holeTo) {
		UP = up;
		LO = lo;
		DI = di;
		HOLE_CP = holeCp;
		HOLE_TO = holeTo;
	}
	static int hole(int cp) {
		int lo = 0;
		int hi = HOLE_CP.length - 1;
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			int v = HOLE_CP[mid];
			if (v == cp) return HOLE_TO[mid];
			if (v < cp) lo = mid + 1; else hi = mid - 1;
		}
		return -1;
	}
	public static byte of(int id) {
		switch (id) {
			case SymbolTable.ID_MATHBF: return BOLD;
			case SymbolTable.ID_MATHIT: return ITALIC;
			case SymbolTable.ID_MATHCAL: return SCRIPT;
			case SymbolTable.ID_MATHFRAK: return FRAKTUR;
			case SymbolTable.ID_MATHBB: return BB;
			case SymbolTable.ID_MATHSF: return SF;
			case SymbolTable.ID_MATHTT: return TT;
		}
		return MATH;
	}
}
