package texon.latex.engine.core;
import java.io.*;
import texon.latex.engine.config.*;
public final class TexonAssets {
	public final GlyphOutlines outlines;
	private TexonAssets(GlyphOutlines outlines) {
		this.outlines = outlines;
	}
	public static TexonAssets load(Assets in, String outlines, long blobBudget) throws IOException {
		return new TexonAssets(GlyphOutlines.load(in, outlines, blobBudget));
	}
	public Texon newTexon(Assets in, String metrics, TexonOptions options) throws IOException {
		return Texon.create(in, metrics, outlines, options);
	}
	public Texon newTexon(Assets in, String metrics) throws IOException {
		return newTexon(in, metrics, new TexonOptions());
	}
	public TexonAssets addFamily(int family, Assets in, String stem) throws IOException {
		outlines.addFamily(family, in, stem);
		return this;
	}
}
