package texon.latex.engine.core.render;
import java.util.*;
import texon.latex.engine.core.*;
public final class RasterSink implements VectorSink {
	private static final int SUBS = 4;
	private static final float BIAS = 1048576f;
	private final GlyphPaths paths;
	private final MaskCache masks;
	private final ArrayList<float[]> polys = new ArrayList<>();
	private int width;
	private int height;
	private float scale;
	private float ox;
	private float oy;
	private int[] pixels;
	private float[] ex0, ey0, ex1, ey1;
	private int edges;
	private long[] ev = new long[256];
	private int[] bucket;
	private int[] next;
	private int[] active;
	private int activeN;
	public RasterSink(GlyphOutlines outlines, int width, int height, float scale, float ox, float oy, int background) {
		this(outlines, null, null, width, height, scale, ox, oy, background);
	}
	public RasterSink(GlyphOutlines outlines, GlyphPaths paths, int width, int height, float scale, float ox, float oy, int background) {
		this(outlines, paths, null, width, height, scale, ox, oy, background);
	}
	public RasterSink(GlyphOutlines outlines, GlyphPaths paths, MaskCache masks, int width, int height, float scale, float ox, float oy, int background) {
		this.paths = paths != null ? paths : new GlyphPaths(outlines, 64);
		this.masks = masks != null ? masks : new MaskCache(512);
		reset(width, height, scale, ox, oy, background);
	}
	public RasterSink reset(int width, int height, float scale, float ox, float oy, int background) {
		this.width = width;
		this.height = height;
		this.scale = scale;
		this.ox = ox;
		this.oy = oy;
		int need = width * height;
		if (pixels == null || pixels.length < need) pixels = new int[need];
		Arrays.fill(pixels, 0, need, background);
		edges = 0;
		polys.clear();
		return this;
	}
	public Raster result() {
		return new Raster(width, height, pixels);
	}
	public void glyph(int cp, float x, float baseline, int percent, int family, int argb) {
		float f = percent * 0.01f;
		float sx = scale * f;
		float tx = ox + x * scale;
		float ty = oy + baseline * scale;
		GlyphMask m = masks.get(cp, family, sx, tx, ty);
		if (m == null) {
			float[][] cs = paths.get(cp, family);
			if (cs.length == 0) return;
			m = cover(cs, sx, tx, ty);
			masks.put(cp, family, sx, tx, ty, m);
		}
		blit(m, argb);
	}
	public void rect(float x, float y, float w, float h, int argb) {
		begin();
		addPoly(rectPoly(ox + x * scale, oy + y * scale, ox + (x + w) * scale, oy + (y + h) * scale, 0f));
		blit(cover(null, 0f, 0f, 0f), argb);
	}
	public void frameRect(float x, float y, float w, float h, int argb, float strokeW) {
		float t = strokeW * scale * 0.5f;
		float x0 = ox + x * scale;
		float y0 = oy + y * scale;
		float x1 = ox + (x + w) * scale;
		float y1 = oy + (y + h) * scale;
		begin();
		addPoly(rectPoly(x0 - t, y0 - t, x1 + t, y1 + t, 0f));
		addPoly(rectPoly(x0 + t, y0 + t, x1 - t, y1 - t, 1f));
		blit(cover(null, 0f, 0f, 0f), argb);
	}
	public void line(float x1, float y1, float x2, float y2, int argb, float strokeW) {
		float ax = ox + x1 * scale;
		float ay = oy + y1 * scale;
		float bx = ox + x2 * scale;
		float by = oy + y2 * scale;
		float dx = bx - ax;
		float dy = by - ay;
		float len = (float) Math.sqrt(dx * dx + dy * dy);
		if (len == 0f) return;
		float t = strokeW * scale * 0.5f;
		float nx = -dy / len * t;
		float ny = dx / len * t;
		begin();
		addPoly(new float[]{ax + nx, ay + ny, bx + nx, by + ny, bx - nx, by - ny, ax - nx, ay - ny});
		blit(cover(null, 0f, 0f, 0f), argb);
	}
	public void beginGroup(int argb, boolean setFill, String cssClass, String id) {
	}
	public void endGroup() {
	}
	private static float[] rectPoly(float x0, float y0, float x1, float y1, float hole) {
		if (hole == 0f) return new float[]{x0, y0, x1, y0, x1, y1, x0, y1};
		return new float[]{x0, y0, x0, y1, x1, y1, x1, y0};
	}
	private void begin() {
		polys.clear();
	}
	private void addPoly(float[] p) {
		polys.add(p);
	}
	private GlyphMask cover(float[][] cs, float sx, float tx, float ty) {
		if (cs != null) {
			polys.clear();
			for (int i = 0; i < cs.length; i++) {
				float[] c = cs[i];
				int cn = c.length;
				float[] t = new float[cn];
				for (int k = 0; k < cn; k += 2) {
					t[k] = sx * c[k] + tx;
					t[k + 1] = sx * c[k + 1] + ty;
				}
				polys.add(t);
			}
		}
		buildEdges();
		if (edges == 0) {
			polys.clear();
			return null;
		}
		float minx = Float.MAX_VALUE;
		float miny = Float.MAX_VALUE;
		float maxx = -Float.MAX_VALUE;
		float maxy = -Float.MAX_VALUE;
		for (int i = 0; i < edges; i++) {
			float a = ex0[i] < ex1[i] ? ex0[i] : ex1[i];
			float b = ex0[i] < ex1[i] ? ex1[i] : ex0[i];
			float c = ey0[i] < ey1[i] ? ey0[i] : ey1[i];
			float d = ey0[i] < ey1[i] ? ey1[i] : ey0[i];
			if (a < minx) minx = a;
			if (b > maxx) maxx = b;
			if (c < miny) miny = c;
			if (d > maxy) maxy = d;
		}
		int bx = (int) Math.floor(minx) - 1;
		int by = (int) Math.floor(miny);
		int w = (int) Math.ceil(maxx) + 1 - bx + 1;
		int h = (int) Math.ceil(maxy) - by + 1;
		if (w < 1) w = 1;
		if (h < 1) h = 1;
		float[] cov = new float[w * h];
		int y0 = by;
		int y1 = by + h - 1;
		int x0 = bx;
		int x1 = bx + w - 1;
		if (bucket == null || bucket.length < h) bucket = new int[h];
		Arrays.fill(bucket, 0, h, -1);
		if (next == null || next.length < edges) next = new int[edges];
		for (int e = 0; e < edges; e++) {
			float ya = ey0[e];
			float yb = ey1[e];
			float lo = ya < yb ? ya : yb;
			float hi = ya < yb ? yb : ya;
			if (hi <= y0) continue;
			int r = (int) Math.floor(lo);
			if (r < y0) r = y0;
			if (r > y1) continue;
			next[e] = bucket[r - y0];
			bucket[r - y0] = e;
		}
		if (active == null || active.length < edges) active = new int[edges];
		activeN = 0;
		if (ev.length < edges) ev = new long[edges];
		float wgt = 1f / SUBS;
		for (int y = y0; y <= y1; y++) {
			int b = bucket[y - y0];
			while (b != -1) {
				active[activeN++] = b;
				b = next[b];
			}
			int keep = 0;
			for (int k = 0; k < activeN; k++) {
				int e = active[k];
				if ((ey0[e] > ey1[e] ? ey0[e] : ey1[e]) > y) active[keep++] = e;
			}
			activeN = keep;
			int off = (y - by) * w - bx;
			for (int k = 0; k < SUBS; k++) {
				float ys = y + (k + 0.5f) / SUBS;
				int m = 0;
				for (int a = 0; a < activeN; a++) {
					int e = active[a];
					float ya = ey0[e];
					float yb = ey1[e];
					if ((ya <= ys && yb > ys) || (yb <= ys && ya > ys)) {
						float xa = ex0[e];
						float xi = xa + (ys - ya) * (ex1[e] - xa) / (yb - ya);
						long key = (long) ((xi + BIAS) * 1024.0);
						ev[m++] = (key << 1) | (yb > ya ? 1L : 0L);
					}
				}
				if (m < 2) continue;
				Arrays.sort(ev, 0, m);
				int wind = 0;
				float start = 0f;
				for (int i = 0; i < m; i++) {
					long v = ev[i];
					float xi = (float) ((v >>> 1) / 1024.0 - BIAS);
					int prev = wind;
					wind += (v & 1L) != 0L ? 1 : -1;
					if (prev == 0 && wind != 0) start = xi;
					else if (prev != 0 && wind == 0) addSpan(cov, off, start, xi, wgt, x0, x1);
				}
			}
		}
		int n = 0;
		int[] sy = new int[16];
		int[] sx0 = new int[16];
		int[] sx1 = new int[16];
		float[] sv = new float[16];
		for (int r = 0; r < h; r++) {
			int base = r * w;
			int start = 0;
			float cur = 0f;
			for (int x = 0; x < w; x++) {
				float v = cov[base + x];
				if (v > 1f) v = 1f;
				if (v != cur) {
					if (cur > 0f) {
						if (n == sv.length) {
							sy = Arrays.copyOf(sy, n << 1);
							sx0 = Arrays.copyOf(sx0, n << 1);
							sx1 = Arrays.copyOf(sx1, n << 1);
							sv = Arrays.copyOf(sv, n << 1);
						}
						sy[n] = r;
						sx0[n] = start;
						sx1[n] = x - 1;
						sv[n] = cur;
						n++;
					}
					cur = v;
					start = x;
				}
			}
			if (cur > 0f) {
				if (n == sv.length) {
					sy = Arrays.copyOf(sy, n << 1);
					sx0 = Arrays.copyOf(sx0, n << 1);
					sx1 = Arrays.copyOf(sx1, n << 1);
					sv = Arrays.copyOf(sv, n << 1);
				}
				sy[n] = r;
				sx0[n] = start;
				sx1[n] = w - 1;
				sv[n] = cur;
				n++;
			}
		}
		polys.clear();
		return new GlyphMask(bx, by, n, sy, sx0, sx1, sv);
	}
	private void addSpan(float[] cov, int off, float xa, float xb, float wgt, int x0, int x1) {
		if (xb <= xa) return;
		int ia = (int) Math.floor(xa);
		int ib = (int) Math.ceil(xb);
		if (ia < x0) ia = x0;
		if (ib > x1 + 1) ib = x1 + 1;
		for (int px = ia; px < ib; px++) {
			float l = xa > px ? xa : px;
			float r = xb < px + 1 ? xb : px + 1;
			if (r > l) cov[off + px] += (r - l) * wgt;
		}
	}
	private void blit(GlyphMask m, int argb) {
		if (m == null) return;
		boolean opaque = (argb >>> 24) == 0xFF;
		for (int i = 0; i < m.n; i++) {
			int y = m.baseY + m.sy[i];
			if (y < 0 || y >= height) continue;
			int a = m.baseX + m.sx0[i];
			int b = m.baseX + m.sx1[i];
			if (a < 0) a = 0;
			if (b >= width) b = width - 1;
			if (a > b) continue;
			float v = m.v[i];
			int row = y * width;
			if (v == 1f && opaque) {
				for (int x = a; x <= b; x++) pixels[row + x] = argb | 0xFF000000;
			} else {
				for (int x = a; x <= b; x++) blend(x, y, argb, v);
			}
		}
	}
	private void buildEdges() {
		int total = 0;
		for (int i = 0; i < polys.size(); i++) total += polys.get(i).length >> 1;
		if (ex0 == null || ex0.length < total) {
			ex0 = new float[total];
			ey0 = new float[total];
			ex1 = new float[total];
			ey1 = new float[total];
		}
		int m = 0;
		for (int i = 0; i < polys.size(); i++) {
			float[] p = polys.get(i);
			int k = p.length >> 1;
			if (k < 3) continue;
			for (int j = 0; j < k; j++) {
				int a = j << 1;
				int b = ((j + 1) % k) << 1;
				ex0[m] = p[a];
				ey0[m] = p[a + 1];
				ex1[m] = p[b];
				ey1[m] = p[b + 1];
				m++;
			}
		}
		edges = m;
	}
	private void blend(int x, int y, int argb, float c) {
		float sa = ((argb >>> 24) / 255f) * c;
		if (sa <= 0f) return;
		int i = y * width + x;
		if (sa >= 1f) {
			pixels[i] = argb | 0xFF000000;
			return;
		}
		int dp = pixels[i];
		float ia = 1f - sa;
		float da = (dp >>> 24) / 255f;
		if (da >= 1f) {
			int r = (int) ((argb >> 16 & 255) * sa + (dp >> 16 & 255) * ia + 0.5f);
			int g = (int) ((argb >> 8 & 255) * sa + (dp >> 8 & 255) * ia + 0.5f);
			int b = (int) ((argb & 255) * sa + (dp & 255) * ia + 0.5f);
			pixels[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
			return;
		}
		float oa = sa + da * ia;
		float keep = da * ia;
		int r = (int) (((argb >> 16 & 255) * sa + (dp >> 16 & 255) * keep) / oa + 0.5f);
		int g = (int) (((argb >> 8 & 255) * sa + (dp >> 8 & 255) * keep) / oa + 0.5f);
		int b = (int) (((argb & 255) * sa + (dp & 255) * keep) / oa + 0.5f);
		pixels[i] = ((int) (oa * 255f + 0.5f) << 24) | (r << 16) | (g << 8) | b;
	}
}