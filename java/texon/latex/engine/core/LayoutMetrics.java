package texon.latex.engine.core;
public final class LayoutMetrics {
	public final float width;
	public final float height;
	public final float baseline;
	public final float inkLeft;
	public LayoutMetrics(float width, float height, float baseline, float inkLeft) {
		this.width = width;
		this.height = height;
		this.baseline = baseline;
		this.inkLeft = inkLeft;
	}
}