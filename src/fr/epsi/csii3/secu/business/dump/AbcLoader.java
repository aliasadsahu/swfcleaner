// This file is part of CyberShadow's SWF tools.
// Some code is based on or derived from the Adobe Flex SDK, and redistribution is subject to the SDK License.

// Copy-edit of flash.swf.tools.AbcPrinter.
// This file is a copy-edit of flash.swf.tools.AbcPrinter.
// Most methods have been stripped of their output code, so they just read (and in some cases store) data.
// See : https://github.com/CyberShadow/swfutilsex/raw/master/src/net/thecybershadow/swf/tools/AbcProcessor.java

package fr.epsi.csii3.secu.business.dump;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class AbcLoader
{
	protected final byte[] abc;
	protected final ByteArrayOutputStream out;
	protected int offset = 0;
	protected int outOffset = 0;
	protected int copyOffset = 0;

	protected int[] intConstants;
	protected long[] uintConstants;
	// protected double[] floatConstants;
	protected String[] stringConstants;
	protected String[] namespaceConstants;
	protected String[][] namespaceSetConstants;
	protected MultiName[] multiNameConstants;

	public MethodInfo[] methods;
	public String[] instanceNames;
	
	public Map<MethodInfo,byte[]> methodBodies;
	public Map<MethodInfo, String> methodBodiesStrings;

	public AbcLoader(byte[] abc)
	{
		this.abc = abc;
		this.out = new ByteArrayOutputStream();
		methodBodies = new HashMap<AbcLoader.MethodInfo, byte[]>();
		methodBodiesStrings = new HashMap<MethodInfo, String>();
	}

	// Copies all read input since last copyInput/discardInput call.
	protected void copyInput()
	{
		out.write(abc, copyOffset, offset - copyOffset);
		outOffset += offset - copyOffset;
		copyOffset = offset;
	}

	// Discards all read input since last copyInput/discardInput call.
	protected void discardInput()
	{
		copyOffset = offset;
	}

	public void process()
	{
		offset += 4; // versions
		readIntConstantPool();
		readUintConstantPool();
		readDoubleConstantPool();
		readStringConstantPool();
		readNamespaceConstantPool();
		readNamespaceSetsConstantPool();
		readMultiNameConstantPool();
		readMethods();
		readMetaData();
		readClasses();
		readScripts();
		//readBodies();
		printBodies();
		copyInput();
	}

	public byte[] toByteArray()
	{
		return out.toByteArray();
	}

	static final int TRAIT_Slot = 0x00;
	static final int TRAIT_Method = 0x01;
	static final int TRAIT_Getter = 0x02;
	static final int TRAIT_Setter = 0x03;
	static final int TRAIT_Class = 0x04;
	static final int TRAIT_Function = 0x05;
	static final int TRAIT_Const = 0x06;

	static final String[] traitKinds = { "var", "function", "function get", "function set", "class", "function",
			"const" };

	static final int OP_bkpt = 0x01;
	static final int OP_nop = 0x02;
	static final int OP_throw = 0x03;
	static final int OP_getsuper = 0x04;
	static final int OP_setsuper = 0x05;
	static final int OP_dxns = 0x06;
	static final int OP_dxnslate = 0x07;
	static final int OP_kill = 0x08;
	static final int OP_label = 0x09;
	static final int OP_ifnlt = 0x0C;
	static final int OP_ifnle = 0x0D;
	static final int OP_ifngt = 0x0E;
	static final int OP_ifnge = 0x0F;
	static final int OP_jump = 0x10;
	static final int OP_iftrue = 0x11;
	static final int OP_iffalse = 0x12;
	static final int OP_ifeq = 0x13;
	static final int OP_ifne = 0x14;
	static final int OP_iflt = 0x15;
	static final int OP_ifle = 0x16;
	static final int OP_ifgt = 0x17;
	static final int OP_ifge = 0x18;
	static final int OP_ifstricteq = 0x19;
	static final int OP_ifstrictne = 0x1A;
	static final int OP_lookupswitch = 0x1B;
	static final int OP_pushwith = 0x1C;
	static final int OP_popscope = 0x1D;
	static final int OP_nextname = 0x1E;
	static final int OP_hasnext = 0x1F;
	static final int OP_pushnull = 0x20;
	static final int OP_pushundefined = 0x21;
	static final int OP_pushintant = 0x22;
	static final int OP_nextvalue = 0x23;
	static final int OP_pushbyte = 0x24;
	static final int OP_pushshort = 0x25;
	static final int OP_pushtrue = 0x26;
	static final int OP_pushfalse = 0x27;
	static final int OP_pushnan = 0x28;
	static final int OP_pop = 0x29;
	static final int OP_dup = 0x2A;
	static final int OP_swap = 0x2B;
	static final int OP_pushstring = 0x2C;
	static final int OP_pushint = 0x2D;
	static final int OP_pushuint = 0x2E;
	static final int OP_pushdouble = 0x2F;
	static final int OP_pushscope = 0x30;
	static final int OP_pushnamespace = 0x31;
	static final int OP_hasnext2 = 0x32;
	static final int OP_newfunction = 0x40;
	static final int OP_call = 0x41;
	static final int OP_construct = 0x42;
	static final int OP_callmethod = 0x43;
	static final int OP_callstatic = 0x44;
	static final int OP_callsuper = 0x45;
	static final int OP_callproperty = 0x46;
	static final int OP_returnvoid = 0x47;
	static final int OP_returnvalue = 0x48;
	static final int OP_constructsuper = 0x49;
	static final int OP_constructprop = 0x4A;
	static final int OP_callsuperid = 0x4B;
	static final int OP_callproplex = 0x4C;
	static final int OP_callinterface = 0x4D;
	static final int OP_callsupervoid = 0x4E;
	static final int OP_callpropvoid = 0x4F;
	static final int OP_newobject = 0x55;
	static final int OP_newarray = 0x56;
	static final int OP_newactivation = 0x57;
	static final int OP_newclass = 0x58;
	static final int OP_getdescendants = 0x59;
	static final int OP_newcatch = 0x5A;
	static final int OP_findpropstrict = 0x5D;
	static final int OP_findproperty = 0x5E;
	static final int OP_finddef = 0x5F;
	static final int OP_getlex = 0x60;
	static final int OP_setproperty = 0x61;
	static final int OP_getlocal = 0x62;
	static final int OP_setlocal = 0x63;
	static final int OP_getglobalscope = 0x64;
	static final int OP_getscopeobject = 0x65;
	static final int OP_getproperty = 0x66;
	static final int OP_getpropertylate = 0x67;
	static final int OP_initproperty = 0x68;
	static final int OP_setpropertylate = 0x69;
	static final int OP_deleteproperty = 0x6A;
	static final int OP_deletepropertylate = 0x6B;
	static final int OP_getslot = 0x6C;
	static final int OP_setslot = 0x6D;
	static final int OP_getglobalslot = 0x6E;
	static final int OP_setglobalslot = 0x6F;
	static final int OP_convert_s = 0x70;
	static final int OP_esc_xelem = 0x71;
	static final int OP_esc_xattr = 0x72;
	static final int OP_convert_i = 0x73;
	static final int OP_convert_u = 0x74;
	static final int OP_convert_d = 0x75;
	static final int OP_convert_b = 0x76;
	static final int OP_convert_o = 0x77;
	static final int OP_coerce = 0x80;
	static final int OP_coerce_b = 0x81;
	static final int OP_coerce_a = 0x82;
	static final int OP_coerce_i = 0x83;
	static final int OP_coerce_d = 0x84;
	static final int OP_coerce_s = 0x85;
	static final int OP_astype = 0x86;
	static final int OP_astypelate = 0x87;
	static final int OP_coerce_u = 0x88;
	static final int OP_coerce_o = 0x89;
	static final int OP_negate = 0x90;
	static final int OP_increment = 0x91;
	static final int OP_inclocal = 0x92;
	static final int OP_decrement = 0x93;
	static final int OP_declocal = 0x94;
	static final int OP_typeof = 0x95;
	static final int OP_not = 0x96;
	static final int OP_bitnot = 0x97;
	static final int OP_concat = 0x9A;
	static final int OP_add_d = 0x9B;
	static final int OP_add = 0xA0;
	static final int OP_subtract = 0xA1;
	static final int OP_multiply = 0xA2;
	static final int OP_divide = 0xA3;
	static final int OP_modulo = 0xA4;
	static final int OP_lshift = 0xA5;
	static final int OP_rshift = 0xA6;
	static final int OP_urshift = 0xA7;
	static final int OP_bitand = 0xA8;
	static final int OP_bitor = 0xA9;
	static final int OP_bitxor = 0xAA;
	static final int OP_equals = 0xAB;
	static final int OP_strictequals = 0xAC;
	static final int OP_lessthan = 0xAD;
	static final int OP_lessequals = 0xAE;
	static final int OP_greaterthan = 0xAF;
	static final int OP_greaterequals = 0xB0;
	static final int OP_instanceof = 0xB1;
	static final int OP_istype = 0xB2;
	static final int OP_istypelate = 0xB3;
	static final int OP_in = 0xB4;
	static final int OP_increment_i = 0xC0;
	static final int OP_decrement_i = 0xC1;
	static final int OP_inclocal_i = 0xC2;
	static final int OP_declocal_i = 0xC3;
	static final int OP_negate_i = 0xC4;
	static final int OP_add_i = 0xC5;
	static final int OP_subtract_i = 0xC6;
	static final int OP_multiply_i = 0xC7;
	static final int OP_getlocal0 = 0xD0;
	static final int OP_getlocal1 = 0xD1;
	static final int OP_getlocal2 = 0xD2;
	static final int OP_getlocal3 = 0xD3;
	static final int OP_setlocal0 = 0xD4;
	static final int OP_setlocal1 = 0xD5;
	static final int OP_setlocal2 = 0xD6;
	static final int OP_setlocal3 = 0xD7;
	static final int OP_debug = 0xEF;
	static final int OP_debugline = 0xF0;
	static final int OP_debugfile = 0xF1;
	static final int OP_bkptline = 0xF2;

	protected int readS24()
	{
		int b = abc[offset++];
		b &= 0xFF;
		b |= abc[offset++] << 8;
		b &= 0xFFFF;
		b |= abc[offset++] << 16;
		return b;
	}

	protected static int readS24(byte[] abc, int offset)
	{
		int b = abc[offset++];
		b &= 0xFF;
		b |= abc[offset++] << 8;
		b &= 0xFFFF;
		b |= abc[offset++] << 16;
		return b;
	}

	protected static void writeS24(byte[] abc, int offset, int v)
	{
		abc[offset++] = (byte) v;
		abc[offset++] = (byte) (v >> 8);
		abc[offset++] = (byte) (v >> 16);
	}

	protected void writeS24(int v)
	{
		out.write((byte) v);
		out.write((byte) (v >> 8));
		out.write((byte) (v >> 16));
		outOffset += 3;
	}

	protected void writeU32(long v)
	{
		if (v < 128 && v > -1)
		{
			out.write((byte) v);
			outOffset += 1;
		}
		else if (v < 16384 && v > -1)
		{
			out.write((byte) ((v & 0x7F) | 0x80));
			out.write((byte) ((v >> 7) & 0x7F));
			outOffset += 2;
		}
		else if (v < 2097152 && v > -1)
		{
			out.write((byte) ((v & 0x7F) | 0x80));
			out.write((byte) ((v >> 7) | 0x80));
			out.write((byte) ((v >> 14) & 0x7F));
			outOffset += 3;
		}
		else if (v < 268435456 && v > -1)
		{
			out.write((byte) ((v & 0x7F) | 0x80));
			out.write((byte) (v >> 7 | 0x80));
			out.write((byte) (v >> 14 | 0x80));
			out.write((byte) ((v >> 21) & 0x7F));
			outOffset += 4;
		}
		else
		{
			out.write((byte) ((v & 0x7F) | 0x80));
			out.write((byte) (v >> 7 | 0x80));
			out.write((byte) (v >> 14 | 0x80));
			out.write((byte) (v >> 21 | 0x80));
			out.write((byte) ((v >> 28) & 0x0F));
			outOffset += 5;
		}
	}

	protected static int getU32length(long v)
	{
		if (v < 128 && v > -1)
			return 1;
		else if (v < 16384 && v > -1)
			return 2;
		else if (v < 2097152 && v > -1)
			return 3;
		else if (v < 268435456 && v > -1)
			return 4;
		else
			return 5;
	}

	protected long readU32()
	{
		long b = abc[offset++];
		b &= 0xFF;
		long u32 = b;
		if (!((u32 & 0x00000080) == 0x00000080))
			return u32;
		b = abc[offset++];
		b &= 0xFF;
		u32 = u32 & 0x0000007f | b << 7;
		if (!((u32 & 0x00004000) == 0x00004000))
			return u32;
		b = abc[offset++];
		b &= 0xFF;
		u32 = u32 & 0x00003fff | b << 14;
		if (!((u32 & 0x00200000) == 0x00200000))
			return u32;
		b = abc[offset++];
		b &= 0xFF;
		u32 = u32 & 0x001fffff | b << 21;
		if (!((u32 & 0x10000000) == 0x10000000))
			return u32;
		b = abc[offset++];
		b &= 0xFF;
		u32 = u32 & 0x0fffffff | b << 28;
		return u32;
	}

	protected String readUTFBytes(long n)
	{
		StringWriter sw = new StringWriter();
		for (int i = 0; i < n; i++)
		{
			sw.write(abc[offset++]);
		}
		return sw.toString();
	}

	protected void readIntConstantPool()
	{
		long n = readU32();
		intConstants = new int[(n > 0) ? (int) n : 1];
		intConstants[0] = 0;
		for (int i = 1; i < n; i++)
		{
			long val = readU32();
			intConstants[i] = (int) val;
		}
	}

	protected void readUintConstantPool()
	{
		long n = readU32();
		uintConstants = new long[(n > 0) ? (int) n : 1];
		uintConstants[0] = 0;
		for (int i = 1; i < n; i++)
		{
			long val = readU32();
			uintConstants[i] = (int) val;
		}
	}

	protected void readDoubleConstantPool()
	{
		long n = readU32();
		if (n > 0)
			offset += (n - 1) * 8;
	}

	protected void readStringConstantPool()
	{
		long n = readU32();
		stringConstants = new String[(n > 0) ? (int) n : 1];
		stringConstants[0] = "";
		for (int i = 1; i < n; i++)
			stringConstants[i] = readUTFBytes(readU32());
	}

	protected void writeUTFBytes(String s)
	{
		StringReader sr = new StringReader(s);
		int byte_;
		try
		{
			while ((byte_ = sr.read()) != -1)
				out.write(byte_);
		}
		catch (IOException e)
		{
			assert false;
			e.printStackTrace();
		}
	}

	protected void readNamespaceConstantPool()
	{
		long n = readU32();
		namespaceConstants = new String[(n > 0) ? (int) n : 1];
		namespaceConstants[0] = "public";
		for (int i = 1; i < n; i++)
		{
			byte b = abc[offset++];
			String s;
			if (b == 5)
			{
				readU32();
				s = "private";
			}
			else
			{
				s = stringConstants[(int) readU32()];
			}
			namespaceConstants[i] = s;
		}
	}

	protected void readNamespaceSetsConstantPool()
	{
		long n = readU32();
		namespaceSetConstants = new String[(n > 0) ? (int) n : 1][];
		namespaceSetConstants[0] = new String[0];
		for (int i = 1; i < n; i++)
		{
			long val = readU32();
			String[] nsset = new String[(int) val];
			namespaceSetConstants[i] = nsset;
			for (int j = 0; j < val; j++)
			{
				nsset[j] = namespaceConstants[(int) readU32()];
			}
		}
	}

	protected void readMultiNameConstantPool()
	{
		long n = readU32();
		multiNameConstants = new MultiName[(n > 0) ? (int) n : 1];
		multiNameConstants[0] = new MultiName();
		for (int i = 1; i < n; i++)
		{
			byte b = abc[offset++];
			multiNameConstants[i] = new MultiName();
			multiNameConstants[i].kind = b;
			switch (b)
			{
				case 0x07: // QName
				case 0x0D:
					multiNameConstants[i].long1 = (int) readU32();
					multiNameConstants[i].long2 = (int) readU32();
					break;
				case 0x0F: // RTQName
				case 0x10:
					multiNameConstants[i].long1 = (int) readU32();
					break;
				case 0x11: // RTQNameL
				case 0x12:
					break;
				case 0x13: // NameL
				case 0x14:
					break;
				case 0x09:
				case 0x0E:
					multiNameConstants[i].long1 = (int) readU32();
					multiNameConstants[i].long2 = (int) readU32();
					break;
				case 0x1B:
				case 0x1C:
					multiNameConstants[i].long1 = (int) readU32();
					break;
				case 0x1D:
					int nameIndex = (int) readU32();
					MultiName mn = multiNameConstants[nameIndex];
					int count = (int) readU32();
					MultiName types[] = new MultiName[count];
					for (int t = 0; t < count; t++)
					{
						int typeIndex = (int) readU32();
						types[t] = multiNameConstants[typeIndex];
					}
					multiNameConstants[i].typeName = mn;
					multiNameConstants[i].types = types;
			}
		}
	}

	protected void readMethods()
	{
		long n = readU32();
		methods = new MethodInfo[(int) n];
		for (int i = 0; i < n; i++)
		{
			int start = offset;
			MethodInfo m = methods[i] = new MethodInfo();
			m.paramCount = (int) readU32();
			m.returnType = (int) readU32();
			m.params = new int[m.paramCount];
			for (int j = 0; j < m.paramCount; j++)
			{
				m.params[j] = (int) readU32();
			}
			int nameIndex = (int) readU32();
			if (nameIndex > 0)
				m.name = stringConstants[nameIndex];
			else
				m.name = "no name";

			m.flags = abc[offset++];
			if ((m.flags & 0x8) == 0x8)
			{
				// read in optional parameter info
				m.optionCount = (int) readU32();
				m.optionIndex = new int[m.optionCount];
				m.optionKinds = new int[m.optionCount];
				for (int k = 0; k < m.optionCount; k++)
				{
					m.optionIndex[k] = (int) readU32();
					m.optionKinds[k] = abc[offset++];
				}
			}
			if ((m.flags & 0x80) == 0x80)
			{
				// read in parameter names info
				m.paramNames = new int[m.paramCount];
				for (int k = 0; k < m.paramCount; k++)
				{
					m.paramNames[k] = (int) readU32();
				}
			}
		}

	}

	protected void readMetaData()
	{
		long n = readU32();
		for (int i = 0; i < n; i++)
		{
			int start = offset;
			String s = stringConstants[(int) readU32()];
			long val = readU32();
			for (int j = 0; j < val; j++)
			{
				s += " " + stringConstants[(int) readU32()];
			}
			for (int j = 0; j < val; j++)
			{
				s += " " + stringConstants[(int) readU32()];
			}
		}
	}

	protected void readClasses()
	{
		long n = readU32();
		instanceNames = new String[(int) n];
		for (int i = 0; i < n; i++)
		{
			int start = offset;
			String name = multiNameConstants[(int) readU32()].toString();
			instanceNames[i] = name;
			String base = multiNameConstants[(int) readU32()].toString();
			int b = abc[offset++];
			if ((b & 0x8) == 0x8)
				readU32(); // eat protected namespace
			long val = readU32();
			String s = "";
			for (int j = 0; j < val; j++)
			{
				s += " " + multiNameConstants[(int) readU32()].toString();
			}
			int init = (int) readU32(); // eat init method
			MethodInfo mi = methods[init];
			mi.name = name;
			mi.className = name;
			mi.kind = TRAIT_Method;

			int numTraits = (int) readU32(); // number of traits
			for (int j = 0; j < numTraits; j++)
			{
				start = offset;
				s = multiNameConstants[(int) readU32()].toString(); // eat trait
				// name;
				b = abc[offset++];
				int kind = b & 0xf;
				switch (kind)
				{
					case 0x00: // slot
					case 0x06: // const
						readU32(); // id
						readU32(); // type
						int index = (int) readU32(); // index;
						if (index != 0)
							offset++; // kind
						break;
					case 0x04: // class
						readU32(); // id
						readU32(); // value;
						break;
					default:
						readU32(); // id
						mi = methods[(int) readU32()]; // method
						mi.name = s;
						mi.className = name;
						mi.kind = kind;
						break;
				}
				if ((b >> 4 & 0x4) == 0x4)
				{
					val = readU32(); // metadata count
					for (int k = 0; k < val; k++)
					{
						readU32(); // metadata
					}
				}
			}
		}
		for (int i = 0; i < n; i++)
		{
			int start = offset;
			MethodInfo mi = methods[(int) readU32()];
			String name = instanceNames[i];
			mi.name = name + "$cinit";
			mi.className = name;
			mi.kind = TRAIT_Method;
			String base = "Class";

			int numTraits = (int) readU32(); // number of traits
			for (int j = 0; j < numTraits; j++)
			{
				start = offset;
				String s = multiNameConstants[(int) readU32()].toString(); // eat
				// trait
				// name;
				int b = abc[offset++];
				int kind = b & 0xf;
				switch (kind)
				{
					case 0x00: // slot
					case 0x06: // const
						readU32(); // id
						readU32(); // type
						int index = (int) readU32(); // index;
						if (index != 0)
							offset++; // kind
						break;
					case 0x04: // class
						readU32(); // id
						readU32(); // value;
						break;
					default:
						readU32(); // id
						mi = methods[(int) readU32()]; // method
						mi.name = s;
						mi.className = name;
						mi.kind = kind;
						break;
				}
				if ((b >> 4 & 0x4) == 0x4)
				{
					int val = (int) readU32(); // metadata count
					for (int k = 0; k < val; k++)
					{
						readU32(); // metadata
					}
				}
			}
		}
	}

	protected void readScripts()
	{
		long n = readU32();
		for (int i = 0; i < n; i++)
		{
			int start = offset;
			String name = "script" + Integer.toString(i);
			int init = (int) readU32(); // eat init method
			MethodInfo mi = methods[init];
			mi.name = name + "$init";
			mi.className = name;
			mi.kind = TRAIT_Method;

			int numTraits = (int) readU32(); // number of traits
			for (int j = 0; j < numTraits; j++)
			{
				start = offset;
				String s = multiNameConstants[(int) readU32()].toString(); // eat
				// trait
				// name;
				int b = abc[offset++];
				int kind = b & 0xf;
				switch (kind)
				{
					case 0x00: // slot
					case 0x06: // const
						readU32(); // id
						readU32(); // type
						int index = (int) readU32(); // index;
						if (index != 0)
							offset++; // kind
						break;
					case 0x04: // class
						readU32(); // id
						readU32(); // value;
						break;
					default:
						readU32(); // id
						mi = methods[(int) readU32()]; // method
						mi.name = s;
						mi.className = name;
						mi.kind = kind;
						break;
				}
				if ((b >> 4 & 0x4) == 0x4)
				{
					int val = (int) readU32(); // metadata count
					for (int k = 0; k < val; k++)
					{
						readU32(); // metadata
					}
				}
			}
		}

	}

	protected void readBodies()
	{
		long n = readU32();
		// printOffset();
		// out.println("===== " + n + " Method Bodies" + " =====");
		for (int i = 0; i < n; i++)
		{
			copyInput();
			int functionStartOffset = offset;
			int functionStartOutOffset = outOffset;
			// printOffset();
			// int start = offset;
			int methodIndex = (int) readU32();
			int maxStack = (int) readU32();
			int localCount = (int) readU32();
			int initScopeDepth = (int) readU32();
			int maxScopeDepth = (int) readU32();
			int codeLength = (int) readU32();
			offset += codeLength;

			MethodInfo mi = methods[methodIndex];
			// TWEAK
			byte[] body = new byte[codeLength];
			for (int j = functionStartOffset; j < functionStartOffset+codeLength; j++) {
				body[j-functionStartOffset] = this.abc[j];				
			}
			this.methodBodies.put(mi,body);
			// /TWEAK
			copyInput();

			int exCount = (int) readU32();
			// printOffset();
			// out.println(exCount + " Extras");
			for (int j = 0; j < exCount; j++)
			{
				// start = offset;
				// printOffset();
				int from = (int) readU32();
				int to = (int) readU32();
				int target = (int) readU32();
				int typeIndex = (int) readU32();
				int nameIndex = (int) readU32();
				// if (showByteCode)
				// {
				// for (int x = start; x < offset; x++)
				// {
				// out.print(hex(abc[(int)x]) + " ");
				// }
				// }
				// out.print(multiNameConstants[nameIndex] + " ");
				// out.print("type:" + multiNameConstants[typeIndex] + " from:"
				// + from + " ");
				// out.println("to:" + to + " target:" + target);
			}

			int numTraits = (int) readU32(); // number of traits
			// printOffset();
			// out.println(numTraits + " Traits Entries");
			for (int j = 0; j < numTraits; j++)
			{
				// printOffset();
				// start = offset;
				String s = multiNameConstants[(int) readU32()].toString(); // eat
				// trait
				// name;
				int b = abc[offset++];
				int kind = b & 0xf;
				switch (kind)
				{
					case 0x00: // slot
					case 0x06: // const
						readU32(); // id
						readU32(); // type
						int index = (int) readU32(); // index;
						if (index != 0)
							offset++; // kind
						break;
					case 0x04: // class
						readU32(); // id
						readU32(); // value;
						break;
					default:
						readU32(); // id
						readU32(); // method
						break;
				}
				if ((b >> 4 & 0x4) == 0x4)
				{
					int val = (int) readU32(); // metadata count
					for (int k = 0; k < val; k++)
					{
						readU32(); // metadata
					}
				}
				// if (showByteCode)
				// {
				// for (int x = start; x < offset; x++)
				// {
				// out.print(hex(abc[(int)x]) + " ");
				// }
				// }
				// out.println(s);
			}
			// out.println("");
			//System.out.println("======================================================================");
		}
	}

	class MultiName
	{
		public MultiName()
		{
		}

		public int kind;
		public int long1;
		public int long2;
		public MultiName typeName;
		public MultiName types[];

		@Override
		public String toString()
		{
			String s = "";

			String[] nsSet;
			int len;
			int j;

			switch (kind)
			{
				case 0x07: // QName
				case 0x0D:
					s = namespaceConstants[long1] + ":";
					s += stringConstants[long2];
					break;
				case 0x0F: // RTQName
				case 0x10:
					s = stringConstants[long1];
					break;
				case 0x11: // RTQNameL
				case 0x12:
					s = "RTQNameL";
					break;
				case 0x13: // NameL
				case 0x14:
					s = "NameL";
					break;
				case 0x09:
				case 0x0E:
					nsSet = namespaceSetConstants[long2];
					len = nsSet.length;
					for (j = 0; j < len - 1; j++)
					{
						s += nsSet[j] + ",";
					}
					if (len > 0)
						s += nsSet[len - 1] + ":";
					s += stringConstants[long1];
					break;
				case 0x1B:
				case 0x1C:
					nsSet = namespaceSetConstants[long1];
					len = nsSet.length;
					for (j = 0; j < len - 1; j++)
					{
						s += nsSet[j] + ",";
					}
					if (len > 0)
						s += nsSet[len - 1] + ":";
					s += "null";
					break;
				case 0x1D:
					s += typeName.toString();
					for (int t = 0; t < types.length; t++)
						s += types[t].toString();
			}
			return s;
		}
	}

	public class MethodInfo
	{
		int paramCount;
		int returnType;
		int[] params;
		String name;
		int kind;
		int flags;
		int optionCount;
		int[] optionKinds;
		int[] optionIndex;
		int[] paramNames;
		String className;
		
		public String getClassName() {
			return this.className;
		}
		
		public String getName() {
			return this.name;
		}
	}
	
	
	
	
	
	
    void printBodies()
    {
    	//PrintStream out = System.out;
        long n = readU32();
        //out.println(n + " Method Bodies");
        for (int i = 0; i < n; i++)
        {
            int start = offset;
            int methodIndex = (int)readU32();
            int maxStack = (int)readU32();
            int localCount = (int)readU32();
            int initScopeDepth = (int)readU32();
            int maxScopeDepth = (int)readU32();
            int codeLength = (int)readU32();
            
            MethodInfo mi = methods[methodIndex];
            /*out.print(traitKinds[mi.kind] + " ");
            out.print(mi.className + "::" + mi.name + "(");
            for (int x = 0; x < mi.paramCount - 1; x++)
            {
                out.print(multiNameConstants[mi.params[x]].toString() + ", ");
            }
            if (mi.paramCount > 0)
                out.print(multiNameConstants[mi.params[mi.paramCount - 1]].toString());
            out.print("):");
            out.println(multiNameConstants[mi.returnType].toString());
            out.print("maxStack:" + maxStack + " localCount:" + localCount + " ");
            out.println("initScopeDepth:" + initScopeDepth + " maxScopeDepth:" + maxScopeDepth);
			*/
            
            LabelMgr labels = new LabelMgr();
            int stopAt = codeLength + offset;
            
            // Tweak
            byte[] body = new byte[stopAt-offset];
            for(int o = offset; o<stopAt;o++)
            	body[o-offset] = abc[o];
            methodBodies.put(mi, body);
            String bodyStr = new String();
            // /Tweak
            
            while (offset < stopAt)
            {
                String s = "";
                start = offset;
                int opcode = abc[offset++] & 0xFF;
					
                if (opcode == OP_label || labels.hasLabelAt(offset - 1)) 
                {
                    s = labels.getLabelAt(offset - 1) + ":";
                    while (s.length() < 4)
                        s += " ";
                }
                else
                    s = "    ";
					
					
                s += opNames[opcode];
                s += opNames[opcode].length() < 8 ? "\t\t" : "\t";
					
                switch (opcode)
                {
                case OP_debugfile:
                case OP_pushstring:
                    s += '"' + stringConstants[(int)readU32()].replaceAll("\n","\\n").replaceAll("\t","\\t") + '"';
                    break;
                case OP_pushnamespace:
                    s += namespaceConstants[(int)readU32()];
                    break;
                case OP_pushint:
                    int k = intConstants[(int)readU32()];
                    s += k + "\t// 0x" + Integer.toHexString(k);
                    break;
                case OP_pushuint:
                    long u = uintConstants[(int)readU32()];
                    s += u + "\t// 0x" + Long.toHexString(u);
                    break;
                case OP_pushdouble:
                    int f = (int)readU32();
                    s += "floatConstant" + f;
                    break;
                case OP_getsuper: 
                case OP_setsuper: 
                case OP_getproperty: 
                case OP_initproperty: 
                case OP_setproperty: 
                case OP_getlex: 
                case OP_findpropstrict: 
                case OP_findproperty:
                case OP_finddef:
                case OP_deleteproperty: 
                case OP_istype: 
                case OP_coerce: 
                case OP_astype: 
                case OP_getdescendants:
                    s += multiNameConstants[(int)readU32()];
                    break;
                case OP_constructprop:
                case OP_callproperty:
                case OP_callproplex:
                case OP_callsuper:
                case OP_callsupervoid:
                case OP_callpropvoid:
                    s += multiNameConstants[(int)readU32()];
                    s += " (" + readU32() + ")";
                    break;
                case OP_newfunction:
                    int method_id = (int)readU32();
                    s += methods[method_id].name;
                    // abc.methods[method_id].anon = true  (do later?)
                    break;
                case OP_callstatic:
                    s += methods[(int)readU32()].name;
                    s += " (" + readU32() + ")";
                    break;
                case OP_newclass: 
                    s += instanceNames[(int)readU32()];
                    break;
                case OP_lookupswitch:
                    int pos = offset - 1;
                    int target = pos + readS24();
                    int maxindex = (int)readU32();
                    s += "default:" + labels.getLabelAt(target); // target + "("+(target-pos)+")"
                    s += " maxcase:" + Integer.toString(maxindex);
                    for (int m = 0; m <= maxindex; m++) 
                    {
                        target = pos + readS24();
                        s += " " + labels.getLabelAt(target); // target + "("+(target-pos)+")"
                    }
                    break;
                case OP_jump:
                case OP_iftrue:		case OP_iffalse:
                case OP_ifeq:		case OP_ifne:
                case OP_ifge:		case OP_ifnge:
                case OP_ifgt:		case OP_ifngt:
                case OP_ifle:		case OP_ifnle:
                case OP_iflt:		case OP_ifnlt:
                case OP_ifstricteq:	case OP_ifstrictne:
                    int delta = readS24();
                    int targ = offset + delta;
                    //s += target + " ("+offset+")"
                    s += labels.getLabelAt(targ);
                    if (!(labels.hasLabelAt(offset)))
                        s += "\n";
                    break;
                case OP_inclocal:
                case OP_declocal:
                case OP_inclocal_i:
                case OP_declocal_i:
                case OP_getlocal:
                case OP_kill:
                case OP_setlocal:
                case OP_debugline:
                case OP_getglobalslot:
                case OP_getslot:
                case OP_setglobalslot:
                case OP_setslot:
                case OP_pushshort:
                case OP_newcatch:
                    s += readU32();
                    break;
                case OP_debug:
                    s += Integer.toString(abc[offset++] & 0xFF); 
                    s += " " + readU32();
                    s += " " + Integer.toString(abc[offset++] & 0xFF);
                    s += " " + readU32();
                    break;
                case OP_newobject:
                    s += "{" + readU32() + "}";
                    break;
                case OP_newarray:
                    s += "[" + readU32() + "]";
                    break;
                case OP_call:
                case OP_construct:
                case OP_constructsuper:
                    s += "(" + readU32() + ")";
                    break;
                case OP_pushbyte:
                case OP_getscopeobject:
                    s += abc[offset++];
                    break;
                case OP_hasnext2:
                    s += readU32() + " " + readU32();
                default:
                    /*if (opNames[opcode] == ("0x"+opcode.toString(16).toUpperCase()))
                      s += " UNKNOWN OPCODE"*/
                    break;
                }
                //out.println(s);
                bodyStr += s + "\n";
            }
            
            // Tweak
            methodBodiesStrings.put(mi, bodyStr);
            // /Tweak
            
            int exCount = (int)readU32();
            //out.println(exCount + " Extras");
            for (int j = 0; j < exCount; j++)
            {
                start = offset;
                int from = (int)readU32();
                int to = (int)readU32();
                int target = (int)readU32();
                int typeIndex = (int)readU32();
                int nameIndex = (int)readU32();
                /*out.print(multiNameConstants[nameIndex] + " ");
                out.print("type:" + multiNameConstants[typeIndex] + " from:" + from + " ");
                out.println("to:" + to + " target:" + target);*/
            }
            int numTraits = (int)readU32(); // number of traits
            //out.println(numTraits + " Traits Entries");
            for (int j = 0; j < numTraits; j++)
            {
                start = offset;
                String s = multiNameConstants[(int)readU32()].toString(); // eat trait name;
                int b =  abc[offset++];
                int kind = b & 0xf;
                switch (kind)
                {
                case 0x00:	// slot
                case 0x06:	// const
                    readU32();	// id
                    readU32();	// type
                    int index = (int)readU32();	// index;
                    if (index != 0)
                        offset++;	// kind
                    break;
                case 0x04:	// class
                    readU32();	// id
                    readU32();	// value;
                    break;
                default:
                    readU32();	// id
                    readU32();  // method
                    break;
                }
                if ((b >> 4 & 0x4) == 0x4)
                {
                    int val = (int)readU32();	// metadata count
                    for (int k = 0; k < val; k++)
                    {
                        readU32();	// metadata
                    }
                }
                //out.println(s);
            }
            //out.println("");
        }
    }
    
    class LabelMgr
    {
        int index = 0;
				
        HashMap<String, Integer> labels;
				
        public LabelMgr()
        {
            labels = new HashMap<String, Integer>();
        }
				
        public String getLabelAt(int offset)
        {
            String key = Integer.toString(offset);
            if (!labels.containsKey(key))
                labels.put(key, new Integer(index++));
            return "L" + labels.get(key).toString();
        }
				
        public boolean hasLabelAt(int offset)
        {
            String key = Integer.toString(offset);
            return labels.containsKey(key);
        }
    }
    
    String[] opNames = {
    	    "OP_0x00       ",
    	    "bkpt          ",
    	    "nop           ",
    	    "throw         ",
    	    "getsuper      ",
    	    "setsuper      ",
    	    "dxns          ",
    	    "dxnslate      ",
    	    "kill          ",
    	    "label         ",
    	    "OP_0x0A       ",
    	    "OP_0x0B       ",
    	    "ifnlt         ",
    	    "ifnle         ",
    	    "ifngt         ",
    	    "ifnge         ",
    	    "jump          ",
    	    "iftrue        ",
    	    "iffalse       ",
    	    "ifeq          ",
    	    "ifne          ",
    	    "iflt          ",
    	    "ifle          ",
    	    "ifgt          ",
    	    "ifge          ",
    	    "ifstricteq    ",
    	    "ifstrictne    ",
    	    "lookupswitch  ",
    	    "pushwith      ",
    	    "popscope      ",
    	    "nextname      ",
    	    "hasnext       ",
    	    "pushnull      ",
    	    "pushundefined ",
    	    "pushconstant  ",
    	    "nextvalue     ",
    	    "pushbyte      ",
    	    "pushshort     ",
    	    "pushtrue      ",
    	    "pushfalse     ",
    	    "pushnan       ",
    	    "pop           ",
    	    "dup           ",
    	    "swap          ",
    	    "pushstring    ",
    	    "pushint       ",
    	    "pushuint      ",
    	    "pushdouble    ",
    	    "pushscope     ",
    	    "pushnamespace ",
    	    "hasnext2      ",
    	    "OP_0x33       ",
    	    "OP_0x34       ",
    	    "OP_0x35       ",
    	    "OP_0x36       ",
    	    "OP_0x37       ",
    	    "OP_0x38       ",
    	    "OP_0x39       ",
    	    "OP_0x3A       ",
    	    "OP_0x3B       ",
    	    "OP_0x3C       ",
    	    "OP_0x3D       ",
    	    "OP_0x3E       ",
    	    "OP_0x3F       ",
    	    "newfunction   ",
    	    "call          ",
    	    "construct     ",
    	    "callmethod    ",
    	    "callstatic    ",
    	    "callsuper     ",
    	    "callproperty  ",
    	    "returnvoid    ",
    	    "returnvalue   ",
    	    "constructsuper",
    	    "constructprop ",
    	    "callsuperid   ",
    	    "callproplex   ",
    	    "callinterface ",
    	    "callsupervoid ",
    	    "callpropvoid  ",
    	    "OP_0x50       ",
    	    "OP_0x51       ",
    	    "OP_0x52       ",
    	    "OP_0x53       ",
    	    "OP_0x54       ",
    	    "newobject     ",
    	    "newarray      ",
    	    "newactivation ",
    	    "newclass      ",
    	    "getdescendants",
    	    "newcatch      ",
    	    "OP_0x5B       ",
    	    "OP_0x5C       ",
    	    "findpropstrict",
    	    "findproperty  ",
    	    "finddef       ",
    	    "getlex        ",
    	    "setproperty   ",
    	    "getlocal      ",
    	    "setlocal      ",
    	    "getglobalscope",
    	    "getscopeobject",
    	    "getproperty   ",
    	    "OP_0x67       ",
    	    "initproperty  ",
    	    "OP_0x69       ",
    	    "deleteproperty",
    	    "OP_0x6A       ",
    	    "getslot       ",
    	    "setslot       ",
    	    "getglobalslot ",
    	    "setglobalslot ",
    	    "convert_s     ",
    	    "esc_xelem     ",
    	    "esc_xattr     ",
    	    "convert_i     ",
    	    "convert_u     ",
    	    "convert_d     ",
    	    "convert_b     ",
    	    "convert_o     ",
    	    "checkfilter   ",
    	    "OP_0x79       ",
    	    "OP_0x7A       ",
    	    "OP_0x7B       ",
    	    "OP_0x7C       ",
    	    "OP_0x7D       ",
    	    "OP_0x7E       ",
    	    "OP_0x7F       ",
    	    "coerce        ",
    	    "coerce_b      ",
    	    "coerce_a      ",
    	    "coerce_i      ",
    	    "coerce_d      ",
    	    "coerce_s      ",
    	    "astype        ",
    	    "astypelate    ",
    	    "coerce_u      ",
    	    "coerce_o      ",
    	    "OP_0x8A       ",
    	    "OP_0x8B       ",
    	    "OP_0x8C       ",
    	    "OP_0x8D       ",
    	    "OP_0x8E       ",
    	    "OP_0x8F       ",
    	    "negate        ",
    	    "increment     ",
    	    "inclocal      ",
    	    "decrement     ",
    	    "declocal      ",
    	    "typeof        ",
    	    "not           ",
    	    "bitnot        ",
    	    "OP_0x98       ",
    	    "OP_0x99       ",
    	    "concat        ",
    	    "add_d         ",
    	    "OP_0x9C       ",
    	    "OP_0x9D       ",
    	    "OP_0x9E       ",
    	    "OP_0x9F       ",
    	    "add           ",
    	    "subtract      ",
    	    "multiply      ",
    	    "divide        ",
    	    "modulo        ",
    	    "lshift        ",
    	    "rshift        ",
    	    "urshift       ",
    	    "bitand        ",
    	    "bitor         ",
    	    "bitxor        ",
    	    "equals        ",
    	    "strictequals  ",
    	    "lessthan      ",
    	    "lessequals    ",
    	    "greaterthan   ",
    	    "greaterequals ",
    	    "instanceof    ",
    	    "istype        ",
    	    "istypelate    ",
    	    "in            ",
    	    "OP_0xB5       ",
    	    "OP_0xB6       ",
    	    "OP_0xB7       ",
    	    "OP_0xB8       ",
    	    "OP_0xB9       ",
    	    "OP_0xBA       ",
    	    "OP_0xBB       ",
    	    "OP_0xBC       ",
    	    "OP_0xBD       ",
    	    "OP_0xBE       ",
    	    "OP_0xBF       ",
    	    "increment_i   ",
    	    "decrement_i   ",
    	    "inclocal_i    ",
    	    "declocal_i    ",
    	    "negate_i      ",
    	    "add_i         ",
    	    "subtract_i    ",
    	    "multiply_i    ",
    	    "OP_0xC8       ",
    	    "OP_0xC9       ",
    	    "OP_0xCA       ",
    	    "OP_0xCB       ",
    	    "OP_0xCC       ",
    	    "OP_0xCD       ",
    	    "OP_0xCE       ",
    	    "OP_0xCF       ",
    	    "getlocal0     ",
    	    "getlocal1     ",
    	    "getlocal2     ",
    	    "getlocal3     ",
    	    "setlocal0     ",
    	    "setlocal1     ",
    	    "setlocal2     ",
    	    "setlocal3     ",
    	    "OP_0xD8       ",
    	    "OP_0xD9       ",
    	    "OP_0xDA       ",
    	    "OP_0xDB       ",
    	    "OP_0xDC       ",
    	    "OP_0xDD       ",
    	    "OP_0xDE       ",
    	    "OP_0xDF       ",
    	    "OP_0xE0       ",
    	    "OP_0xE1       ",
    	    "OP_0xE2       ",
    	    "OP_0xE3       ",
    	    "OP_0xE4       ",
    	    "OP_0xE5       ",
    	    "OP_0xE6       ",
    	    "OP_0xE7       ",
    	    "OP_0xE8       ",
    	    "OP_0xE9       ",
    	    "OP_0xEA       ",
    	    "OP_0xEB       ",
    	    "OP_0xEC       ",
    	    "OP_0xED       ",
    	    "OP_0xEE       ",
    	    "debug         ",
    	    "debugline     ",
    	    "debugfile     ",
    	    "bkptline      ",
    	    "timestamp     ",
    	    "OP_0xF4       ",
    	    "verifypass    ",
    	    "alloc         ",
    	    "mark          ",
    	    "wb            ",
    	    "prologue      ",
    	    "sendenter     ",
    	    "doubletoatom  ",
    	    "sweep         ",
    	    "codegenop     ",
    	    "verifyop      ",
    	    "decode        "
        };
        
}