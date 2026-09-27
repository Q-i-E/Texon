package texon.latex.engine.config;
public final class AccentCompat {
	// 需要兼容间距的重音码点（升序）
	public final int[] cp = {0x300, 0x301, 0x302, 0x303, 0x304, 0x306, 0x307, 0x308, 0x30A, 0x30C, 0x20D6, 0x20D7, 0x20DB, 0x20DC, 0x20E1};
	// cp[i] 对应的间距修正
	public final int[] gap = {-38, -38, -8, 23, 5, -27, 15, 23, -11, -32, 0, -33, 40, 40, 0};
	// 宽重音（widehat / widetilde）的码点
	public final int[] wcp = {0x302, 0x303};
	// wcp[i] 对应的间距修正
	public final int[] wgap = {17, 65};
}
