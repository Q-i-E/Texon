package texon.latex.engine.core.layout;
import java.util.*;
import texon.latex.engine.core.box.*;
import texon.latex.engine.core.parse.*;
public final class LayoutCache {
	private final Layout layout;
	private final Ast ast;
	private final Parser parser;
	private final LinkedHashMap<String, Box> map;
	private final int cap;
	public LayoutCache(Layout layout, Ast ast, Parser parser, int capacity) {
		this.layout = layout;
		this.ast = ast;
		this.parser = parser;
		this.cap = capacity < 1 ? 1 : capacity;
		this.map = new LinkedHashMap<String, Box>(cap, 0.75f, true);
	}
	public Box build(CharSequence src) {
		String key = src.toString();
		Box box = map.get(key);
		if (box != null) return box;
		box = layout.build(ast, parser.parse(src));
		if (map.size() >= cap) {
			Iterator<String> it = map.keySet().iterator();
			if (it.hasNext()) {
				it.next();
				it.remove();
			}
		}
		map.put(key, box);
		return box;
	}
}
