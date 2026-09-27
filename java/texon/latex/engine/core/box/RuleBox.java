package texon.latex.engine.core.box;
public final class RuleBox extends Box {
	public RuleBox(int width, int height, int depth) {
		super(RULE, width, height, depth, 0, 0);
	}
	public static RuleBox horizontal(int width, int thickness) {
		return new RuleBox(width, thickness, 0);
	}
}