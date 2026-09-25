package texon.latex.engine.core.box;
public final class HBox extends Box {
	public final Box[] children;
	public final int[] shift;
	public final int[] left;
	public final int[] kern;
	public HBox(Box[] children, int[] shift) {
		this(children, shift, null);
	}
	public HBox(Box[] children, int[] shift, int[] kern) {
		this(children, shift, kern, widthOf(children, kern));
	}
	private HBox(Box[] children, int[] shift, int[] kern, int width) {
		super(HBOX, width, heightOf(children, shift), depthOf(children, shift), italicOf(children, kern, width), inkLeftOf(children));
		this.children = children;
		this.shift = shift;
		this.kern = kern;
		this.left = prefix(children, kern);
	}
	public static HBox of(Box[] children) {
		return new HBox(children, new int[children.length]);
	}
	public static HBox of(Box[] children, int[] shift) {
		return new HBox(children, shift);
	}
	public static HBox kerned(Box[] children, int[] shift, int[] kern) {
		return new HBox(children, shift, kern);
	}
	private static int[] prefix(Box[] children, int[] kern) {
		int[] out = new int[children.length];
		int x = 0;
		for (int i = 0; i < children.length; i++) {
			out[i] = x;
			x += children[i].width;
			if (kern != null) x += kern[i];
		}
		return out;
	}
	private static int widthOf(Box[] children, int[] kern) {
		int w = 0;
		for (int i = 0; i < children.length; i++) {
			w += children[i].width;
			if (kern != null) w += kern[i];
		}
		return w;
	}
	private static int heightOf(Box[] children, int[] shift) {
		int h = 0;
		for (int i = 0; i < children.length; i++) {
			int top = children[i].height + shift[i];
			if (top > h) h = top;
		}
		return h;
	}
	private static int depthOf(Box[] children, int[] shift) {
		int d = 0;
		for (int i = 0; i < children.length; i++) {
			int bottom = children[i].depth - shift[i];
			if (bottom > d) d = bottom;
		}
		return d;
	}
	private static int italicOf(Box[] children, int[] kern, int width) {
		if (children.length == 0) return 0;
		int n = children.length - 1;
		int x = 0;
		for (int i = 0; i < n; i++) {
			x += children[i].width;
			if (kern != null) x += kern[i];
		}
		return x + children[n].width + children[n].italic - width;
	}
	private static int inkLeftOf(Box[] children) {
		return children.length == 0 ? 0 : children[0].inkLeft;
	}
}