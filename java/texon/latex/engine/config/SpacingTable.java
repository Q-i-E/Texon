package texon.latex.engine.config;
public final class SpacingTable {
	// 原子类型：有序（Ord）
	public static final byte ORD = 0;
	// 原子类型：大运算符（Op）
	public static final byte OP = 1;
	// 原子类型：二元运算（Bin）
	public static final byte BIN = 2;
	// 原子类型：关系（Rel）
	public static final byte REL = 3;
	// 原子类型：开定界（Open）
	public static final byte OPEN = 4;
	// 原子类型：闭定界（Close）
	public static final byte CLOSE = 5;
	// 原子类型：标点（Punct）
	public static final byte PUNCT = 6;
	// 原子类型：内层（Inner）
	public static final byte INNER = 7;
	// 1 mu = unitsPerEm / MU_DIV（历史常量，运行时以 TexonOptions.muDiv 为准）
	public static final int MU_DIV = 18;
	// 定界符额外间距（历史常量，运行时以 TexonOptions.delimiterPad 为准）
	public static final int DELIMITER_PAD = 100;
	// 定界符外侧间距（历史常量，预留）
	public static final int DELIMITER_SPACE = 0;
	// 原子类型间距矩阵，8×8 按 ORD..INNER 排列，单位 mu
	public byte[] pair = new byte[64];
	// 码点表（升序，供二分查找）
	public int[] cp = new int[0];
	// cp[i] 对应的原子类型
	public byte[] cls = new byte[0];
}
