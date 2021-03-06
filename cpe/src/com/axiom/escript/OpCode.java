package com.axiom.escript ;
import java.lang.reflect.* ;

public abstract class OpCode {

    static final int NOP = 0 ;
    static final int
    ACONST_NULL = 1,
    ICONST_M1 = 2,
    ICONST_0 = 3,
    ICONST_1 = 4,
    ICONST_2 = 5,
    ICONST_3 = 6,
    ICONST_4 = 7,
    ICONST_5 = 8,
    LCONST_0 = 9,
    LCONST_1 = 10,
    FCONST_0 = 11,
    FCONST_1 = 12,
    FCONST_2 = 13,
    DCONST_0 = 14,
    DCONST_1 = 15,
    BIPUSH = 16,
    SIPUSH = 17,
    LDC = 18,
    LDC_w = 19,
    LDC2_w = 20,
    ILOAD = 21,
    LLOAD = 22,
    FLOAD = 23,
    DLOAD = 24,
    ALOAD = 25,
    ILOAD_0 = 26,
    ILOAD_1 = 27,
    ILOAD_2 = 28,
    ILOAD_3 = 29,
    LLOAD_0 = 30,
    LLOAD_1 = 31,
    LLOAD_2 = 32,
    LLOAD_3 = 33,
    FLOAD_0 = 34,
    FLOAD_1 = 35,
    FLOAD_2 = 36,
    FLOAD_3 = 37,
    DLOAD_0 = 38,
    DLOAD_1 = 39,
    DLOAD_2 = 40,
    DLOAD_3 = 41,
    ALOAD_0 = 42,
    ALOAD_1 = 43,
    ALOAD_2 = 44,
    ALOAD_3 = 45,
    IALOAD = 46,
    LALOAD = 47,
    FALOAD = 48,
    DALOAD = 49,
    AALOAD = 50,
    BALOAD = 51,
    CALOAD = 52,
    SALOAD = 53,
    ISTORE = 54,
    LSTORE = 55,
    FSTORE = 56,
    DSTORE = 57,
    ASTORE = 58,
    ISTORE_0 = 59,
    ISTORE_1 = 60,
    ISTORE_2 = 61,
      ISTORE_3 = 62,
      LSTORE_0 = 63,
      LSTORE_1 = 64,
      LSTORE_2 = 65,
      LSTORE_3 = 66,
      FSTORE_0 = 67,
      FSTORE_1 = 68,
      FSTORE_2 = 69,
      FSTORE_3 = 70,
      DSTORE_0 = 71,
      DSTORE_1 = 72,
      DSTORE_2 = 73,
      DSTORE_3 = 74,
      ASTORE_0 = 75,
      ASTORE_1 = 76,
      ASTORE_2 = 77,
      ASTORE_3 = 78,
      IASTORE = 79,
      LASTORE = 80,
      FASTORE = 81,
      DASTORE = 82,
      AASTORE = 83,
      BASTORE = 84,
      CASTORE = 85,
      SASTORE = 86,
      POP = 87,
      POP2 = 88,
      DUP = 89,
      DUP_X1 = 90,
      DUP_X2 = 91,
      DUP2 = 92,
      DUP2_x1 = 93,
      DUP2_x2 = 94,
      SWAP = 95,
      IADD = 96,
      LADD = 97,
      FADD = 98,
      DADD = 99,
      ISUB = 100,
      LSUB = 101,
      FSUB = 102,
      DSUB = 103,
      IMUL = 104,
      LMUL = 105,
      FMUL = 106,
      DMUL = 107,
      IDIV = 108,
      LDIV = 109,
      FDIV = 110,
      DDIV = 111,
      IREM = 112,
      LREM = 113,
      FREM = 114,
      DREM = 115,
      INEG = 116,
      LNEG = 117,
      FNEG = 118,
      DNEG = 119,
      ISHL = 120,
      LSHL = 121,
      ISHR = 122,
      LSHR = 123,
      IUSHR = 124,
      LUSHR = 125,
      IAND = 126,
      LAND = 127,
      IOR = 128,
      LOR = 129,
      IXOR = 130,
      LXOR = 131,
      IINC = 132,
      I2L = 133,
      I2F = 134,
      I2D = 135,
      L2I = 136,
      L2F = 137,
      L2D = 138,
      F2I = 139,
      F2L = 140,
      F2D = 141,
      D2I = 142,
      D2L = 143,
      d2f = 144,
      i2b = 145,
      i2c = 146,
      i2s = 147,
      LCMP = 148,
      FCMLL = 149,
      FCMPG = 150,
      DCMPL = 151,
      DCMPG = 152,
      IFEQ = 153,
      IFNE = 154,
      IFLT = 155,
      IFGE = 156,
      IFGT = 157,
      IFLE = 158,
      IF_ICMPEQ = 159,
      if_icmpne = 160,
      if_icmplt = 161,
      if_icmpge = 162,
      if_icmpgt = 163,
      if_icmple = 164,
      if_acmpeq = 165,
      if_acmpne = 166,
      GOTO = 167,
      JSR = 168,
      ret = 169,
      tableswitch = 170,
      lookupswitch = 171,
      ireturn = 172,
      lreturn = 173,
      freturn = 174,
      dreturn = 175,
      areturn = 176,
      RETURN = 177,
      getstatic = 178,
      putstatic = 179,
      getfield = 180,
      putfield = 181,
      invokevirtual = 182,
      invokespecial = 183,
      invokestatic = 184,
      invokeinterface = 185,
      xxxunusedxxx = 186,
      NEW = 187,
      newarray = 188,
      anewarray = 189,
      arraylength = 190,
      athrow = 191,
      checkcast = 192,
      INSTANCEOF = 193,
      monitorenter = 194,
      monitorexit = 195,
      wide = 196,
      multianewarray = 197,
      ifnull = 198,
      ifnonnull = 199,
      goto_w = 200,
      jsr_w = 201,
      breakpoint = 202,
      ldc_quick = 203,
      ldc_w_quick = 204,
      ldc2_w_quick = 205,
      getfield_quick = 206,
      putfield_quick = 207,
      getfield2_quick = 208,
      putfield2_quick = 209,
      getstatic_quick = 210,
      putstatic_quick = 211,
      getstatic2_quick = 212,
      putstatic2_quick = 213,
      invokevirtual_quick = 214,
      invokenonvirtual_quick = 215,
      invokesuper_quick = 216,
      invokestatic_quick = 217,
      invokeinterface_quick = 218,
      invokevirtualobject_quick = 219,
      invokeignored_quick = 220,
      new_quick = 221,
      anewarray_quick = 222,
      multianewarray_quick = 223,
      checkcast_quick = 224,
      instanceof_quick = 225,
      invokevirtual_quick_w = 226,
      getfield_quick_w = 227,
      putfield_quick_w = 228,
      nonnull_quick = 229,
      first_unused_index = 230,
      software = 254,
      hardware = 255,
      dummy = 0xF0000000 ;

