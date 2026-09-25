package texon.latex.engine.core.box;
public final class ErrorBox extends Box {
	public final int pos;
	public final int len;
	public final int code;
	public ErrorBox(int width, int height, int depth) {
		this(width, height, depth, -1, 0, 0);
	}
	public ErrorBox(int width, int height, int depth, int pos, int len, int code) {
		super(ERROR, width, height, depth, 0, 0);
		this.pos = pos;
		this.len = len;
		this.code = code;
	}
}