package texon.latex.engine.config;
public final class FamilyTable {
	// 族 0：数学主族
	public static final byte MATH = 0;
	// 族 1：粗体（mathbf）
	public static final byte BOLD = 1;
	// 族 2：斜体（mathit）
	public static final byte ITALIC = 2;
	// 族 3：花体（mathcal）
	public static final byte SCRIPT = 3;
	// 族 4：哥特体（mathfrak）
	public static final byte FRAKTUR = 4;
	// 族 5：双线体（mathbb）
	public static final byte BB = 5;
	// 族 6：无衬线（mathsf）
	public static final byte SF = 6;
	// 族 7：等宽（mathtt）
	public static final byte TT = 7;
	// 族总数，与资产族表段长度一致
	public static final int COUNT = 8;
	// 各族的拉丁大写字母基准码点
	public int[] up = new int[COUNT];
	// 各族的拉丁小写字母基准码点
	public int[] lo = new int[COUNT];
	// 各族的数字基准码点
	public int[] di = new int[COUNT];
	// 需要改道的码点（升序）
	public int[] holeCp = new int[0];
	// holeCp[i] 改道后的目标码点
	public int[] holeTo = new int[0];
}
