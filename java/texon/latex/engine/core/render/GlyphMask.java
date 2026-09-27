package texon.latex.engine.core.render;
public final class GlyphMask {
	public final int baseX;
	public final int baseY;
	public final int n;
	public final int[] sy;
	public final int[] sx0;
	public final int[] sx1;
	public final float[] v;
	public GlyphMask(int baseX, int baseY, int n, int[] sy, int[] sx0, int[] sx1, float[] v) {
		this.baseX = baseX;
		this.baseY = baseY;
		this.n = n;
		this.sy = sy;
		this.sx0 = sx0;
		this.sx1 = sx1;
		this.v = v;
	}
}