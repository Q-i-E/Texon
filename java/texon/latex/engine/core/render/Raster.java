package texon.latex.engine.core.render;
public final class Raster {
	public final int width;
	public final int height;
	public final int[] pixels;
	public final float baseline;
	public final int inkX;
	public final int inkY;
	public final int inkW;
	public final int inkH;
	public Raster(int width, int height, int[] pixels, float baseline, int inkX, int inkY, int inkW, int inkH) {
		this.width = width;
		this.height = height;
		this.pixels = pixels;
		this.baseline = baseline;
		this.inkX = inkX;
		this.inkY = inkY;
		this.inkW = inkW;
		this.inkH = inkH;
	}
}