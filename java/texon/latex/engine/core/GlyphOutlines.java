package texon.latex.engine.core;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;
public final class GlyphOutlines {
	private static final int MAGIC = 0x54584F31;
	private static final int MAX_CACHED = 8;
	private final Assets in;
	private final String stem;
	private final Parts parts;
	private final Blob single;
	private final LinkedHashMap<Integer, Blob> cache;
	private Family[] fams;
	private GlyphOutlines(Blob single) {
		this.in = null;
		this.stem = null;
		this.parts = null;
		this.single = single;
		this.cache = null;
	}
	private GlyphOutlines(Assets in, String stem, Parts parts) {
		this.in = in;
		this.stem = stem;
		this.parts = parts;
		this.single = null;
		this.cache = new LinkedHashMap<Integer, Blob>(16, 0.75f, true);
	}
	public static GlyphOutlines load(InputStream stream) throws IOException {
		return new GlyphOutlines(Blob.parse(readAll(stream)));
	}
	public static GlyphOutlines load(Assets in, String stem) throws IOException {
		return new GlyphOutlines(in, stem, Parts.load(in, stem));
	}
	public void addFamily(int family, Assets in, String stem) throws IOException {
		if (fams == null) fams = new Family[16];
		fams[family] = new Family(in, stem, Parts.load(in, stem));
	}
	public int size() {
		return single != null ? single.cp.length : parts.total;
	}
	public int parts() {
		return single != null ? 1 : parts.size();
	}
	public boolean has(int code) {
		return has(code, 0);
	}
	public boolean has(int code, int family) {
		return path(code, family) != null;
	}
	public String path(int code) {
		return path(code, 0);
	}
	public String path(int code, int family) {
		Family f = fams != null && family > 0 && family < fams.length ? fams[family] : null;
		if (f != null) {
			String p = f.path(code);
			if (p != null) return p;
		}
		if (single != null) return single.path(code);
		int i = parts.find(code);
		if (i < 0) return null;
		return blob(i).path(code);
	}
	private Blob blob(int i) {
		Blob b = cache.get(i);
		if (b != null) return b;
		try {
			b = Blob.parse(readAll(in.open(stem + "/" + parts.name[i] + ".bin")));
		} catch (IOException e) {
			b = Blob.EMPTY;
		}
		if (cache.size() >= MAX_CACHED) {
			Iterator<Integer> it = cache.keySet().iterator();
			if (it.hasNext()) {
				it.next();
				it.remove();
			}
		}
		cache.put(i, b);
		return b;
	}
	private static byte[] readAll(InputStream in) throws IOException {
		byte[] out = new byte[1 << 16];
		byte[] buf = new byte[1 << 13];
		int total = 0;
		int n;
		while ((n = in.read(buf)) > 0) {
			if (total + n > out.length) {
				int grown = out.length << 1;
				while (grown < total + n) grown <<= 1;
				out = java.util.Arrays.copyOf(out, grown);
			}
			System.arraycopy(buf, 0, out, total, n);
			total += n;
		}
		in.close();
		return total == out.length ? out : java.util.Arrays.copyOf(out, total);
	}
	private static final class Family {
		private final Assets in;
		private final String stem;
		private final Parts parts;
		private final LinkedHashMap<Integer, Blob> cache;
		Family(Assets in, String stem, Parts parts) {
			this.in = in;
			this.stem = stem;
			this.parts = parts;
			this.cache = new LinkedHashMap<Integer, Blob>(16, 0.75f, true);
		}
		String path(int code) {
			int i = parts.find(code);
			if (i < 0) return null;
			return blob(i).path(code);
		}
		private Blob blob(int i) {
			Blob b = cache.get(i);
			if (b != null) return b;
			try {
				b = Blob.parse(readAll(in.open(stem + "/" + parts.name[i] + ".bin")));
			} catch (IOException e) {
				b = Blob.EMPTY;
			}
			if (cache.size() >= MAX_CACHED) {
				Iterator<Integer> it = cache.keySet().iterator();
				if (it.hasNext()) {
					it.next();
					it.remove();
				}
			}
			cache.put(i, b);
			return b;
		}
	}
	private static final class Blob {
		static final Blob EMPTY = new Blob(new int[0], new int[1], new byte[0], 0);
		final int[] cp;
		final int[] off;
		final byte[] data;
		final int base;
		Blob(int[] cp, int[] off, byte[] data, int base) {
			this.cp = cp;
			this.off = off;
			this.data = data;
			this.base = base;
		}
		static Blob parse(byte[] all) throws IOException {
			ByteBuffer head = ByteBuffer.wrap(all).order(ByteOrder.BIG_ENDIAN);
			if (all.length < 8 || head.getInt() != MAGIC) throw new IOException("bad outlines magic");
			int rawLen = head.getInt();
			byte[] raw = new byte[rawLen];
			Inflater inf = new Inflater();
			inf.setInput(all, 8, all.length - 8);
			try {
				int n = 0;
				while (!inf.finished() && n < rawLen) {
					int k = inf.inflate(raw, n, rawLen - n);
					if (k == 0 && inf.needsInput()) break;
					n += k;
				}
			} catch (DataFormatException e) {
				throw new IOException(e);
			} finally {
				inf.end();
			}
			ByteBuffer b = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN);
			int n = b.getInt();
			int[] cp = new int[n];
			for (int i = 0; i < n; i++) cp[i] = b.getInt();
			int[] off = new int[n + 1];
			for (int i = 0; i <= n; i++) off[i] = b.getInt();
			return new Blob(cp, off, raw, b.position());
		}
		int indexOf(int target) {
			int lo = 0;
			int hi = cp.length - 1;
			while (lo <= hi) {
				int mid = (lo + hi) >>> 1;
				int v = cp[mid];
				if (v == target) return mid;
				if (v < target) lo = mid + 1; else hi = mid - 1;
			}
			return -1;
		}
		String path(int code) {
			int i = indexOf(code);
			if (i < 0) return null;
			return new String(data, base + off[i], off[i + 1] - off[i], StandardCharsets.UTF_8);
		}
	}
}
