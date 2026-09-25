package texon.latex.engine.core.render;
public interface VectorSink {
	void glyph(int cp, float x, float baseline, int percent, int family, int argb);
	void rect(float x, float y, float w, float h, int argb);
	void frameRect(float x, float y, float w, float h, int argb, float strokeW);
	void line(float x1, float y1, float x2, float y2, int argb, float strokeW);
	void beginGroup(int argb, boolean setFill, String cssClass, String id);
	void endGroup();
}