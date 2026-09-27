# Texon

<sub>简体中文 · [English](README-en.md)</sub>

**一个纯 Java 的 LaTeX 数学公式渲染库** —— 解析 -> 排版 -> 渲染全部在本地完成，**不依赖 Android，也不依赖任何第三方运行时库**。

```java
Texon texon = Texon.load(Assets.dir(new File("assets/fonts")), "metrics", "outlines");
String svg   = texon.svg("\\frac{a}{b}", 48f);          // 矢量：自包含 SVG
byte[] png   = texon.png("\\sum_{i=1}^{n} a_i", 96f);   // 光栅：PNG 字节
```

---

## 目录

- [效果预览](#效果预览)
- [特性](#特性)
- [目录结构](#目录结构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [使用](#使用)
- [配置](#配置)
- [度量](#度量)
- [批量与流式排版](#批量与流式排版)
- [多实例与共享资产](#多实例与共享资产)
- [预解码](#预解码)
- [渲染输出](#渲染输出)
- [字形资产格式](#字形资产格式)
- [支持的 LaTeX](#支持的-latex)
- [字体资产](#字体资产)
- [常见问题](#常见问题)
- [许可](#许可)

---

## 效果预览

![showcase](showcase/pure.png)

---

## 特性

- **纯 Java，零依赖**：只用到 `java.*`，可运行在桌面、服务端、Android，只要是 JVM 即可。
- **两套渲染后端**，作用于同一棵布局树：
  - **矢量**：`svg(...)` → 返回自包含 SVG 字符串（内嵌字形路径）。
  - **光栅**：`png(...)` / `raster(...)` → PNG 字节 / ARGB 像素。
- **分片资产、按需加载**：清单（`.idx`）+ 独立分片；命中一个字符只解压对应分片（度量每片 4096 码点、轮廓每片 1024 码点）。
- **CJK 双族覆盖**：`\text{…}` 走衬线（含中文），`\mathsf{…}` / `\texttt{…}` 走无衬线覆盖族；缺失时自动回退默认族。
- **实时渲染友好**：布局缓存 + 跨帧字形路径缓存 + 字形覆盖率缓存 + 光栅 sink 复用，适合编辑回显、连续渲染。
- **确定性构建**：同一套源码 + 同一批字体输入，资产逐字节可复现。

---

## 目录结构

```
Texon/
├── assets/
│   └── fonts/
│       ├── metrics.idx                      主字体度量清单
│       ├── metrics/<bucket>.bin             主字体度量分片（符号 / 间距 / 重音 / 族表）
│       ├── outlines.idx                     主字体轮廓清单
│       ├── outlines/<bucket>.bin            主字体字形轮廓分片（含衬线 CJK）
│       ├── cjk-sans-metrics.idx             无衬线 CJK 族度量清单
│       ├── cjk-sans-metrics/<bucket>.bin    无衬线 CJK 族度量分片
│       ├── cjk-sans-outlines.idx            无衬线 CJK 族轮廓清单
│       └── cjk-sans-outlines/<bucket>.bin   无衬线 CJK 族轮廓分片
└── java/
    └── texon/latex/engine/
        ├── core/                    引擎：解析 / 排版 / 渲染
        │   ├── Texon.java           门面：load / svg / raster / png
        │   ├── TexonAssets.java     可共享的轮廓资产（多实例复用）
        │   ├── LayoutMetrics.java   度量结果：宽 / 高 / 基线 / 左侧墨迹
        │   ├── Assets.java          资产接口（InputStream open）
        │   ├── FontMetrics.java     度量解析（清单 + 分片）
        │   ├── GlyphOutlines.java   轮廓解析（清单 + 分片）
        │   ├── Parts.java           分片表与直查
        │   ├── Tables.java          表段装载（写入 config）
        │   ├── TexonContext.java    运行期上下文：查表索引 + 配置 API
        │   ├── lex/
        │   │   └── Lexer.java       词法
        │   ├── parse/
        │   │   ├── Ast.java         语法树
        │   │   └── Parser.java      LaTeX 子集文法（递归下降）
        │   ├── box/
        │   │   └── Box.java + 子类   布局中间表示
        │   ├── layout/
        │   │   ├── Layout.java      排版
        │   │   └── LayoutCache.java 布局缓存
        │   └── render/
        │       ├── Renderer.java    布局树遍历
        │       ├── VectorSink.java  矢量绘制接口
        │       ├── SvgRenderer.java SVG 后端
        │       ├── SvgPath.java     SVG path 解析
        │       ├── RasterSink.java  光栅后端
        │       ├── GlyphMask.java   字形覆盖率掩码
        │       ├── MaskCache.java   字形覆盖率缓存
        │       ├── GlyphPaths.java  字形路径缓存
        │       ├── PathSink.java    路径接收接口
        │       ├── Raster.java      ARGB 位图
        │       └── Png.java         PNG 编码
        └── config/                  纯配置：只有数据与常量，无任何算法（每实例独立）
            ├── TexonOptions.java    数值 / 容量 / 开关
            ├── SymbolTable.java     符号 / 命令表
            ├── SpacingTable.java    数学分类 + 间距
            ├── AccentTable.java     重音表
            ├── AccentCompat.java    重音兼容间距
            ├── FamilyTable.java     字体族映射
            ├── DelimTable.java      定界符拉伸表
            └── ColorTable.java      颜色表
```

`java/` 是源码根：包分为 `texon.latex.engine.core.*`（引擎）与 `texon.latex.engine.config.*`（纯配置）。`assets/` 是构建好的运行时数据，已随仓库预置。

---

## 环境要求

- JDK 8+（官方目标 `javac --release 11`，纯 Java 语法，JDK 8 亦可编译运行）
- 无需任何第三方 JAR、无需 Android SDK

---

## 快速开始

```bash
git clone <your>/Texon.git
cd Texon

# 编译库
mkdir -p out && javac -encoding utf-8 -d out $(find java -name '*.java')

# 写一个入口（见下）
cat > Demo.java <<'EOF'
import java.io.File;
import texon.latex.engine.core.Assets;
import texon.latex.engine.core.Texon;
public class Demo {
	public static void main(String[] a) {
		Texon t = Texon.load(Assets.dir(new File("assets/fonts")), "metrics", "outlines");
		System.out.println(t.svg("\\frac{a}{b}", 48f).substring(0, 60) + "...");
	}
}
EOF
javac -encoding utf-8 -cp out -d out Demo.java
java -cp out Demo
```

运行需要 `assets/fonts/` 存在于仓库（**已预置**）。

---

## 使用

### 加载

```java
import java.io.File;
import texon.latex.engine.core.Assets;
import texon.latex.engine.core.Texon;

// 方式一：从目录加载（纯 Java，推荐）
Texon texon = Texon.load(Assets.dir(new File("assets/fonts")), "metrics", "outlines");

// 方式二：任意两个 InputStream（清单入口）
Texon texon = Texon.load(metricsStream, outlinesStream);
```

`Assets` 是一个只有一个方法 `InputStream open(String name)` 的接口，随库附带 `Assets.dir(File)`（基于文件系统）；你也可以按需实现它来对接自定义存储（Zip、网络、Android `AssetManager` 等）。

### 渲染

```java
// 矢量：返回自包含 SVG 字符串
String svg = texon.svg("\\int_0^1 x\\,dx = \\frac{1}{2}", 48f);

// 光栅像素
Raster r = texon.raster("\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}", 48f, 0xFFFFFFFF);
int width = r.width, height = r.height;
int[] pixels = r.pixels;                       // ARGB8888

// PNG 字节
byte[] png = texon.png("E = mc^2", 96f);

// 实时：复用上一帧 sink，适合编辑回显/连续渲染
Raster frame = texon.render(source, pxPerEm, background);
```

---

## 配置

所有输出侧与容量侧参数都集中在 `config/TexonOptions` —— 纯数据、无算法，且**每个 `Texon` 实例独立**：

```java
TexonOptions opts = new TexonOptions();
opts.pngLevel = 6;               // PNG 压缩级别
opts.rasterSupersample = 2;      // 光栅超采样倍率
opts.svgPrecision = 3;           // SVG 坐标小数位
opts.layoutCacheCapacity = 512;  // 布局缓存条目上限
Texon texon = Texon.load(Assets.dir(new File("assets/fonts")), "metrics", "outlines", opts);
```

| 字段 | 默认 | 含义 |
|---|---|---|
| `braceAboveGap` / `braceBelowGap` | `35` / `35` | `\overbrace` / `\underbrace` 横线与内容的垂直间距 |
| `stackAboveGap` | `193` | `\atop` 等上下堆叠的基线间距 |
| `alignRowGap` | `753` | `align` / `gather` 等多行环境的行距 |
| `boxPad` | `300` | `\boxed` / `\fbox` 边框与内容间距 |
| `muDiv` | `18` | 1 mu = `unitsPerEm / muDiv` |
| `delimiterPad` | `100` | 定界符与内容的额外间距 |
| `accentGap` | `0` | 重音字形的额外基线间距 |
| `errorColor` | `0xFFFF3B30` | 错误占位符 ARGB |
| `layoutCacheCapacity` | `256` | 布局缓存条目上限 |
| `glyphPathsCapacity` | `4096` | 字形路径缓存条目上限 |
| `maskCacheCapacity` | `4096` | 字形覆盖率缓存条目上限 |
| `blobBudget` | `8L << 20` | 轮廓分片解压后允许驻留的内存预算（字节） |
| `pngLevel` | `6` | PNG 压缩级别，见 `java.util.zip.Deflater` |
| `svgPrecision` | `3` | SVG 坐标小数位精度（`0` = 仅整数） |
| `svgIdPrefix` | `"g"` | SVG 字形 path 的 id 前缀（多图内联时避免冲突） |
| `rasterSupersample` | `2` | 每像素超采样倍率（越大越平滑越慢） |
| `pngFilter` | `PNG_FILTER_NONE` | PNG 行滤波：`PNG_FILTER_NONE` / `0..4`（固定）/ `PNG_FILTER_AUTO`（逐行自适应） |
| `pngColorMode` | `PNG_COLOR_RGB` | PNG 颜色模式：`PNG_COLOR_RGB` / `PNG_COLOR_AUTO`（全像素为灰时转灰度） |

默认值即改造前的写死值，因此**默认配置下输出与旧版逐字节相同**；改动任一项，对应产物随之变化 —— 差异由你控制，而非隐式行为。

**关于 PNG 行滤波**：实测对公式类内容**不滤波最优** —— 逐行切换滤波类型会破坏「全白行完全相同」的长程匹配，体积反而增大 17%~75%（全量 1944×5980 画布：filter 0 = 720KB，auto = 884KB）。故默认 `PNG_FILTER_NONE`，`PNG_FILTER_AUTO` 作为可选，供照片类内容使用。

**关于灰度**：仅当全像素 `r == g == b` 才启用；实测省 12%~15%，但部分解码器按线性灰度解释会改变观感，故默认关闭。

---

## 度量

`measure(...)` 返回 `LayoutMetrics`，单位一律为**像素**：

```java
LayoutMetrics m = texon.measure("\\frac{a+b}{c}", 48f);
m.width;      // 总宽（含右侧斜体修正）
m.height;     // 总高（上伸 + 下延）
m.baseline;   // 基线距顶部
m.inkLeft;    // 左侧墨迹（可能为负，用于描边 / 裁边对齐）
```

光栅结果同样携带基线与墨迹包围盒，便于行内对齐与紧凑导出：

```java
Raster r = texon.raster("\\sum_{i=1}^{n} a_i", 48f);
r.baseline;                        // 基线在像素坐标中的位置
r.inkX; r.inkY; r.inkW; r.inkH;    // 非背景像素的包围盒（空公式时 inkW = inkH = 0）
```

包围盒由光栅后端在绘制时 **O(1)** 跟踪，无额外遍历；`measure().baseline` 与 `raster().baseline` 由测试锁定一致。

---

## 批量与流式排版

把多条公式组合成一行或一段，再一次性渲染。实现完全复用现有 `HBox` / `VBox` + `Renderer`，**零新增渲染代码**：

```java
// 基线对齐横排（默认使用原子间距）
Box line = texon.row(new String[]{"a+b", "=", "c"});

// 指定字距（字体单位，与 unitsPerEm 同尺度）
Box line2 = texon.row(new String[]{"x", "y"}, 200);

// 逐行基线对齐 + 行距；align 取 VBox.LEFT / VBox.CENTER
Box block = texon.block(new String[]{"\\frac{a}{b}", "= c"}, 400, VBox.LEFT);

// 渲染整块
Raster px = texon.raster(block, 48f);                 // 白底
Raster tr = texon.raster(block, 48f, 0x00000000);     // 透明底
byte[] png = Png.encode(px, 6, TexonOptions.PNG_FILTER_NONE, TexonOptions.PNG_COLOR_RGB);

// 或直接画到任意 VectorSink（SVG 后端 / 自定义后端）
texon.draw(block, 0xFF000000, sink);
```

`row` 用 `HBox.of` / `HBox.kerned`，`block` 用 `VBox.of` 并按 `off[i] = off[i-1] - (depth[i-1] + lineGap + height[i])` 递推行位置 —— 与内部排版共用同一套基线规则。

---

## 多实例与共享资产

轮廓层（`GlyphOutlines`，资产里最大的一块）可以跨实例共享；度量层与配置绑定 `TexonContext`，**不共享**：

```java
TexonAssets shared = TexonAssets.load(Assets.dir(new File("assets/fonts")), "outlines", 8L << 20);

TexonOptions a = new TexonOptions();
TexonOptions b = new TexonOptions();
b.errorColor = 0xFF0000FF;

Texon t1 = shared.newTexon(Assets.dir(new File("assets/fonts")), "metrics", a);
Texon t2 = shared.newTexon(Assets.dir(new File("assets/fonts")), "metrics", b);
// t1.outlines() == t2.outlines()，但 t1.context() != t2.context()
```

- 共享 `GlyphOutlines`（112 片 / 约 22MB 压缩、56MB 解压），两个实例只解压一份轮廓；每个实例仍持有自己的 `blobBudget` 与配置。
- ⚠️ `FontMetrics` **不可共享**：符号 / 间距 / 重音 / 族表是在装载时 `Tables.install` 写进 `TexonContext` 的，跨实例共享会导致配置串台。
- `Texon.load(...)` 语义不变（各自建轮廓）；共享是 **opt-in**。
- `TexonAssets.addFamily(...)` 可向共享轮廓追加族（如无衬线 CJK 覆盖族）。

---

## 预解码

编辑回显场景可先把即将用到的公式解析并解码一遍，避免首帧抖动：

```java
texon.prefetch("\\int_0^1 \\frac{x^2}{1+x}\\,dx");
texon.prefetchAsync(source, executor);   // 异步版
```

实现方式复用 `Renderer.walk` + 一个**空操作 sink**：在 `glyph(cp, …, family, …)` 里只调 `metrics.width(cp, family)` 与 `outlines.path(cp, family)` 触发分片解码。`family` 由排版器精确解析，因此无需自行遍历语法树。

---

## 渲染输出

| 方法 | 返回 | 说明 |
|---|---|---|
| `String svg(latex, pxPerEm)` | `String` | 自包含 `<svg>` 文本，内嵌字形路径 |
| `Raster raster(latex, pxPerEm, bg)` | `Raster` | `{width, height, int[] pixels}`，ARGB8888 |
| `byte[] png(latex, pxPerEm[, bg])` | `byte[]` | PNG 编码字节 |
| `Raster render(latex, pxPerEm, bg)` | `Raster` | 复用上一帧 sink（实时） |
| `LayoutMetrics measure(latex, pxPerEm)` | `LayoutMetrics` | 尺寸 / 基线 / 墨迹，见「[度量](#度量)」 |
| `Box box(latex)` | `Box` | 解析 + 排版，得到布局树 |
| `Box row(String[] parts[, gap])` | `Box` | 基线对齐横排，见「[批量与流式排版](#批量与流式排版)」 |
| `Box block(String[] lines, lineGap[, align])` | `Box` | 多行基线对齐 + 行距 |
| `Raster raster(Box root, pxPerEm[, bg])` | `Raster` | 渲染任意布局树 |
| `void draw(Box root, argb, sink)` | — | 画到任意 `VectorSink` |
| `void prefetch(latex)` / `prefetchAsync(latex, executor)` | — | 预解析并解码所需分片，见「[预解码](#预解码)」 |

---

## 字形资产格式

二进制、**大端**。所有分片都是独立可解析的完整单体，清单只记录分片表。

### 清单 `TXID`

```
magic "TXID" | version i32 | bits i32 | total i32 | parts i32
per part: base i32 | count i32 | flags i32 | nameLen i16 | name ascii
flags bit0 = CORE（携带常量/变体链/拼装/表段，必须常驻）
```

### 度量分片 `TXM2`

```
"TXM2" | rawLen i32 | zlib(body)
body = "TXM1" | unitsPerEm i32 | asc i32 | desc i32 | lineGap i32
       | constCount i32 | consts[]{ keyLen i16, key, value i32 }
       | glyphCount i32 | cp[] i32 | w[] i32 | h/d/ic/il/ta/it/ib[] i16
       | chains[] | hchains[] | asms[]          （仅 CORE 分片）
       | tablesLen i32 | tables[]                （仅 CORE 分片，TXTB 表段）
```

### 轮廓分片 `TXO1`

```
"TXO1" | rawLen i32 | zlib(payload)
payload = n i32 | cp[n] i32 | off[n+1] i32 | data[]（UTF-8 的 SVG path 串拼接）
```

分桶：度量 `cp >> 12`（每片 4096 码点），轮廓 `cp >> 10`（每片 1024 码点）。

---

## 支持的 LaTeX

递归下降解析器覆盖大部分 LaTeX 数学子集（**并非**完整 TeX）。覆盖类别包括：

- **上下标 / 分式 / 根式**：`_ ^`、`\frac \dfrac \tfrac \cfrac`、`\sqrt \sqrt[3]`
- **大型算符与极限**：`\sum \prod \int \oint \lim \liminf \argmax \projlim`
- **自动伸缩定界符**：`\left( \right]`、`\bigl \Bigl \biggl` …
- **矩阵 / 环境**：`pmatrix bmatrix matrix array cases align gather`，`\hline \multicolumn`，`\substack \sideset \smashoperator`
- **重音与装饰**：`\hat \tilde \bar \vec \dot \utilde \overgroup \underbar \overbrace \underbrace`
- **盒 / 颜色**：`\boxed \cancel \cancelto \textcolor \colorbox`
- **文本与字体族**：`\text \textrm \textsf \texttt`、`\mathbf \mathcal \mathfrak \mathsf \mathtt \mathbb`
- **箭头/关系/符号**：`\xrightarrow \xLeftrightarrow \xmapsto`、`\coloneqq \subseteqq \nexists \nleq …`
- **中文/CJK**：`\text{中文}` 走衬线，`\mathsf{한글}` 走无衬线覆盖族

---

## 字体资产

`assets/fonts/` 已随仓库提供（分片压缩、按需加载）：

```java
Texon texon = Texon.load(Assets.dir(new File("assets/fonts")), "metrics", "outlines");
```

资产是从上游开源字体离线提取的衍生数据；来源与许可证见 [`assets/NOTICE.md`](assets/NOTICE.md)。

---

## 常见问题

**Q：运行时提示找不到 `metrics.idx` / `outlines.idx`？**
`assets/fonts/` 缺失或路径不对。请确保仓库含 `assets/fonts/`（随库发布），或先用 `Assets.dir(new File("正确路径/fonts"))` 指向正确位置。

**Q：某个字符渲染成空白 / 缺字？**
先确认对应字形在资产里；`assets/fonts/` 为预构建数据，覆盖范围见 [`assets/NOTICE.md`](assets/NOTICE.md)。

**Q：能放到 Android 用吗？**
能。库本身零 Android 依赖；在 Android 上把 `Assets` 实现为基于 `AssetManager` 打开（`manager.open("fonts/" + name)`），并把 `assets/fonts` 打包进 APK 即可。

**Q：资产多大？**
全量约 33MB（分片压缩）。运行时不整包解压，按命中分片加载。

---

## 许可

- **代码（`java/`）**：**MIT License**，见根目录 [`LICENSE`](LICENSE)。
- **资产（`assets/fonts/`）**：为上游开源字体的**衍生数据**，须遵守各来源许可证，见 [`assets/NOTICE.md`](assets/NOTICE.md)。