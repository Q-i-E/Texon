package texon.latex.engine.config;
public final class ColorTable {
	// 颜色解析失败 / 未定义
	public static final int NONE = 0;
	// 内置颜色名（全部小写，可自定义追加）
	public String[] names = {
		"black", "white", "red", "green", "blue", "cyan", "aqua", "magenta", "fuchsia", "yellow",
		"gray", "grey", "silver", "maroon", "olive", "lime", "teal", "navy", "purple", "orange",
		"pink", "brown", "gold", "darkred", "darkgreen", "darkblue", "lightgray", "lightgrey", "transparent"
	};
	// names[i] 对应的 ARGB
	public int[] values = {
		0xFF000000, 0xFFFFFFFF, 0xFFFF0000, 0xFF008000, 0xFF0000FF, 0xFF00FFFF, 0xFF00FFFF, 0xFFFF00FF, 0xFFFF00FF, 0xFFFFFF00,
		0xFF808080, 0xFF808080, 0xFFC0C0C0, 0xFF800000, 0xFF808000, 0xFF00FF00, 0xFF008080, 0xFF000080, 0xFF800080, 0xFFFFA500,
		0xFFFFC0CB, 0xFFA52A2A, 0xFFFFD700, 0xFF8B0000, 0xFF006400, 0xFF00008B, 0xFFD3D3D3, 0xFFD3D3D3, 0x00000000
	};
}