    public OpCode( int code, int req, int before, int after ) {
        this.code = ( byte ) code ; this.req = ( byte ) req; this.before = ( byte )before ;
        this.after = ( byte ) after ;
    }

    public abstract void executeOp( VMRuntime.StackFrame frame ) ;

    public class AALOAD extends OpCode {
       public AALOAD() { super( AALOAD, 0, 2, 1 ) ; }
       public void executeOp( VMRuntime.StackFrame frame ) {
           try {
                VMRuntime.ObjectEntry top = ( VMRuntime.ObjectEntry ) frame.popOpStackItem() ;
                VMRuntime.IntEntry index = ( VMRuntime.IntEntry ) frame.popOpStackItem() ;
                Class vclass = top.value.getClass() ;
                // vclass must be an array of non-primitive type!
                Object o = Array.get( top.value, index.value ) ;
                frame.pushOpStackItem( new VMRuntime.ObjectEntry( o ) ) ;

           }
           catch ( ClassCastException e ) {
                // Whoa, internal error.
           }
       }
    }

    public class INVOKEVIRTUAL extends OpCode {
        public INVOKEVIRTUAL() { super( invokevirtual, 2, 0, 0 ) ; }

        public void executeOp( VMRuntime.StackFrame frame ) {
            // Get index into constant pool
            int index = ( ( int ) frame.method.code.code[ frame.pc++] ) << 8
                        + frame.method.code.code[frame.pc++] ;
            ClassObject classObject = VMRuntime.getClassObjectForStubClass( frame.extern.getClass() );
            RefConstantInfo info = ( RefConstantInfo ) classObject.resolveConstant( index ) ;

            // This is a reference to method
        }

    }

    public byte code, req, before, after ;
}