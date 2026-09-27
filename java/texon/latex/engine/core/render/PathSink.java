package texon.latex.engine.core.render;
public interface PathSink {
	void moveTo(float x, float y);
	void lineTo(float x, float y);
	void quadTo(float cx, float cy, float x, float y);
	void cubicTo(float c1x, float c1y, float c2x, float c2y, float x, float y);
	void close();
}