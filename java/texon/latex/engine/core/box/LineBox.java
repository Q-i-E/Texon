package texon.latex.engine.core.box;
public final class LineBox extends Box {
	public final int x0;
	public final int y0;
	public final int x1;
	public final int y1;
	public final int thickness;
	public LineBox(int width, int height, int depth, int x0, int y0, int x1, int y1, int thickness) {
		super(LINE, width, height, depth, 0, 0);
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
		this.thickness = thickness;
	}
}