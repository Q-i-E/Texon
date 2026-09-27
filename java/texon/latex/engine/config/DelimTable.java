package texon.latex.engine.config;
public final class DelimTable {
	// 可拉伸定界符的码点（升序，供二分查找）
	public int[] cp = {
		0x28, 0x29, 0x2F, 0x5B, 0x5C, 0x5D, 0x7B, 0x7C, 0x7D,
		0x2016, 0x2191, 0x2193, 0x2195,
		0x2308, 0x2309, 0x230A, 0x230B, 0x27E8, 0x27E9
	};
	// 未拉伸时的附加基距
	public int[] base = {
		-7, -7, 0, -5, -5, -5, -7, -5, -7,
		16, -7, -7, -7,
		-11, -11, -11, -11, -5, -5
	};
	// 拉伸时的附加基距
	public int[] stretch = {
		82, 82, 98, 97, 98, 97, 120, 137, 120,
		186, 190, 190, 153,
		90, 90, 90, 90, 133, 133
	};
}
