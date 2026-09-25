package texon.latex.engine.core.render;
public final class SvgPath {
	private SvgPath() {
	}
	private static final float[] BUF = new float[6];
	public static void parse(String d, PathSink s) {
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
			for (int j = 0; j < k; j++) i = read(d, i, j);
			switch (cmd) {
				case 'M':
					x = BUF[0];
					y = BUF[1];
					sx = x;
					sy = y;
					s.moveTo(x, y);
					cmd = 'L';
					break;
				case 'm':
					x += BUF[0];
					y += BUF[1];
					sx = x;
					sy = y;
					s.moveTo(x, y);
					cmd = 'l';
					break;
				case 'L':
					x = BUF[0];
					y = BUF[1];
					s.lineTo(x, y);
					break;
				case 'l':
					x += BUF[0];
					y += BUF[1];
					s.lineTo(x, y);
					break;
				case 'H':
					x = BUF[0];
					s.lineTo(x, y);
					break;
				case 'h':
					x += BUF[0];
					s.lineTo(x, y);
					break;
				case 'V':
					y = BUF[0];
					s.lineTo(x, y);
					break;
				case 'v':
					y += BUF[0];
					s.lineTo(x, y);
					break;
				case 'C':
					s.cubicTo(BUF[0], BUF[1], BUF[2], BUF[3], BUF[4], BUF[5]);
					x = BUF[4];
					y = BUF[5];
					break;
				case 'c':
					s.cubicTo(x + BUF[0], y + BUF[1], x + BUF[2], y + BUF[3], x + BUF[4], y + BUF[5]);
					x += BUF[4];
					y += BUF[5];
					break;
				case 'Q':
					s.quadTo(BUF[0], BUF[1], BUF[2], BUF[3]);
					x = BUF[2];
					y = BUF[3];
					break;
				case 'q':
					s.quadTo(x + BUF[0], y + BUF[1], x + BUF[2], y + BUF[3]);
					x += BUF[2];
					y += BUF[3];
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
	private static int read(String d, int i, int slot) {
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
		BUF[slot] = i == start ? 0f : Float.parseFloat(d.substring(start, i));
		return i;
	}
}