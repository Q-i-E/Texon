package texon.latex.engine.core;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.zip.*;
public final class FontMetrics {
	public enum C {
		ACCENT_BASE_HEIGHT("AccentBaseHeight"),
		AXIS_HEIGHT("AxisHeight"),
		DELIMITED_SUB_FORMULA_MIN_HEIGHT("DelimitedSubFormulaMinHeight"),
		DISPLAY_OPERATOR_MIN_HEIGHT("DisplayOperatorMinHeight"),
		FLATTENED_ACCENT_BASE_HEIGHT("FlattenedAccentBaseHeight"),
		FRACTION_DENOM_DISPLAY_STYLE_GAP_MIN("FractionDenomDisplayStyleGapMin"),
		FRACTION_DENOMINATOR_DISPLAY_STYLE_SHIFT_DOWN("FractionDenominatorDisplayStyleShiftDown"),
		FRACTION_DENOMINATOR_GAP_MIN("FractionDenominatorGapMin"),
		FRACTION_DENOMINATOR_SHIFT_DOWN("FractionDenominatorShiftDown"),
		FRACTION_NUM_DISPLAY_STYLE_GAP_MIN("FractionNumDisplayStyleGapMin"),
		FRACTION_NUMERATOR_DISPLAY_STYLE_SHIFT_UP("FractionNumeratorDisplayStyleShiftUp"),
		FRACTION_NUMERATOR_GAP_MIN("FractionNumeratorGapMin"),
		FRACTION_NUMERATOR_SHIFT_UP("FractionNumeratorShiftUp"),
		FRACTION_RULE_THICKNESS("FractionRuleThickness"),
		LOWER_LIMIT_BASELINE_DROP_MIN("LowerLimitBaselineDropMin"),
		LOWER_LIMIT_GAP_MIN("LowerLimitGapMin"),
		MATH_LEADING("MathLeading"),
		OVERBAR_EXTRA_ASCENDER("OverbarExtraAscender"),
		OVERBAR_RULE_THICKNESS("OverbarRuleThickness"),
		OVERBAR_VERTICAL_GAP("OverbarVerticalGap"),
		RADICAL_DEGREE_BOTTOM_RAISE_PERCENT("RadicalDegreeBottomRaisePercent"),
		RADICAL_DISPLAY_STYLE_VERTICAL_GAP("RadicalDisplayStyleVerticalGap"),
		RADICAL_EXTRA_ASCENDER("RadicalExtraAscender"),
		RADICAL_KERN_AFTER_DEGREE("RadicalKernAfterDegree"),
		RADICAL_KERN_BEFORE_DEGREE("RadicalKernBeforeDegree"),
		RADICAL_RULE_THICKNESS("RadicalRuleThickness"),
		RADICAL_VERTICAL_GAP("RadicalVerticalGap"),
		SCRIPT_PERCENT_SCALE_DOWN("ScriptPercentScaleDown"),
		SCRIPT_SCRIPT_PERCENT_SCALE_DOWN("ScriptScriptPercentScaleDown"),
		SKEWED_FRACTION_HORIZONTAL_GAP("SkewedFractionHorizontalGap"),
		SKEWED_FRACTION_VERTICAL_GAP("SkewedFractionVerticalGap"),
		SPACE_AFTER_SCRIPT("SpaceAfterScript"),
		STACK_BOTTOM_DISPLAY_STYLE_SHIFT_DOWN("StackBottomDisplayStyleShiftDown"),
		STACK_BOTTOM_SHIFT_DOWN("StackBottomShiftDown"),
		STACK_DISPLAY_STYLE_GAP_MIN("StackDisplayStyleGapMin"),
		STACK_GAP_MIN("StackGapMin"),
		STACK_TOP_DISPLAY_STYLE_SHIFT_UP("StackTopDisplayStyleShiftUp"),
		STACK_TOP_SHIFT_UP("StackTopShiftUp"),
		STRETCH_STACK_BOTTOM_SHIFT_DOWN("StretchStackBottomShiftDown"),
		STRETCH_STACK_GAP_ABOVE_MIN("StretchStackGapAboveMin"),
		STRETCH_STACK_GAP_BELOW_MIN("StretchStackGapBelowMin"),
		STRETCH_STACK_TOP_SHIFT_UP("StretchStackTopShiftUp"),
		SUB_SUPERSCRIPT_GAP_MIN("SubSuperscriptGapMin"),
		SUBSCRIPT_BASELINE_DROP_MIN("SubscriptBaselineDropMin"),
		SUBSCRIPT_SHIFT_DOWN("SubscriptShiftDown"),
		SUBSCRIPT_TOP_MAX("SubscriptTopMax"),
		SUPERSCRIPT_BASELINE_DROP_MAX("SuperscriptBaselineDropMax"),
		SUPERSCRIPT_BOTTOM_MAX_WITH_SUBSCRIPT("SuperscriptBottomMaxWithSubscript"),
		SUPERSCRIPT_BOTTOM_MIN("SuperscriptBottomMin"),
		SUPERSCRIPT_SHIFT_UP("SuperscriptShiftUp"),
		SUPERSCRIPT_SHIFT_UP_CRAMPED("SuperscriptShiftUpCramped"),
		UNDERBAR_EXTRA_DESCENDER("UnderbarExtraDescender"),
		UNDERBAR_RULE_THICKNESS("UnderbarRuleThickness"),
		UNDERBAR_VERTICAL_GAP("UnderbarVerticalGap"),
		UPPER_LIMIT_BASELINE_RISE_MIN("UpperLimitBaselineRiseMin"),
		UPPER_LIMIT_GAP_MIN("UpperLimitGapMin");
		public final String key;
		C(String key) {
			this.key = key;
		}
	}
	public static final class Chain {
		public final int[] cp;
		public final int[] advance;
		public final int size;
		Chain(int[] cp, int[] advance) {
			this.cp = cp;
			this.advance = advance;
			this.size = cp.length;
		}
		public int select(int neededHeight) {
			int lo = 1;
			int hi = size - 1;
			while (lo < hi) {
				int mid = (lo + hi) >>> 1;
				if (advance[mid] >= neededHeight) hi = mid; else lo = mid + 1;
			}
			return lo;
		}
	}
	public static final class Assembly {
		public final int[] cp;
		public final short[] sc;
		public final short[] ec;
		public final short[] fa;
		public final byte[] ex;
		public final int italics;
		Assembly(int[] cp, short[] sc, short[] ec, short[] fa, byte[] ex, int italics) {
			this.cp = cp;
			this.sc = sc;
			this.ec = ec;
			this.fa = fa;
			this.ex = ex;
			this.italics = italics;
		}
		public int size() {
			return cp.length;
		}
		public int extenders() {
			int n = 0;
			for (byte b : ex) if (b != 0) n++;
			return n;
		}
		private static final int JOINT = 32;
		private int total(int copies) {
			int lo = 0;
			int total = 0;
			int prevEc = 0;
			int prevFa = 0;
			boolean first = true;
			for (int i = 0; i < cp.length; i++) {
				int reps = ex[i] != 0 ? copies : 1;
				for (int c = 0; c < reps; c++) {
					int overlap = first ? 0 : (c > 0 ? JOINT : Math.min(prevEc, sc[i]) + JOINT);
					if (!first) lo += prevFa - overlap;
					first = false;
					prevEc = ec[i];
					prevFa = fa[i];
					total = lo + fa[i];
				}
			}
			return total;
		}
		public int[] build(int needed) {
			int slots = extenders();
			int copies = 1;
			int total = total(copies);
			if (slots > 0 && needed > total) {
				int stride = total(2) - total;
				if (stride < 1) stride = 1;
				copies += (needed - total + stride - 1) / stride;
				total = total(copies);
			}
			int n = 0;
			for (int i = 0; i < cp.length; i++) n += ex[i] != 0 ? copies : 1;
			int[] out = new int[1 + n * 2];
			out[0] = total;
			int lo = 0;
			int prevEc = 0;
			int prevFa = 0;
			boolean first = true;
			int k = 1;
			for (int i = 0; i < cp.length; i++) {
				int reps = ex[i] != 0 ? copies : 1;
				for (int c = 0; c < reps; c++) {
					int overlap = first ? 0 : (c > 0 ? JOINT : Math.min(prevEc, sc[i]) + JOINT);
					if (!first) lo += prevFa - overlap;
					first = false;
					prevEc = ec[i];
					prevFa = fa[i];
					out[k++] = cp[i];
					out[k++] = lo;
				}
			}
			return out;
		}
	}
	private static final int MAGIC = 0x54584D31;
	private static final int MAGIC_Z = 0x54584D32;
	private static final int MAX_BMP = 0x10000;
	private static final Block EMPTY = new Block(0, 0, 0, 0, new int[C.values().length], new int[0], new int[0], new short[0], new short[0], new short[0], new short[0], new short[0], new short[0], new short[0], null, null, null, null);
	public final int unitsPerEm;
	public final int ascender;
	public final int descender;
	public final int lineGap;
	private final int[] constants;
	private final Chain[] cpChain;
	private final Chain[] hChain;
	private final Assembly[] asmV;
	private final Assembly[] asmH;
	private final Block single;
	private final Assets in;
	private final String stem;
	private final Parts parts;
	private final Part[] loaded;
	private Family[] fams;
	private Block lastBlock;
	private int lastIdx;
	private FontMetrics(Block single) {
		this.unitsPerEm = single.unitsPerEm;
		this.ascender = single.ascender;
		this.descender = single.descender;
		this.lineGap = single.lineGap;
		this.constants = single.constants;
		this.cpChain = single.cpChain;
		this.hChain = single.hChain;
		this.asmV = single.asmV;
		this.asmH = single.asmH;
		this.single = single;
		this.in = null;
		this.stem = null;
		this.parts = null;
		this.loaded = null;
	}
	private FontMetrics(Block core, Assets in, String stem, Parts parts) {
		this.unitsPerEm = core.unitsPerEm;
		this.ascender = core.ascender;
		this.descender = core.descender;
		this.lineGap = core.lineGap;
		this.constants = core.constants;
		this.cpChain = core.cpChain;
		this.hChain = core.hChain;
		this.asmV = core.asmV;
		this.asmH = core.asmH;
		this.single = null;
		this.in = in;
		this.stem = stem;
		this.parts = parts;
		this.loaded = new Part[parts.size()];
	}
	public static FontMetrics load(InputStream stream) throws IOException {
		return new FontMetrics(parse(readAll(stream)));
	}
	public static FontMetrics load(Assets in, String stem) throws IOException {
		Parts parts = Parts.load(in, stem);
		int ci = parts.core();
		if (ci < 0) throw new IOException("empty parts index");
		Block core = part(in, stem, parts, ci).block();
		return new FontMetrics(core, in, stem, parts);
	}
	private static Part part(Assets in, String stem, Parts parts, int i) {
		return new Part(parts.name[i], in, stem);
	}
	private Block block(int cp) {
		if (single != null) return single;
		int i = parts.find(cp);
		if (i < 0) return null;
		return partAt(i).block();
	}
	private Part partAt(int i) {
		Part p = loaded[i];
		if (p == null) {
			p = part(in, stem, parts, i);
			loaded[i] = p;
		}
		return p;
	}
	public int constant(C c) {
		return constants[c.ordinal()];
	}
	public int width(int cp) {
		return width(cp, 0);
	}
	public int width(int cp, int family) {
		return find(cp, family) ? lastBlock.width[lastIdx] : 0;
	}
	public int height(int cp) {
		return height(cp, 0);
	}
	public int height(int cp, int family) {
		return find(cp, family) ? lastBlock.height[lastIdx] : 0;
	}
	public int depth(int cp) {
		return depth(cp, 0);
	}
	public int depth(int cp, int family) {
		return find(cp, family) ? lastBlock.depth[lastIdx] : 0;
	}
	public int italic(int cp) {
		return italic(cp, 0);
	}
	public int italic(int cp, int family) {
		return find(cp, family) ? lastBlock.italic[lastIdx] : 0;
	}
	public int inkLeft(int cp) {
		return inkLeft(cp, 0);
	}
	public int inkLeft(int cp, int family) {
		return find(cp, family) ? lastBlock.inkLeft[lastIdx] : 0;
	}
	public int topAccent(int cp) {
		return find(cp, 0) ? lastBlock.topAccent[lastIdx] : 0;
	}
	public int inkTop(int cp) {
		return find(cp, 0) ? lastBlock.inkTop[lastIdx] : 0;
	}
	public int inkBottom(int cp) {
		return find(cp, 0) ? lastBlock.inkBottom[lastIdx] : 0;
	}
	public boolean hasGlyph(int cp) {
		return hasGlyph(cp, 0);
	}
	public boolean hasGlyph(int cp, int family) {
		return find(cp, family);
	}
	public boolean hasMath(int cp) {
		return find(cp, 0);
	}
	public void addFamily(int family, Assets in, String stem) throws IOException {
		if (fams == null) fams = new Family[16];
		fams[family] = new Family(in, stem, Parts.load(in, stem));
	}
	private boolean find(int cp, int family) {
		Family f = fams != null && family > 0 && family < fams.length ? fams[family] : null;
		if (f != null) {
			Block b = f.block(cp);
			if (b != null) {
				int i = indexOf(b, cp);
				if (i >= 0) {
					lastBlock = b;
					lastIdx = i;
					return true;
				}
			}
		}
		Block b = block(cp);
		if (b == null) return false;
		int i = indexOf(b, cp);
		if (i < 0) return false;
		lastBlock = b;
		lastIdx = i;
		return true;
	}
	public Chain chain(int cp) {
		return cp < MAX_BMP && cpChain != null ? cpChain[cp] : null;
	}
	public Assembly assembly(int cp, boolean horizontal) {
		if (cp >= MAX_BMP) return null;
		Assembly[] a = horizontal ? asmH : asmV;
		return a == null ? null : a[cp];
	}
	public int resolve(int cp, int neededHeight) {
		Chain c = chain(cp);
		if (c == null || neededHeight <= c.advance[0]) return cp;
		return c.cp[c.select(neededHeight)];
	}
	public int resolveInk(int cp, int neededHeight) {
		Chain c = chain(cp);
		if (c == null) return cp;
		if (height(c.cp[0]) + depth(c.cp[0]) >= neededHeight) return cp;
		for (int i = 1; i < c.size; i++) {
			int v = c.cp[i];
			if (height(v) + depth(v) >= neededHeight) return v;
		}
		return c.cp[c.size - 1];
	}
	public int resolveH(int cp, int neededWidth) {
		Chain c = cp < MAX_BMP && hChain != null ? hChain[cp] : null;
		if (c == null || neededWidth <= c.advance[0]) return cp;
		return c.cp[c.select(neededWidth)];
	}
	private static int indexOf(Block b, int target) {
		int lo = 0;
		int hi = b.cp.length - 1;
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			int v = b.cp[mid];
			if (v == target) return mid;
			if (v < target) lo = mid + 1; else hi = mid - 1;
		}
		return -1;
	}
	private static C byKey(String key) {
		C[] all = C.values();
		for (int i = 0; i < all.length; i++) {
			if (all[i].key.equals(key)) return all[i];
		}
		return null;
	}
	private static Block parse(byte[] all) throws IOException {
		if (all.length >= 8 && ByteBuffer.wrap(all).order(ByteOrder.BIG_ENDIAN).getInt() == MAGIC_Z) {
			int rawLen = ByteBuffer.wrap(all, 4, 4).order(ByteOrder.BIG_ENDIAN).getInt();
			byte[] raw = new byte[rawLen];
			Inflater inf = new Inflater();
			inf.setInput(all, 8, all.length - 8);
			try {
				int got = 0;
				while (!inf.finished() && got < rawLen) {
					int k = inf.inflate(raw, got, rawLen - got);
					if (k == 0 && inf.needsInput()) break;
					got += k;
				}
			} catch (DataFormatException e) {
				throw new IOException(e);
			} finally {
				inf.end();
			}
			return parseBody(raw);
		}
		return parseBody(all);
	}
	private static Block parseBody(byte[] all) throws IOException {
		ByteBuffer b = ByteBuffer.wrap(all).order(ByteOrder.BIG_ENDIAN);
		if (all.length < 8 || b.getInt() != MAGIC) throw new IOException("bad metrics magic");
		int upm = b.getInt();
		int ascender = b.getInt();
		int descender = b.getInt();
		int lineGap = b.getInt();
		int nConst = b.getInt();
		int[] constants = new int[C.values().length];
		byte[] nameBuf = new byte[64];
		for (int i = 0; i < nConst; i++) {
			int len = b.getShort() & 0xFFFF;
			b.get(nameBuf, 0, len);
			C c = byKey(new String(nameBuf, 0, len, StandardCharsets.US_ASCII));
			int value = b.getInt();
			if (c != null) constants[c.ordinal()] = value;
		}
		int n = b.getInt();
		int[] cp = new int[n];
		int[] width = new int[n];
		short[] height = new short[n];
		short[] depth = new short[n];
		short[] italic = new short[n];
		short[] inkLeft = new short[n];
		short[] topAccent = new short[n];
		short[] inkTop = new short[n];
		short[] inkBottom = new short[n];
		for (int i = 0; i < n; i++) cp[i] = b.getInt();
		for (int i = 0; i < n; i++) width[i] = b.getInt();
		for (int i = 0; i < n; i++) height[i] = b.getShort();
		for (int i = 0; i < n; i++) depth[i] = b.getShort();
		for (int i = 0; i < n; i++) italic[i] = b.getShort();
		for (int i = 0; i < n; i++) inkLeft[i] = b.getShort();
		for (int i = 0; i < n; i++) topAccent[i] = b.getShort();
		for (int i = 0; i < n; i++) inkTop[i] = b.getShort();
		for (int i = 0; i < n; i++) inkBottom[i] = b.getShort();
		int nChain = b.getInt();
		Chain[] cpChain = nChain > 0 ? new Chain[MAX_BMP] : null;
		for (int i = 0; i < nChain; i++) {
			int owner = b.getInt();
			int cnt = b.getInt();
			int[] cps = new int[cnt];
			int[] adv = new int[cnt];
			for (int k = 0; k < cnt; k++) cps[k] = b.getInt();
			for (int k = 0; k < cnt; k++) adv[k] = b.getInt();
			Chain chain = new Chain(cps, adv);
			if (owner < MAX_BMP) cpChain[owner] = chain;
			for (int k = 0; k < cnt; k++) {
				int code = cps[k];
				if (code < MAX_BMP) cpChain[code] = chain;
			}
		}
		int nH = b.getInt();
		Chain[] hChain = nH > 0 ? new Chain[MAX_BMP] : null;
		for (int i = 0; i < nH; i++) {
			int owner = b.getInt();
			int cnt = b.getInt();
			int[] cps = new int[cnt];
			int[] adv = new int[cnt];
			for (int k = 0; k < cnt; k++) cps[k] = b.getInt();
			for (int k = 0; k < cnt; k++) adv[k] = b.getInt();
			Chain chain = new Chain(cps, adv);
			if (owner < MAX_BMP) hChain[owner] = chain;
			for (int k = 0; k < cnt; k++) {
				int code = cps[k];
				if (code < MAX_BMP) hChain[code] = chain;
			}
		}
		int nAsm = b.getInt();
		Assembly[] asmV = nAsm > 0 ? new Assembly[MAX_BMP] : null;
		Assembly[] asmH = nAsm > 0 ? new Assembly[MAX_BMP] : null;
		for (int i = 0; i < nAsm; i++) {
			int owner = b.getInt();
			int axis = b.get() & 0xFF;
			int italics = b.getShort();
			int cnt = b.getInt();
			int[] cps = new int[cnt];
			short[] sc = new short[cnt];
			short[] ec = new short[cnt];
			short[] fa = new short[cnt];
			byte[] ex = new byte[cnt];
			for (int k = 0; k < cnt; k++) {
				cps[k] = b.getInt();
				sc[k] = b.getShort();
				ec[k] = b.getShort();
				fa[k] = b.getShort();
				ex[k] = b.get();
			}
			Assembly asm = new Assembly(cps, sc, ec, fa, ex, italics);
			Assembly[] target = axis == 0 ? asmV : asmH;
			if (owner < MAX_BMP) target[owner] = asm;
		}
		if (b.remaining() >= 4) {
			int tlen = b.getInt();
			if (tlen > 0 && b.remaining() >= tlen) {
				byte[] tb = new byte[tlen];
				b.get(tb);
				Tables.install(tb);
			}
		}
		return new Block(upm, ascender, descender, lineGap, constants, cp, width, height, depth, italic, inkLeft, topAccent, inkTop, inkBottom, cpChain, hChain, asmV, asmH);
	}
	private static byte[] readAll(InputStream in) throws IOException {
		byte[] out = new byte[1 << 15];
		byte[] buf = new byte[1 << 13];
		int total = 0;
		int n;
		while ((n = in.read(buf)) > 0) {
			if (total + n > out.length) {
				int grown = out.length << 1;
				while (grown < total + n) grown <<= 1;
				byte[] bigger = new byte[grown];
				System.arraycopy(out, 0, bigger, 0, total);
				out = bigger;
			}
			System.arraycopy(buf, 0, out, total, n);
			total += n;
		}
		in.close();
		if (total == out.length) return out;
		byte[] exact = new byte[total];
		System.arraycopy(out, 0, exact, 0, total);
		return exact;
	}
	private static final class Block {
		final int unitsPerEm;
		final int ascender;
		final int descender;
		final int lineGap;
		final int[] constants;
		final int[] cp;
		final int[] width;
		final short[] height;
		final short[] depth;
		final short[] italic;
		final short[] inkLeft;
		final short[] topAccent;
		final short[] inkTop;
		final short[] inkBottom;
		final Chain[] cpChain;
		final Chain[] hChain;
		final Assembly[] asmV;
		final Assembly[] asmH;
		Block(int unitsPerEm, int ascender, int descender, int lineGap, int[] constants, int[] cp, int[] width, short[] height, short[] depth, short[] italic, short[] inkLeft, short[] topAccent, short[] inkTop, short[] inkBottom, Chain[] cpChain, Chain[] hChain, Assembly[] asmV, Assembly[] asmH) {
			this.unitsPerEm = unitsPerEm;
			this.ascender = ascender;
			this.descender = descender;
			this.lineGap = lineGap;
			this.constants = constants;
			this.cp = cp;
			this.width = width;
			this.height = height;
			this.depth = depth;
			this.italic = italic;
			this.inkLeft = inkLeft;
			this.topAccent = topAccent;
			this.inkTop = inkTop;
			this.inkBottom = inkBottom;
			this.cpChain = cpChain;
			this.hChain = hChain;
			this.asmV = asmV;
			this.asmH = asmH;
		}
	}
	private static final class Part {
		final String name;
		private final Assets in;
		private final String stem;
		private Block block;
		Part(String name, Assets in, String stem) {
			this.name = name;
			this.in = in;
			this.stem = stem;
		}
		Block block() {
			if (block != null) return block;
			try {
				block = parse(readAll(in.open(stem + "/" + name + ".bin")));
			} catch (IOException e) {
				block = EMPTY;
			}
			return block;
		}
	}
	private static final class Family {
		private final Assets in;
		private final String stem;
		private final Parts parts;
		private final Part[] loaded;
		Family(Assets in, String stem, Parts parts) {
			this.in = in;
			this.stem = stem;
			this.parts = parts;
			this.loaded = new Part[parts.size()];
		}
		Block block(int cp) {
			int i = parts.find(cp);
			if (i < 0) return null;
			Part p = loaded[i];
			if (p == null) {
				p = new Part(parts.name[i], in, stem);
				loaded[i] = p;
			}
			return p.block();
		}
	}
}
