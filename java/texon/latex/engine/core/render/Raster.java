package texon.latex.engine.core.render;
public final class Raster {
	public final int width;
	public final int height;
	public final int[] pixels;
	public Raster(int width, int height, int[] pixels) {
		this.width = width;
		this.height = height;
		this.pixels = pixels;
	}
}
