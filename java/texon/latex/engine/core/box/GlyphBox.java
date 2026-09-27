package texon.latex.engine.core.box;
import texon.latex.engine.core.*;
public final class GlyphBox extends Box {
	public static final int FULL = 100;
	public final int cp;
	public final int percent;
	public byte family;
	public GlyphBox(int cp, int width, int height, int depth, int percent, int italic, int inkLeft) {
		super(GLYPH, width, height, depth, italic, inkLeft);
		this.cp = cp;
		this.percent = percent;
	}
	public static GlyphBox of(FontMetrics fm, int cp) {
		return of(fm, cp, 0);
	}
	public static GlyphBox of(FontMetrics fm, int cp, int family) {
		GlyphBox b = new GlyphBox(cp, fm.width(cp, family), fm.height(cp, family), fm.depth(cp, family), FULL, fm.italic(cp, family), fm.inkLeft(cp, family));
		b.family = (byte) family;
		return b;
	}
	public static GlyphBox scaled(FontMetrics fm, int cp, int percent) {
		return scaled(fm, cp, percent, 0);
	}
	public static GlyphBox scaled(FontMetrics fm, int cp, int percent, int family) {
		GlyphBox b = new GlyphBox(
			cp,
			fm.width(cp, family) * percent / FULL,
			fm.height(cp, family) * percent / FULL,
			fm.depth(cp, family) * percent / FULL,
			percent,
			fm.italic(cp, family) * percent / FULL,
			fm.inkLeft(cp, family) * percent / FULL
		);
		b.family = (byte) family;
		return b;
	}
}
