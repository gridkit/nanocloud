package org.gridkit.lab.interceptor;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.objectweb.asm.Opcodes;

/**
 * A simple class file parser for inspecting constant pool.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class ConstPoolParser {
	
    /**
     * The type of CONSTANT_Class constant pool items.
     */
    static final int CLASS = 7;

    /**
     * The type of CONSTANT_Fieldref constant pool items.
     */
    static final int FIELD = 9;

    /**
     * The type of CONSTANT_Methodref constant pool items.
     */
    static final int METH = 10;

    /**
     * The type of CONSTANT_InterfaceMethodref constant pool items.
     */
    static final int IMETH = 11;

    /**
     * The type of CONSTANT_String constant pool items.
     */
    static final int STR = 8;

    /**
     * The type of CONSTANT_Integer constant pool items.
     */
    static final int INT = 3;

    /**
     * The type of CONSTANT_Float constant pool items.
     */
    static final int FLOAT = 4;

    /**
     * The type of CONSTANT_Long constant pool items.
     */
    static final int LONG = 5;

    /**
     * The type of CONSTANT_Double constant pool items.
     */
    static final int DOUBLE = 6;

    /**
     * The type of CONSTANT_NameAndType constant pool items.
     */
    static final int NAME_TYPE = 12;

    /**
     * The type of CONSTANT_Utf8 constant pool items.
     */
    static final int UTF8 = 1;

    /**
     * The type of CONSTANT_MethodType constant pool items.
     */
    static final int MTYPE = 16;

    /**
     * The type of CONSTANT_MethodHandle constant pool items.
     */
    static final int HANDLE = 15;

    /**
     * The type of CONSTANT_InvokeDynamic constant pool items.
     */
    static final int INDY = 18;

    /**
     * The base value for all CONSTANT_MethodHandle constant pool items.
     * Internally, ASM store the 9 variations of CONSTANT_MethodHandle into
     * 9 different items.
     */
    static final int HANDLE_BASE = 20;
	
	private byte[] b;
	private ConstEntry[] entries;
	
	public ConstPoolParser(byte[] data) {
		this.b = data;

		if (readShort(6) > Opcodes.V1_7) {
            throw new IllegalArgumentException("Unsupported class file version: " + readShort(6));
        }
		
		int count = readUnsignedShort(8);
		entries = new ConstEntry[count];
		int offs = 10;
		for(int i = 1; i != count; ++i) {
			int size;	
			int ctype = b[offs]; 
            switch (ctype) {
            case CLASS:
            	entries[i] = new ConstClass();
            	size = entries[i].parse(offs);
            	break;
            case FIELD:
            case METH:
            case IMETH:
            	entries[i] = new ConstFieldOrMethod();
            	size = entries[i].parse(offs);
            	break;
            case NAME_TYPE:
            	entries[i] = new ConstTypeAndName();
            	size = entries[i].parse(offs);
            	break;
            case INT:
            case FLOAT:
            case INDY:
                size = 5;
                break;
            case LONG:
            case DOUBLE:
                size = 9;
                ++i;
                break;
            case UTF8:
                entries[i] = new ConstUtf8();
                size = entries[i].parse(offs);
                break;
            case HANDLE:
            	size = 4;
            	break;
            case STR:
            	entries[i] = new ConstString();
            	size = entries[i].parse(offs);
            	break;
            case MTYPE:
            	size = 3;
            	break;
            default:
            	throw new IllegalArgumentException("Unknown CONST type: " + ctype);
            }
            offs += size;
		}
	}
	
	public ConstEntry get(int n) {
		return entries[n];
	}
	
	public Iterable<ConstEntry> all() {
		return new Iterable<ConstPoolParser.ConstEntry>() {
			@Override
			public Iterator<ConstEntry> iterator() {
				return new Iterator<ConstPoolParser.ConstEntry>() {

					int n = 0;
					
					{
						next();
					}
					
					@Override
					public boolean hasNext() {
						return n < entries.length;
					}

					@Override
					public ConstEntry next() {
						if (!hasNext()) {
							throw new NoSuchElementException();
						}
						ConstEntry ce = entries[n];
						while((++n) < entries.length && entries[n] == null) {};
						return ce;
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}
	
    private short readShort(final int index) {
        byte[] b = this.b;
        return (short) (((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF));
    }
	
    private int readUnsignedShort(final int index) {
        byte[] b = this.b;
        return ((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF);
    }
    
    /**
     * Reads UTF8 string in {@link #b b}.
     *
     * @param index start offset of the UTF8 string to be read.
     * @param utfLen length of the UTF8 string to be read.
     * @param buf buffer to be used to read the string. This buffer must be
     *        sufficiently large. It is not automatically resized.
     * @return the String corresponding to the specified UTF8 string.
     */
    private static String readUTF(byte[] b, int index, final int utfLen, final char[] buf) {
        int endIndex = index + utfLen;
        int strLen = 0;
        int c;
        int st = 0;
        char cc = 0;
        while (index < endIndex) {
            c = b[index++];
            switch (st) {
                case 0:
                    c = c & 0xFF;
                    if (c < 0x80) {  // 0xxxxxxx
                        buf[strLen++] = (char) c;
                    } else if (c < 0xE0 && c > 0xBF) {  // 110x xxxx 10xx xxxx
                        cc = (char) (c & 0x1F);
                        st = 1;
                    } else {  // 1110 xxxx 10xx xxxx 10xx xxxx
                        cc = (char) (c & 0x0F);
                        st = 2;
                    }
                    break;

                case 1:  // byte 2 of 2-byte char or byte 3 of 3-byte char
                    buf[strLen++] = (char) ((cc << 6) | (c & 0x3F));
                    st = 0;
                    break;

                case 2:  // byte 2 of 3-byte char
                    cc = (char) ((cc << 6) | (c & 0x3F));
                    st = 1;
                    break;
            }
        }
        return new String(buf, 0, strLen);
    }
    
    interface ConstEntry {
    	
    	public int parse(int offs);
    	
    }
    
    /**
	 * http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.4.1
	 */
	class ConstClass implements ConstEntry {
		
		private int nameRef;
	
		@Override
		public int parse(int offs) {
			nameRef = readUnsignedShort(offs + 1);
			return 3;
		}
	
		public String getName() {
			return entries[nameRef].toString();
		}

		@Override
		public String toString() {
			return "CLASS " + getName();
		}
	}
	
    /**
	 * http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.4.2
	 */
	class ConstFieldOrMethod implements ConstEntry {
		
		private int type;
		private int classNameRef;
		private int typeAndNameRef;
		
		@Override
		public int parse(int offs) {
			type = b[offs];
			classNameRef = readUnsignedShort(offs + 1);
			typeAndNameRef = readUnsignedShort(offs + 3);
			return 5;
		}
		
		public String getClassName() {
			return ((ConstClass)entries[classNameRef]).getName();
		}
		
		public boolean isField() {
			return type == FIELD;
		}
		
		public boolean isInterfaceMethod() {
			return type == IMETH;
		}
		
		public String getName() {
			return ((ConstTypeAndName)entries[typeAndNameRef]).getName();
		}

		public String getType() {
			return ((ConstTypeAndName)entries[typeAndNameRef]).getType();
		}

		@Override
		public String toString() {
			return (isField() ? "FIELD " : isInterfaceMethod() ? "IMETHOD " : "METHOD ")
					+ getClassName() + "::" + getName() + " -> " + getType();
		}
	}

    /**
	 * http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.4.3
	 */
	class ConstString implements ConstEntry {

		private int utfRef;

		@Override
		public int parse(int offs) {
			utfRef = readUnsignedShort(offs);
			return 3;
		}
	
		public String toString() {
			return entries[utfRef].toString();
		}
	}
	
    /**
	 * http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.4.6
	 */
	class ConstTypeAndName implements ConstEntry {

		private int nameRef;
		private int typeRef;
		
		@Override
		public int parse(int offs) {
			nameRef = readUnsignedShort(offs + 1);
			typeRef = readUnsignedShort(offs + 3);
			return 5;
		}
		
		public String getName() {
			return entries[nameRef].toString();
		}
		
		public String getType() {
			return entries[typeRef].toString();
		}

		@Override
		public String toString() {
			return "NAME_AND_TYPE " + getName() + " -> " + getType();
		}
	}
	
    /**
	 * http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.4.7
	 */
	class ConstUtf8 implements ConstEntry {
    	
    	private int offs;
    	private int len;
    	private String text;
    	
    	@Override
    	public int parse(int offs) {
    		this.offs = offs + 3;
            len = readUnsignedShort(offs + 1);
            return len + 3;
    	}
    	
		public String toString() {
    		if (text == null) {
    			text = readUTF(b, offs, len, new char[len]);
    		}
    		return text;
    	}
    }
}
