package texon.latex.engine.core.box;
public abstract class Box {
	public static final int GLYPH = 0;
	public static final int RULE = 1;
	public static final int HBOX = 2;
	public static final int VBOX = 3;
	public static final int ERROR = 4;
	public static final int SMASH = 5;
	public static final int LINE = 6;
	public static final int PHANTOM = 7;
	public static final int STYLED = 8;
	public static final int SMASHOP = 9;
	public final int kind;
	public final int width;
	public final int height;
	public final int depth;
	public final int italic;
	public final int inkLeft;
	protected Box(int kind, int width, int height, int depth, int italic, int inkLeft) {
		this.kind = kind;
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.italic = italic;
		this.inkLeft = inkLeft;
	}
	public final int totalHeight() {
		return height + depth;
	}
}