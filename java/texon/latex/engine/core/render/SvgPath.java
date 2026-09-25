package texon.latex.engine.core.render;
public final class SvgPath {
	private SvgPath() {
	}
	private static final ThreadLocal<float[]> BUF = ThreadLocal.withInitial(() -> new float[6]);
	public static void parse(String d, PathSink s) {
		float[] buf = BUF.get();
		int n = d.length();
		int i = 0;
		char cmd = 0;
		float x = 0f, y = 0f, sx = 0f, sy = 0f;
		while (i < n) {
			char c = d.charAt(i);
			if (c == ' ' || c == ',' || c == '\n' || c == '\r' || c == '\t') {
				i++;
				continue;
			}
			if (Character.isLetter(c)) {
				i++;
				if (c == 'Z' || c == 'z') {
					s.close();
					x = sx;
					y = sy;
				} else {
					cmd = c;
				}
				continue;
			}
			if (cmd == 0) {
				i++;
				continue;
			}
			int k = arity(cmd);
			for (int j = 0; j < k; j++) i = read(d, i, j, buf);
			switch (cmd) {
				case 'M':
					x = buf[0];
					y = buf[1];
					sx = x;
					sy = y;
					s.moveTo(x, y);
					cmd = 'L';
					break;
				case 'm':
					x += buf[0];
					y += buf[1];
					sx = x;
					sy = y;
					s.moveTo(x, y);
					cmd = 'l';
					break;
				case 'L':
					x = buf[0];
					y = buf[1];
					s.lineTo(x, y);
					break;
				case 'l':
					x += buf[0];
					y += buf[1];
					s.lineTo(x, y);
					break;
				case 'H':
					x = buf[0];
					s.lineTo(x, y);
					break;
				case 'h':
					x += buf[0];
					s.lineTo(x, y);
					break;
				case 'V':
					y = buf[0];
					s.lineTo(x, y);
					break;
				case 'v':
					y += buf[0];
					s.lineTo(x, y);
					break;
				case 'C':
					s.cubicTo(buf[0], buf[1], buf[2], buf[3], buf[4], buf[5]);
					x = buf[4];
					y = buf[5];
					break;
				case 'c':
					s.cubicTo(x + buf[0], y + buf[1], x + buf[2], y + buf[3], x + buf[4], y + buf[5]);
					x += buf[4];
					y += buf[5];
					break;
				case 'Q':
					s.quadTo(buf[0], buf[1], buf[2], buf[3]);
					x = buf[2];
					y = buf[3];
					break;
				case 'q':
					s.quadTo(x + buf[0], y + buf[1], x + buf[2], y + buf[3]);
					x += buf[2];
					y += buf[3];
					break;
				default:
					i++;
					break;
			}
		}
	}
	private static int arity(char c) {
		switch (c) {
			case 'H': case 'h': case 'V': case 'v': return 1;
			case 'M': case 'm': case 'L': case 'l': return 2;
			case 'Q': case 'q': return 4;
			case 'C': case 'c': return 6;
			default: return 0;
		}
	}
	private static int read(String d, int i, int slot, float[] buf) {
		int n = d.length();
		while (i < n) {
			char c = d.charAt(i);
			if (c == ' ' || c == ',' || c == '\n' || c == '\r' || c == '\t') i++; else break;
		}
		int start = i;
		if (i < n && (d.charAt(i) == '-' || d.charAt(i) == '+')) i++;
		while (i < n) {
			char c = d.charAt(i);
			if ((c >= '0' && c <= '9') || c == '.') i++; else break;
		}
		buf[slot] = i == start ? 0f : Float.parseFloat(d.substring(start, i));
		return i;
	}
}