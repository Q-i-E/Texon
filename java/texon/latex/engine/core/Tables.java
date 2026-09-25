package texon.latex.engine.core;
import java.nio.*;
import java.nio.charset.*;
import texon.latex.engine.core.layout.*;
import texon.latex.engine.core.lex.*;
public final class Tables {
	private static final int MAGIC = 0x54585442;
	private Tables() {
	}
	public static void install(byte[] data) {
		if (data.length < 4) return;
		ByteBuffer b = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
		if (b.getInt() != MAGIC) return;
		int n = b.getInt();
		String[] name = new String[n];
		byte[] kind = new byte[n];
		int[] value = new int[n];
		for (int i = 0; i < n; i++) {
			int len = b.getShort() & 0xFFFF;
			byte[] nb = new byte[len];
			b.get(nb);
			name[i] = new String(nb, StandardCharsets.UTF_8);
			kind[i] = b.get();
			value[i] = b.getInt();
		}
		SymbolTable.install(name, kind, value);
		int nc = b.getInt();
		int[] cp = new int[nc];
		for (int i = 0; i < nc; i++) cp[i] = b.getInt();
		byte[] cls = new byte[nc];
		b.get(cls);
		byte[] pair = new byte[64];
		b.get(pair);
		SpacingTable.install(cp, cls, pair);
		int na = b.getInt();
		int[] aid = new int[na];
		int[] acp = new int[na];
		short[] ab = new short[na];
		for (int i = 0; i < na; i++) aid[i] = b.getInt();
		for (int i = 0; i < na; i++) acp[i] = b.getInt();
		for (int i = 0; i < na; i++) ab[i] = b.getShort();
		AccentTable.install(aid, acp, ab);
		int[] up = new int[FamilyTable.COUNT];
		int[] lo = new int[FamilyTable.COUNT];
		int[] di = new int[FamilyTable.COUNT];
		for (int i = 0; i < FamilyTable.COUNT; i++) up[i] = b.getInt();
		for (int i = 0; i < FamilyTable.COUNT; i++) lo[i] = b.getInt();
		for (int i = 0; i < FamilyTable.COUNT; i++) di[i] = b.getInt();
		int nh = b.getInt();
		int[] hc = new int[nh];
		int[] ht = new int[nh];
		for (int i = 0; i < nh; i++) {
			hc[i] = b.getInt();
			ht[i] = b.getInt();
		}
		FamilyTable.install(up, lo, di, hc, ht);
	}
}
