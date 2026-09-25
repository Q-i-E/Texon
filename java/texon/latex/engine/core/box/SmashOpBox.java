package texon.latex.engine.core.box;
public final class SmashOpBox extends Box {
	public final Box child;
	public final int shift;
	public SmashOpBox(Box child, int width, int shift) {
		super(SMASHOP, width, child.height, child.depth, child.italic, child.inkLeft);
		this.child = child;
		this.shift = shift;
	}
}