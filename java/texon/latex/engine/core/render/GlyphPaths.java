package texon.latex.engine.core.render;
import java.util.*;
import texon.latex.engine.core.*;
public final class GlyphPaths {
	private static final float[][] NONE = new float[0][];
	private final GlyphOutlines outlines;
	private final LinkedHashMap<Integer, float[][]> map;
	private final int cap;
	public GlyphPaths(GlyphOutlines outlines, int capacity) {
		this.outlines = outlines;
		this.cap = capacity < 1 ? 1 : capacity;
		this.map = new LinkedHashMap<Integer, float[][]>(cap, 0.75f, true);
	}
	public float[][] get(int cp) {
		return get(cp, 0);
	}
	public float[][] get(int cp, int family) {
		Integer key = (family << 21) | cp;
		float[][] cs = map.get(key);
		if (cs != null) return cs;
		String d = outlines == null ? null : outlines.path(cp, family);
		if (d == null || d.length() == 0) {
			cs = NONE;
		} else {
			Flat f = new Flat();
			SvgPath.parse(d, f);
			cs = f.polys.toArray(new float[0][]);
			if (cs.length == 0) cs = NONE;
		}
		if (map.size() >= cap) {
			Iterator<Integer> it = map.keySet().iterator();
			if (it.hasNext()) {
				it.next();
				it.remove();
			}
		}
		map.put(key, cs);
		return cs;
	}
	public void clear() {
		map.clear();
	}
	private static final class Flat implements PathSink {
		private final ArrayList<float[]> polys = new ArrayList<>();
		private float[] pts = new float[256];
		private int n;
		private float cx, cy;
		public void moveTo(float x, float y) {
			flush();
			pt(x, y);
			cx = x;
			cy = y;
		}
		public void lineTo(float x, float y) {
			pt(x, y);
			cx = x;
			cy = y;
		}
		public void quadTo(float qx, float qy, float x, float y) {
			float x0 = cx;
			float y0 = cy;
			for (int i = 1; i <= 8; i++) {
				float t = i / 8f;
				float u = 1f - t;
				pt(u * u * x0 + 2f * u * t * qx + t * t * x, u * u * y0 + 2f * u * t * qy + t * t * y);
			}
			cx = x;
			cy = y;
		}
		public void cubicTo(float ax, float ay, float bx, float by, float x, float y) {
			float x0 = cx;
			float y0 = cy;
			for (int i = 1; i <= 12; i++) {
				float t = i / 12f;
				float u = 1f - t;
				pt(u * u * u * x0 + 3f * u * u * t * ax + 3f * u * t * t * bx + t * t * t * x, u * u * u * y0 + 3f * u * u * t * ay + 3f * u * t * t * by + t * t * t * y);
			}
			cx = x;
			cy = y;
		}
		public void close() {
			flush();
		}
		private void pt(float x, float y) {
			if (n + 2 > pts.length) pts = Arrays.copyOf(pts, pts.length << 1);
			pts[n++] = x;
			pts[n++] = y;
		}
		private void flush() {
			if (n >= 6) polys.add(Arrays.copyOf(pts, n));
			n = 0;
		}
	}
}
