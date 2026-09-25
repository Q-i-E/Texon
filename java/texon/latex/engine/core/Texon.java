package texon.latex.engine.core;
import java.io.*;
import texon.latex.engine.core.box.*;
import texon.latex.engine.core.layout.*;
import texon.latex.engine.core.lex.*;
import texon.latex.engine.core.parse.*;
import texon.latex.engine.core.render.*;
public final class Texon {
	private final FontMetrics metrics;
	private final GlyphOutlines outlines;
	private final Layout layout;
	private final Ast ast = new Ast();
	private final Parser parser;
	private final LayoutCache cache;
	private final GlyphPaths paths;
	private final MaskCache masks;
	private RasterSink realtime;
	private Texon(FontMetrics metrics, GlyphOutlines outlines) {
		this.metrics = metrics;
		this.outlines = outlines;
		this.layout = new Layout(metrics);
		this.parser = new Parser(new Lexer(), ast);
		this.cache = new LayoutCache(layout, ast, parser, 256);
		this.paths = new GlyphPaths(outlines, 4096);
		this.masks = new MaskCache(4096);
	}
	public static Texon load(InputStream metrics, InputStream outlines) throws IOException {
		return new Texon(FontMetrics.load(metrics), GlyphOutlines.load(outlines));
	}
	public static Texon load(Assets in, String metrics, String outlines) throws IOException {
		FontMetrics fm = FontMetrics.load(in, metrics);
		GlyphOutlines ol = GlyphOutlines.load(in, outlines);
		families(fm, ol, in);
		return new Texon(fm, ol);
	}
	public static void families(FontMetrics fm, GlyphOutlines ol, Assets in) {
		addFamily(fm, ol, in, FamilyTable.SF, "cjk-sans");
		addFamily(fm, ol, in, FamilyTable.TT, "cjk-sans");
	}
	private static void addFamily(FontMetrics fm, GlyphOutlines ol, Assets in, int family, String asset) {
		try {
			fm.addFamily(family, in, asset + "-metrics");
			ol.addFamily(family, in, asset + "-outlines");
		} catch (IOException e) {
		}
	}
	public static Texon loadResource(String metrics, String outlines) throws IOException {
		InputStream a = Texon.class.getClassLoader().getResourceAsStream(metrics);
		InputStream b = Texon.class.getClassLoader().getResourceAsStream(outlines);
		if (a == null || b == null) throw new IOException("resource not found");
		return load(a, b);
	}
	public String svg(String latex, float pxPerEm) {
		int upm = metrics.unitsPerEm;
		return new SvgRenderer(outlines).scale(pxPerEm / upm).render(box(latex));
	}
	public Raster raster(String latex, float pxPerEm) {
		return raster(latex, pxPerEm, 0xFFFFFFFF);
	}
	public Raster raster(String latex, float pxPerEm, int background) {
		Box root = box(latex);
		RasterSink sink = sink(root, pxPerEm, background, null);
		Renderer.walk(root, 0xFF000000, sink);
		return sink.result();
	}
	public Raster render(String latex, float pxPerEm) {
		return render(latex, pxPerEm, 0xFFFFFFFF);
	}
	public Raster render(String latex, float pxPerEm, int background) {
		Box root = box(latex);
		realtime = sink(root, pxPerEm, background, realtime);
		Renderer.walk(root, 0xFF000000, realtime);
		return realtime.result();
	}
	public byte[] png(String latex, float pxPerEm) {
		return Png.encode(raster(latex, pxPerEm));
	}
	public byte[] png(String latex, float pxPerEm, int background) {
		return Png.encode(raster(latex, pxPerEm, background));
	}
	private RasterSink sink(Box root, float pxPerEm, int background, RasterSink reuse) {
		int upm = metrics.unitsPerEm;
		float scale = pxPerEm / upm;
		int ox = root.inkLeft < 0 ? -root.inkLeft : 0;
		int w = (int) Math.ceil((root.width + root.italic + ox) * scale);
		int h = (int) Math.ceil(root.totalHeight() * scale);
		if (w < 1) w = 1;
		if (h < 1) h = 1;
		if (reuse == null) return new RasterSink(outlines, paths, masks, w, h, scale, ox * scale, root.height * scale, background);
		return reuse.reset(w, h, scale, ox * scale, root.height * scale, background);
	}
	private Box box(String latex) {
		return cache.build(latex);
	}
}
