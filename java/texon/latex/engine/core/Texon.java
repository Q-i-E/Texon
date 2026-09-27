package texon.latex.engine.core;
import java.io.*;
import java.util.concurrent.*;
import texon.latex.engine.config.*;
import texon.latex.engine.core.box.*;
import texon.latex.engine.core.layout.*;
import texon.latex.engine.core.lex.*;
import texon.latex.engine.core.parse.*;
import texon.latex.engine.core.render.*;
public final class Texon {
	private final FontMetrics metrics;
	private final GlyphOutlines outlines;
	private final TexonContext ctx;
	private final Layout layout;
	private final Ast ast = new Ast();
	private final Parser parser;
	private final LayoutCache cache;
	private final GlyphPaths paths;
	private final MaskCache masks;
	private RasterSink realtime;
	private Texon(FontMetrics metrics, GlyphOutlines outlines, TexonContext ctx) {
		this.metrics = metrics;
		this.outlines = outlines;
		this.ctx = ctx;
		this.layout = new Layout(metrics, ctx);
		this.parser = new Parser(new Lexer(ctx), ast, ctx);
		this.cache = new LayoutCache(layout, ast, parser, ctx.options.layoutCacheCapacity);
		this.paths = new GlyphPaths(outlines, ctx.options.glyphPathsCapacity);
		this.masks = new MaskCache(ctx.options.maskCacheCapacity);
	}
	public TexonContext context() {
		return ctx;
	}
	public FontMetrics metrics() {
		return metrics;
	}
	public GlyphOutlines outlines() {
		return outlines;
	}
	public Layout layout() {
		return layout;
	}
	public Box box(String latex) {
		return cache.build(latex);
	}
	public void draw(String latex, int argb, VectorSink sink) {
		Renderer.walk(box(latex), argb, sink, ctx.options.errorColor);
	}
	public void draw(Box root, int argb, VectorSink sink) {
		Renderer.walk(root, argb, sink, ctx.options.errorColor);
	}
	public LayoutMetrics measure(String latex, float pxPerEm) {
		return measure(box(latex), pxPerEm);
	}
	public LayoutMetrics measure(Box root, float pxPerEm) {
		float scale = pxPerEm / metrics.unitsPerEm;
		int ox = root.inkLeft < 0 ? -root.inkLeft : 0;
		return new LayoutMetrics((root.width + root.italic + ox) * scale, root.totalHeight() * scale, root.height * scale, ox * scale);
	}
	public Box row(String[] parts) {
		Box[] b = new Box[parts.length];
		for (int i = 0; i < parts.length; i++) b[i] = box(parts[i]);
		return HBox.of(b);
	}
	public Box row(String[] parts, int gap) {
		Box[] b = new Box[parts.length];
		int[] kern = new int[parts.length];
		for (int i = 0; i < parts.length; i++) {
			b[i] = box(parts[i]);
			if (i < parts.length - 1) kern[i] = gap;
		}
		return HBox.kerned(b, new int[parts.length], kern);
	}
	public Box block(String[] lines, int lineGap) {
		return block(lines, lineGap, VBox.LEFT);
	}
	public Box block(String[] lines, int lineGap, int align) {
		Box[] b = new Box[lines.length];
		for (int i = 0; i < lines.length; i++) b[i] = box(lines[i]);
		int[] off = new int[b.length];
		for (int i = 1; i < b.length; i++) off[i] = off[i - 1] - (b[i - 1].depth + lineGap + b[i].height);
		return VBox.of(b, off, align);
	}
	public Raster raster(Box root, float pxPerEm) {
		return raster(root, pxPerEm, 0xFFFFFFFF);
	}
	public Raster raster(Box root, float pxPerEm, int background) {
		RasterSink sink = sink(root, pxPerEm, background, null);
		Renderer.walk(root, 0xFF000000, sink, ctx.options.errorColor);
		return sink.result();
	}
	public void prefetch(CharSequence latex) {
		Renderer.walk(box(latex.toString()), 0xFF000000, new PrefetchSink(metrics, outlines), ctx.options.errorColor);
	}
	public void prefetchAsync(CharSequence latex, Executor executor) {
		executor.execute(() -> prefetch(latex));
	}
	private static final class PrefetchSink implements VectorSink {
		private final FontMetrics fm;
		private final GlyphOutlines ol;
		PrefetchSink(FontMetrics fm, GlyphOutlines ol) {
			this.fm = fm;
			this.ol = ol;
		}
		public void glyph(int cp, float x, float baseline, int percent, int family, int argb) {
			fm.width(cp, family);
			ol.path(cp, family);
		}
		public void rect(float x, float y, float w, float h, int argb) {
		}
		public void frameRect(float x, float y, float w, float h, int argb, float strokeW) {
		}
		public void line(float x1, float y1, float x2, float y2, int argb, float strokeW) {
		}
		public void beginGroup(int argb, boolean setFill, String cssClass, String id) {
		}
		public void endGroup() {
		}
	}
	public Texon addFamily(int family, Assets in, String stem) throws IOException {
		metrics.addFamily(family, in, stem);
		outlines.addFamily(family, in, stem);
		return this;
	}
	public static Texon load(InputStream metrics, InputStream outlines) throws IOException {
		return load(metrics, outlines, new TexonOptions());
	}
	public static Texon load(InputStream metrics, InputStream outlines, TexonOptions options) throws IOException {
		TexonContext ctx = new TexonContext(options);
		return new Texon(FontMetrics.load(metrics, ctx), GlyphOutlines.load(outlines), ctx);
	}
	public static Texon load(Assets in, String metrics, String outlines) throws IOException {
		return load(in, metrics, outlines, new TexonOptions());
	}
	public static Texon load(Assets in, String metrics, String outlines, TexonOptions options) throws IOException {
		return TexonAssets.load(in, outlines, options.blobBudget).newTexon(in, metrics, options);
	}
	static Texon create(Assets in, String metrics, GlyphOutlines outlines, TexonOptions options) throws IOException {
		TexonContext ctx = new TexonContext(options);
		FontMetrics fm = FontMetrics.load(in, metrics, ctx);
		families(fm, outlines, in);
		return new Texon(fm, outlines, ctx);
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
		return new SvgRenderer(outlines, ctx.options.svgPrecision, ctx.options.svgIdPrefix).scale(pxPerEm / upm).render(box(latex));
	}
	public Raster raster(String latex, float pxPerEm) {
		return raster(latex, pxPerEm, 0xFFFFFFFF);
	}
	public Raster raster(String latex, float pxPerEm, int background) {
		Box root = box(latex);
		RasterSink sink = sink(root, pxPerEm, background, null);
		Renderer.walk(root, 0xFF000000, sink, ctx.options.errorColor);
		return sink.result();
	}
	public Raster render(String latex, float pxPerEm) {
		return render(latex, pxPerEm, 0xFFFFFFFF);
	}
	public Raster render(String latex, float pxPerEm, int background) {
		Box root = box(latex);
		realtime = sink(root, pxPerEm, background, realtime);
		Renderer.walk(root, 0xFF000000, realtime, ctx.options.errorColor);
		return realtime.result();
	}
	public byte[] png(String latex, float pxPerEm) {
		return Png.encode(raster(latex, pxPerEm), ctx.options.pngLevel, ctx.options.pngFilter, ctx.options.pngColorMode);
	}
	public byte[] png(String latex, float pxPerEm, int background) {
		return Png.encode(raster(latex, pxPerEm, background), ctx.options.pngLevel, ctx.options.pngFilter, ctx.options.pngColorMode);
	}
	private RasterSink sink(Box root, float pxPerEm, int background, RasterSink reuse) {
		int upm = metrics.unitsPerEm;
		float scale = pxPerEm / upm;
		int ox = root.inkLeft < 0 ? -root.inkLeft : 0;
		int w = (int) Math.ceil((root.width + root.italic + ox) * scale);
		int h = (int) Math.ceil(root.totalHeight() * scale);
		if (w < 1) w = 1;
		if (h < 1) h = 1;
		if (reuse == null) return new RasterSink(outlines, paths, masks, w, h, scale, ox * scale, root.height * scale, background, ctx.options.rasterSupersample);
		return reuse.reset(w, h, scale, ox * scale, root.height * scale, background);
	}
}
