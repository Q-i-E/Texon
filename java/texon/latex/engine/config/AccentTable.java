package texon.latex.engine.config;
public final class AccentTable {
	// 重音与内容间距的哨兵索引
	public static final int GAP = 0;
	// 重音表容量（与资产一致）
	public static final int COUNT = 8;
	// 重音对应的 SymbolTable ID
	public int[] id = new int[0];
	// 重音的码点
	public int[] cp = new int[0];
	// 重音字形的底部墨迹高度
	public short[] bottom = new short[0];
}
