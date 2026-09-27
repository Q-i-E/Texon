package texon.latex.engine.core.render;
import java.io.*;
import java.util.zip.*;
public final class Png {
	private Png() {
	}
	public static byte[] encode(Raster raster) {
		return encode(raster.pixels, raster.width, raster.height);
	}
	public static byte[] encode(Raster raster, int level) {
		return encode(raster.pixels, raster.width, raster.height, level);
	}
	public static byte[] encode(Raster raster, int level, int filter, int colorMode) {
		return encode(raster.pixels, raster.width, raster.height, level, filter, colorMode);
	}
	public static byte[] encode(int[] pixels, int width, int height) {
		return encode(pixels, width, height, Deflater.BEST_COMPRESSION, 0, 1);
	}
	public static byte[] encode(int[] pixels, int width, int height, int level) {
		return encode(pixels, width, height, level, 0, 1);
	}
	public static byte[] encode(int[] pixels, int width, int height, int level, int filter, int colorMode) {
		boolean alpha = false;
		boolean gray = colorMode == 0;
		int count = width * height;
		for (int i = 0; i < count; i++) {
			int v = pixels[i];
			if ((v >>> 24) != 0xFF) alpha = true;
			if (gray && ((v >> 16 & 255) != (v >> 8 & 255) || (v >> 8 & 255) != (v & 255))) gray = false;
			if (alpha && !gray) break;
		}
		int type;
		int ch;
		if (gray && alpha) {
			type = 4;
			ch = 2;
		} else if (gray) {
			type = 0;
			ch = 1;
		} else if (alpha) {
			type = 6;
			ch = 4;
		} else {
			type = 2;
			ch = 3;
		}
		int stride = width * ch;
		byte[] raw = new byte[height * stride];
		int o = 0;
		for (int y = 0; y < height; y++) {
			int base = y * width;
			for (int x = 0; x < width; x++) {
				int v = pixels[base + x];
				if (ch == 1) {
					raw[o++] = (byte) (v >> 16);
				} else if (ch == 2) {
					raw[o++] = (byte) (v >> 16);
					raw[o++] = (byte) (v >>> 24);
				} else if (ch == 3) {
					raw[o++] = (byte) (v >> 16);
					raw[o++] = (byte) (v >> 8);
					raw[o++] = (byte) v;
				} else {
					raw[o++] = (byte) (v >> 16);
					raw[o++] = (byte) (v >> 8);
					raw[o++] = (byte) v;
					raw[o++] = (byte) (v >>> 24);
				}
			}
		}
		byte[] body = filter(raw, width, height, ch, filter);
		Deflater def = new Deflater(level);
		def.setInput(body);
		def.finish();
		ByteArrayOutputStream z = new ByteArrayOutputStream(body.length >> 1);
		byte[] buf = new byte[1 << 16];
		while (!def.finished()) z.write(buf, 0, def.deflate(buf));
		def.end();
		ByteArrayOutputStream out = new ByteArrayOutputStream(z.size() + 128);
		out.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, 0, 8);
		byte[] ihdr = new byte[13];
		putInt(ihdr, 0, width);
		putInt(ihdr, 4, height);
		ihdr[8] = 8;
		ihdr[9] = (byte) type;
		chunk(out, "IHDR", ihdr, 0, 13);
		byte[] idat = z.toByteArray();
		chunk(out, "IDAT", idat, 0, idat.length);
		chunk(out, "IEND", new byte[0], 0, 0);
		return out.toByteArray();
	}
	private static byte[] filter(byte[] raw, int width, int height, int ch, int mode) {
		int stride = width * ch;
		byte[] out = new byte[height * (stride + 1)];
		byte[] cur = new byte[stride];
		byte[] prev = new byte[stride];
		byte[] cand = new byte[stride];
		byte[] best = new byte[stride];
		int o = 0;
		for (int y = 0; y < height; y++) {
			System.arraycopy(raw, y * stride, cur, 0, stride);
			int f = mode;
			if (mode < 0) {
				int bestSum = Integer.MAX_VALUE;
				for (int k = 0; k <= 4; k++) {
					predict(cur, prev, stride, ch, k, cand);
					int sum = 0;
					for (int i = 0; i < stride; i++) {
						int v = cand[i];
						sum += v < 0 ? -v : v;
					}
					if (sum < bestSum) {
						bestSum = sum;
						f = k;
						System.arraycopy(cand, 0, best, 0, stride);
					}
				}
			} else {
				predict(cur, prev, stride, ch, mode, best);
			}
			out[o++] = (byte) f;
			System.arraycopy(best, 0, out, o, stride);
			o += stride;
			System.arraycopy(cur, 0, prev, 0, stride);
		}
		return out;
	}
	private static void predict(byte[] cur, byte[] prev, int n, int ch, int f, byte[] dst) {
		for (int i = 0; i < n; i++) {
			int x = cur[i] & 0xFF;
			int a = i >= ch ? (cur[i - ch] & 0xFF) : 0;
			int b = prev[i] & 0xFF;
			int c = i >= ch ? (prev[i - ch] & 0xFF) : 0;
			int v;
			if (f == 0) v = x;
			else if (f == 1) v = x - a;
			else if (f == 2) v = x - b;
			else if (f == 3) v = x - ((a + b) >> 1);
			else v = x - paeth(a, b, c);
			dst[i] = (byte) v;
		}
	}
	private static int paeth(int a, int b, int c) {
		int p = a + b - c;
		int pa = p > a ? p - a : a - p;
		int pb = p > b ? p - b : b - p;
		int pc = p > c ? p - c : c - p;
		if (pa <= pb && pa <= pc) return a;
		if (pb <= pc) return b;
		return c;
	}
	private static void chunk(ByteArrayOutputStream out, String type, byte[] data, int off, int len) {
		byte[] head = new byte[8];
		putInt(head, 0, len);
		head[4] = (byte) type.charAt(0);
		head[5] = (byte) type.charAt(1);
		head[6] = (byte) type.charAt(2);
		head[7] = (byte) type.charAt(3);
		out.write(head, 0, 8);
		out.write(data, off, len);
		CRC32 crc = new CRC32();
		crc.update(head, 4, 4);
		crc.update(data, off, len);
		byte[] c = new byte[4];
		putInt(c, 0, (int) crc.getValue());
		out.write(c, 0, 4);
	}
	private static void putInt(byte[] b, int o, int v) {
		b[o] = (byte) (v >> 24);
		b[o + 1] = (byte) (v >> 16);
		b[o + 2] = (byte) (v >> 8);
		b[o + 3] = (byte) v;
	}
}
