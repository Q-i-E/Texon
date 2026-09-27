package texon.latex.engine.core.box;
public final class StyledBox extends Box {
	public static final int NOCOLOR = 0;
	public final Box child;
	public final int fg;
	public final int bg;
	public final int frame;
	public final int frameW;
	public final int pad;
	public final String cssClass;
	public final String id;
	public StyledBox(Box child, int fg, int bg, int frame, int frameW, int pad, String cssClass, String id) {
		super(STYLED, child.width + pad * 2, child.height + pad, child.depth + pad, child.italic, min(child.inkLeft, -pad));
		this.child = child;
		this.fg = fg;
		this.bg = bg;
		this.frame = frame;
		this.frameW = frameW;
		this.pad = pad;
		this.cssClass = cssClass;
		this.id = id;
	}
	private static int min(int a, int b) {
		return a < b ? a : b;
	}
}