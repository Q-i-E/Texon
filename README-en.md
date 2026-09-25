# Texon

<sub>English · [中文](README.md)</sub>

**A pure-Java LaTeX math rendering library** — parsing → layout → rendering (vector / raster) all happen locally, **without Android and without any third-party runtime dependency**. Glyph data ships as sharded, compressed assets built offline.

```java
Texon texon = Texon.load(Assets.dir(new File("assets/fonts")), "metrics", "outlines");
String svg = texon.svg("\\frac{a}{b}", 48f);            // vector: self-contained SVG
byte[] png = texon.png("\\sum_{i=1}^{n} a_i", 96f);     // raster: PNG bytes
```

---

## Preview

Pure-Java backend output for the full render list (`assets/showcase.txt`, 598 items):

![showcase](showcase/pure.png)

---

## Contents

- [Preview](#preview)
- [Features](#features)
- [Repository layout](#repository-layout)
- [Requirements](#requirements)
- [Quick start](#quick-start)
- [Usage](#usage)
- [Render output](#render-output)
- [Glyph asset format](#glyph-asset-format)
- [Supported LaTeX](#supported-latex)
- [Font assets](#font-assets)
- [FAQ](#faq)
- [License](#license)

---

## Features

- **Pure Java, zero dependencies**: only `java.*` — runs on desktop, servers, and Android, anywhere there is a JVM.
- **Two render backends** over the same layout tree:
  - **Vector**: `svg(...)` → a self-contained SVG string (glyph paths embedded).
  - **Raster**: `png(...)` / `raster(...)` → PNG bytes / ARGB pixels.
- **Sharded, on-demand assets**: an index (`.idx`) plus independent shards; hitting a character decompresses only the matching shard (metrics at 4096 code points per shard, outlines at 1024).
- **CJK dual-family**: `\text{…}` uses the serif family (incl. Chinese), `\mathsf{…}` / `\texttt{…}` use a sans-serif overlay family, falling back to the default automatically.
- **Real-time friendly**: layout cache + cross-frame glyph-path cache + reusable raster sink, suitable for edit previews and continuous rendering.
- **Deterministic build**: the same source plus the same font inputs reproduce assets byte-for-byte.

---

## Repository layout

```
assets/
  fonts/
    metrics.idx + metrics/<bucket>.bin              main metrics + tables (symbols/spacing/accents/families)
    outlines.idx + outlines/<bucket>.bin            main glyph outlines (incl. serif CJK)
    cjk-sans-metrics.idx + cjk-sans-metrics/<bucket>.bin     sans-serif CJK metrics overlay
    cjk-sans-outlines.idx + cjk-sans-outlines/<bucket>.bin   sans-serif CJK outlines overlay

java/
  texon/latex/engine/core/
    Texon.java          facade: load / svg / raster / png
    FontMetrics.java  GlyphOutlines.java  Assets.java   asset parsing (shards + index)
    Tables.java  Parts.java  ColorTable.java
    lex/      Lexer, SymbolTable          tokenizing
    parse/    Ast, Parser                recursive-descent LaTeX subset grammar
    box/      Box and its subclasses     layout intermediate representation
    layout/   Layout, LayoutCache, *Table  typesetting & caching
    render/   Renderer, VectorSink, SvgRenderer, RasterSink, Raster, Png, GlyphPaths, …
```

`java/` is the source root: after compilation the packages are `texon.latex.engine.core.*`. `assets/` holds prebuilt runtime data and is shipped with the repository.

---

## Requirements

- JDK 8+ (officially built with `javac --release 11`; plain Java, so JDK 8 can compile it too)
- No third-party JARs, no Android SDK

---

## Quick start

```bash
git clone <your>/Texon.git
cd Texon

# compile the library
mkdir -p out && javac -encoding utf-8 -d out $(find java -name '*.java')

# write an entry point (see below)
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

Running requires `assets/fonts/` to be present (**shipped with the repo**).

---

## Usage

### Loading

```java
import java.io.File;
import texon.latex.engine.core.Assets;
import texon.latex.engine.core.Texon;

// from a directory (pure Java, recommended)
Texon texon = Texon.load(Assets.dir(new File("assets/fonts")), "metrics", "outlines");

// from any two InputStreams (the index entry points)
Texon texon = Texon.load(metricsStream, outlinesStream);
```

`Assets` is a single-method interface (`InputStream open(String name)`), shipped with `Assets.dir(File)` for the file system; implement it yourself to plug in Zip, network, an Android `AssetManager`, etc.

### Rendering

```java
// vector: returns a self-contained SVG string
String svg = texon.svg("\\int_0^1 x\\,dx = \\frac{1}{2}", 48f);

// raster pixels
Raster r = texon.raster("\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}", 48f, 0xFFFFFFFF);
int width = r.width, height = r.height;
int[] pixels = r.pixels;                        // ARGB8888

// PNG bytes
byte[] png = texon.png("E = mc^2", 96f);

// real-time: reuses the previous frame's sink, good for edit previews / continuous rendering
Raster frame = texon.render(source, pxPerEm, background);
```

---

## Render output

| Method | Returns | Description |
|---|---|---|
| `String svg(latex, pxPerEm)` | `String` | self-contained `<svg>` text, glyph paths embedded |
| `Raster raster(latex, pxPerEm, bg)` | `Raster` | `{width, height, int[] pixels}`, ARGB8888 |
| `byte[] png(latex, pxPerEm[, bg])` | `byte[]` | PNG-encoded bytes |
| `Raster render(latex, pxPerEm, bg)` | `Raster` | reuses the previous frame's sink (real-time) |

---

## Glyph asset format

Binary, **big-endian**. Every shard is a complete, independently parseable unit; the index only records the shard table.

### Index `TXID`

```
magic "TXID" | version i32 | bits i32 | total i32 | parts i32
per part: base i32 | count i32 | flags i32 | nameLen i16 | name ascii
flags bit0 = CORE (carries constants / variant chains / assemblies / table segment; must stay resident)
```

### Metrics shard `TXM2`

```
"TXM2" | rawLen i32 | zlib(body)
body = "TXM1" | unitsPerEm i32 | asc i32 | desc i32 | lineGap i32
       | constCount i32 | consts[]{ keyLen i16, key, value i32 }
       | glyphCount i32 | cp[] i32 | w[] i32 | h/d/ic/il/ta/it/ib[] i16
       | chains[] | hchains[] | asms[]          (CORE shards only)
       | tablesLen i32 | tables[]                (CORE shards only, TXTB segment)
```

### Outline shard `TXO1`

```
"TXO1" | rawLen i32 | zlib(payload)
payload = n i32 | cp[n] i32 | off[n+1] i32 | data[] (concatenated UTF-8 SVG path strings)
```

Bucketing: metrics `cp >> 12` (4096 code points per shard), outlines `cp >> 10` (1024 per shard).

---

## Supported LaTeX

A recursive-descent parser covers a common LaTeX math subset (**not** full TeX). Covered categories include:

- **Sub/superscripts, fractions, roots**: `_ ^`, `\frac \dfrac \tfrac \cfrac`, `\sqrt \sqrt[3]`
- **Big operators and limits**: `\sum \prod \int \oint \lim \liminf \argmax \projlim`
- **Auto-sizing delimiters**: `\left( \right]`, `\bigl \Bigl \biggl` …
- **Matrix / environments**: `pmatrix bmatrix matrix array cases align gather`, `\hline \multicolumn`, `\substack \sideset \smashoperator`
- **Accents & decorations**: `\hat \tilde \bar \vec \dot \utilde \overgroup \underbar \overbrace \underbrace`
- **Boxes / colors**: `\boxed \cancel \cancelto \textcolor \colorbox`
- **Text & font families**: `\text \textrm \textsf \texttt`, `\mathbf \mathcal \mathfrak \mathsf \mathtt \mathbb`
- **Arrows / relations / symbols**: `\xrightarrow \xLeftrightarrow \xmapsto`, `\coloneqq \subseteqq \nexists \nleq …`
- **Chinese / CJK**: `\text{中文}` uses serif, `\mathsf{한글}` uses the sans-serif overlay family

---

## Font assets

`assets/fonts/` is shipped with the repository (sharded, compressed, loaded on demand) — **works out of the box, no rebuild needed**:

```java
Texon texon = Texon.load(Assets.dir(new File("assets/fonts")), "metrics", "outlines");
```

The assets are derived data extracted offline from upstream open fonts; see [`assets/NOTICE-en.txt`](assets/NOTICE-en.txt) for provenance and licenses.

---

## FAQ

**Q: At runtime it says `metrics.idx` / `outlines.idx` not found?**
`assets/fonts/` is missing or the path is wrong. Make sure the repo contains `assets/fonts/` (shipped), or point `Assets.dir(new File("correct/path/fonts"))` at the right location.

**Q: Some character renders blank / missing?**
Verify the glyph exists in the assets; `assets/fonts/` is prebuilt data, see [`assets/NOTICE-en.txt`](assets/NOTICE-en.txt) for coverage and provenance.

**Q: Can I use it on Android?**
Yes — the library itself has zero Android dependencies. On Android, implement `Assets` over `AssetManager` (`manager.open("fonts/" + name)`) and package `assets/fonts` into the APK.

**Q: How large are the assets?**
About 33MB total (sharded + compressed). Not decompressed all at once; only matching shards are loaded on demand.

---

## License

- **Code (`java/`)**: **MIT License**, see [`LICENSE`](LICENSE) in the repository root.
- **Assets (`assets/fonts/`)**: derived data extracted from upstream open fonts, subject to each source license (Noto CJK OFL, Latin Modern GFL, DejaVu, DroidSansFallback Apache-2.0, STIX OFL, …) — see [`assets/NOTICE-en.txt`](assets/NOTICE-en.txt) (or the Chinese `assets/NOTICE.txt`).