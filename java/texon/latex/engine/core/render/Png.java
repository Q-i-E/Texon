package texon.latex.engine.core.render;
import java.io.*;
import java.util.zip.*;
public final class Png {
	private Png() {
	}
	public static byte[] encode(Raster raster) {
		return encode(raster.pixels, raster.width, raster.height);
	}
	public static byte[] encode(int[] pixels, int width, int height) {
		boolean alpha = false;
		int count = width * height;
		for (int i = 0; i < count; i++) {
			if ((pixels[i] >>> 24) != 0xFF) {
				alpha = true;
				break;
			}
		}
		int channels = alpha ? 4 : 3;
		byte[] raw = new byte[height * (1 + width * channels)];
		int o = 0;
		for (int y = 0; y < height; y++) {
			o++;
			int base = y * width;
			for (int x = 0; x < width; x++) {
				int v = pixels[base + x];
				raw[o++] = (byte) (v >> 16);
				raw[o++] = (byte) (v >> 8);
				raw[o++] = (byte) v;
				if (alpha) raw[o++] = (byte) (v >>> 24);
			}
		}
		Deflater def = new Deflater(Deflater.BEST_COMPRESSION);
		def.setInput(raw);
		def.finish();
		ByteArrayOutputStream z = new ByteArrayOutputStream(raw.length >> 1);
		byte[] buf = new byte[1 << 16];
		while (!def.finished()) z.write(buf, 0, def.deflate(buf));
		def.end();
		ByteArrayOutputStream out = new ByteArrayOutputStream(z.size() + 128);
		out.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, 0, 8);
		byte[] ihdr = new byte[13];
		putInt(ihdr, 0, width);
		putInt(ihdr, 4, height);
		ihdr[8] = 8;
		ihdr[9] = (byte) (alpha ? 6 : 2);
		chunk(out, "IHDR", ihdr, 0, 13);
		byte[] idat = z.toByteArray();
		chunk(out, "IDAT", idat, 0, idat.length);
		chunk(out, "IEND", new byte[0], 0, 0);
		return out.toByteArray();
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
