package texon.latex.engine.core;
import java.io.*;
public interface Assets {
	InputStream open(String name) throws IOException;
	static Assets dir(final File root) {
		return new Assets() {
			public InputStream open(String name) throws IOException {
				return new FileInputStream(new File(root, name));
			}
		};
	}
}
