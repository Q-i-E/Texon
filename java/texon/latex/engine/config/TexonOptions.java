package texon.latex.engine.config;
public final class TexonOptions {
	// 上花括号 overbrace 横线与内容的垂直间距
	public int braceAboveGap = 35;
	// 下花括号 underbrace 横线与内容的垂直间距
	public int braceBelowGap = 35;
	// 上下堆叠（atop 等）时上方内容的基线间距
	public int stackAboveGap = 193;
	// align/gather 等多行环境的行间距
	public int alignRowGap = 753;
	// boxed / fbox 边框与内容的间距
	public int boxPad = 300;
	// 1 mu = unitsPerEm / muDiv（数学单位换算分母）
	public int muDiv = 18;
	// 定界符与内容之间的额外间距
	public int delimiterPad = 100;
	// 重音字形的额外基线间距
	public int accentGap = 0;
	// 错误占位符的 ARGB 颜色
	public int errorColor = 0xFFFF3B30;
	// 布局缓存条目上限
	public int layoutCacheCapacity = 256;
	// 字形路径缓存条目上限
	public int glyphPathsCapacity = 4096;
	// 字形覆盖率掩码缓存条目上限
	public int maskCacheCapacity = 4096;
	// 轮廓分片解压后允许驻留的内存预算（字节）
	public long blobBudget = 8L << 20;
	// PNG 压缩级别，见 java.util.zip.Deflater
	public int pngLevel = 6;
	// SVG 坐标的小数位精度（0 = 仅整数）
	public int svgPrecision = 3;
	// SVG 字形 path 的 id 前缀（多图内联时避免冲突）
	public String svgIdPrefix = "g";
	// 光栅化每像素超采样倍率（越大越平滑越慢）
	public int rasterSupersample = 2;
	// PNG 行滤波：不滤波（默认，文本/大面积同色最优）
	public static final int PNG_FILTER_NONE = 0;
	// PNG 行滤波：自适应（逐行取压缩最优，照片类内容可能更优）
	public static final int PNG_FILTER_AUTO = -1;
	// PNG 颜色模式：真彩（默认）
	public static final int PNG_COLOR_RGB = 1;
	// PNG 颜色模式：全像素为灰时用灰度（更小，但部分解码器按线性灰度解释，观感会变）
	public static final int PNG_COLOR_AUTO = 0;
	// PNG 行滤波：PNG_FILTER_NONE 或 0..4（固定），或 PNG_FILTER_AUTO
	public int pngFilter = PNG_FILTER_NONE;
	// PNG 颜色模式：PNG_COLOR_RGB / PNG_COLOR_AUTO
	public int pngColorMode = PNG_COLOR_RGB;
}
