package texon.latex.engine.core;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
public final class Parts {
	private static final int MAGIC = 0x54584944;
	public static final int CORE = 1;
	public final int bits;
	public final int total;
	public final int[] base;
	public final int[] count;
	public final int[] flags;
	public final String[] name;
	private Parts(int bits, int total, int[] base, int[] count, int[] flags, String[] name) {
		this.bits = bits;
		this.total = total;
		this.base = base;
		this.count = count;
		this.flags = flags;
		this.name = name;
	}
	public int size() {
		return base.length;
	}
	public int core() {
		for (int i = 0; i < base.length; i++) {
			if ((flags[i] & CORE) != 0) return i;
		}
		return base.length > 0 ? 0 : -1;
	}
	public int find(int cp) {
		int lo = 0;
		int hi = base.length - 1;
		int found = -1;
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			if (base[mid] <= cp) {
				found = mid;
				lo = mid + 1;
			} else hi = mid - 1;
		}
		if (found < 0) return -1;
		if (cp >= base[found] + (1 << bits)) return -1;
		return found;
	}
	public static Parts load(Assets in, String stem) throws IOException {
		return load(in.open(stem + ".idx"));
	}
	public static Parts load(InputStream stream) throws IOException {
		byte[] all = readAll(stream);
		ByteBuffer b = ByteBuffer.wrap(all).order(ByteOrder.BIG_ENDIAN);
		if (all.length < 20 || b.getInt() != MAGIC) throw new IOException("bad parts magic");
		b.getInt();
		int bits = b.getInt();
		int total = b.getInt();
		int n = b.getInt();
		int[] base = new int[n];
		int[] count = new int[n];
		int[] flags = new int[n];
		String[] name = new String[n];
		for (int i = 0; i < n; i++) {
			base[i] = b.getInt();
			count[i] = b.getInt();
			flags[i] = b.getInt();
			int len = b.getShort() & 0xFFFF;
			byte[] nb = new byte[len];
			b.get(nb);
			name[i] = new String(nb, StandardCharsets.US_ASCII);
		}
		return new Parts(bits, total, base, count, flags, name);
	}
	private static byte[] readAll(InputStream in) throws IOException {
		byte[] out = new byte[1 << 12];
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
}
