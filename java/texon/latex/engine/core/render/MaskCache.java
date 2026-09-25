package texon.latex.engine.core.render;
public final class MaskCache {
	private final int[] kCp;
	private final byte[] kFamily;
	private final int[] kSx;
	private final int[] kTx;
	private final int[] kTy;
	private final GlyphMask[] val;
	private final int mask;
	private final int limit;
	private int size;
	public MaskCache(int capacity) {
		int c = Integer.highestOneBit(Math.max(16, capacity) * 2 - 1) << 1;
		this.mask = c - 1;
		this.limit = c * 3 / 4;
		this.kCp = new int[c];
		this.kFamily = new byte[c];
		this.kSx = new int[c];
		this.kTx = new int[c];
		this.kTy = new int[c];
		this.val = new GlyphMask[c];
	}
	public GlyphMask get(int cp, int family, float sx, float tx, float ty) {
		int isx = Float.floatToRawIntBits(sx);
		int itx = Float.floatToRawIntBits(tx);
		int ity = Float.floatToRawIntBits(ty);
		int i = hash(cp, family, isx, itx, ity);
		while (true) {
			GlyphMask m = val[i];
			if (m == null) return null;
			if (kCp[i] == cp && kFamily[i] == family && kSx[i] == isx && kTx[i] == itx && kTy[i] == ity) return m;
			i = (i + 1) & mask;
		}
	}
	public void put(int cp, int family, float sx, float tx, float ty, GlyphMask m) {
		if (size >= limit) clear();
		int isx = Float.floatToRawIntBits(sx);
		int itx = Float.floatToRawIntBits(tx);
		int ity = Float.floatToRawIntBits(ty);
		int i = hash(cp, family, isx, itx, ity);
		while (val[i] != null) i = (i + 1) & mask;
		kCp[i] = cp;
		kFamily[i] = (byte) family;
		kSx[i] = isx;
		kTx[i] = itx;
		kTy[i] = ity;
		val[i] = m;
		size++;
	}
	private void clear() {
		java.util.Arrays.fill(val, null);
		size = 0;
	}
	private int hash(int cp, int family, int sx, int tx, int ty) {
		int h = cp * 0x9E3779B1;
		h = (h ^ family) * 0x85EBCA77;
		h = (h ^ sx) * 0xC2B2AE3D;
		h = (h ^ tx) * 0x27D4EB2F;
		h ^= ty;
		h ^= h >>> 15;
		return h & mask;
	}
}