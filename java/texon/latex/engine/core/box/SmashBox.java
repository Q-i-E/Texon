package texon.latex.engine.core.box;
public final class SmashBox extends Box {
	public final Box child;
	public SmashBox(Box child) {
		super(SMASH, child.width, 0, 0, child.italic, child.inkLeft);
		this.child = child;
	}
}