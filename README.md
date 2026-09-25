# Texon

<sub>中文 · [English](README-en.md)</sub>

**纯 Java 的 LaTeX 数学公式渲染库** —— 解析 → 排版 → 渲染（矢量 / 光栅）全部在本地完成，**不依赖 Android，也不依赖任何第三方运行时库**。字形数据以分片压缩资产形式离线提供，随库发布。

```java
Texon texon = Texon.load(Assets.dir(new File("assets/fonts")), "metrics", "outlines");
String svg   = texon.svg("\\frac{a}{b}", 48f);          // 矢量：自包含 SVG
byte[] png   = texon.png("\\sum_{i=1}^{n} a_i", 96f);   // 光栅：PNG 字节
```

---

## 效果预览

全量渲染清单（`assets/showcase.txt`，598 条）的纯 Java 后端输出：

![showcase](showcase/pure.png)

---

## 目录

- [效果预览](#效果预览)
- [特性](#特性)
- [目录结构](#目录结构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [使用](#使用)
- [渲染输出](#渲染输出)
- [字形资产格式](#字形资产格式)
- [支持的 LaTeX](#支持的-latex)
- [字体资产](#字体资产)
- [常见问题](#常见问题)
- [许可](#许可)

---

## 特性

- **纯 Java，零依赖**：只用到 `java.*`，可运行在桌面、服务端、Android，只要是 JVM 即可。
- **两套渲染后端**，作用于同一棵布局树：
  - **矢量**：`svg(...)` → 返回自包含 SVG 字符串（字形路径已内嵌）。
  - **光栅**：`png(...)` / `raster(...)` → PNG 字节 / ARGB 像素。
- **分片资产、按需加载**：清单（`.idx`）+ 独立分片；命中一个字符只解压对应分片（度量每片 4096 码点、轮廓每片 1024 码点）。
- **CJK 双族覆盖**：`\text{…}` 走衬线（含中文），`\mathsf{…}` / `\texttt{…}` 走无衬线覆盖族；缺失时自动回退默认族。
- **实时渲染友好**：布局缓存 + 跨帧字形路径缓存 + 光栅 sink 复用，适合编辑回显、连续渲染。
- **确定性构建**：同一套源码 + 同一批字体输入，资产逐字节可复现。

---

## 目录结构

```
assets/
  fonts/
    metrics.idx + metrics/<bucket>.bin            主字体度量 + 表段（符号/间距/重音/族）
    outlines.idx + outlines/<bucket>.bin          主字体字形轮廓（含衬线 CJK）
    cjk-sans-metrics.idx + cjk-sans-metrics/<bucket>.bin   无衬线 CJK 族度量覆盖
    cjk-sans-outlines.idx + cjk-sans-outlines/<bucket>.bin 无衬线 CJK 族轮廓覆盖

java/
  texon/latex/engine/core/
    Texon.java           门面：load / svg / raster / png
    FontMetrics.java  GlyphOutlines.java  Assets.java  资产解析（分片 + 清单）
    Tables.java  Parts.java  ColorTable.java
    lex/      Lexer, SymbolTable          词法
    parse/    Ast, Parser                 LaTeX 子集文法（递归下降）
    box/      Box 及其子类                布局中间表示
    layout/   Layout, LayoutCache, *Table 排版与缓存
    render/   Renderer, VectorSink, SvgRenderer, RasterSink, Raster, Png, GlyphPaths, …
```

`java/` 是源码根：编译后包名/引导类为 `texon.latex.engine.core.*`。`assets/` 是构建好的运行时数据，已随仓库预置。

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

## 渲染输出

| 方法 | 返回 | 说明 |
|---|---|---|
| `String svg(latex, pxPerEm)` | `String` | 自包含 `<svg>` 文本，内嵌字形路径 |
| `Raster raster(latex, pxPerEm, bg)` | `Raster` | `{width, height, int[] pixels}`，ARGB8888 |
| `byte[] png(latex, pxPerEm[, bg])` | `byte[]` | PNG 编码字节 |
| `Raster render(latex, pxPerEm, bg)` | `Raster` | 复用上一帧 sink（实时） |

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

递归下降解析器覆盖常用 LaTeX 数学子集（**不是**完整 TeX）。覆盖类别包括：

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

`assets/fonts/` 已随仓库提供（分片压缩、按需加载），**开箱即用，无需任何重建**：

```java
Texon texon = Texon.load(Assets.dir(new File("assets/fonts")), "metrics", "outlines");
```

资产是从上游开源字体离线提取的衍生数据；来源与许可证见 [`assets/NOTICE.txt`](assets/NOTICE.txt)。

---

## 常见问题

**Q：运行时提示找不到 `metrics.idx` / `outlines.idx`？**
`assets/fonts/` 缺失或路径不对。请确保仓库含 `assets/fonts/`（随库发布），或先用 `Assets.dir(new File("正确路径/fonts"))` 指向正确位置。

**Q：某个字符渲染成空白 / 缺字？**
先确认对应字形在资产里；`assets/fonts/` 为预构建数据，覆盖范围见 [`assets/NOTICE.txt`](assets/NOTICE.txt)。

**Q：能放到 Android 用吗？**
能。库本身零 Android 依赖；在 Android 上把 `Assets` 实现为基于 `AssetManager` 打开（`manager.open("fonts/" + name)`），并把 `assets/fonts` 打包进 APK 即可。

**Q：资产多大？**
全量约 33MB（分片压缩）。运行时不整包解压，按命中分片加载。

---

## 许可

- **代码（`java/`）**：**MIT License**，见根目录 [`LICENSE`](LICENSE)。
- **资产（`assets/fonts/`）**：为上游开源字体的**衍生数据**，须遵守各来源许可证（Noto CJK OFL、Latin Modern GFL、DejaVu、DroidSansFallback Apache-2.0、STIX OFL 等），见 [`assets/NOTICE.txt`](assets/NOTICE.txt)。