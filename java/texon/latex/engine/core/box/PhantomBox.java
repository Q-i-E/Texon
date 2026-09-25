package texon.latex.engine.core.box;
public final class PhantomBox extends Box {
	public PhantomBox(int width, int height, int depth) {
		super(PHANTOM, width, height, depth, 0, 0);
	}
}