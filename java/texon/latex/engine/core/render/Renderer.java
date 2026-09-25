package texon.latex.engine.core.render;
import texon.latex.engine.core.box.*;
public final class Renderer {
	public static final int ERROR_COLOR = 0xFFFF3B30;
	private Renderer() {
	}
	public static void walk(Box box, int argb, VectorSink sink) {
		walk(box, 0f, 0f, argb, sink);
	}
	public static void walk(Box box, float x, float baseline, int argb, VectorSink sink) {
		switch (box.kind) {
			case Box.GLYPH: {
				GlyphBox g = (GlyphBox) box;
				sink.glyph(g.cp, x, baseline, g.percent, g.family, argb);
				break;
			}
			case Box.RULE:
				sink.rect(x, baseline - box.height, box.width, box.height + box.depth, argb);
				break;
			case Box.ERROR:
				sink.rect(x, baseline - box.height, box.width, box.height + box.depth, ERROR_COLOR);
				break;
			case Box.SMASH:
				walk(((SmashBox) box).child, x, baseline, argb, sink);
				break;
			case Box.SMASHOP: {
				SmashOpBox b = (SmashOpBox) box;
				walk(b.child, x + b.shift, baseline, argb, sink);
				break;
			}
			case Box.STYLED: {
				StyledBox b = (StyledBox) box;
				if (b.bg != StyledBox.NOCOLOR) sink.rect(x, baseline - b.height, b.width, b.height + b.depth, b.bg);
				if (b.frame != StyledBox.NOCOLOR) sink.frameRect(x, baseline - b.height, b.width, b.height + b.depth, b.frame, b.frameW);
				int child = b.fg != StyledBox.NOCOLOR ? b.fg : argb;
				boolean wrap = b.fg != StyledBox.NOCOLOR || b.cssClass != null || b.id != null;
				if (wrap) sink.beginGroup(child, b.fg != StyledBox.NOCOLOR, b.cssClass, b.id);
				walk(b.child, x + b.pad, baseline, child, sink);
				if (wrap) sink.endGroup();
				break;
			}
			case Box.LINE: {
				LineBox b = (LineBox) box;
				sink.line(x + b.x0, baseline - b.y0, x + b.x1, baseline - b.y1, argb, b.thickness);
				break;
			}
			case Box.HBOX: {
				HBox h = (HBox) box;
				for (int i = 0; i < h.children.length; i++) walk(h.children[i], x + h.left[i], baseline - h.shift[i], argb, sink);
				break;
			}
			case Box.VBOX: {
				VBox v = (VBox) box;
				for (int i = 0; i < v.children.length; i++) walk(v.children[i], x + v.childX(i), baseline - v.offset[i], argb, sink);
				break;
			}
		}
	}
}
