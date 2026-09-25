package texon.latex.engine.core.layout;
import texon.latex.engine.core.*;
import texon.latex.engine.core.box.*;
import texon.latex.engine.core.lex.*;
import texon.latex.engine.core.parse.*;
public final class Layout {
	private static final int D = Ast.S_DISPLAY;
	private static final int T = Ast.S_TEXT;
	private static final int S = Ast.S_SCRIPT;
	private static final int SS = Ast.S_SCRIPTSCRIPT;
	private static final int BRACE_ABOVE_GAP = 35;
	private static final int BRACE_BELOW_GAP = 35;
	private static final int STACK_ABOVE_GAP = 193;
	private static final int ALIGN_ROW_GAP = 753;
	private static final int BOX_PAD = 300;
	private final FontMetrics fm;
	private int style;
	private boolean cramped;
	private byte family;
	public Layout(FontMetrics fm) {
		this.fm = fm;
	}
	public static int mathLetter(int cp) {
		return mathLetter(cp, FamilyTable.MATH);
	}
	public static int mathLetter(int cp, byte fam) {
		if (fam == FamilyTable.MATH) {
			if (cp >= 'A' && cp <= 'Z') return 0x1D434 + cp - 'A';
			if (cp >= 'a' && cp <= 'z') return cp == 'h' ? 0x210E : 0x1D44E + cp - 'a';
			return cp;
		}
		int gi = greekIndex(cp);
		if (gi < 0) gi = uprightGreekIndex(cp);
		if (gi >= 0) return greekBase(fam) + gi;
		int base;
		int off;
		if (cp >= 'a' && cp <= 'z') {
			base = FamilyTable.LO[fam];
			off = cp - 'a';
		} else if (cp >= 'A' && cp <= 'Z') {
			base = FamilyTable.UP[fam];
			off = cp - 'A';
		} else if (cp >= '0' && cp <= '9') {
			base = FamilyTable.DI[fam];
			off = cp - '0';
		} else {
			return cp;
		}
		if (base == 0) return cp;
		int hole = FamilyTable.hole(base + off);
		return hole < 0 ? base + off : hole;
	}
	private static final int[] GREEK = {0x1D6A8, 0x1D6E2, 0x1D71C, 0x1D756, 0x1D790};
	private static final int GREEK_SPAN = 58;
	static int greekIndex(int cp) {
		for (int i = 0; i < GREEK.length; i++) {
			if (cp >= GREEK[i] && cp < GREEK[i] + GREEK_SPAN) return cp - GREEK[i];
		}
		return -1;
	}
	static int greekBase(byte fam) {
		switch (fam) {
			case FamilyTable.BOLD: return GREEK[0];
			case FamilyTable.SF:
			case FamilyTable.TT: return GREEK[3];
			default: return GREEK[1];
		}
	}
	static int uprightGreekIndex(int cp) {
		if (cp >= 0x391 && cp <= 0x3A1) return cp - 0x391;
		if (cp >= 0x3A3 && cp <= 0x3A9) return cp - 0x392;
		if (cp >= 0x3B1 && cp <= 0x3C9) return 26 + cp - 0x3B1;
		switch (cp) {
			case 0x3F4: return 17;
			case 0x2202: return 51;
			case 0x3F5: return 52;
			case 0x3D1: return 53;
			case 0x3F0: return 54;
			case 0x3D5: return 55;
			case 0x3F1: return 56;
			case 0x3D6: return 57;
		}
		return -1;
	}
	private static final int[] LATIN_BLOCK = {
		0x1D400, 0x1D434, 0x1D468, 0x1D49C, 0x1D4D0, 0x1D504,
		0x1D538, 0x1D56C, 0x1D5A0, 0x1D5D4, 0x1D608, 0x1D63C, 0x1D670
	};
	private static final int[] DIGIT_BLOCK = {0x1D7CE, 0x1D7D8, 0x1D7E2, 0x1D7EC, 0x1D7F6};
	static int plainUpright(int cp) {
		for (int b : LATIN_BLOCK) {
			if (cp >= b && cp < b + 52) {
				int i = cp - b;
				return i < 26 ? 'A' + i : 'a' + i - 26;
			}
		}
		for (int b : DIGIT_BLOCK) {
			if (cp >= b && cp < b + 10) return '0' + cp - b;
		}
		int gi = greekIndex(cp);
		if (gi >= 0) return uprightGreekCp(gi);
		return cp;
	}
	static int uprightGreekCp(int index) {
		if (index < 17) return 0x391 + index;
		if (index == 17) return 0x3F4;
		if (index <= 24) return 0x391 + index;
		if (index < 51) return 0x3B1 + index - 26;
		switch (index) {
			case 51: return 0x2202;
			case 52: return 0x3F5;
			case 53: return 0x3D1;
			case 54: return 0x3F0;
			case 55: return 0x3D5;
			case 56: return 0x3F1;
			case 57: return 0x3D6;
		}
		return -1;
	}
	public Box text(int codePoint) {
		return GlyphBox.of(fm, mathLetter(codePoint, family), family);
	}
	public Box text(CharSequence s) {
		int n = s.length();
		if (n == 0) return HBox.of(new Box[0]);
		Box[] kids = new Box[n];
		int count = 0;
		for (int i = 0; i < n; ) {
			int cp = Character.codePointAt(s, i);
			kids[count++] = GlyphBox.of(fm, mathLetter(cp, family), family);
			i += Character.charCount(cp);
		}
		if (count == 1) return kids[0];
		if (count != n) {
			Box[] trimmed = new Box[count];
			System.arraycopy(kids, 0, trimmed, 0, count);
			kids = trimmed;
		}
		return HBox.of(kids);
	}
	public Box script(Box b) {
		return scale(b, fm.constant(FontMetrics.C.SCRIPT_PERCENT_SCALE_DOWN));
	}
	public Box sup(Box base, Box sup) {
		int u = supShiftUp(base, sup);
		int gap = fm.constant(FontMetrics.C.SPACE_AFTER_SCRIPT);
		int kern = base.italic + gap + overhang(sup);
		Box[] kids = {base, sup};
		int[] shift = {0, u};
		int[] kerns = {kern, 0};
		return HBox.kerned(kids, shift, kerns);
	}
	public Box limit(Box base, Box sub, Box sup) {
		if (sub == null) return sup == null ? base : VBox.of(new Box[]{sup, base}, new int[]{upperLimitShift(base, sup), 0}, VBox.CENTER);
		if (sup == null) return VBox.of(new Box[]{base, sub}, new int[]{0, -lowerLimitShift(base, sub)}, VBox.CENTER);
		return VBox.of(new Box[]{sup, base, sub}, new int[]{upperLimitShift(base, sup), 0, -lowerLimitShift(base, sub)}, VBox.CENTER);
	}
	private int upperLimitShift(Box base, Box sup) {
		int rise = fm.constant(FontMetrics.C.UPPER_LIMIT_BASELINE_RISE_MIN);
		int gap = base.height + sup.depth + fm.constant(FontMetrics.C.UPPER_LIMIT_GAP_MIN);
		return rise > gap ? rise : gap;
	}
	private int lowerLimitShift(Box base, Box sub) {
		int drop = fm.constant(FontMetrics.C.LOWER_LIMIT_BASELINE_DROP_MIN);
		int gap = base.depth + sub.height + fm.constant(FontMetrics.C.LOWER_LIMIT_GAP_MIN);
		return drop > gap ? drop : gap;
	}
	public Box sub(Box base, Box sub) {
		int v = subShiftDown(base, sub);
		int gap = fm.constant(FontMetrics.C.SPACE_AFTER_SCRIPT);
		int kern = base.italic + gap + overhang(sub);
		Box[] kids = {base, sub};
		int[] shift = {0, -v};
		int[] kerns = {kern, 0};
		return HBox.kerned(kids, shift, kerns);
	}
	public Box subsup(Box base, Box sub, Box sup) {
		int u = supShiftUp(base, sup);
		int v = subShiftDown(base, sub);
		int gapMin = fm.constant(FontMetrics.C.SUB_SUPERSCRIPT_GAP_MIN);
		int clearance = (u - sup.depth) - (sub.height - v);
		if (clearance < gapMin) {
			int need = gapMin - clearance;
			u += (need + 1) / 2;
			v += need / 2;
		}
		int topMax = fm.constant(FontMetrics.C.SUPERSCRIPT_BOTTOM_MAX_WITH_SUBSCRIPT);
		if (u - sup.depth < topMax) u = sup.depth + topMax;
		Box[] stackKids = {sub, sup};
		int[] stackOff = {-v, u};
		VBox stack = VBox.of(stackKids, stackOff, VBox.LEFT);
		int gap = fm.constant(FontMetrics.C.SPACE_AFTER_SCRIPT);
		int kern = base.italic + gap + overhang(stack);
		Box[] kids = {base, stack};
		int[] shift = {0, 0};
		int[] kerns = {kern, 0};
		return HBox.kerned(kids, shift, kerns);
	}
	private int overhang(Box b) {
		return b.inkLeft < 0 ? -b.inkLeft : 0;
	}
	private static int childStyle(int st) {
		return st == D ? T : st == T ? S : SS;
	}
	private int styleScale(int st) {
		if (st == S) return fm.constant(FontMetrics.C.SCRIPT_PERCENT_SCALE_DOWN);
		if (st == SS) return fm.constant(FontMetrics.C.SCRIPT_SCRIPT_PERCENT_SCALE_DOWN);
		return GlyphBox.FULL;
	}
	private int styleRatio(int from, int to) {
		int a = styleScale(from);
		return (styleScale(to) * GlyphBox.FULL + a - 1) / a;
	}
	private Box inStyle(Ast ast, int node, int st, boolean cramp) {
		int s = style;
		boolean c = cramped;
		style = st;
		cramped = cramp;
		Box b = layout(ast, node);
		style = s;
		cramped = c;
		return b;
	}
	public Box frac(Box num, Box den) {
		return frac(num, den, style != D, false);
	}
	public Box frac(Box num, Box den, boolean nested, boolean leftAlign) {
		int rule = fm.constant(FontMetrics.C.FRACTION_RULE_THICKNESS);
		int numGap = nested ? fm.constant(FontMetrics.C.FRACTION_NUMERATOR_GAP_MIN) : fm.constant(FontMetrics.C.FRACTION_NUM_DISPLAY_STYLE_GAP_MIN);
		int denGap = nested ? fm.constant(FontMetrics.C.FRACTION_DENOMINATOR_GAP_MIN) : fm.constant(FontMetrics.C.FRACTION_DENOM_DISPLAY_STYLE_GAP_MIN);
		int numUp = nested ? fm.constant(FontMetrics.C.FRACTION_NUMERATOR_SHIFT_UP) : fm.constant(FontMetrics.C.FRACTION_NUMERATOR_DISPLAY_STYLE_SHIFT_UP);
		int denDown = nested ? fm.constant(FontMetrics.C.FRACTION_DENOMINATOR_SHIFT_DOWN) : fm.constant(FontMetrics.C.FRACTION_DENOMINATOR_DISPLAY_STYLE_SHIFT_DOWN);
		int barY = fm.constant(FontMetrics.C.AXIS_HEIGHT);
		int half = rule / 2;
		int numBase = Math.max(numUp, barY + half + numGap + num.depth);
		int denBase = Math.min(-denDown, barY - half - denGap - den.height);
		int w = num.width > den.width ? num.width : den.width;
		Box bar = RuleBox.horizontal(w, rule);
		Box[] kids = {num, bar, den};
		int[] offset = {numBase, barY - half, denBase};
		return VBox.of(kids, offset, leftAlign ? VBox.LEFT : VBox.CENTER);
	}
	public Box sqrt(Box content) {
		return sqrt(content, null);
	}
	public Box sqrt(Box content, Box index) {
		int gap = fm.constant(style == D ? FontMetrics.C.RADICAL_DISPLAY_STYLE_VERTICAL_GAP : FontMetrics.C.RADICAL_VERTICAL_GAP);
		int rule = fm.constant(FontMetrics.C.RADICAL_RULE_THICKNESS);
		int barY = content.height + gap;
		int barTop = barY + rule;
		int needed = barTop + content.depth;
		int cp = fm.resolve(0x221A, needed);
		int ink = fm.height(cp) + fm.depth(cp);
		int pct = ink <= needed ? GlyphBox.FULL : (needed * GlyphBox.FULL + ink - 1) / ink;
		Box radical = GlyphBox.scaled(fm, cp, pct);
		int shift = barTop - radical.height;
		int radTotal = radical.height + radical.depth;
		int radBottom = barTop - radTotal;
		Box bar = RuleBox.horizontal(content.width, rule);
		Box body = VBox.of(new Box[]{bar, content}, new int[]{barY, 0});
		if (index == null) {
			Box[] kids = {radical, body};
			int[] shifts = {shift, 0};
			return HBox.of(kids, shifts);
		}
		int raise = fm.constant(FontMetrics.C.RADICAL_DEGREE_BOTTOM_RAISE_PERCENT);
		int degreeBottom = radBottom + radTotal * raise / GlyphBox.FULL;
		int idxShift = degreeBottom + index.depth;
		int kb = fm.constant(FontMetrics.C.RADICAL_KERN_BEFORE_DEGREE);
		int idxX = radical.width - kb - index.width;
		int padW = idxX < 0 ? -idxX : 0;
		Box pad = new RuleBox(padW, 0, 0);
		int start = padW + idxX;
		Box[] kids = {pad, index, radical, body};
		int[] shifts = {0, idxShift, shift, 0};
		int[] kerns = {start, -(start + index.width), 0, 0};
		return HBox.kerned(kids, shifts, kerns);
	}
	private int supShiftUp(Box base, Box sup) {
		int u = fm.constant(cramped ? FontMetrics.C.SUPERSCRIPT_SHIFT_UP_CRAMPED : FontMetrics.C.SUPERSCRIPT_SHIFT_UP);
		int drop = base.height - fm.constant(FontMetrics.C.SUPERSCRIPT_BASELINE_DROP_MAX);
		if (drop > u) u = drop;
		int bottom = sup.depth + fm.constant(FontMetrics.C.SUPERSCRIPT_BOTTOM_MIN);
		if (bottom > u) u = bottom;
		return u;
	}
	private int subShiftDown(Box base, Box sub) {
		int v = fm.constant(FontMetrics.C.SUBSCRIPT_SHIFT_DOWN);
		int top = sub.height - fm.constant(FontMetrics.C.SUBSCRIPT_TOP_MAX);
		if (top > v) v = top;
		int drop = base.depth + fm.constant(FontMetrics.C.SUBSCRIPT_BASELINE_DROP_MIN);
		if (drop > v) v = drop;
		return v;
	}
	public Box build(Ast ast) {
		return build(ast, ast.root);
	}
	public Box build(Ast ast, int node) {
		style = D;
		cramped = false;
		family = FamilyTable.MATH;
		return layout(ast, node);
	}
	private Box layout(Ast ast, int node) {
		if (node == Ast.NONE) return HBox.of(new Box[0]);
		switch (ast.kind[node]) {
			case Ast.GLYPH:
				return glyph(ast, node);
			case Ast.SEQ:
				return buildSeq(ast, node);
			case Ast.TEXT:
				return buildText(ast, ast.a[node]);
			case Ast.FRAC: {
				int cs = childStyle(style);
				int down = styleRatio(style, cs);
				Box num = inStyle(ast, ast.a[node], cs, false);
				Box den = inStyle(ast, ast.b[node], cs, true);
				if (down != GlyphBox.FULL) {
					num = scale(num, down);
					den = scale(den, down);
				}
				Box result;
				int id = ast.c[node];
				if (id == SymbolTable.ID_BINOM || id == SymbolTable.ID_CHOOSE) {
					result = delimited(frac(num, den), 0x28, 0x29);
				} else if (id == SymbolTable.ID_DBINOM) {
					result = delimited(frac(num, den, false, false), 0x28, 0x29);
				} else if (id == SymbolTable.ID_TBINOM) {
					result = delimited(frac(num, den, true, false), 0x28, 0x29);
				} else if (id == SymbolTable.ID_BRACE) {
					result = delimited(frac(num, den), 0x7B, 0x7D);
				} else if (id == SymbolTable.ID_BRACK) {
					result = delimited(frac(num, den), 0x5B, 0x5D);
				} else if (id == SymbolTable.ID_ATOP) {
					result = atop(num, den);
				} else if (id == SymbolTable.ID_DFRAC) {
					result = frac(num, den, false, false);
				} else if (id == SymbolTable.ID_TFRAC) {
					result = frac(num, den, true, false);
				} else if (id == SymbolTable.ID_CFRAC) {
					result = frac(num, den, false, false);
				} else {
					result = frac(num, den);
				}
				int dl = ast.d[node];
				int dr = ast.e[node];
				if (dl != Ast.NONE || dr != Ast.NONE) result = delimited(result, dl, dr);
				return result;
			}
			case Ast.SQRT: {
				Box c = inStyle(ast, ast.a[node], style, true);
				int d = ast.b[node];
				Box idx = null;
				if (d != Ast.NONE) {
					idx = scale(inStyle(ast, d, SS, true), styleRatio(style, SS));
				}
				return sqrt(c, idx);
			}
			case Ast.SCRIPT:
				return buildScript(ast, node);
			case Ast.OVER:
				return over(layout(ast, ast.a[node]));
			case Ast.UNDER:
				return under(layout(ast, ast.a[node]));
			case Ast.OVERBRACE:
				return overbrace(layout(ast, ast.a[node]));
			case Ast.UNDERBRACE:
				return underbrace(layout(ast, ast.a[node]));
			case Ast.STACK: {
				Box base = layout(ast, ast.a[node]);
				Box top = layout(ast, ast.b[node]);
				return stack(base, top);
			}
			case Ast.PHANTOM: {
				int mode = ast.b[node];
				if (mode == 3) return new PhantomBox(0, fm.height(0x28), fm.depth(0x28));
				Box inner = layout(ast, ast.a[node]);
				if (mode == 1) return new PhantomBox(inner.width, 0, 0);
				if (mode == 2) return new PhantomBox(0, inner.height, inner.depth);
				return new PhantomBox(inner.width, inner.height, inner.depth);
			}
			case Ast.NOT:
				return not(layout(ast, ast.a[node]));
			case Ast.LEFT:
				return left(ast, node);
			case Ast.ACCENT: {
				int id = ast.b[node];
				Box base = layout(ast, ast.a[node]);
				int i = AccentTable.indexOf(id);
				if (i < 0) return wideAccent(base, id == SymbolTable.ID_WIDEHAT ? 0x302 : 0x303);
				int acp = AccentTable.cp(i);
				boolean stretchy = id == SymbolTable.ID_OVERLEFTARROW || id == SymbolTable.ID_OVERRIGHTARROW;
				if (stretchy && base.width > fm.unitsPerEm) return wideAccent(base, acp);
				return accent(base, acp);
			}
			case Ast.XACCENT: {
				Box xb = layout(ast, ast.a[node]);
				int xcp = ast.b[node];
				if (xb.width > fm.unitsPerEm && fm.assembly(xcp, true) != null) return wideAccent(xb, xcp);
				return topAccent(xb, xcp);
			}
			case Ast.UACCENT:
				return bottomAccent(layout(ast, ast.a[node]), ast.b[node]);
			case Ast.WACCENT:
				return wideAccentEx(layout(ast, ast.a[node]), ast.b[node]);
			case Ast.MATHCHOICE: {
				int pick = style == D ? ast.a[node] : style == T ? ast.b[node] : style == S ? ast.c[node] : ast.d[node];
				return pick == Ast.NONE ? new RuleBox(0, 0, 0) : layout(ast, pick);
			}
			case Ast.ACCENTSET: {
				Box base = layout(ast, ast.a[node]);
				Box acc = layout(ast, ast.b[node]);
				return acc.kind == Box.GLYPH ? topAccent(base, ((GlyphBox) acc).cp) : stack(base, acc);
			}
			case Ast.UNDERACCENT: {
				Box base = layout(ast, ast.a[node]);
				Box acc = layout(ast, ast.b[node]);
				return acc.kind == Box.GLYPH ? bottomAccent(base, ((GlyphBox) acc).cp) : underStack(base, acc);
			}
			case Ast.PRESCRIPT: {
				Box base = layout(ast, ast.c[node]);
				int ns = style <= T ? S : SS;
				int pct = styleRatio(style, ns);
				Box sup = ast.a[node] == Ast.NONE ? null : scale(inStyle(ast, ast.a[node], ns, false), pct);
				Box sub = ast.b[node] == Ast.NONE ? null : scale(inStyle(ast, ast.b[node], ns, false), pct);
				return prescript(base, sub, sup);
			}
			case Ast.SIDESET: {
				Box base = layout(ast, ast.e[node]);
				int ns = style <= T ? S : SS;
				int pct = styleRatio(style, ns);
				Box lsup = ast.b[node] == Ast.NONE ? null : scale(inStyle(ast, ast.b[node], ns, false), pct);
				Box lsub = ast.a[node] == Ast.NONE ? null : scale(inStyle(ast, ast.a[node], ns, false), pct);
				Box rsup = ast.d[node] == Ast.NONE ? null : scale(inStyle(ast, ast.d[node], ns, false), pct);
				Box rsub = ast.c[node] == Ast.NONE ? null : scale(inStyle(ast, ast.c[node], ns, false), pct);
				Box left = scriptStack(base, lsub, lsup);
				Box right = scriptStack(base, rsub, rsup);
				if (left == null) return right == null ? base : HBox.of(new Box[]{base, right});
				if (right == null) return HBox.of(new Box[]{left, base});
				return HBox.of(new Box[]{left, base, right});
			}
			case Ast.SMASHOP: {
				int arg = ast.a[node];
				int baseNode = arg;
				while (baseNode != Ast.NONE) {
					byte k = ast.kind[baseNode];
					if (k == Ast.SCRIPT || k == Ast.SEQ) baseNode = ast.a[baseNode];
					else break;
				}
				Box full = layout(ast, arg);
				Box base = baseNode == Ast.NONE ? full : layout(ast, baseNode);
				return new SmashOpBox(full, base.width, -(full.width - base.width) / 2);
			}
			case Ast.CLASS:
				return layout(ast, ast.a[node]);
			case Ast.FAMILY: {
				byte saved = family;
				family = FamilyTable.of(ast.b[node]);
				Box b = layout(ast, ast.a[node]);
				family = saved;
				return b;
			}
			case Ast.DELIM:
				return bigDelim(ast.a[node], ast.b[node]);
			case Ast.ERROR:
				return errorBox(ast.at[node], ast.c[node], ast.b[node]);
			case Ast.STYLE: {
				int code = ast.b[node];
				int ns = code == Ast.S_DISPLAY ? D : code == Ast.S_TEXT ? T : code == Ast.S_SCRIPT ? S : SS;
				Box b = inStyle(ast, ast.a[node], ns, false);
				if (code == Ast.S_SCRIPT) return scale(b, styleRatio(style, S));
				if (code == Ast.S_SCRIPTSCRIPT) return scale(b, styleRatio(style, SS));
				return b;
			}
			case Ast.ENV:
				return env(ast, node);
			case Ast.GENFRAC: {
				int style = ast.c[node];
				Box num = layout(ast, ast.a[node]);
				Box den = layout(ast, ast.b[node]);
				Box body = frac(num, den, style >= 1, false);
				int dl = ast.d[node];
				int dr = ast.e[node];
				if (dl != Ast.NONE || dr != Ast.NONE) body = delimited(body, dl, dr);
				return body;
			}
			case Ast.BOXED:
				return boxed(layout(ast, ast.a[node]));
			case Ast.CLAP:
				return clap(layout(ast, ast.a[node]), ast.b[node]);
			case Ast.XARROW:
				return xarrow(ast.a[node], ast.b[node] == Ast.NONE ? null : layout(ast, ast.b[node]), ast.c[node] == Ast.NONE ? null : layout(ast, ast.c[node]));
			case Ast.SMASH:
				return new SmashBox(layout(ast, ast.a[node]));
			case Ast.CANCEL: {
				int mode = ast.b[node];
				Box value = ast.c[node] == Ast.NONE ? null : layout(ast, ast.c[node]);
				return cancel(layout(ast, ast.a[node]), mode, value);
			}
			case Ast.RULE: {
				int w = ast.a[node] * fm.unitsPerEm / 1000;
				int h = ast.b[node] * fm.unitsPerEm / 1000;
				int raise = ast.c[node] * fm.unitsPerEm / 1000;
				Box rule = new RuleBox(w, h, 0);
				if (raise == 0) return rule;
				return HBox.kerned(new Box[]{rule}, new int[]{raise}, new int[]{0});
			}
			case Ast.RAISE: {
				Box b = layout(ast, ast.a[node]);
				int amount = ast.b[node] * fm.unitsPerEm / 1000;
				if (amount == 0) return b;
				return HBox.kerned(new Box[]{b}, new int[]{amount}, new int[]{0});
			}
			case Ast.DIMSPACE:
			case Ast.MIDDLE:
				return HBox.of(new Box[0]);
			case Ast.ATTR: {
				Box c = layout(ast, ast.a[node]);
				int fg = ast.b[node];
				int bg = ast.c[node];
				int frame = ast.d[node];
				int pad = ast.e[node] > 0 ? ast.e[node] * fm.unitsPerEm / 1000 : 0;
				int frameW = frame != StyledBox.NOCOLOR ? fm.constant(FontMetrics.C.FRACTION_RULE_THICKNESS) : 0;
				return new StyledBox(c, fg, bg, frame, frameW, pad, null, null);
			}
			case Ast.TAG: {
				Box c = layout(ast, ast.a[node]);
				String cls = ast.b[node] < 0 ? null : ast.str[ast.b[node]];
				String tid = ast.c[node] < 0 ? null : ast.str[ast.c[node]];
				return new StyledBox(c, StyledBox.NOCOLOR, StyledBox.NOCOLOR, StyledBox.NOCOLOR, 0, 0, cls, tid);
			}
			case Ast.COLORDECL:
				return new StyledBox(layout(ast, ast.a[node]), ast.b[node], StyledBox.NOCOLOR, StyledBox.NOCOLOR, 0, 0, null, null);
			case Ast.SPACE:
				return HBox.of(new Box[0]);
		}
		return HBox.of(new Box[0]);
	}
	private byte classOf(Ast ast, int node) {
		if (node < 0) return SpacingTable.ORD;
		byte m = ast.klass[node];
		if (m != 0) return (byte) (m - 1);
		byte r = computeClass(ast, node);
		ast.klass[node] = (byte) (r + 1);
		return r;
	}
	private byte computeClass(Ast ast, int node) {
		switch (ast.kind[node]) {
			case Ast.CLASS:
				return classOfId(ast.b[node]);
			case Ast.GLYPH:
				if (ast.b[node] == Ast.LARGE_OP || ast.b[node] == Ast.LARGE_OP_NL) return SpacingTable.OP;
				return SpacingTable.of(ast.a[node]);
			case Ast.TEXT:
				if (ast.b[node] == Ast.LARGE_OP || ast.b[node] == Ast.LARGE_OP_NL) return SpacingTable.OP;
				return SpacingTable.INNER;
			case Ast.SCRIPT:
				return classOf(ast, ast.a[node]);
			case Ast.STACK:
				if (ast.c[node] >= 0) return SpacingTable.REL;
				return SpacingTable.ORD;
			case Ast.PHANTOM:
			case Ast.NOT:
			case Ast.ACCENT:
			case Ast.FAMILY:
				return SpacingTable.ORD;
			case Ast.DELIM: {
				int id = ast.b[node];
				if (id == SymbolTable.ID_BIGL || id == SymbolTable.ID_BIGL_224 || id == SymbolTable.ID_BIGGL || id == SymbolTable.ID_BIGGL_230) return SpacingTable.OPEN;
				if (id == SymbolTable.ID_BIGR || id == SymbolTable.ID_BIGR_225 || id == SymbolTable.ID_BIGGR || id == SymbolTable.ID_BIGGR_231) return SpacingTable.CLOSE;
				if (id == SymbolTable.ID_BIGM || id == SymbolTable.ID_BIGM_226 || id == SymbolTable.ID_BIGGM || id == SymbolTable.ID_BIGGM_232) return SpacingTable.REL;
				return SpacingTable.of(ast.a[node]);
			}
			case Ast.FRAC:
			case Ast.SQRT:
			case Ast.OVER:
			case Ast.UNDER:
			case Ast.LEFT:
				return SpacingTable.INNER;
			case Ast.OVERBRACE:
			case Ast.UNDERBRACE:
				return SpacingTable.ORD;
			case Ast.STYLE:
				return classOf(ast, ast.a[node]);
			case Ast.ENV:
				return SpacingTable.INNER;
			case Ast.GENFRAC:
				return SpacingTable.INNER;
			case Ast.BOXED:
				return SpacingTable.ORD;
			case Ast.XARROW:
				return SpacingTable.REL;
			case Ast.CLAP:
			case Ast.SMASH:
				return classOf(ast, ast.a[node]);
			case Ast.RAISE:
				return classOf(ast, ast.a[node]);
			case Ast.ATTR:
				return SpacingTable.ORD;
			case Ast.TAG:
				return classOf(ast, ast.a[node]);
			case Ast.COLORDECL: {
				int c = ast.a[node];
				if (c != Ast.NONE && ast.kind[c] == Ast.SEQ && ast.a[c] != Ast.NONE) c = ast.a[c];
				return c == Ast.NONE ? SpacingTable.ORD : classOf(ast, c);
			}
			case Ast.ACCENTSET:
			case Ast.UNDERACCENT:
				return ast.a[node] == Ast.NONE ? SpacingTable.ORD : classOf(ast, ast.a[node]);
			case Ast.PRESCRIPT:
				return ast.c[node] == Ast.NONE ? SpacingTable.ORD : classOf(ast, ast.c[node]);
			case Ast.SIDESET:
				return ast.e[node] == Ast.NONE ? SpacingTable.ORD : classOf(ast, ast.e[node]);
			case Ast.SMASHOP:
				return ast.a[node] == Ast.NONE ? SpacingTable.ORD : classOf(ast, ast.a[node]);
			case Ast.CANCEL:
			case Ast.RULE:
			case Ast.DIMSPACE:
			case Ast.MIDDLE:
				return SpacingTable.ORD;
			case Ast.ERROR:
				return SpacingTable.ORD;
			default:
				return SpacingTable.ORD;
		}
	}
	static byte classOfId(int id) {
		switch (id) {
			case SymbolTable.ID_MATHOP: return SpacingTable.OP;
			case SymbolTable.ID_MATHREL: return SpacingTable.REL;
			case SymbolTable.ID_MATHBIN: return SpacingTable.BIN;
		}
		return SpacingTable.ORD;
	}
	private Box glyph(Ast ast, int node) {
		int cp = mathLetter(ast.a[node], family);
		if (!fm.hasGlyph(cp, family)) cp = plainFallback(ast.a[node], cp);
		if (ast.b[node] == Ast.LARGE_OP || ast.b[node] == Ast.LARGE_OP_NL) {
			if (style == D) return GlyphBox.of(fm, fm.resolve(cp, fm.constant(FontMetrics.C.DISPLAY_OPERATOR_MIN_HEIGHT)), family);
		}
		return GlyphBox.of(fm, cp, family);
	}
	private int plainFallback(int raw, int styled) {
		int m = mathLetter(raw, FamilyTable.MATH);
		if (fm.hasGlyph(m, family)) return m;
		if (fm.hasGlyph(raw, family)) return raw;
		return 0xFFFD;
	}
	private Box buildSeq(Ast ast, int head) {
		int first = ast.a[head];
		if (first == Ast.NONE) return HBox.of(new Box[0]);
		int n = 0;
		for (int c = first; c != Ast.NONE; c = ast.next[c]) {
			byte k = ast.kind[c];
			if (k != Ast.SPACE && k != Ast.DIMSPACE) n++;
		}
		if (n == 0) return HBox.of(new Box[0]);
		if (n == 1) {
			for (int c = first; c != Ast.NONE; c = ast.next[c]) {
				byte k = ast.kind[c];
				if (k != Ast.SPACE && k != Ast.DIMSPACE) return layout(ast, c);
			}
		}
		Box[] kids = new Box[n];
		int[] kern = new int[n];
		int mu = fm.unitsPerEm / SpacingTable.MU_DIV;
		int pending = 0;
		int i = 0;
		byte prev = -1;
		for (int c = first; c != Ast.NONE; c = ast.next[c]) {
			byte kc = ast.kind[c];
			if (kc == Ast.SPACE) {
				pending += ast.a[c] * mu;
				continue;
			}
			if (kc == Ast.DIMSPACE) {
				pending += ast.a[c] * fm.unitsPerEm / 1000;
				continue;
			}
			byte cls = classOf(ast, c);
			kids[i] = layout(ast, c);
			if (i > 0) kern[i - 1] = pending + SpacingTable.space(prev, cls, mu);
			pending = 0;
			prev = cls;
			i++;
		}
		return HBox.kerned(kids, new int[n], kern);
	}
	private Box buildText(Ast ast, int seqNode) {
		int first = ast.a[seqNode];
		if (first == Ast.NONE) return HBox.of(new Box[0]);
		if (ast.next[first] == Ast.NONE) return textGlyph(ast.a[first]);
		int n = 0;
		for (int c = first; c != Ast.NONE; c = ast.next[c]) n++;
		Box[] kids = new Box[n];
		int i = 0;
		for (int c = first; c != Ast.NONE; c = ast.next[c]) kids[i++] = textGlyph(ast.a[c]);
		return HBox.of(kids);
	}
	private Box textGlyph(int cp) {
		if (cp == ' ') return new RuleBox(fm.unitsPerEm / 4, 0, 0);
		int plain = plainUpright(cp);
		if (family == FamilyTable.MATH) return GlyphBox.of(fm, plain, family);
		int styled = mathLetter(plain, family);
		for (int i = 0; i < 3 && !fm.hasGlyph(styled, family); i++) {
			styled = i == 0 ? mathLetter(cp, family) : i == 1 ? plain : cp;
		}
		return GlyphBox.of(fm, fm.hasGlyph(styled, family) ? styled : plain, family);
	}
	private Box buildScript(Ast ast, int node) {
		int baseNode = ast.a[node];
		Box base = layout(ast, baseNode);
		int supNode = ast.b[node];
		int subNode = ast.c[node];
		int ns = style <= T ? S : SS;
		int pct = styleRatio(style, ns);
		Box up = supNode == Ast.NONE ? null : scale(inStyle(ast, supNode, ns, false), pct);
		Box down = subNode == Ast.NONE ? null : scale(inStyle(ast, subNode, ns, false), pct);
		if (isLimits(baseNode, ast)) return limit(base, down, up);
		if (isIntegral(baseNode, ast)) return integralScript(base, down, up);
		if (up != null && down != null) return subsup(base, down, up);
		if (up != null) return sup(base, up);
		if (down != null) return sub(base, down);
		return base;
	}
	private boolean isLimits(int node, Ast ast) {
		byte k = ast.kind[node];
		if (k != Ast.GLYPH && k != Ast.TEXT && k != Ast.OVERBRACE && k != Ast.UNDERBRACE) return false;
		return ast.b[node] == Ast.LARGE_OP;
	}
	static boolean isIntegralCp(int cp) {
		return (cp >= 0x222B && cp <= 0x2233) || (cp >= 0x2A0B && cp <= 0x2A1C);
	}
	private boolean isIntegral(int node, Ast ast) {
		byte k = ast.kind[node];
		if (k != Ast.GLYPH && k != Ast.TEXT) return false;
		if (ast.b[node] != Ast.LARGE_OP && ast.b[node] != Ast.LARGE_OP_NL) return false;
		return isIntegralCp(ast.a[node]);
	}
	private Box integralScript(Box base, Box sub, Box sup) {
		if (sub == null && sup == null) return base;
		int indent = base.width * 5 / 8;
		int gap = fm.constant(FontMetrics.C.SPACE_AFTER_SCRIPT);
		int inkH = base.height + base.depth;
		int tailGuard = inkH <= 0 ? 0 : base.width * base.width / inkH;
		if (sup == null) {
			int v = subShiftDown(base, sub);
			int subX = Math.max(tailGuard, base.width + gap - indent);
			int kern = subX - base.width;
			return HBox.kerned(new Box[]{base, sub}, new int[]{0, -v}, new int[]{kern, 0});
		}
		if (sub == null) return sup(base, sup);
		int u = supShiftUp(base, sup);
		int v = subShiftDown(base, sub);
		int gapMin = fm.constant(FontMetrics.C.SUB_SUPERSCRIPT_GAP_MIN);
		int clearance = (u - sup.depth) - (sub.height - v);
		if (clearance < gapMin) {
			int need = gapMin - clearance;
			u += (need + 1) / 2;
			v += need / 2;
		}
		int topMax = fm.constant(FontMetrics.C.SUPERSCRIPT_BOTTOM_MAX_WITH_SUBSCRIPT);
		if (u - sup.depth < topMax) u = sup.depth + topMax;
		int kSup = base.italic + gap + overhang(sup);
		int supX = base.width + kSup;
		int subX = Math.max(tailGuard, supX - indent);
		Box[] kids = {base, sup, sub};
		int[] shift = {0, u, -v};
		int[] kern = {kSup, subX - (supX + sup.width), 0};
		return HBox.kerned(kids, shift, kern);
	}
	public Box left(Ast ast, int node) {
		int inner = ast.c[node];
		if (inner == Ast.NONE) return HBox.of(new Box[0]);
		if (ast.kind[inner] == Ast.SEQ && hasMiddle(ast, inner)) return middleDelimited(ast, inner, ast.a[node], ast.b[node]);
		return delimited(layout(ast, inner), ast.a[node], ast.b[node]);
	}
	private static boolean hasMiddle(Ast ast, int seq) {
		for (int c = ast.a[seq]; c != Ast.NONE; c = ast.next[c]) {
			if (ast.kind[c] == Ast.MIDDLE) return true;
		}
		return false;
	}
	private Box middleDelimited(Ast ast, int seq, int leftCp, int rightCp) {
		int count = 0;
		for (int c = ast.a[seq]; c != Ast.NONE; c = ast.next[c]) {
			if (ast.kind[c] == Ast.MIDDLE) count++;
		}
		Box[] parts = new Box[count + 1];
		int[] mids = new int[count];
		int bi = 0;
		int mi = 0;
		int segFirst = ast.a[seq];
		int prev = Ast.NONE;
		for (int c = ast.a[seq]; c != Ast.NONE; c = ast.next[c]) {
			if (ast.kind[c] != Ast.MIDDLE) {
				prev = c;
				continue;
			}
			int save = Ast.NONE;
			if (prev != Ast.NONE) {
				save = ast.next[prev];
				ast.next[prev] = Ast.NONE;
			}
			parts[bi++] = ast.a[segFirst] == Ast.NONE ? HBox.of(new Box[0]) : layout(ast, ast.add(Ast.SEQ, segFirst, Ast.NONE, Ast.NONE, Ast.NONE));
			if (prev != Ast.NONE) ast.next[prev] = save;
			mids[mi++] = ast.a[c];
			segFirst = ast.next[c];
			prev = Ast.NONE;
		}
		parts[bi++] = segFirst == Ast.NONE ? HBox.of(new Box[0]) : layout(ast, ast.add(Ast.SEQ, segFirst, Ast.NONE, Ast.NONE, Ast.NONE));
		int needed = 0;
		for (int i = 0; i < parts.length; i++) {
			int t = parts[i].totalHeight();
			if (t > needed) needed = t;
		}
		int pad = SpacingTable.DELIMITER_PAD;
		needed += pad + pad;
		Box l = leftCp == Ast.NONE ? null : delim(leftCp, needed);
		Box r = rightCp == Ast.NONE ? null : delim(rightCp, needed);
		int kl = leftCp == Ast.NONE ? 0 : DelimTable.kern(leftCp, fm.resolveInk(leftCp, needed) != leftCp);
		int kr = rightCp == Ast.NONE ? 0 : DelimTable.kern(rightCp, fm.resolveInk(rightCp, needed) != rightCp);
		Box[] md = new Box[mids.length];
		int[] mk = new int[mids.length];
		for (int i = 0; i < mids.length; i++) {
			md[i] = mids[i] == Ast.NONE ? null : delim(mids[i], needed);
			mk[i] = mids[i] == Ast.NONE ? 0 : DelimTable.kern(mids[i], fm.resolveInk(mids[i], needed) != mids[i]);
		}
		int len = parts.length + mids.length + (l == null ? 0 : 1) + (r == null ? 0 : 1);
		Box[] kids = new Box[len];
		int[] shift = new int[len];
		int[] kern = new int[len];
		int center = 0;
		for (int i = 0; i < parts.length; i++) {
			int c = (parts[i].height - parts[i].depth) / 2;
			if (c > center) center = c;
		}
		int k = 0;
		if (l != null) {
			kids[k] = l;
			shift[k] = center - (l.height - l.depth) / 2;
			kern[k] = kl;
			k++;
		}
		for (int i = 0; i < parts.length; i++) {
			kids[k] = parts[i];
			shift[k] = center - (parts[i].height - parts[i].depth) / 2;
			kern[k] = i < mids.length && md[i] != null ? mk[i] : kr;
			k++;
			if (i < mids.length && md[i] != null) {
				kids[k] = md[i];
				shift[k] = center - (md[i].height - md[i].depth) / 2;
				kern[k] = mk[i];
				k++;
			}
		}
		if (r != null) {
			kids[k] = r;
			shift[k] = center - (r.height - r.depth) / 2;
			kern[k] = 0;
		} else if (k > 0) kern[k - 1] = 0;
		return HBox.kerned(kids, shift, kern);
	}
	private Box delimited(Box inner, int leftCp, int rightCp) {
		int pad = SpacingTable.DELIMITER_PAD;
		int needed = inner.totalHeight() + pad + pad;
		Box l = leftCp == Ast.NONE ? null : delim(leftCp, needed);
		Box r = rightCp == Ast.NONE ? null : delim(rightCp, needed);
		if (l == null && r == null) return inner;
		int kl = leftCp == Ast.NONE ? 0 : fm.resolveInk(leftCp, needed) != leftCp ? DelimTable.stretch(leftCp) : DelimTable.base(leftCp);
		int kr = rightCp == Ast.NONE ? 0 : fm.resolveInk(rightCp, needed) != rightCp ? DelimTable.stretch(rightCp) : DelimTable.base(rightCp);
		int center = (inner.height - inner.depth) / 2;
		if (l == null) {
			int cs = (r.height - r.depth) / 2;
			return HBox.kerned(new Box[]{inner, r}, new int[]{0, center - cs}, new int[]{kr, 0});
		}
		if (r == null) {
			int cs = (l.height - l.depth) / 2;
			return HBox.kerned(new Box[]{l, inner}, new int[]{center - cs, 0}, new int[]{kl, 0});
		}
		int ls = (l.height - l.depth) / 2;
		int rs = (r.height - r.depth) / 2;
		Box[] kids = {l, inner, r};
		int[] shift = {center - ls, 0, center - rs};
		return HBox.kerned(kids, shift, new int[]{kl, kr, 0});
	}
	private Box delim(int cp, int needed) {
		int resolved = fm.resolveInk(cp, needed);
		int ink = fm.height(resolved) + fm.depth(resolved);
		if (ink < needed) {
			FontMetrics.Assembly asm = fm.assembly(cp, false);
			if (asm != null) {
				Box assembled = assemble(asm, needed);
				if (assembled != null) return assembled;
			}
			return GlyphBox.scaled(fm, resolved, needed * GlyphBox.FULL / ink);
		}
		return GlyphBox.of(fm, resolved);
	}
	private Box assemble(FontMetrics.Assembly asm, int needed) {
		int[] pl = asm.build(needed);
		int total = pl[0];
		int n = (pl.length - 1) / 2;
		if (n <= 1 || total < needed) return null;
		int axis = fm.constant(FontMetrics.C.AXIS_HEIGHT);
		int h = total / 2 + axis;
		int d = total - h;
		Box[] kids = new Box[n];
		int[] off = new int[n];
		for (int i = 0; i < n; i++) {
			int pcp = pl[1 + i * 2];
			int lo = pl[2 + i * 2];
			if (!fm.hasGlyph(pcp)) return null;
			kids[i] = GlyphBox.of(fm, pcp);
			off[i] = lo - d - fm.inkBottom(pcp);
		}
		return VBox.of(kids, off);
	}
	public Box bigDelim(int cp, int id) {
		int level = bigLevel(id);
		int needed = (level + 1) * fm.unitsPerEm * 3 / 5;
		return GlyphBox.of(fm, fm.resolve(cp, needed));
	}
	private static int bigLevel(int id) {
		if (id == SymbolTable.ID_BIG || id == SymbolTable.ID_BIGL || id == SymbolTable.ID_BIGR || id == SymbolTable.ID_BIGM) return 1;
		if (id == SymbolTable.ID_BIG_18 || id == SymbolTable.ID_BIGL_224 || id == SymbolTable.ID_BIGR_225 || id == SymbolTable.ID_BIGM_226) return 2;
		if (id == SymbolTable.ID_BIGG || id == SymbolTable.ID_BIGGL || id == SymbolTable.ID_BIGGR || id == SymbolTable.ID_BIGGM) return 3;
		return 4;
	}
	public Box stack(Box base, Box top) {
		int gap = STACK_ABOVE_GAP;
		int shift = base.height + top.depth + gap;
		return VBox.of(new Box[]{top, base}, new int[]{shift, 0}, VBox.CENTER);
	}
	public Box not(Box b) {
		Box slash = GlyphBox.of(fm, 0x338);
		int baseTa = b.kind == Box.GLYPH ? fm.topAccent(((GlyphBox) b).cp) : b.width / 2;
		int x = baseTa - fm.topAccent(0x338);
		if (x < 0) x = 0;
		Box pad = new RuleBox(x, 0, 0);
		Box[] kids = {pad, slash, b};
		int[] offsets = {0, (b.height - b.depth) / 2, 0};
		int[] kerns = {0, -(x + slash.width), 0};
		return HBox.kerned(kids, offsets, kerns);
	}
	public Box overbrace(Box b) {
		Box brace = hbrace(0x23DE, b.width);
		int barY = b.height + BRACE_ABOVE_GAP - inkBottomOf(brace);
		Box[] kids = {brace, b};
		int[] offset = {barY, 0};
		return VBox.of(kids, offset);
	}
	public Box underbrace(Box b) {
		Box brace = hbrace(0x23DF, b.width);
		int barY = -(b.depth + BRACE_BELOW_GAP) - inkTopOf(brace);
		Box[] kids = {brace, b};
		int[] offset = {barY, 0};
		return VBox.of(kids, offset);
	}
	private int inkTopOf(Box b) {
		switch (b.kind) {
			case Box.GLYPH: {
				GlyphBox g = (GlyphBox) b;
				return fm.inkTop(g.cp) * g.percent / GlyphBox.FULL;
			}
			case Box.HBOX: {
				HBox h = (HBox) b;
				int t = Integer.MIN_VALUE;
				for (int i = 0; i < h.children.length; i++) {
					int v = inkTopOf(h.children[i]) + h.shift[i];
					if (v > t) t = v;
				}
				return t == Integer.MIN_VALUE ? b.height : t;
			}
			case Box.VBOX: {
				VBox v = (VBox) b;
				int t = Integer.MIN_VALUE;
				for (int i = 0; i < v.children.length; i++) {
					int x = inkTopOf(v.children[i]) + v.offset[i];
					if (x > t) t = x;
				}
				return t == Integer.MIN_VALUE ? b.height : t;
			}
		}
		return b.height;
	}
	private int inkBottomOf(Box b) {
		switch (b.kind) {
			case Box.GLYPH: {
				GlyphBox g = (GlyphBox) b;
				return fm.inkBottom(g.cp) * g.percent / GlyphBox.FULL;
			}
			case Box.HBOX: {
				HBox h = (HBox) b;
				int t = Integer.MAX_VALUE;
				for (int i = 0; i < h.children.length; i++) {
					int v = inkBottomOf(h.children[i]) + h.shift[i];
					if (v < t) t = v;
				}
				return t == Integer.MAX_VALUE ? -b.depth : t;
			}
			case Box.VBOX: {
				VBox v = (VBox) b;
				int t = Integer.MAX_VALUE;
				for (int i = 0; i < v.children.length; i++) {
					int x = inkBottomOf(v.children[i]) + v.offset[i];
					if (x < t) t = x;
				}
				return t == Integer.MAX_VALUE ? -b.depth : t;
			}
		}
		return -b.depth;
	}
	private Box hbrace(int cp, int needed) {
		return hstretch(cp, needed, false);
	}
	private Box hstretch(int cp, int needed, boolean fit) {
		int resolved = fm.resolveH(cp, needed);
		int w = fm.width(resolved);
		if (w >= needed) {
			if (fit && w > needed) return GlyphBox.scaled(fm, resolved, needed * GlyphBox.FULL / w);
			return GlyphBox.of(fm, resolved);
		}
		FontMetrics.Assembly asm = fm.assembly(cp, true);
		if (asm != null) {
			Box a = hassemble(asm, needed);
			if (a != null) return a;
		}
		return GlyphBox.scaled(fm, resolved, needed * GlyphBox.FULL / w);
	}
	private Box hassemble(FontMetrics.Assembly asm, int needed) {
		int[] pl = asm.build(needed);
		int total = pl[0];
		int n = (pl.length - 1) / 2;
		if (n <= 1 || total < needed) return null;
		Box[] kids = new Box[n];
		int[] kern = new int[n];
		int prevRight = 0;
		for (int i = 0; i < n; i++) {
			int pcp = pl[1 + i * 2];
			if (!fm.hasGlyph(pcp)) return null;
			kids[i] = GlyphBox.of(fm, pcp);
			int origin = pl[2 + i * 2] - fm.inkLeft(pcp);
			if (i > 0) kern[i - 1] = origin - prevRight;
			prevRight = origin + fm.width(pcp);
		}
		return HBox.kerned(kids, new int[n], kern);
	}
	public Box over(Box b) {
		int base = fm.constant(FontMetrics.C.ACCENT_BASE_HEIGHT);
		int barY = (b.height > base ? b.height : base) + fm.constant(FontMetrics.C.OVERBAR_VERTICAL_GAP);
		Box bar = RuleBox.horizontal(b.width, fm.constant(FontMetrics.C.OVERBAR_RULE_THICKNESS));
		return VBox.of(new Box[]{bar, b}, new int[]{barY, 0});
	}
	public Box accent(Box b, int cp) {
		int base = fm.constant(FontMetrics.C.ACCENT_BASE_HEIGHT);
		int flatten = fm.constant(FontMetrics.C.FLATTENED_ACCENT_BASE_HEIGHT);
		if (b.height > flatten) base = flatten;
		int target = (b.height > base ? b.height : base) + fm.constant(FontMetrics.C.OVERBAR_VERTICAL_GAP) + AccentTable.GAP + AccentCompat.gap(cp);
		Box accent = GlyphBox.of(fm, cp);
		int shift = target - AccentTable.bottomOf(cp);
		int baseTa = b.kind == Box.GLYPH ? fm.topAccent(((GlyphBox) b).cp) : b.width / 2;
		int x = baseTa - fm.topAccent(cp);
		if (x < 0) x = 0;
		Box pad = new RuleBox(x, 0, 0);
		Box[] kids = {pad, accent, b};
		int[] offsets = {0, shift, 0};
		int[] kerns = {0, -(x + accent.width), 0};
		return HBox.kerned(kids, offsets, kerns);
	}
	public Box wideAccent(Box b, int cp) {
		Box accent = hstretch(cp, b.width, true);
		int base = fm.constant(FontMetrics.C.ACCENT_BASE_HEIGHT);
		int flatten = fm.constant(FontMetrics.C.FLATTENED_ACCENT_BASE_HEIGHT);
		if (b.height > flatten) base = flatten;
		int target = (b.height > base ? b.height : base) + fm.constant(FontMetrics.C.OVERBAR_VERTICAL_GAP) + AccentCompat.wideGap(cp);
		int shift = target - inkBottomOf(accent);
		int baseX = b.kind == Box.GLYPH ? fm.topAccent(((GlyphBox) b).cp) : b.width / 2;
		int x = baseX - accent.width / 2;
		if (x < 0) x = 0;
		Box pad = new RuleBox(x, 0, 0);
		return HBox.kerned(new Box[]{pad, accent, b}, new int[]{0, shift, 0}, new int[]{0, -(x + accent.width), 0});
	}
	public Box topAccent(Box b, int cp) {
		int base = fm.constant(FontMetrics.C.ACCENT_BASE_HEIGHT);
		int flatten = fm.constant(FontMetrics.C.FLATTENED_ACCENT_BASE_HEIGHT);
		if (b.height > flatten) base = flatten;
		int target = (b.height > base ? b.height : base) + fm.constant(FontMetrics.C.OVERBAR_VERTICAL_GAP) + AccentCompat.gap(cp);
		Box accent = GlyphBox.of(fm, cp);
		int shift = target - fm.inkBottom(cp);
		int baseX = b.kind == Box.GLYPH ? fm.topAccent(((GlyphBox) b).cp) : b.width / 2;
		int x = baseX - fm.topAccent(cp);
		if (x < 0) x = 0;
		Box pad = new RuleBox(x, 0, 0);
		return HBox.kerned(new Box[]{pad, accent, b}, new int[]{0, shift, 0}, new int[]{0, -(x + accent.width), 0});
	}
	public Box bottomAccent(Box b, int cp) {
		int gap = fm.constant(FontMetrics.C.UNDERBAR_VERTICAL_GAP);
		Box accent = GlyphBox.of(fm, cp);
		int shift = -(b.depth + gap) - fm.inkTop(cp);
		int baseX = b.kind == Box.GLYPH ? fm.topAccent(((GlyphBox) b).cp) : b.width / 2;
		int x = baseX - fm.topAccent(cp);
		if (x < 0) x = 0;
		Box pad = new RuleBox(x, 0, 0);
		return HBox.kerned(new Box[]{pad, accent, b}, new int[]{0, shift, 0}, new int[]{0, -(x + accent.width), 0});
	}
	public Box wideAccentEx(Box b, int cp) {
		return wideAccent(b, cp);
	}
	public Box underStack(Box base, Box acc) {
		int gap = fm.constant(FontMetrics.C.UNDERBAR_VERTICAL_GAP);
		return VBox.of(new Box[]{acc, base}, new int[]{-(base.depth + gap + acc.height), 0}, VBox.CENTER);
	}
	public Box prescript(Box base, Box sub, Box sup) {
		Box scripts = scriptStack(base, sub, sup);
		return scripts == null ? base : HBox.of(new Box[]{scripts, base});
	}
	public Box scriptStack(Box base, Box sub, Box sup) {
		if (sub == null) return sup == null ? null : VBox.of(new Box[]{sup}, new int[]{supShiftUp(base, sup)}, VBox.LEFT);
		if (sup == null) return VBox.of(new Box[]{sub}, new int[]{-subShiftDown(base, sub)}, VBox.LEFT);
		int u = supShiftUp(base, sup);
		int v = subShiftDown(base, sub);
		int gapMin = fm.constant(FontMetrics.C.SUB_SUPERSCRIPT_GAP_MIN);
		int clearance = (u - sup.depth) - (sub.height - v);
		if (clearance < gapMin) {
			int need = gapMin - clearance;
			u += (need + 1) / 2;
			v += need / 2;
		}
		return VBox.of(new Box[]{sub, sup}, new int[]{-v, u}, VBox.LEFT);
	}
	public Box under(Box b) {
		int gap = fm.constant(FontMetrics.C.UNDERBAR_VERTICAL_GAP);
		int rule = fm.constant(FontMetrics.C.UNDERBAR_RULE_THICKNESS);
		Box bar = RuleBox.horizontal(b.width, rule);
		return VBox.of(new Box[]{bar, b}, new int[]{-(b.depth + gap + rule), 0});
	}
	private Box scale(Box b, int percent) {
		if (percent == GlyphBox.FULL) return b;
		switch (b.kind) {
			case Box.GLYPH: {
				GlyphBox g = (GlyphBox) b;
				return GlyphBox.scaled(fm, g.cp, g.percent * percent / GlyphBox.FULL, g.family);
			}
			case Box.RULE:
				return new RuleBox(b.width * percent / GlyphBox.FULL, b.height * percent / GlyphBox.FULL, b.depth * percent / GlyphBox.FULL);
			case Box.HBOX: {
				HBox h = (HBox) b;
				Box[] kids = new Box[h.children.length];
				int[] shift = new int[kids.length];
				for (int i = 0; i < kids.length; i++) {
					kids[i] = scale(h.children[i], percent);
					shift[i] = h.shift[i] * percent / GlyphBox.FULL;
				}
				if (h.kern == null) return HBox.of(kids, shift);
				int[] kern = new int[kids.length];
				for (int i = 0; i < kids.length; i++) {
					kern[i] = h.kern[i] * percent / GlyphBox.FULL;
				}
				return HBox.kerned(kids, shift, kern);
			}
			case Box.VBOX: {
				VBox v = (VBox) b;
				Box[] kids = new Box[v.children.length];
				int[] off = new int[kids.length];
				for (int i = 0; i < kids.length; i++) {
					kids[i] = scale(v.children[i], percent);
					off[i] = v.offset[i] * percent / GlyphBox.FULL;
				}
				return VBox.of(kids, off, v.align);
			}
			case Box.ERROR:
				return new ErrorBox(b.width * percent / GlyphBox.FULL, b.height * percent / GlyphBox.FULL, b.depth * percent / GlyphBox.FULL);
			case Box.SMASH:
				return new SmashBox(scale(((SmashBox) b).child, percent));
			case Box.LINE: {
				LineBox lb = (LineBox) b;
				return new LineBox(lb.width * percent / GlyphBox.FULL, lb.height * percent / GlyphBox.FULL, lb.depth * percent / GlyphBox.FULL, lb.x0 * percent / GlyphBox.FULL, lb.y0 * percent / GlyphBox.FULL, lb.x1 * percent / GlyphBox.FULL, lb.y1 * percent / GlyphBox.FULL, lb.thickness * percent / GlyphBox.FULL);
			}
			case Box.PHANTOM:
				return new PhantomBox(b.width * percent / GlyphBox.FULL, b.height * percent / GlyphBox.FULL, b.depth * percent / GlyphBox.FULL);
			case Box.SMASHOP: {
				SmashOpBox s = (SmashOpBox) b;
				return new SmashOpBox(scale(s.child, percent), s.width * percent / GlyphBox.FULL, s.shift * percent / GlyphBox.FULL);
			}
			case Box.STYLED: {
				StyledBox s = (StyledBox) b;
				return new StyledBox(scale(s.child, percent), s.fg, s.bg, s.frame, s.frameW * percent / GlyphBox.FULL, s.pad * percent / GlyphBox.FULL, s.cssClass, s.id);
			}
		}
		return b;
	}
	private Box errorBox(int pos, int len, int code) {
		return new ErrorBox(fm.unitsPerEm / 2, fm.unitsPerEm / 2, fm.unitsPerEm / 6, pos, len, code);
	}
	public Box atop(Box num, Box den) {
		int gap = fm.constant(style == D ? FontMetrics.C.STACK_DISPLAY_STYLE_GAP_MIN : FontMetrics.C.STACK_GAP_MIN);
		int numBase = den.height + gap + num.depth;
		return VBox.of(new Box[]{num, den}, new int[]{numBase, 0});
	}
	private Box env(Ast ast, int node) {
		int code = ast.a[node];
		int spec = ast.c[node];
		int rows = 0;
		for (int r = ast.b[node]; r != Ast.NONE; r = ast.next[r]) rows++;
		if (rows == 0) return HBox.of(new Box[0]);
		int cols = 0;
		for (int r = ast.b[node]; r != Ast.NONE; r = ast.next[r]) {
			int n = 0;
			for (int c = ast.a[r]; c != Ast.NONE; c = ast.next[c]) n += ast.b[c] > 0 ? ast.b[c] : 1;
			if (n > cols) cols = n;
		}
		if (cols < 1) cols = 1;
		Box[][] grid = new Box[rows][];
		int[][] spgrid = new int[rows][];
		int[][] algrid = new int[rows][];
		int[] colW = new int[cols];
		int[] extra = new int[rows];
		boolean[] ruleAbove = new boolean[rows];
		boolean anySpan = false;
		int ri = 0;
		for (int r = ast.b[node]; r != Ast.NONE; r = ast.next[r]) {
			extra[ri] = ast.b[r] < 0 ? 0 : ast.b[r] * fm.unitsPerEm;
			ruleAbove[ri] = ast.c[r] > 0;
			Box[] cells = new Box[cols];
			int[] sp = new int[cols];
			int[] al = new int[cols];
			int ci = 0;
			int spanned = 0;
			for (int c = ast.a[r]; c != Ast.NONE && ci < cols; c = ast.next[c]) {
				cells[ci] = layout(ast, ast.a[c]);
				sp[ci] = ast.b[c] > 0 ? ast.b[c] : 1;
				al[ci] = ast.c[c];
				if (sp[ci] > 1) anySpan = true;
				spanned += sp[ci];
				ci++;
			}
			for (; spanned < cols && ci < cols; spanned++) {
				cells[ci] = HBox.of(new Box[0]);
				sp[ci] = 1;
				al[ci] = -1;
				ci++;
			}
			grid[ri] = cells;
			spgrid[ri] = sp;
			algrid[ri] = al;
			ri++;
		}
		boolean align = isAlignEnv(code);
		int colSep = align ? 0 : fm.unitsPerEm / 2;
		int gap = align ? ALIGN_ROW_GAP : fm.unitsPerEm / 2;
		for (int i = 0; i < rows; i++) {
			int col = 0;
			for (int j = 0; j < cols; j++) {
				if (spgrid[i][j] == 1 && col < cols && grid[i][j].width > colW[col]) colW[col] = grid[i][j].width;
				col += spgrid[i][j];
			}
		}
		for (int i = 0; i < rows; i++) {
			int col = 0;
			for (int j = 0; j < cols; j++) {
				int s = spgrid[i][j];
				if (s > 1) {
					int total = (s - 1) * colSep;
					for (int k = col; k < col + s && k < cols; k++) total += colW[k];
					int last = col + s - 1 < cols ? col + s - 1 : cols - 1;
					if (last >= 0 && grid[i][j].width > total) colW[last] += grid[i][j].width - total;
				}
				col += s;
			}
		}
		Box[] rowBoxes = new Box[rows];
		for (int i = 0; i < rows; i++) {
			if (!anySpan) {
				Box[] rc = new Box[cols];
				int[] kern = new int[cols];
				for (int j = 0; j < cols; j++) {
					rc[j] = padded(grid[i][j], colW[j], colAlign(code, j, spec));
					if (j < cols - 1) kern[j] = align ? ((j & 1) == 1 ? colSep + fm.unitsPerEm / 2 : 0) : colSep;
				}
				rowBoxes[i] = HBox.kerned(rc, new int[cols], kern);
			} else {
				Box[] rc = new Box[cols];
				int[] kern = new int[cols];
				int col = 0;
				int k = 0;
				for (int j = 0; j < cols; j++) {
					if (col >= cols) break;
					int s = spgrid[i][j];
					int width;
					int a;
					if (s == 1) {
						width = col < cols ? colW[col] : 0;
						a = algrid[i][j] >= 0 ? algrid[i][j] : colAlign(code, col, spec);
						col++;
					} else {
						width = (s - 1) * colSep;
						for (int q = col; q < col + s && q < cols; q++) width += colW[q];
						a = algrid[i][j] >= 0 ? algrid[i][j] : 0;
						col += s;
					}
					if (k > 0) kern[k - 1] = colSep;
					rc[k++] = padded(grid[i][j], width, a);
				}
				if (k == 0) {
					rc = new Box[0];
					kern = new int[0];
				} else if (k < cols) {
					rc = java.util.Arrays.copyOf(rc, k);
					kern = java.util.Arrays.copyOf(kern, k);
				}
				rowBoxes[i] = HBox.kerned(rc, new int[rc.length], kern);
			}
		}
		int[] off = new int[rows];
		off[0] = 0;
		for (int i = 1; i < rows; i++) off[i] = off[i - 1] - rowBoxes[i - 1].depth - gap - extra[i - 1] - rowBoxes[i].height;
		boolean trailing = ast.d[node] > 0;
		int ruleCount = trailing ? 1 : 0;
		for (int i = 0; i < rows; i++) if (ruleAbove[i]) ruleCount++;
		Box body;
		if (ruleCount == 0) {
			body = VBox.of(rowBoxes, off, VBox.LEFT);
		} else {
			int t = fm.constant(FontMetrics.C.FRACTION_RULE_THICKNESS);
			int bodyW = 0;
			for (int i = 0; i < rows; i++) if (rowBoxes[i].width > bodyW) bodyW = rowBoxes[i].width;
			Box[] kids = new Box[rows + ruleCount];
			int[] koff = new int[kids.length];
			int k = 0;
			for (int i = 0; i < rows; i++) {
				if (ruleAbove[i]) {
					int center = i == 0 ? off[0] + rowBoxes[0].height + gap / 2 : ((off[i - 1] - rowBoxes[i - 1].depth) + (off[i] + rowBoxes[i].height)) / 2;
					kids[k] = RuleBox.horizontal(bodyW, t);
					koff[k] = center - t / 2;
					k++;
				}
				kids[k] = rowBoxes[i];
				koff[k] = off[i];
				k++;
			}
			if (trailing) {
				int center = off[rows - 1] - rowBoxes[rows - 1].depth - gap / 2;
				kids[k] = RuleBox.horizontal(bodyW, t);
				koff[k] = center - t / 2;
				k++;
			}
			body = VBox.of(kids, koff, VBox.LEFT);
		}
		if (code == Ast.E_SMALLMATRIX) body = scale(body, fm.constant(FontMetrics.C.SCRIPT_PERCENT_SCALE_DOWN));
		if (!isAlignEnv(code) && body.kind == Box.VBOX) {
			VBox v = (VBox) body;
			int s = (body.depth - body.height) / 2 + fm.constant(FontMetrics.C.AXIS_HEIGHT);
			if (s != 0) {
				int[] no = new int[v.offset.length];
				for (int i = 0; i < no.length; i++) no[i] = v.offset[i] + s;
				body = VBox.of(v.children, no, v.align);
			}
		}
		int left = Ast.NONE;
		int right = Ast.NONE;
		switch (code) {
			case Ast.E_PMATRIX:
			case Ast.E_PMATRIX_S:
				left = 0x28;
				right = 0x29;
				break;
			case Ast.E_BMATRIX:
			case Ast.E_BMATRIX_S:
				left = 0x5B;
				right = 0x5D;
				break;
			case Ast.E_BMATRIX_B:
				left = 0x7B;
				right = 0x7D;
				break;
			case Ast.E_VMATRIX:
				left = 0x7C;
				right = 0x7C;
				break;
			case Ast.E_VMATRIX_B:
				left = 0x2016;
				right = 0x2016;
				break;
			case Ast.E_CASES:
				left = 0x7B;
				break;
		}
		if (left == Ast.NONE && right == Ast.NONE) return body;
		return delimited(body, left, right);
	}
	private static boolean isAlignEnv(int code) {
		switch (code) {
			case Ast.E_ALIGN:
			case Ast.E_ALIGN_S:
			case Ast.E_ALIGNED:
			case Ast.E_ALIGNAT:
			case Ast.E_ALIGNAT_S:
			case Ast.E_ALIGNEDAT:
			case Ast.E_FLALIGN:
			case Ast.E_FLALIGN_S:
			case Ast.E_GATHER:
			case Ast.E_GATHER_S:
			case Ast.E_GATHERED:
			case Ast.E_SPLIT:
				return true;
		}
		return false;
	}
	private int colAlign(int code, int col, int spec) {
		switch (code) {
			case Ast.E_CASES:
				return 1;
			case Ast.E_ARRAY:
			case Ast.E_SUBARRAY:
				if (spec < 0) return 0;
				return (spec >>> (col * 2)) & 3;
			case Ast.E_ALIGN:
			case Ast.E_ALIGN_S:
			case Ast.E_ALIGNED:
			case Ast.E_ALIGNAT:
			case Ast.E_ALIGNAT_S:
			case Ast.E_ALIGNEDAT:
			case Ast.E_FLALIGN:
			case Ast.E_FLALIGN_S:
			case Ast.E_SPLIT:
				return (col & 1) == 0 ? 2 : 1;
		}
		return 0;
	}
	private Box padded(Box cell, int width, int align) {
		int slack = width - cell.width;
		if (slack <= 0) return cell;
		int l = align == 2 ? slack : align == 0 ? slack / 2 : 0;
		int r = slack - l;
		if (l == 0) return HBox.kerned(new Box[]{cell}, new int[]{0}, new int[]{r});
		if (r == 0) return HBox.kerned(new Box[]{new RuleBox(l, 0, 0), cell}, new int[]{0, 0}, new int[]{0, 0});
		return HBox.kerned(new Box[]{new RuleBox(l, 0, 0), cell}, new int[]{0, 0}, new int[]{0, r});
	}
	public Box boxed(Box b) {
		int t = fm.constant(FontMetrics.C.FRACTION_RULE_THICKNESS);
		int pad = BOX_PAD;
		Box left = new RuleBox(t, b.height + pad, b.depth + pad);
		Box right = new RuleBox(t, b.height + pad, b.depth + pad);
		Box inner = HBox.kerned(new Box[]{left, b, right}, new int[]{0, 0, 0}, new int[]{pad, pad, 0});
		Box top = RuleBox.horizontal(inner.width, t);
		Box bottom = RuleBox.horizontal(inner.width, t);
		Box[] rows = {top, inner, bottom};
		int[] off = {inner.height, 0, -inner.depth};
		return VBox.of(rows, off);
	}
	public Box clap(Box b, int mode) {
		if (mode == 1) return HBox.kerned(new Box[]{new RuleBox(0, 0, 0), b}, new int[]{0, 0}, new int[]{-b.width, 0});
		if (mode == 2) return HBox.kerned(new Box[]{b}, new int[]{0}, new int[]{-b.width});
		int half = b.width / 2;
		return HBox.kerned(new Box[]{new RuleBox(0, 0, 0), b}, new int[]{0, 0}, new int[]{-half, -(b.width - half)});
	}
	public Box xarrow(int cp, Box above, Box below) {
		int labelW = 0;
		if (above != null && above.width > labelW) labelW = above.width;
		if (below != null && below.width > labelW) labelW = below.width;
		int needed = labelW + fm.unitsPerEm / 2;
		Box arrow = hstretch(cp, needed, false);
		if (above == null && below == null) return arrow;
		int gap = fm.constant(FontMetrics.C.STRETCH_STACK_GAP_ABOVE_MIN);
		int up = arrow.height + gap + (above == null ? 0 : above.depth);
		int down = -(arrow.depth + gap + (below == null ? 0 : below.height));
		if (above != null && below != null) {
			return VBox.of(new Box[]{above, arrow, below}, new int[]{up, 0, down}, VBox.CENTER);
		}
		if (above != null) return VBox.of(new Box[]{above, arrow}, new int[]{up, 0}, VBox.CENTER);
		return VBox.of(new Box[]{arrow, below}, new int[]{0, down}, VBox.CENTER);
	}
	public Box cancel(Box b, int mode, Box value) {
		int t = fm.constant(FontMetrics.C.RADICAL_RULE_THICKNESS);
		Box line1 = new LineBox(b.width, b.height, b.depth, 0, -b.depth, b.width, b.height, t);
		Box base;
		if (mode == 2) {
			Box line2 = new LineBox(b.width, b.height, b.depth, 0, b.height, b.width, -b.depth, t);
			base = overlay(b, new Box[]{line1, line2});
		} else if (mode == 1) {
			base = overlay(b, new Box[]{new LineBox(b.width, b.height, b.depth, 0, b.height, b.width, -b.depth, t)});
		} else {
			base = overlay(b, new Box[]{line1});
		}
		if (value == null) return base;
		int gap = fm.constant(FontMetrics.C.STRETCH_STACK_GAP_ABOVE_MIN);
		return VBox.of(new Box[]{value, base}, new int[]{base.height + gap + value.depth, 0}, VBox.CENTER);
	}
	private Box overlay(Box b, Box[] lines) {
		Box[] kids = new Box[lines.length + 1];
		int[] kern = new int[lines.length + 1];
		kids[0] = b;
		for (int i = 0; i < lines.length; i++) {
			kids[i + 1] = lines[i];
			kern[i] = -lines[i].width;
		}
		return HBox.kerned(kids, new int[kids.length], kern);
	}
}
