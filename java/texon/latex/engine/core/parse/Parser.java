package texon.latex.engine.core.parse;
import texon.latex.engine.core.*;
import texon.latex.engine.core.lex.*;
public final class Parser {
	private static final int NO_LAST = -1;
	private static final int STOP_RIGHT = 1;
	private static final int STOP_ALIGN = 2;
	private static final int STOP_ROW = 4;
	private static final int STOP_END = 8;
	private final Lexer lex;
	private final Ast ast;
	private int pos;
	private int stop;
	private int specSub;
	private int specSup;
	public Parser(Lexer lex, Ast ast) {
		this.lex = lex;
		this.ast = ast;
	}
	public int parse(CharSequence src) {
		lex.tokenize(src);
		ast.clear();
		pos = 0;
		stop = 0;
		ast.root = parseSeq();
		return ast.root;
	}
	private int seq(int first) {
		return ast.add(Ast.SEQ, first, Ast.NONE, Ast.NONE, Ast.NONE);
	}
	private int parseSeq() {
		int head = seq(NO_LAST);
		int last = NO_LAST;
		int prev = NO_LAST;
		while (true) {
			byte k = lex.kind[pos];
			if (k == Lexer.EOF || k == Lexer.CLOSE) break;
			if ((stop & STOP_RIGHT) != 0 && k == Lexer.COMMAND && lex.val[pos] == SymbolTable.ID_RIGHT) break;
			if ((stop & STOP_ALIGN) != 0 && k == Lexer.ALIGN) break;
			if ((stop & STOP_ROW) != 0 && k == Lexer.ROW) break;
			if ((stop & STOP_END) != 0 && k == Lexer.COMMAND && lex.val[pos] == SymbolTable.ID_END) break;
			if (k == Lexer.SUP || k == Lexer.SUB) {
				int s = attachScript(head, last, prev, parseScriptArg(), k == Lexer.SUP);
				if (s != Ast.NONE) last = s;
				continue;
			}
			if (k == Lexer.ALIGN || k == Lexer.ROW) {
				prev = last;
				last = append(head, last, ast.error(lex.at[pos], 1, Ast.ERR_STRAY));
				pos++;
				continue;
			}
			if (k == Lexer.COMMAND) {
				int id = lex.val[pos];
				if (id == SymbolTable.ID_OVER || id == SymbolTable.ID_ATOP || id == SymbolTable.ID_ABOVE || id == SymbolTable.ID_CHOOSE || id == SymbolTable.ID_BRACE || id == SymbolTable.ID_BRACK) {
					int num = ast.a[head] == NO_LAST ? Ast.NONE : head;
					int at = lex.at[pos];
					pos++;
					return ast.add(Ast.FRAC, num, parseSeq(), id, at);
				}
				if (id == SymbolTable.ID_OVERWITHDELIMS || id == SymbolTable.ID_ATOPWITHDELIMS || id == SymbolTable.ID_ABOVEWITHDELIMS) {
					int num = ast.a[head] == NO_LAST ? Ast.NONE : head;
					int at = lex.at[pos];
					pos++;
					int left = readDelim();
					int right = readDelim();
					int base = id == SymbolTable.ID_OVERWITHDELIMS ? SymbolTable.ID_OVER : id == SymbolTable.ID_ATOPWITHDELIMS ? SymbolTable.ID_ATOP : SymbolTable.ID_ABOVE;
					return ast.add6(Ast.FRAC, num, parseSeq(), base, left, right, at);
				}
				if (id == SymbolTable.ID_LIMITS || id == SymbolTable.ID_NOLIMITS || id == SymbolTable.ID_DISPLAYLIMITS) {
					pos++;
					setLimits(last, id == SymbolTable.ID_NOLIMITS);
					continue;
				}
				if (id == SymbolTable.ID_DISPLAYSTYLE || id == SymbolTable.ID_TEXTSTYLE || id == SymbolTable.ID_SCRIPTSTYLE || id == SymbolTable.ID_SCRIPTSCRIPTSTYLE) {
					int at = lex.at[pos];
					pos++;
					append(head, last, ast.style(parseSeq(), styleCode(id), at));
					break;
				}
				if (id == SymbolTable.ID_COLOR) {
					int at = lex.at[pos];
					pos++;
					int fg = readColorArg();
					append(head, last, ast.colorDecl(parseSeq(), fg, at));
					break;
				}
				if (id == SymbolTable.ID_END) {
					prev = last;
					last = append(head, last, ast.error(lex.at[pos], 1, Ast.ERR_STRAY));
					pos++;
					skipGroup();
					continue;
				}
			}
			int node = parseAtom();
			if (node == Ast.NONE) continue;
			prev = last;
			last = append(head, last, node);
		}
		if (ast.a[head] == NO_LAST) return Ast.NONE;
		return head;
	}
	private int append(int head, int last, int node) {
		if (last == NO_LAST) ast.a[head] = node;
		else ast.next[last] = node;
		return node;
	}
	private void setLimits(int last, boolean nolimits) {
		if (last == NO_LAST) return;
		byte lk = ast.kind[last];
		if ((lk == Ast.GLYPH || lk == Ast.TEXT) && (ast.b[last] == Ast.LARGE_OP || ast.b[last] == Ast.LARGE_OP_NL)) {
			ast.b[last] = nolimits ? Ast.LARGE_OP_NL : Ast.LARGE_OP;
		}
	}
	private static int styleCode(int id) {
		if (id == SymbolTable.ID_DISPLAYSTYLE) return Ast.S_DISPLAY;
		if (id == SymbolTable.ID_TEXTSTYLE) return Ast.S_TEXT;
		if (id == SymbolTable.ID_SCRIPTSTYLE) return Ast.S_SCRIPT;
		return Ast.S_SCRIPTSCRIPT;
	}
	private int attachScript(int head, int last, int prev, int arg, boolean sup) {
		if (last == NO_LAST) return arg;
		if (ast.kind[last] == Ast.SCRIPT) {
			if (sup) {
				if (ast.b[last] == Ast.NONE) ast.b[last] = arg;
			} else {
				if (ast.c[last] == Ast.NONE) ast.c[last] = arg;
			}
			return last;
		}
		int s = ast.add(Ast.SCRIPT, last, sup ? arg : Ast.NONE, sup ? Ast.NONE : arg, arg >= 0 ? ast.at[arg] : ast.at[last]);
		if (prev == NO_LAST) {
			ast.a[head] = s;
		} else {
			ast.next[prev] = s;
		}
		return s;
	}
	private int parseAtom() {
		byte k = lex.kind[pos];
		switch (k) {
			case Lexer.CHAR: {
				int node = ast.glyph(lex.val[pos], lex.at[pos]);
				pos++;
				return node;
			}
			case Lexer.SYMBOL: {
				int cp = lex.val[pos];
				int at = lex.at[pos];
				if (isAccentMark(cp) && isBaseStart(lex.kind[pos + 1])) {
					pos++;
					int base = parseArg();
					if (base != Ast.NONE) return ast.add(Ast.XACCENT, base, cp, Ast.NONE, at);
					return ast.glyph(cp, at);
				}
				pos++;
				return ast.glyph(cp, at);
			}
			case Lexer.OP: {
				int node = ast.largeOp(lex.val[pos], lex.at[pos]);
				pos++;
				return node;
			}
			case Lexer.OPNL: {
				int node = ast.largeOpNl(lex.val[pos], lex.at[pos]);
				pos++;
				return node;
			}
			case Lexer.OPEN:
				pos++;
				return parseGroup();
			case Lexer.PRIME: {
				pos++;
				return ast.glyph(0x2032, lex.at[pos - 1]);
			}
			case Lexer.COMMAND:
				return parseCommand();
			case Lexer.UNKNOWN: {
				int i = pos++;
				return ast.error(lex.at[i], lex.end[i] - lex.at[i], Ast.ERR_UNKNOWN);
			}
			case Lexer.SUP:
			case Lexer.SUB:
				pos++;
				return Ast.NONE;
			default:
				pos++;
				return Ast.NONE;
		}
	}
	private int parseGroup() {
		int inner = parseSeq();
		if (lex.kind[pos] == Lexer.CLOSE) pos++;
		return inner;
	}
	private int parseScriptArg() {
		pos++;
		if (lex.kind[pos] == Lexer.OPEN) {
			pos++;
			return parseGroup();
		}
		if (lex.kind[pos] == Lexer.EOF || lex.kind[pos] == Lexer.CLOSE) return Ast.NONE;
		return parseAtom();
	}
	private void parseScriptSpec() {
		specSub = Ast.NONE;
		specSup = Ast.NONE;
		if (lex.kind[pos] != Lexer.OPEN) return;
		pos++;
		while (lex.kind[pos] != Lexer.CLOSE && lex.kind[pos] != Lexer.EOF) {
			byte k = lex.kind[pos];
			if (k == Lexer.SUB) {
				pos++;
				specSub = parseSpecArg();
			} else if (k == Lexer.SUP) {
				pos++;
				specSup = parseSpecArg();
			} else if (k == Lexer.PRIME) {
				pos++;
				if (specSup == Ast.NONE) specSup = ast.glyph(0x2032, lex.at[pos - 1]);
			} else {
				pos++;
			}
		}
		if (lex.kind[pos] == Lexer.CLOSE) pos++;
	}
	private int parseSpecArg() {
		if (lex.kind[pos] == Lexer.OPEN) {
			pos++;
			return parseGroup();
		}
		if (lex.kind[pos] == Lexer.EOF || lex.kind[pos] == Lexer.CLOSE) return Ast.NONE;
		return parseAtom();
	}
	private int parseCommand() {
		int id = lex.val[pos];
		int start = lex.at[pos];
		pos++;
		switch (id) {
			case SymbolTable.ID_FRAC:
				return binary(Ast.FRAC, start);
			case SymbolTable.ID_BINOM:
			case SymbolTable.ID_CHOOSE: {
				int left = parseArg();
				int right = parseArg();
				return ast.add(Ast.FRAC, left, right, SymbolTable.ID_BINOM, start);
			}
			case SymbolTable.ID_OVER:
				return binary(Ast.FRAC, start);
			case SymbolTable.ID_DISPLAYSTYLE:
			case SymbolTable.ID_TEXTSTYLE:
				return Ast.NONE;
			case SymbolTable.ID_BEGIN:
				return parseEnv(start);
			case SymbolTable.ID_END:
				return ast.error(start, 4, Ast.ERR_STRAY);
			case SymbolTable.ID_SQRT:
				return sqrt(start);
			case SymbolTable.ID_TEXT:
			case SymbolTable.ID_MATHRM:
				return textArg(start, id);
			case SymbolTable.ID_OVERLINE:
				return ast.add(Ast.OVER, parseArg(), Ast.NONE, Ast.NONE, start);
			case SymbolTable.ID_UNDERLINE:
				return ast.add(Ast.UNDER, parseArg(), Ast.NONE, Ast.NONE, start);
			case SymbolTable.ID_OVERBRACE: {
				int arg = parseArg();
				if (arg == Ast.NONE) return Ast.NONE;
				return ast.add(Ast.OVERBRACE, arg, Ast.LARGE_OP, Ast.NONE, start);
			}
			case SymbolTable.ID_UNDERBRACE: {
				int arg = parseArg();
				if (arg == Ast.NONE) return Ast.NONE;
				return ast.add(Ast.UNDERBRACE, arg, Ast.LARGE_OP, Ast.NONE, start);
			}
			case SymbolTable.ID_LEFT:
				return delimited(start);
			case SymbolTable.ID_RIGHT:
				return Ast.NONE;
			case SymbolTable.ID_LIM:
			case SymbolTable.ID_MAX:
			case SymbolTable.ID_MIN:
			case SymbolTable.ID_SUP:
			case SymbolTable.ID_INF:
			case SymbolTable.ID_DEG:
				return opName(start + 1, lex.end[pos - 1], start, true);
			case SymbolTable.ID_SIN:
			case SymbolTable.ID_COS:
			case SymbolTable.ID_TAN:
			case SymbolTable.ID_LOG:
			case SymbolTable.ID_LN:
			case SymbolTable.ID_EXP:
			case SymbolTable.ID_SINH:
			case SymbolTable.ID_COSH:
			case SymbolTable.ID_TANH:
			case SymbolTable.ID_COT:
			case SymbolTable.ID_SEC:
			case SymbolTable.ID_CSC:
			case SymbolTable.ID_ARCSIN:
			case SymbolTable.ID_ARCCOS:
			case SymbolTable.ID_ARCTAN:
			case SymbolTable.ID_ARG:
			case SymbolTable.ID_KER:
			case SymbolTable.ID_HOM:
			case SymbolTable.ID_DIM:
			case SymbolTable.ID_GCD:
			case SymbolTable.ID_PR:
			case SymbolTable.ID_DET:
			case SymbolTable.ID_LG:
				return opName(start + 1, lex.end[pos - 1], start, false);
			case SymbolTable.ID_OPERATORNAME:
				return opNameArg(start);
			case SymbolTable.ID_HAT:
			case SymbolTable.ID_TILDE:
			case SymbolTable.ID_BAR:
			case SymbolTable.ID_DOT:
			case SymbolTable.ID_DDOT:
			case SymbolTable.ID_VEC:
			case SymbolTable.ID_OVERLEFTARROW:
			case SymbolTable.ID_OVERRIGHTARROW:
			case SymbolTable.ID_WIDEHAT:
			case SymbolTable.ID_WIDETILDE: {
				int arg = parseArg();
				if (arg == Ast.NONE) return Ast.NONE;
				return ast.add(Ast.ACCENT, arg, id, Ast.NONE, start);
			}
			case SymbolTable.ID_OVERSET:
			case SymbolTable.ID_UNDERSET: {
				int top = parseArg();
				int base = parseArg();
				if (base == Ast.NONE) return Ast.NONE;
				if (top == Ast.NONE) return base;
				return ast.add(Ast.STACK, base, top, Ast.NONE, start);
			}
			case SymbolTable.ID_STACKREL: {
				int top = parseArg();
				int base = parseArg();
				if (base == Ast.NONE) return Ast.NONE;
				if (top == Ast.NONE) return base;
				return ast.add(Ast.STACK, base, top, SymbolTable.ID_MATHREL, start);
			}
			case SymbolTable.ID_MATHSTRUT:
				return ast.add(Ast.PHANTOM, Ast.NONE, 3, Ast.NONE, start);
			case SymbolTable.ID_NOT: {
				int arg = parseAtom();
				if (arg == Ast.NONE) return Ast.NONE;
				return ast.add(Ast.NOT, arg, Ast.NONE, Ast.NONE, start);
			}
			case SymbolTable.ID_PHANTOM:
			case SymbolTable.ID_HPHANTOM:
			case SymbolTable.ID_VPHANTOM: {
				int arg = parseArg();
				if (arg == Ast.NONE) return Ast.NONE;
				int mode = id == SymbolTable.ID_HPHANTOM ? 1 : id == SymbolTable.ID_VPHANTOM ? 2 : 0;
				return ast.add(Ast.PHANTOM, arg, mode, Ast.NONE, start);
			}
			case SymbolTable.ID_BANG:
				return ast.space(-3, start);
			case SymbolTable.ID_COMMA:
				return ast.space(3, start);
			case SymbolTable.ID_GT:
				return ast.space(4, start);
			case SymbolTable.ID_MATHOP:
			case SymbolTable.ID_MATHREL:
			case SymbolTable.ID_MATHBIN:
			case SymbolTable.ID_MATHORD: {
				int arg = parseArg();
				if (arg == Ast.NONE) return Ast.NONE;
				return ast.add(Ast.CLASS, arg, id, Ast.NONE, start);
			}
			case SymbolTable.ID_MATHBF:
			case SymbolTable.ID_MATHIT:
			case SymbolTable.ID_MATHCAL:
			case SymbolTable.ID_MATHFRAK:
			case SymbolTable.ID_MATHBB:
			case SymbolTable.ID_MATHSF:
			case SymbolTable.ID_MATHTT: {
				int arg = parseArg();
				if (arg == Ast.NONE) return Ast.NONE;
				return ast.add(Ast.FAMILY, arg, id, Ast.NONE, start);
			}
			case SymbolTable.ID_BIG:
			case SymbolTable.ID_BIG_18:
			case SymbolTable.ID_BIGG:
			case SymbolTable.ID_BIGG_20: {
				int cp = readDelim();
				if (cp == Ast.NONE) return Ast.NONE;
				return ast.add(Ast.DELIM, cp, id, Ast.NONE, start);
			}
			case SymbolTable.ID_COLON:
				return ast.space(4, start);
			case SymbolTable.ID_SEMI:
				return ast.space(5, start);
			case SymbolTable.ID_QUAD:
				return ast.space(18, start);
			case SymbolTable.ID_QQUAD:
				return ast.space(36, start);
			case SymbolTable.ID_DFRAC:
			case SymbolTable.ID_TFRAC:
			case SymbolTable.ID_CFRAC:
			case SymbolTable.ID_DBINOM:
			case SymbolTable.ID_TBINOM: {
				int a = parseArg();
				int b = parseArg();
				return ast.add(Ast.FRAC, a, b, id, start);
			}
			case SymbolTable.ID_GENFRAC:
				return genfrac(start);
			case SymbolTable.ID_BOXED: {
				int a = parseArg();
				if (a == Ast.NONE) return Ast.NONE;
				return ast.boxed(a, start);
			}
			case SymbolTable.ID_XRIGHTARROW:
				return xarrow(0x2192, start);
			case SymbolTable.ID_XLEFTARROW:
				return xarrow(0x2190, start);
			case SymbolTable.ID_MATHCLAP:
			case SymbolTable.ID_MATHLLAP:
			case SymbolTable.ID_MATHRLAP: {
				int a = parseArg();
				if (a == Ast.NONE) return Ast.NONE;
				int mode = id == SymbolTable.ID_MATHLLAP ? 1 : id == SymbolTable.ID_MATHRLAP ? 2 : 0;
				return ast.clap(a, mode, start);
			}
			case SymbolTable.ID_SUBSTACK:
				return substack(start);
			case SymbolTable.ID_BMOD:
				return ast.add(Ast.CLASS, word("mod", start), SymbolTable.ID_MATHBIN, Ast.NONE, start);
			case SymbolTable.ID_PMOD:
			case SymbolTable.ID_MOD:
				return pmod(start, id == SymbolTable.ID_PMOD);
			case SymbolTable.ID_MBOX:
			case SymbolTable.ID_TEXTNORMAL:
			case SymbolTable.ID_TEXTRM:
			case SymbolTable.ID_TEXTBF:
			case SymbolTable.ID_TEXTIT:
			case SymbolTable.ID_TEXTSF:
			case SymbolTable.ID_TEXTTT:
				return textArg(start, id);
			case SymbolTable.ID_ENSPACE:
				return ast.space(9, start);
			case SymbolTable.ID_THINSPACE:
				return ast.space(3, start);
			case SymbolTable.ID_MEDSPACE:
				return ast.space(4, start);
			case SymbolTable.ID_THICKSPACE:
				return ast.space(5, start);
			case SymbolTable.ID_NEGTHINSPACE:
				return ast.space(-3, start);
			case SymbolTable.ID_NEGMEDSPACE:
				return ast.space(-4, start);
			case SymbolTable.ID_NEGTHICKSPACE:
				return ast.space(-5, start);
			case SymbolTable.ID_SMASH: {
				int a = parseArg();
				if (a == Ast.NONE) return Ast.NONE;
				return ast.smash(a, start);
			}
			case SymbolTable.ID_MIDDLE:
				return ast.middle(readDelim(), start);
			case SymbolTable.ID_CANCEL:
			case SymbolTable.ID_BCANCEL:
			case SymbolTable.ID_XCANCEL: {
				int a = parseArg();
				if (a == Ast.NONE) return Ast.NONE;
				int mode = id == SymbolTable.ID_BCANCEL ? 1 : id == SymbolTable.ID_XCANCEL ? 2 : 0;
				return ast.cancel(a, mode, Ast.NONE, start);
			}
			case SymbolTable.ID_CANCELTO: {
				int value = parseArg();
				int a = parseArg();
				if (a == Ast.NONE) return Ast.NONE;
				return ast.cancel(a, 3, value, start);
			}
			case SymbolTable.ID_KERN:
			case SymbolTable.ID_HSPACE:
			case SymbolTable.ID_MKERN:
			case SymbolTable.ID_MSPACE:
				return ast.add(Ast.DIMSPACE, readDimen(), Ast.NONE, Ast.NONE, start);
			case SymbolTable.ID_RULE: {
				int raise = 0;
				if (lex.kind[pos] == Lexer.CHAR && lex.val[pos] == '[') raise = readBracketDimen();
				int w = readDimen();
				int h = readDimen();
				return ast.rule(w, h, raise, start);
			}
			case SymbolTable.ID_RAISEBOX: {
				int amount = readDimen();
				int a = parseArg();
				if (a == Ast.NONE) return Ast.NONE;
				return ast.raise(a, amount, start);
			}
			case SymbolTable.ID_BOLDSYMBOL:
			case SymbolTable.ID_BM: {
				int a = parseArg();
				if (a == Ast.NONE) return Ast.NONE;
				return ast.add(Ast.FAMILY, a, SymbolTable.ID_MATHBF, Ast.NONE, start);
			}
			case SymbolTable.ID_MATHSCR: {
				int a = parseArg();
				if (a == Ast.NONE) return Ast.NONE;
				return ast.add(Ast.FAMILY, a, SymbolTable.ID_MATHCAL, Ast.NONE, start);
			}
			case SymbolTable.ID_MATHNORMAL: {
				int a = parseArg();
				if (a == Ast.NONE) return Ast.NONE;
				return ast.add(Ast.FAMILY, a, SymbolTable.ID_TEXT, Ast.NONE, start);
			}
			case SymbolTable.ID_XLEFTARROW_147:
				return xarrow(0x21D0, start);
			case SymbolTable.ID_XRIGHTARROW_148:
				return xarrow(0x21D2, start);
			case SymbolTable.ID_XLEFTRIGHTARROW:
				return xarrow(0x21D4, start);
			case SymbolTable.ID_XMAPSTO:
				return xarrow(0x21A6, start);
			case SymbolTable.ID_XLONGEQUAL:
				return xarrow(0x3D, start);
			case SymbolTable.ID_XHOOKLEFTARROW:
				return xarrow(0x21A9, start);
			case SymbolTable.ID_XHOOKRIGHTARROW:
				return xarrow(0x21AA, start);
			case SymbolTable.ID_XLEFTHARPOONUP:
				return xarrow(0x21BC, start);
			case SymbolTable.ID_XRIGHTHARPOONUP:
				return xarrow(0x21C0, start);
			case SymbolTable.ID_XLEFTHARPOONDOWN:
				return xarrow(0x21BD, start);
			case SymbolTable.ID_XRIGHTHARPOONDOWN:
				return xarrow(0x21C1, start);
			case SymbolTable.ID_XLEFTRIGHTHARPOONS:
				return xarrow(0x21CB, start);
			case SymbolTable.ID_XRIGHTLEFTHARPOONS:
				return xarrow(0x21CC, start);
			case SymbolTable.ID_XTOFROM:
				return xarrow(0x21C4, start);
			case SymbolTable.ID_XTWOHEADLEFTARROW:
				return xarrow(0x219E, start);
			case SymbolTable.ID_XTWOHEADRIGHTARROW:
				return xarrow(0x21A0, start);
			case SymbolTable.ID_COMPLEX:
			case SymbolTable.ID_CNUMS:
			case SymbolTable.ID_C:
				return mathbb('C', start);
			case SymbolTable.ID_N:
			case SymbolTable.ID_NATNUMS:
				return mathbb('N', start);
			case SymbolTable.ID_Q:
				return mathbb('Q', start);
			case SymbolTable.ID_R:
			case SymbolTable.ID_REALS:
				return mathbb('R', start);
			case SymbolTable.ID_Z:
				return mathbb('Z', start);
			case SymbolTable.ID_ARGMAX:
				return wordOp("arg max", start, Ast.LARGE_OP);
			case SymbolTable.ID_ARGMIN:
				return wordOp("arg min", start, Ast.LARGE_OP);
			case SymbolTable.ID_LIMINF:
			case SymbolTable.ID_VARLIMINF:
				return wordOp("lim inf", start, Ast.LARGE_OP);
			case SymbolTable.ID_LIMSUP:
			case SymbolTable.ID_VARLIMSUP:
				return wordOp("lim sup", start, Ast.LARGE_OP);
			case SymbolTable.ID_PLIM:
				return wordOp("plim", start, Ast.LARGE_OP);
			case SymbolTable.ID_PROJLIM:
			case SymbolTable.ID_VARPROJLIM:
				return wordOp("proj lim", start, Ast.LARGE_OP);
			case SymbolTable.ID_INJLIM:
			case SymbolTable.ID_VARINJLIM:
				return wordOp("inj lim", start, Ast.LARGE_OP);
			case SymbolTable.ID_TG:
				return wordOp("tg", start, Ast.LARGE_OP_NL);
			case SymbolTable.ID_TH:
				return wordOp("th", start, Ast.LARGE_OP_NL);
			case SymbolTable.ID_SH:
				return wordOp("sh", start, Ast.LARGE_OP_NL);
			case SymbolTable.ID_COSEC:
				return wordOp("cosec", start, Ast.LARGE_OP_NL);
			case SymbolTable.ID_COTG:
				return wordOp("cotg", start, Ast.LARGE_OP_NL);
			case SymbolTable.ID_COTH:
				return wordOp("coth", start, Ast.LARGE_OP_NL);
			case SymbolTable.ID_CTG:
				return wordOp("ctg", start, Ast.LARGE_OP_NL);
			case SymbolTable.ID_CTH:
				return wordOp("cth", start, Ast.LARGE_OP_NL);
			case SymbolTable.ID_ARCTG:
				return wordOp("arctg", start, Ast.LARGE_OP_NL);
			case SymbolTable.ID_ARCCTG:
				return wordOp("arcctg", start, Ast.LARGE_OP_NL);
			case SymbolTable.ID_BRA:
			case SymbolTable.ID_BRA_197:
				return braced(0x27E8, 0x7C, start);
			case SymbolTable.ID_KET:
			case SymbolTable.ID_KET_198:
				return braced(0x7C, 0x27E9, start);
			case SymbolTable.ID_BRAKET:
			case SymbolTable.ID_BRAKET_199:
				return braced(0x27E8, 0x27E9, start);
			case SymbolTable.ID_SET:
				return braced(0x7B, 0x7D, start);
			case SymbolTable.ID_LLAP:
			case SymbolTable.ID_RLAP: {
				int a = parseArg();
				if (a == Ast.NONE) return Ast.NONE;
				return ast.clap(a, id == SymbolTable.ID_LLAP ? 1 : 2, start);
			}
			case SymbolTable.ID_OVERGROUP:
			case SymbolTable.ID_OVERLINESEGMENT:
				return ast.add(Ast.OVER, parseArg(), Ast.NONE, Ast.NONE, start);
			case SymbolTable.ID_UNDERGROUP:
			case SymbolTable.ID_UNDERLINESEGMENT:
			case SymbolTable.ID_UNDERBAR:
				return ast.add(Ast.UNDER, parseArg(), Ast.NONE, Ast.NONE, start);
			case SymbolTable.ID_UTILDE:
				return explicitAccent(Ast.UACCENT, 0x330, start);
			case SymbolTable.ID_MATHRING:
				return explicitAccent(Ast.XACCENT, 0x30A, start);
			case SymbolTable.ID_WIDEPAREN:
				return explicitAccent(Ast.WACCENT, 0x23DC, start);
			case SymbolTable.ID_PMB: {
				int a = parseArg();
				if (a == Ast.NONE) return Ast.NONE;
				return ast.add(Ast.FAMILY, a, SymbolTable.ID_MATHBF, Ast.NONE, start);
			}
			case SymbolTable.ID_MATRIX:
				return parseEnvDirect(Ast.E_MATRIX, Ast.COLS_ALIGN, start);
			case SymbolTable.ID_PMATRIX:
				return parseEnvDirect(Ast.E_PMATRIX, Ast.COLS_ALIGN, start);
			case SymbolTable.ID_BMATRIX:
				return parseEnvDirect(Ast.E_BMATRIX, Ast.COLS_ALIGN, start);
			case SymbolTable.ID_BMATRIX_215:
				return parseEnvDirect(Ast.E_BMATRIX_B, Ast.COLS_ALIGN, start);
			case SymbolTable.ID_VMATRIX:
				return parseEnvDirect(Ast.E_VMATRIX, Ast.COLS_ALIGN, start);
			case SymbolTable.ID_VMATRIX_217:
				return parseEnvDirect(Ast.E_VMATRIX_B, Ast.COLS_ALIGN, start);
			case SymbolTable.ID_CASES:
				return parseEnvDirect(Ast.E_CASES, Ast.COLS_ALIGN, start);
			case SymbolTable.ID_FBOX: {
				int a = parseArg();
				if (a == Ast.NONE) return Ast.NONE;
				return ast.boxed(a, start);
			}
			case SymbolTable.ID_XLEFTRIGHTARROW_220:
				return xarrow(0x2194, start);
			case SymbolTable.ID_BIGL:
			case SymbolTable.ID_BIGR:
			case SymbolTable.ID_BIGM:
			case SymbolTable.ID_BIGL_224:
			case SymbolTable.ID_BIGR_225:
			case SymbolTable.ID_BIGM_226:
			case SymbolTable.ID_BIGGL:
			case SymbolTable.ID_BIGGR:
			case SymbolTable.ID_BIGGM:
			case SymbolTable.ID_BIGGL_230:
			case SymbolTable.ID_BIGGR_231:
			case SymbolTable.ID_BIGGM_232: {
				int cp = readDelim();
				if (cp == Ast.NONE) return Ast.NONE;
				return ast.add(Ast.DELIM, cp, id, Ast.NONE, start);
			}
			case SymbolTable.ID_TEXTCOLOR: {
				int fg = readColorArg();
				int a = parseArg();
				if (a == Ast.NONE) return Ast.NONE;
				return ast.attr(a, fg, 0, 0, 0, start);
			}
			case SymbolTable.ID_COLORBOX: {
				int bg = readColorArg();
				int a = parseArg();
				if (a == Ast.NONE) return Ast.NONE;
				return ast.attr(a, 0, bg, 0, 300, start);
			}
			case SymbolTable.ID_FCOLORBOX: {
				int frame = readColorArg();
				int bg = readColorArg();
				int a = parseArg();
				if (a == Ast.NONE) return Ast.NONE;
				return ast.attr(a, 0, bg, frame, 300, start);
			}
			case SymbolTable.ID_CLASS:
			case SymbolTable.ID_HTMLCLASS:
			case SymbolTable.ID_CSSCLASS: {
				String cls = readNameArg();
				int a = parseArg();
				if (a == Ast.NONE) return Ast.NONE;
				return ast.tag(a, cls == null ? -1 : ast.str(cls), -1, start);
			}
			case SymbolTable.ID_HTMLID:
			case SymbolTable.ID_CSSID: {
				String tn = readNameArg();
				int a = parseArg();
				if (a == Ast.NONE) return Ast.NONE;
				return ast.tag(a, -1, tn == null ? -1 : ast.str(tn), start);
			}
			case SymbolTable.ID_MATHCHOICE: {
				int dc = parseArg();
				if (dc == Ast.NONE) return Ast.NONE;
				return ast.mathchoice(dc, parseArg(), parseArg(), parseArg(), start);
			}
			case SymbolTable.ID_ACCENTSET: {
				int accent = parseArg();
				int base = parseArg();
				if (base == Ast.NONE) return Ast.NONE;
				if (accent == Ast.NONE) return base;
				return ast.accentSet(base, accent, start);
			}
			case SymbolTable.ID_UNDERACCENT: {
				int accent = parseArg();
				int base = parseArg();
				if (base == Ast.NONE) return Ast.NONE;
				if (accent == Ast.NONE) return base;
				return ast.underAccent(base, accent, start);
			}
			case SymbolTable.ID_PRESCRIPT: {
				int sup = parseArg();
				int sub = parseArg();
				int base = parseArg();
				if (base == Ast.NONE) return Ast.NONE;
				if (sup == Ast.NONE && sub == Ast.NONE) return base;
				return ast.prescript(sup, sub, base, start);
			}
			case SymbolTable.ID_SIDESET: {
				parseScriptSpec();
				int lsub = specSub;
				int lsup = specSup;
				parseScriptSpec();
				int rsub = specSub;
				int rsup = specSup;
				int op = parseArg();
				if (op == Ast.NONE) return Ast.NONE;
				return ast.sideset(lsub, lsup, rsub, rsup, op, start);
			}
			case SymbolTable.ID_SMASHOPERATOR: {
				int arg = parseArg();
				if (arg == Ast.NONE) return Ast.NONE;
				return ast.smashOp(arg, start);
			}
			case SymbolTable.ID_HLINE:
				return Ast.NONE;
			case SymbolTable.ID_MULTICOLUMN: {
				readIntArg();
				readAlignArg();
				return parseArg();
			}
			default:
				return Ast.NONE;
		}
	}
	private int parseEnv(int start) {
		if (lex.kind[pos] != Lexer.OPEN) return ast.error(start);
		int nameStart = lex.end[pos];
		pos++;
		int nameEnd = -1;
		int d = 1;
		while (lex.kind[pos] != Lexer.EOF) {
			byte k = lex.kind[pos];
			if (k == Lexer.OPEN) d++;
			else if (k == Lexer.CLOSE) {
				d--;
				if (d == 0) {
					nameEnd = lex.at[pos];
					pos++;
					break;
				}
			}
			pos++;
		}
		if (nameEnd < 0) return ast.error(start);
		int code = envCode(lex.source().subSequence(nameStart, nameEnd).toString());
		if (code < 0) return ast.error(start);
		int spec = Ast.COLS_ALIGN;
		if (code == Ast.E_ARRAY || code == Ast.E_SUBARRAY) spec = readColSpec();
		return parseRows(code, spec, start, true);
	}
	private int parseEnvDirect(int code, int spec, int start) {
		if (lex.kind[pos] != Lexer.OPEN) return ast.error(start);
		pos++;
		return parseRows(code, spec, start, false);
	}
	private int parseRows(int code, int spec, int start, boolean named) {
		int firstRow = NO_LAST;
		int lastRow = NO_LAST;
		boolean trailing = false;
		while (true) {
			boolean top = consumeHlines();
			if (top && (lex.kind[pos] == Lexer.CLOSE || (named && lex.kind[pos] == Lexer.COMMAND && lex.val[pos] == SymbolTable.ID_END))) {
				trailing = true;
				break;
			}
			int firstCell = NO_LAST;
			int lastCell = NO_LAST;
			while (true) {
				int cell;
				if (lex.kind[pos] == Lexer.COMMAND && lex.val[pos] == SymbolTable.ID_MULTICOLUMN) {
					cell = parseMultiCell();
				} else {
					int saved = stop;
					stop = saved | STOP_ALIGN | STOP_ROW | STOP_END;
					int content = parseSeq();
					stop = saved;
					cell = ast.cell(content);
				}
				if (firstCell == NO_LAST) firstCell = cell;
				else ast.next[lastCell] = cell;
				lastCell = cell;
				if (lex.kind[pos] == Lexer.ALIGN) {
					pos++;
					continue;
				}
				break;
			}
			int row = ast.row(firstCell);
			if (top) ast.c[row] = 1;
			if (firstRow == NO_LAST) firstRow = row;
			else ast.next[lastRow] = row;
			lastRow = row;
			if (lex.kind[pos] == Lexer.ROW) {
				pos++;
				if (named && lex.kind[pos] == Lexer.CHAR && lex.val[pos] == '[') ast.b[row] = readBracketDimen();
				continue;
			}
			break;
		}
		if (named) {
			if (lex.kind[pos] == Lexer.COMMAND && lex.val[pos] == SymbolTable.ID_END) {
				pos++;
				skipGroup();
			}
		} else if (lex.kind[pos] == Lexer.CLOSE) pos++;
		if (firstRow == NO_LAST) return ast.error(start);
		int env = ast.env(code, firstRow, spec, start);
		if (trailing) ast.d[env] = 1;
		return env;
	}
	private int readColSpec() {
		if (lex.kind[pos] != Lexer.OPEN) return Ast.COLS_ALIGN;
		int s = lex.end[pos];
		pos++;
		int e = -1;
		int d = 1;
		while (lex.kind[pos] != Lexer.EOF) {
			byte k = lex.kind[pos];
			if (k == Lexer.OPEN) d++;
			else if (k == Lexer.CLOSE) {
				d--;
				if (d == 0) {
					e = lex.at[pos];
					pos++;
					break;
				}
			}
			pos++;
		}
		if (e < 0) return Ast.COLS_ALIGN;
		int packed = 0;
		int col = 0;
		CharSequence src = lex.source();
		for (int i = s; i < e && col < 16; i++) {
			char ch = src.charAt(i);
			if (ch == 'l') packed |= 1 << (col * 2);
			else if (ch == 'r') packed |= 2 << (col * 2);
			else if (ch == 'c') { }
			else continue;
			col++;
		}
		return packed;
	}
	private boolean consumeHlines() {
		boolean any = false;
		while (lex.kind[pos] == Lexer.COMMAND && lex.val[pos] == SymbolTable.ID_HLINE) {
			pos++;
			any = true;
		}
		return any;
	}
	private int readIntArg() {
		if (lex.kind[pos] != Lexer.OPEN) return -1;
		int s = lex.end[pos];
		pos++;
		int e = -1;
		int d = 1;
		while (lex.kind[pos] != Lexer.EOF) {
			byte k = lex.kind[pos];
			if (k == Lexer.OPEN) d++;
			else if (k == Lexer.CLOSE) {
				d--;
				if (d == 0) {
					e = lex.at[pos];
					pos++;
					break;
				}
			}
			pos++;
		}
		if (e < 0) return -1;
		int v = 0;
		CharSequence src = lex.source();
		for (int i = s; i < e; i++) {
			char ch = src.charAt(i);
			if (ch >= '0' && ch <= '9') v = v * 10 + (ch - '0');
		}
		return v;
	}
	private int readAlignArg() {
		if (lex.kind[pos] != Lexer.OPEN) return -1;
		pos++;
		int d = 1;
		int align = -1;
		while (lex.kind[pos] != Lexer.EOF) {
			byte k = lex.kind[pos];
			if (k == Lexer.OPEN) d++;
			else if (k == Lexer.CLOSE) {
				d--;
				if (d == 0) {
					pos++;
					break;
				}
			} else if (k == Lexer.CHAR && align < 0) {
				char ch = (char) lex.val[pos];
				if (ch == 'l') align = 2;
				else if (ch == 'c') align = 0;
				else if (ch == 'r') align = 1;
			}
			pos++;
		}
		return align;
	}
	private int parseMultiCell() {
		pos++;
		int span = readIntArg();
		int align = readAlignArg();
		int content = parseArg();
		int cell = ast.cell(content);
		if (span > 1) ast.b[cell] = span;
		ast.c[cell] = align;
		return cell;
	}
	private void skipGroup() {
		if (lex.kind[pos] != Lexer.OPEN) return;
		pos++;
		int d = 1;
		while (lex.kind[pos] != Lexer.EOF) {
			byte k = lex.kind[pos];
			if (k == Lexer.OPEN) d++;
			else if (k == Lexer.CLOSE) {
				d--;
				if (d == 0) {
					pos++;
					return;
				}
			}
			pos++;
		}
	}
	private static int envCode(String name) {
		if (name.equals("matrix")) return Ast.E_MATRIX;
		if (name.equals("pmatrix")) return Ast.E_PMATRIX;
		if (name.equals("bmatrix")) return Ast.E_BMATRIX;
		if (name.equals("Bmatrix")) return Ast.E_BMATRIX_B;
		if (name.equals("vmatrix")) return Ast.E_VMATRIX;
		if (name.equals("Vmatrix")) return Ast.E_VMATRIX_B;
		if (name.equals("smallmatrix")) return Ast.E_SMALLMATRIX;
		if (name.equals("matrix*")) return Ast.E_MATRIX_S;
		if (name.equals("pmatrix*")) return Ast.E_PMATRIX_S;
		if (name.equals("bmatrix*")) return Ast.E_BMATRIX_S;
		if (name.equals("array")) return Ast.E_ARRAY;
		if (name.equals("cases")) return Ast.E_CASES;
		if (name.equals("subarray")) return Ast.E_SUBARRAY;
		if (name.equals("align")) return Ast.E_ALIGN;
		if (name.equals("align*")) return Ast.E_ALIGN_S;
		if (name.equals("aligned")) return Ast.E_ALIGNED;
		if (name.equals("alignat")) return Ast.E_ALIGNAT;
		if (name.equals("alignat*")) return Ast.E_ALIGNAT_S;
		if (name.equals("alignedat")) return Ast.E_ALIGNEDAT;
		if (name.equals("flalign")) return Ast.E_FLALIGN;
		if (name.equals("flalign*")) return Ast.E_FLALIGN_S;
		if (name.equals("gather")) return Ast.E_GATHER;
		if (name.equals("gather*")) return Ast.E_GATHER_S;
		if (name.equals("gathered")) return Ast.E_GATHERED;
		if (name.equals("split")) return Ast.E_SPLIT;
		return -1;
	}
	private int binary(byte k, int start) {
		int left = parseArg();
		int right = parseArg();
		return ast.add(k, left, right, Ast.NONE, start);
	}
	private int sqrt(int start) {
		int degree = -1;
		if (lex.kind[pos] == Lexer.CHAR && lex.val[pos] == '[') {
			pos++;
			int end = pos;
			int d = 1;
			while (lex.kind[end] != Lexer.EOF && d > 0) {
				if (lex.kind[end] == Lexer.CHAR && lex.val[end] == '[') d++;
				else if (lex.kind[end] == Lexer.CHAR && lex.val[end] == ']') d--;
				if (d == 0) break;
				end++;
			}
			byte saved = lex.kind[end];
			lex.kind[end] = Lexer.CLOSE;
			degree = parseSeq();
			lex.kind[end] = saved;
			pos = end;
			if (lex.kind[pos] != Lexer.EOF) pos++;
		}
		int arg = parseArg();
		return ast.add(Ast.SQRT, arg, degree, Ast.NONE, start);
	}
	private int textArg(int start, int id) {
		if (lex.kind[pos] != Lexer.OPEN) return Ast.NONE;
		pos++;
		int first = NO_LAST;
		int last = NO_LAST;
		int d = 1;
		CharSequence src = lex.source();
		while (lex.kind[pos] != Lexer.EOF && d > 0) {
			byte k = lex.kind[pos];
			if (k == Lexer.OPEN) d++;
			else if (k == Lexer.CLOSE) {
				d--;
				if (d == 0) break;
			}
			int from = lex.at[pos];
			int to = lex.end[pos];
			if (k == Lexer.SYMBOL || k == Lexer.OP || k == Lexer.OPNL) {
				int g = ast.glyph(lex.val[pos], from);
				if (first == NO_LAST) first = g; else ast.next[last] = g;
				last = g;
			} else if (k != Lexer.COMMAND) {
				for (int i = from; i < to; i++) {
					int g = ast.glyph(src.charAt(i), i);
					if (first == NO_LAST) first = g; else ast.next[last] = g;
					last = g;
				}
			}
			pos++;
		}
		if (lex.kind[pos] == Lexer.CLOSE) pos++;
		if (first == NO_LAST) return Ast.NONE;
		int node = ast.add(Ast.TEXT, seq(first), Ast.NONE, Ast.NONE, start);
		int fam = id == SymbolTable.ID_TEXTBF ? SymbolTable.ID_MATHBF
			: id == SymbolTable.ID_TEXTIT ? SymbolTable.ID_MATHIT
			: id == SymbolTable.ID_TEXTSF ? SymbolTable.ID_MATHSF
			: id == SymbolTable.ID_TEXTTT ? SymbolTable.ID_MATHTT : -1;
		if (fam < 0) return node;
		return ast.add(Ast.FAMILY, node, fam, Ast.NONE, start);
	}
	private int delimited(int start) {
		int left = readDelim();
		int saved = stop;
		stop = saved | STOP_RIGHT;
		int inner = parseSeq();
		stop = saved;
		int right = Ast.NONE;
		if (lex.kind[pos] == Lexer.COMMAND && lex.val[pos] == SymbolTable.ID_RIGHT) {
			pos++;
			right = readDelim();
		}
		return ast.add(Ast.LEFT, left, right, inner, start);
	}
	private int readDelim() {
		byte k = lex.kind[pos];
		if (k == Lexer.CHAR) {
			int cp = lex.val[pos];
			pos++;
			return cp == '.' ? Ast.NONE : cp;
		}
		if (k == Lexer.SYMBOL) {
			int cp = lex.val[pos];
			pos++;
			return cp;
		}
		if (k == Lexer.OPEN) {
			pos++;
			return '(';
		}
		if (k == Lexer.CLOSE) {
			pos++;
			return ')';
		}
		return Ast.NONE;
	}
	private int readColorArg() {
		if (lex.kind[pos] != Lexer.OPEN) return ColorTable.NONE;
		int s = lex.end[pos];
		pos++;
		int d = 1;
		int e = -1;
		while (lex.kind[pos] != Lexer.EOF) {
			byte k = lex.kind[pos];
			if (k == Lexer.OPEN) d++;
			else if (k == Lexer.CLOSE) {
				d--;
				if (d == 0) {
					e = lex.at[pos];
					pos++;
					break;
				}
			}
			pos++;
		}
		if (e < 0) return ColorTable.NONE;
		return ColorTable.parse(lex.source(), s, e);
	}
	private String readNameArg() {
		if (lex.kind[pos] != Lexer.OPEN) return null;
		int s = lex.end[pos];
		pos++;
		int d = 1;
		int e = -1;
		while (lex.kind[pos] != Lexer.EOF) {
			byte k = lex.kind[pos];
			if (k == Lexer.OPEN) d++;
			else if (k == Lexer.CLOSE) {
				d--;
				if (d == 0) {
					e = lex.at[pos];
					pos++;
					break;
				}
			}
			pos++;
		}
		if (e < 0) return null;
		return lex.source().subSequence(s, e).toString().trim();
	}
	private int opName(int from, int to, int start, boolean limits) {
		if (to <= from) return Ast.NONE;
		CharSequence src = lex.source();
		int first = NO_LAST;
		int last = NO_LAST;
		for (int i = from; i < to; ) {
			char ch = src.charAt(i);
			if (ch == '\\') {
				i++;
				if (i >= to) break;
				char c = src.charAt(i);
				if (c == ',' || c == ';' || c == ':' || c == ' ' || c == '>') {
					int g = ast.glyph(' ', i);
					if (first == NO_LAST) first = g; else ast.next[last] = g;
					last = g;
					i++;
				} else if (c == '!') {
					i++;
				} else if (isLetter(c)) {
					while (i < to && isLetter(src.charAt(i))) i++;
				} else {
					i++;
				}
				continue;
			}
			int g = ast.glyph(ch, i);
			if (first == NO_LAST) first = g; else ast.next[last] = g;
			last = g;
			i++;
		}
		if (first == NO_LAST) return Ast.NONE;
		int inner = seq(first);
		int head = ast.add(Ast.TEXT, inner, Ast.NONE, Ast.NONE, start);
		ast.b[head] = limits ? Ast.LARGE_OP : Ast.LARGE_OP_NL;
		return head;
	}
	private static boolean isLetter(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}
	private int opNameArg(int start) {
		boolean limits = false;
		while (lex.kind[pos] == Lexer.CHAR && lex.val[pos] == '*') {
			limits = true;
			pos++;
		}
		if (lex.kind[pos] != Lexer.OPEN) return Ast.NONE;
		int from = lex.end[pos];
		pos++;
		int d = 1;
		int to = from;
		while (lex.kind[pos] != Lexer.EOF) {
			byte k = lex.kind[pos];
			if (k == Lexer.OPEN) d++;
			else if (k == Lexer.CLOSE) {
				d--;
				if (d == 0) {
					to = lex.at[pos];
					pos++;
					break;
				}
			}
			pos++;
		}
		return opName(from, to, start, limits);
	}
	private static boolean isAccentMark(int cp) {
		return (cp >= 0x300 && cp <= 0x36F) || (cp >= 0x1AB0 && cp <= 0x1AFF) || (cp >= 0x1DC0 && cp <= 0x1DFF) || (cp >= 0x20D0 && cp <= 0x20FF);
	}
	private static boolean isBaseStart(byte k) {
		return k == Lexer.CHAR || k == Lexer.SYMBOL || k == Lexer.OPEN || k == Lexer.PRIME;
	}
	private int parseArg() {
		byte k = lex.kind[pos];
		if (k == Lexer.OPEN) {
			pos++;
			return parseGroup();
		}
		if (k == Lexer.EOF || k == Lexer.CLOSE) return Ast.NONE;
		return parseAtom();
	}
	private int word(String s, int pos) {
		int first = NO_LAST;
		int last = NO_LAST;
		for (int i = 0; i < s.length(); i++) {
			int g = ast.glyph(s.charAt(i), pos);
			if (first == NO_LAST) first = g; else ast.next[last] = g;
			last = g;
		}
		return ast.add(Ast.TEXT, seq(first), Ast.NONE, Ast.NONE, pos);
	}
	private int mathbb(int cp, int start) {
		return ast.add(Ast.FAMILY, ast.glyph(cp, start), SymbolTable.ID_MATHBB, Ast.NONE, start);
	}
	private int wordOp(String s, int start, int kind) {
		int head = word(s, start);
		ast.b[head] = kind;
		return head;
	}
	private int braced(int left, int right, int start) {
		int a = parseArg();
		if (a == Ast.NONE) return Ast.NONE;
		return ast.add(Ast.LEFT, left, right, a, start);
	}
	private int explicitAccent(byte kind, int cp, int start) {
		int a = parseArg();
		if (a == Ast.NONE) return Ast.NONE;
		return ast.add(kind, a, cp, Ast.NONE, start);
	}
	private int pmod(int start, boolean paren) {
		int mod = word("mod", start);
		int sp = ast.space(6, start);
		int arg = parseArg();
		ast.next[mod] = sp;
		if (arg != Ast.NONE) ast.next[sp] = arg;
		int inner = seq(mod);
		if (paren) return ast.add(Ast.LEFT, '(', ')', inner, start);
		return inner;
	}
	private int xarrow(int cp, int start) {
		int below = Ast.NONE;
		if (lex.kind[pos] == Lexer.CHAR && lex.val[pos] == '[') below = parseOptional();
		int above = parseArg();
		return ast.xarrow(cp, above, below, start);
	}
	private int parseOptional() {
		if (lex.kind[pos] != Lexer.CHAR || lex.val[pos] != '[') return Ast.NONE;
		pos++;
		int end = pos;
		int d = 1;
		while (lex.kind[end] != Lexer.EOF) {
			if (lex.kind[end] == Lexer.CHAR && lex.val[end] == '[') d++;
			else if (lex.kind[end] == Lexer.CHAR && lex.val[end] == ']') {
				d--;
				if (d == 0) break;
			}
			end++;
		}
		byte saved = lex.kind[end];
		lex.kind[end] = Lexer.CLOSE;
		int inner = parseSeq();
		lex.kind[end] = saved;
		pos = end;
		if (lex.kind[pos] != Lexer.EOF) pos++;
		return inner;
	}
	private int genfrac(int start) {
		int left = parseDelimArg();
		int right = parseDelimArg();
		parseArg();
		int style = parseStyleArg();
		int num = parseArg();
		int den = parseArg();
		return ast.genfrac(num, den, style, left, right, start);
	}
	private int parseDelimArg() {
		if (lex.kind[pos] != Lexer.OPEN) return Ast.NONE;
		int s = lex.end[pos];
		pos++;
		int e = -1;
		int d = 1;
		while (lex.kind[pos] != Lexer.EOF) {
			byte k = lex.kind[pos];
			if (k == Lexer.OPEN) d++;
			else if (k == Lexer.CLOSE) {
				d--;
				if (d == 0) {
					e = lex.at[pos];
					pos++;
					break;
				}
			}
			pos++;
		}
		if (e < 0) return Ast.NONE;
		CharSequence src = lex.source();
		for (int i = s; i < e; i++) {
			char ch = src.charAt(i);
			if (ch == ' ' || ch == '\t') continue;
			if (ch == '\\') {
				if (i + 1 < e) return src.charAt(i + 1);
				return Ast.NONE;
			}
			return ch;
		}
		return Ast.NONE;
	}
	private int parseStyleArg() {
		if (lex.kind[pos] != Lexer.OPEN) return Ast.S_TEXT;
		int s = lex.end[pos];
		pos++;
		int e = -1;
		int d = 1;
		while (lex.kind[pos] != Lexer.EOF) {
			byte k = lex.kind[pos];
			if (k == Lexer.OPEN) d++;
			else if (k == Lexer.CLOSE) {
				d--;
				if (d == 0) {
					e = lex.at[pos];
					pos++;
					break;
				}
			}
			pos++;
		}
		if (e < 0) return Ast.S_TEXT;
		CharSequence src = lex.source();
		for (int i = s; i < e; i++) {
			char ch = src.charAt(i);
			if (ch >= '0' && ch <= '3') return ch - '0';
		}
		return Ast.S_TEXT;
	}
	private int substack(int start) {
		if (lex.kind[pos] != Lexer.OPEN) return ast.error(start);
		pos++;
		int firstRow = NO_LAST;
		int lastRow = NO_LAST;
		while (true) {
			int saved = stop;
			stop = saved | STOP_ROW | STOP_END;
			int content = parseSeq();
			stop = saved;
			int row = ast.row(ast.cell(content));
			if (firstRow == NO_LAST) firstRow = row;
			else ast.next[lastRow] = row;
			lastRow = row;
			if (lex.kind[pos] == Lexer.ROW) {
				pos++;
				continue;
			}
			break;
		}
		if (lex.kind[pos] == Lexer.CLOSE) pos++;
		if (firstRow == NO_LAST) return ast.error(start);
		return ast.env(Ast.E_GATHERED, firstRow, Ast.COLS_GATHER, start);
	}
	private int readDimen() {
		if (lex.kind[pos] == Lexer.OPEN) {
			int s = lex.end[pos];
			pos++;
			int d = 1;
			int e = -1;
			while (lex.kind[pos] != Lexer.EOF) {
				byte k = lex.kind[pos];
				if (k == Lexer.OPEN) d++;
				else if (k == Lexer.CLOSE) {
					d--;
					if (d == 0) {
						e = lex.at[pos];
						pos++;
						break;
					}
				}
				pos++;
			}
			if (e < 0) return 0;
			return parseDim(lex.source(), s, e);
		}
		int s = lex.at[pos];
		int e = lex.end[pos];
		pos++;
		while (lex.kind[pos] == Lexer.CHAR) {
			char c = (char) lex.val[pos];
			if ((c >= '0' && c <= '9') || c == '.') {
				e = lex.end[pos];
				pos++;
			} else break;
		}
		int un = 0;
		while (un < 2 && lex.kind[pos] == Lexer.CHAR) {
			char c = (char) lex.val[pos];
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
				e = lex.end[pos];
				pos++;
				un++;
			} else break;
		}
		return parseDim(lex.source(), s, e);
	}
	private int readBracketDimen() {
		if (lex.kind[pos] != Lexer.CHAR || lex.val[pos] != '[') return 0;
		int s = lex.at[pos] + 1;
		int end = pos;
		int d = 0;
		while (lex.kind[end] != Lexer.EOF) {
			if (lex.kind[end] == Lexer.CHAR && lex.val[end] == '[') d++;
			else if (lex.kind[end] == Lexer.CHAR && lex.val[end] == ']') {
				d--;
				if (d == 0) break;
			}
			end++;
		}
		int e = lex.at[end];
		pos = end;
		if (lex.kind[pos] != Lexer.EOF) pos++;
		return parseDim(lex.source(), s, e);
	}
	private static int parseDim(CharSequence src, int s, int e) {
		int n = src.length();
		if (s < 0) s = 0;
		if (e > n) e = n;
		if (s > e) s = e;
		int i = s;
		double num = 0;
		boolean dot = false;
		double frac = 0.1;
		while (i < e) {
			char c = src.charAt(i);
			if (c >= '0' && c <= '9') {
				if (dot) {
					num += (c - '0') * frac;
					frac /= 10;
				} else num = num * 10 + (c - '0');
				i++;
			} else if (c == '.') {
				dot = true;
				i++;
			} else break;
		}
		char u1 = 0;
		char u2 = 0;
		if (i < e) u1 = src.charAt(i++);
		if (i < e) u2 = src.charAt(i++);
		double f;
		if (u1 == 'e' && u2 == 'm') f = 1;
		else if (u1 == 'p' && u2 == 't') f = 0.1;
		else if (u1 == 'b' && u2 == 'p') f = 0.1;
		else if (u1 == 'e' && u2 == 'x') f = 0.431;
		else if (u1 == 'm' && u2 == 'u') f = 1.0 / 18;
		else if (u1 == 'c' && u2 == 'm') f = 2.845;
		else if (u1 == 'm' && u2 == 'm') f = 0.2845;
		else if (u1 == 'i' && u2 == 'n') f = 7.227;
		else if (u1 == 'p' && u2 == 'x') f = 0.75;
		else if (u1 == 's' && u2 == 'p') f = 1.0 / 655360;
		else f = 1;
		return (int) Math.round(num * f * 1000);
	}
}
