package texon.latex.engine.core.render;
import java.util.*;
import texon.latex.engine.core.*;
import texon.latex.engine.core.box.*;
public final class SvgRenderer implements VectorSink {
	private final GlyphOutlines outlines;
	private final LinkedHashMap<Integer, String> glyphs = new LinkedHashMap<>();
	private final StringBuilder sb = new StringBuilder(1 << 16);
	private StringBuilder out = sb;
	private float scale = 1f;
	private int fill = 0xFF000000;
	private int groupFill;
	private int[] gstack = new int[4];
	private int depth;
	public SvgRenderer(GlyphOutlines outlines) {
		this.outlines = outlines;
		this.groupFill = fill;
	}
	public SvgRenderer scale(float px) {
		this.scale = px;
		return this;
	}
	public SvgRenderer color(int argb) {
		this.fill = argb;
		this.groupFill = argb;
		return this;
	}
	public String render(Box root) {
		glyphs.clear();
		sb.setLength(0);
		if (root == null) return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1\" height=\"1\"/>";
		StringBuilder body = new StringBuilder(1 << 16);
		out = body;
		int ox = root.inkLeft < 0 ? -root.inkLeft : 0;
		openGroup(ox, root.height, fill);
		Renderer.walk(root, fill, this);
		body.append("</g>");
		out = sb;
		int wu = root.width + root.italic + ox;
		int hu = root.totalHeight();
		sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\"").append((int) Math.ceil(wu * scale)).append("\" height=\"").append((int) Math.ceil(hu * scale)).append("\" viewBox=\"0 0 ").append(wu).append(' ').append(hu).append("\">");
		appendDefs(sb);
		sb.append(body).append("</svg>");
		return sb.toString();
	}
	public String group(Box root) {
		return group(root, 0, 0);
	}
	public String group(Box root, int x, int y) {
		sb.setLength(0);
		out = sb;
		int ox = root.inkLeft < 0 ? -root.inkLeft : 0;
		openGroup(x + ox, y + root.height, fill);
		Renderer.walk(root, fill, this);
		sb.append("</g>");
		return sb.toString();
	}
	public String defs() {
		if (glyphs.isEmpty()) return "";
		StringBuilder b = new StringBuilder(1 << 16);
		appendDefs(b);
		return b.toString();
	}
	private void appendDefs(StringBuilder b) {
		if (glyphs.isEmpty()) return;
		b.append("<defs>");
		for (Map.Entry<Integer, String> e : glyphs.entrySet()) {
			b.append("<path id=\"").append(gidOf(e.getKey())).append("\" d=\"").append(e.getValue()).append("\"/>");
		}
		b.append("</defs>");
	}
	public void glyph(int cp, float x, float baseline, int percent, int family, int argb) {
		int k = key(cp, family);
		String d = glyphs.get(k);
		if (d == null) {
			d = outlines == null ? null : outlines.path(cp, family);
			if (d == null || d.length() == 0) return;
			glyphs.put(k, d);
		}
		out.append("<use xlink:href=\"#").append(gid(cp, family)).append('"');
		if (percent != GlyphBox.FULL) {
			float f = percent * 0.01f;
			out.append(" transform=\"translate(").append(num(x)).append(' ').append(num(baseline)).append(") scale(").append(num(f)).append(")\"");
		} else if (x != 0 || baseline != 0) {
			out.append(" transform=\"translate(").append(num(x)).append(' ').append(num(baseline)).append(")\"");
		}
		fillAttr(argb);
		out.append("/>");
	}
	public void rect(float x, float y, float w, float h, int argb) {
		out.append("<rect x=\"").append(num(x)).append("\" y=\"").append(num(y)).append("\" width=\"").append(num(w)).append("\" height=\"").append(num(h)).append('"');
		fillAttr(argb);
		out.append("/>");
	}
	public void frameRect(float x, float y, float w, float h, int argb, float strokeW) {
		int sw = (int) strokeW;
		int half = sw / 2;
		out.append("<rect x=\"").append(num(x + half)).append("\" y=\"").append(num(y + half)).append("\" width=\"").append(num(w - sw)).append("\" height=\"").append(num(h - sw)).append("\" fill=\"none\" stroke=\"").append(rgb(argb)).append('"');
		opacity(argb, "stroke-opacity");
		out.append(" stroke-width=\"").append(sw).append("\"/>");
	}
	public void line(float x1, float y1, float x2, float y2, int argb, float strokeW) {
		out.append("<line x1=\"").append(num(x1)).append("\" y1=\"").append(num(y1)).append("\" x2=\"").append(num(x2)).append("\" y2=\"").append(num(y2)).append("\" stroke=\"").append(rgb(argb)).append('"');
		opacity(argb, "stroke-opacity");
		out.append(" stroke-width=\"").append(num(strokeW)).append("\"/>");
	}
	public void beginGroup(int argb, boolean setFill, String cssClass, String id) {
		if (depth == gstack.length) gstack = java.util.Arrays.copyOf(gstack, depth << 1);
		gstack[depth++] = groupFill;
		out.append("<g");
		if (setFill) fillAttr(argb);
		if (cssClass != null) out.append(" class=\"").append(attr(cssClass)).append('"');
		if (id != null) out.append(" id=\"").append(attr(id)).append('"');
		out.append('>');
		groupFill = argb;
	}
	public void endGroup() {
		out.append("</g>");
		groupFill = gstack[--depth];
	}
	private void openGroup(int x, int y, int color) {
		out.append("<g transform=\"translate(").append(x).append(' ').append(y).append(")\"");
		if (color != 0xFF000000) {
			out.append(" fill=\"").append(rgb(color)).append('"');
			opacity(color, "fill-opacity");
		}
		out.append('>');
		groupFill = color;
	}
	private void fillAttr(int argb) {
		if (argb != groupFill) {
			out.append(" fill=\"").append(rgb(argb)).append('"');
			opacity(argb, "fill-opacity");
		}
	}
	private void opacity(int argb, String name) {
		int a = argb >>> 24;
		if (a != 0xFF) out.append(' ').append(name).append("=\"").append(num(a / 255f)).append('"');
	}
	private static int key(int cp, int family) {
		return (family << 21) | cp;
	}
	private static String gidOf(int k) {
		return gid(k & 0x1FFFFF, k >>> 21 & 15);
	}
	private static String gid(int cp, int family) {
		return family == 0 ? "g" + Integer.toHexString(cp) : "g" + Integer.toHexString(cp) + "f" + Integer.toHexString(family);
	}
	private static String rgb(int argb) {
		return String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF);
	}
	private static void escape(StringBuilder b, int cp) {
		if (cp == '&') b.append('&').append("amp;");
		else if (cp == '<') b.append('&').append("lt;");
		else if (cp == '>') b.append('&').append("gt;");
		else if (cp == '"') b.append('&').append("quot;");
		else b.appendCodePoint(cp);
	}
	private static String attr(String s) {
		StringBuilder b = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) escape(b, s.charAt(i));
		return b.toString();
	}
	private static String num(float v) {
		if (v == (long) v) return Long.toString((long) v);
		String s = String.format(Locale.ROOT, "%.3f", v);
		while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
		if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
		return s;
	}
}
