package texon.latex.engine.core.box;
public final class VBox extends Box {
	public static final int CENTER = 0;
	public static final int LEFT = 1;
	public final Box[] children;
	public final int[] offset;
	public final int align;
	public VBox(Box[] children, int[] offset) {
		this(children, offset, CENTER);
	}
	public VBox(Box[] children, int[] offset, int align) {
		this(children, offset, align, widthOf(children));
	}
	private VBox(Box[] children, int[] offset, int align, int width) {
		super(VBOX, width, heightOf(children, offset), depthOf(children, offset), italicOf(children, align, width), inkLeftOf(children, align, width));
		this.children = children;
		this.offset = offset;
		this.align = align;
	}
	public static VBox of(Box[] children, int[] offset) {
		return new VBox(children, offset);
	}
	public static VBox of(Box[] children, int[] offset, int align) {
		return new VBox(children, offset, align);
	}
	public int childX(int index) {
		if (align == LEFT) return 0;
		return (width - children[index].width) / 2;
	}
	private static int widthOf(Box[] children) {
		int w = 0;
		for (int i = 0; i < children.length; i++) {
			if (children[i].width > w) w = children[i].width;
		}
		return w;
	}
	private static int heightOf(Box[] children, int[] offset) {
		int h = 0;
		for (int i = 0; i < children.length; i++) {
			int top = offset[i] + children[i].height;
			if (top > h) h = top;
		}
		return h;
	}
	private static int depthOf(Box[] children, int[] offset) {
		int d = 0;
		for (int i = 0; i < children.length; i++) {
			int bottom = children[i].depth - offset[i];
			if (bottom > d) d = bottom;
		}
		return d;
	}
	private static int italicOf(Box[] children, int align, int width) {
		int ic = 0;
		for (int i = 0; i < children.length; i++) {
			int x = align == LEFT ? 0 : (width - children[i].width) / 2;
			int right = x + children[i].width + children[i].italic - width;
			if (right > ic) ic = right;
		}
		return ic;
	}
	private static int inkLeftOf(Box[] children, int align, int width) {
		if (children.length == 0) return 0;
		int min = Integer.MAX_VALUE;
		for (int i = 0; i < children.length; i++) {
			int x = align == LEFT ? 0 : (width - children[i].width) / 2;
			int ink = x + children[i].inkLeft;
			if (ink < min) min = ink;
		}
		return min;
	}
}