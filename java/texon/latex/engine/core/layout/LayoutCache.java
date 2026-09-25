package texon.latex.engine.core.layout;
import java.util.*;
import texon.latex.engine.core.box.*;
import texon.latex.engine.core.parse.*;
public final class LayoutCache {
	private final Layout layout;
	private final Ast ast;
	private final Parser parser;
	private final LinkedHashMap<CharSequence, Box> map;
	private final int cap;
	private final Key probe = new Key();
	public LayoutCache(Layout layout, Ast ast, Parser parser, int capacity) {
		this.layout = layout;
		this.ast = ast;
		this.parser = parser;
		this.cap = capacity < 1 ? 1 : capacity;
		this.map = new LinkedHashMap<CharSequence, Box>(this.cap, 0.75f, true);
	}
	public Box build(CharSequence src) {
		probe.set(src);
		Box box = map.get(probe);
		if (box != null) return box;
		box = layout.build(ast, parser.parse(src));
		if (map.size() >= cap) {
			Iterator<CharSequence> it = map.keySet().iterator();
			if (it.hasNext()) {
				it.next();
				it.remove();
			}
		}
		map.put(src.toString(), box);
		return box;
	}
	private static final class Key implements CharSequence {
		private CharSequence s;
		private int hash;
		Key set(CharSequence s) {
			this.s = s;
			int h = 0;
			for (int i = 0; i < s.length(); i++) h = 31 * h + s.charAt(i);
			this.hash = h;
			return this;
		}
		public int length() {
			return s.length();
		}
		public char charAt(int i) {
			return s.charAt(i);
		}
		public CharSequence subSequence(int a, int b) {
			return s.subSequence(a, b);
		}
		public int hashCode() {
			return hash;
		}
		public boolean equals(Object o) {
			if (!(o instanceof CharSequence)) return false;
			CharSequence t = (CharSequence) o;
			int n = s.length();
			if (t.length() != n) return false;
			for (int i = 0; i < n; i++) if (s.charAt(i) != t.charAt(i)) return false;
			return true;
		}
	}
}