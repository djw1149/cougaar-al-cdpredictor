package com.axiom.lib.util;
import java.util.*;
import java.io.*;
import java.text.*;

/** Multidimensional matrix of float primitives.  Eventually will implement
 *  <code>NumberArray</code> interface.
 */
public class FloatMatrix implements Serializable, Cloneable, NumberArray  {
  protected static final long serialVersionUID = 1L;

  public FloatMatrix( int row, int column ) {
      int[] tmp = { row, column };
      this.dimensions = tmp ;
      multFactor = new int[dimensions.length];
      size = 1;
      for (int i=0;i<dimensions.length;i++) {
          multFactor[i] = size ;
          size *= dimensions[i];
      }
      array = new float[size];
  }

  /** Constructor taking array of dimensions.
   *
   *  @param dimensions  The dimensions of the multidim. array.
   */
  public FloatMatrix( int[] dimensions ) {
     this.dimensions = ( int[] ) dimensions.clone();
     multFactor = new int[dimensions.length];
     size = 1;
     for (int i=0;i<dimensions.length;i++) {
       multFactor[i] = size ;
       size *= dimensions[i];
     }
     array = new float[size];
  }

  public int getRank() {
     return dimensions.length ;
  }

  public int[] getDimensions() {
     return dimensions ;
  }

  public int getSize() {
     return size ;
  }

  public Class getType() { return Float.TYPE ; }

  /** Implements NumberArray.valueAt().
   * @param Linear array index.
   */
  public double valueAt( int i ) {
     return array[ i ];
  }

  /** Implements NumberArray.intAt().
   *  @param Linear array index.
   */
  public int intAt( int i ) {
     return ( int ) array[ i ] ;
  }

  public float floatAt( int i ) {
     return array[i] ;
  }

  public Object clone() {
     FloatMatrix matrix = new FloatMatrix( ( int[] ) dimensions.clone() ) ;
     System.arraycopy( array, 0, matrix.array, 0, array.length ) ;
     return matrix ;
  }

  public int linearIndex( int[] index ) throws MatrixException {
     int ia ;
     if ( index[0] < dimensions[0] )
       ia = index[0];
     else
       throw new MatrixException();

     for (int i=1;i<index.length;i++ )
     {
       if ( index[i] < dimensions[i] )
         ia += index[i] * multFactor[i] ;
       else
         throw new MatrixException();
     }
     return ia ;
  }
  
    public int getNumRows() {
        return dimensions[0];
    }
    
    public int getNumColumns() {
        if ( dimensions.length < 2 ) {
            return 0 ;
        }
        return dimensions[1] ;
    }

    public FloatMatrix get( int[] lower, int[] upper ) {
        try {
        if ( lower.length != getRank() || upper.length != getRank() ) {
            throw new MatrixException( "Range arrays do not match rank=" + getRank() ) ;
        }
        }
        catch ( NullPointerException e ) {
            throw new IllegalArgumentException( "Range arrays must be non-null." ) ;
        }

        int[] size = new int[ lower.length ] ;
        int last = 0 ;
        for (int i=0;i<lower.length;i++) {
            if ( lower[i] < 0 ) {
                throw new MatrixException( "Index lower[" + i + "] must be >=0: " + lower[i] ) ;
            }
            if ( upper[i] >= dimensions[i] ) {
                throw new MatrixException( "Index upper[" + i + "] must be >=0: " + upper[i] ) ;
            }
            size[i] = upper[i] - lower[i] + 1 ;
            if ( size[i] < 0 ) {
                size[i] = 0 ;
            }
            if( size[i] > 1 ) { // Find the last dimension >= 1, e.g. a [5,1,1,1] matrix reduces to [5]
                last = i ;
            }
        }
        if ( last == 0 ) {
            last = 1 ;
        }

        int[] newSize = new int[ last + 1] ;
        for (int i=0;i<last+1;i++) {
            newSize[i] = size[i] ;
        }

        FloatMatrix result = new FloatMatrix( newSize ) ;
        float[] farr = result.getArray() ;
        FloatMatrixEnumeration enm = new FloatMatrixEnumeration( this, lower, upper ) ;
        for ( int i=0; enm.hasMoreElements() ; ) {
            float value = enm.nextElement() ;
            farr[i++] = value ;
        }
        return result ;
    }
  
    /**
     *  Convenience method for two-dimensional matrices.
     *  @return value at row, column
     */
    public float at( int row, int column ) {
        int ia ;
        
        if ( row < dimensions[0] && row >= 0 && dimensions.length >= 2) {
            ia = row ;
        }
        else
            throw new MatrixException( makeIndexExceptionString( row, column ) ) ;

        if ( column < dimensions[1] && column >= 0 )
            ia += column * multFactor[1];
        else
            throw new MatrixException( makeIndexExceptionString( row, column ) ) ;
        return array[ia] ;
    }

    /** Set element at linear index to value.
     *  @param index Linear index.
     *  @param value Value to set.
     */
    public void setAt( int index, float value ) {
        array[index] = value ;
    }

    /**
     *  Convenience method for two-dimensional matrices.
     */    
    public void set( int row, int column, float value ) {
        int ia ;
        
        if ( row < dimensions[0] && row >= 0  && dimensions.length >= 2) {
            ia = row ;
        }
        else
            throw new MatrixException( makeIndexExceptionString( row, column ) ) ;

        if ( column < dimensions[1] && column >= 0 ) 
            ia += column * multFactor[1];
        else
            throw new MatrixException( makeIndexExceptionString( row, column ) ) ;
        array[ia] = value ;
    }

    private final static String _INDEX = "Matrix index " ;
    private final static String _IS_OUT_OF_RANGE = " is out of range." ;

    private final static String makeIndexExceptionString( int r, int c ) {
        return _INDEX + r + " " + c + _IS_OUT_OF_RANGE ;
    }

    private String makeIndexExceptionString( int[] index ) {
        return _INDEX + ArrayMath.toString( index ) + _IS_OUT_OF_RANGE ;
    }

  /** Returns element at <code>index</code>.
   */
  public float at( int[] index ) throws MatrixException {
     int ia ;
     int ind = index[0] ;
     if ( ind < dimensions[0] && ind >= 0 )
       ia = index[0];
     else
       throw new MatrixException( makeIndexExceptionString( index ) );

     for (int i=1;i<index.length;i++ )
     {
       ind = index[i] ;
       if ( ind < dimensions[i] && ind >= 0 )
         ia += ind * multFactor[i] ;
       else
         throw new MatrixException( makeIndexExceptionString( index ) );
     }
     return array[ia] ;
  }

  public void set( int index, double value ) {
     array[index] = ( float ) value ;
  }

 /** Returns element at index.
  */

  public void set( int[] index, float value ) throws MatrixException {
     int ia ;
     int ind = index[0] ;
     if ( ind < dimensions[0] && ind >= 0 )
       ia = index[0];
     else
       throw new MatrixException( makeIndexExceptionString( index ) );

     for (int i=1;i<index.length;i++ )
     {
       ind = index[i] ;
       if ( ind < dimensions[i] && ind >= 0 )
         ia += ind * multFactor[i] ;
       else
         throw new MatrixException( makeIndexExceptionString( index ) );
     }
     array[ia] = value;
  }

  public float max() {
    float result = array[0];
    for (int i=1;i<array.length;i++)
      if ( array[i] > result )
        result = array[i];
    return result ;
  }

  public float min() {
    float result = array[0];
    for (int i=1;i<array.length;i++)
      if ( array[i] < result )
        result = array[i];
    return result ;
  }

  /**
   *  Returns the underlying array.
   */
  public float[] getArray() {
      return array ;
  }
  
  
    /**
     *  Flips a 2D matrix left-right in place.  It assumes the convention that the 
     *  first (0) index corresponds to row and the second index (1)
     *  corresponds to column.
     */
    public void flipLR() {
        int numRows = getNumRows(), numColumns = getNumColumns() ;
        if ( numColumns <= 1 ) {
            return ;
        }
        float tmp ;
        
        for (int i=0;i<numRows;i++) {
            for (int j=0;j< numColumns/2 ;j++) {
                tmp = at( i, j ) ;
                set( i, j, at( i, numColumns - j - 1) ) ;
                set( i, numColumns - j -1, tmp ) ; 
            }
        }
       
    }

    /**
     *  Flips a 2D matrix up down in place.  It assumes the convention that the 
     *  first (0) index corresponds to row and the second index (1)
     *  corresponds to column.
     */    
    public void flipUD() {
        int numRows = getNumRows(), numColumns = getNumColumns() ;
        if ( numRows <= 1 ) {
            return ;
        }
        float tmp ;
        
        for (int i=0;i<numRows/2;i++) {
            for (int j=0;j< numColumns;j++) {
                tmp = at( i, j ) ;
                set( i, j, at( numRows - i - 1, j ) ) ;
                set( numRows - i - 1, j, tmp ) ; 
            }
        }        
    }

  /**
   *  Initializes each element in the matrix to be equal to the sum of its indices.
   */
  public void seqdiag() {
    /*
    try {
    for ( FloatMatrixEnumeration e=this.getMatrixElements(); e.hasMoreElements(); ) {
      int value = IntArray.sum( e.getIndex() );
      set( e.getIndex(), value );
      e.nextElement();
    }
    }
    catch ( FloatMatrixException e ) {
      e.printStackTrace();
    }
    */
     int[] index = new int[dimensions.length];

     array[0]= ArrayMath.sum( index );
     for (int aindex=1;aindex<array.length;aindex++) {
       for (int i=0;i<dimensions.length;i++) {
         if ( index[i] < ( dimensions[i] - 1) ) {
           index[i]++;
           break ;
         }
         else
           index[i]=0;
       }
       array[aindex] = ArrayMath.sum( index );
     }

  }

  /** Overrides toString().  Prints in two-dimensional matrix format.
   */

    public String toString() {
        StringBuffer result = new StringBuffer();

        for ( FloatMatrixEnumeration e=this.getMatrixElements(); e.hasMoreElements(); ) {
            float element = e.at();
            result.append( element ).append( " " ) ;

            if ( e.getIndex().length >= 2 ) {
                if ( e.getIndex()[0] == dimensions[0] - 1 ) {
                    result.append( '\n' );
                    if ( e.getIndex()[1] == dimensions[1] - 1 ) {
                        result.append( '\n' );
                    }
                }
            }
            e.nextElement();
        }
        return result.toString();
    }

  /** Reads an entire FloatMatrix from a stream tokenizer.
   *
   *  @return number of elements read.
   */

  public int read( StreamTokenizer s ) {
    Number number ;
    int i=0;

    try {
      for (i=0;i<array.length;i++) {
        if ( s.nextToken() != StreamTokenizer.TT_EOF )
        {
           if ( s.ttype == StreamTokenizer.TT_NUMBER ) {
             array[i] = ( float ) s.nval ;
           }
        }
        else
          return i ;
      }
    }
    catch ( Exception e ) {
      e.printStackTrace();
      return i ;
    }

    return array.length ;
  }

  public void zero() {
    for (int i=0;i<array.length;i++)
      array[i] = (float) 0.0 ;
  }

  /** Write this array, starting with lowest indices.
   */

  public void write( PrintWriter ps ) throws IOException {
    for (int i=0;i<array.length;i++) {
      ps.print( array[i] );
      ps.print( ' ' );
    }
  }

  public FloatMatrixEnumeration getMatrixElements() {
    return new FloatMatrixEnumeration( this );
  }

  public FloatMatrixEnumeration getMatrixElements( int[] lower, int[] upper ) {
    return new FloatMatrixEnumeration( this, lower, upper );
  }

  public static void main( String argv[] ) {

    try {
      int[] dimensions = {4,4,4} ;
      FloatMatrix testMatrix = new FloatMatrix( dimensions );

      int[] dimensions2 = {4,4,4,4};
      FloatMatrix testMatrix2 = new FloatMatrix( dimensions2 ) ;

      testMatrix.seqdiag();

      System.out.println( testMatrix.toString() );

      int[] lower = { 1, 2, 1 };
      int[] upper = { 1, 3, 3 };
      System.out.println( "Enumerating elements from " + ArrayMath.toString( lower ) + " to " +
                          ArrayMath.toString( upper ) ) ;
      for ( FloatMatrixEnumeration e= testMatrix.getMatrixElements(lower, upper);
          e.hasMoreElements(); ) {
        int[] index = e.getIndex() ;
        System.out.println( ArrayMath.toString( index ) );
        System.out.println( e.nextElement() );
        System.out.println( testMatrix.at(index) ) ;
      }

      System.out.println( "\nTesting FloatMatrixEnumeration " ) ;
      FloatMatrixEnumeration e2 = new FloatMatrixEnumeration( testMatrix, lower, upper ) ;
      for ( ; e2.hasMoreElements(); ) {
        System.out.println( ArrayMath.toString( e2.getIndex() ) );
        System.out.println( e2.nextElement() );
      }

      System.out.println( "\nTesting testMatrix.get()" ) ;
      FloatMatrix t1 = testMatrix.get( new int[] { 0, 2, 2}, new int[] { 3,2,2} ) ;
      System.out.println( t1 );

        FloatMatrixEnumeration e3 = new FloatMatrixEnumeration( testMatrix2 ) ;
        for ( ; e3.hasMoreElements(); ) {
            int[] index = e3.getIndex() ;
            e3.set( ArrayMath.sum( index ) ) ;
            e3.nextElement() ;
        }

        System.out.println( "Testing matrix enumeration: " );
        System.out.println( testMatrix2 ) ;

      /** Test exception handling
      e2 = new FloatMatrixEnumeration2( testMatrix, lower, upper ) ;
      while ( e2.hasMoreElements() ) {
        System.out.println( ArrayMath.toString( e2.getIndex() ) );
        System.out.println( e2.nextElement() );
      }

      e2.nextElement() ;
      */
      System.out.println("Testing set and get operations:" );
      
        int[] dims = { 3, 4 };
        FloatMatrix testMatrix3 = new FloatMatrix( dims ) ;
        for (int i=0;i<testMatrix3.getNumRows();i++) {
            for (int j=0;j<testMatrix3.getNumColumns();j++) {
                testMatrix3.set( i, j, ( float ) Math.random() ) ;
            }
        }
        System.out.println("\n") ;
        System.out.println( testMatrix3 ) ;

        FloatMatrix testMatrix4 = testMatrix3.get( new int[] { 0, 1 }, new int[] { 2, 3 } ) ;
        System.out.println( "testMatrix4.get( 0:2,1:2 )" ) ;
        System.out.println( testMatrix4 ) ;

        testMatrix3.flipLR() ;
        System.out.println( "Flip LR:" );
        System.out.println( testMatrix3 ) ;

        testMatrix3.flipUD() ;
        System.out.println( "Flip UD:" );
        System.out.println( testMatrix3 ) ;

      // Testing out of bounds exception

      try {
         int[] oindex = { 4, 2, 1 } ;
         float moose = testMatrix.at( oindex ) ;
      }
      catch ( MatrixException e ) {
         System.out.println( e ) ;
         e.printStackTrace() ;
      }

      try {
         int[] oindex = { -1, 2, 1 } ;
         float moose = testMatrix.at( oindex ) ;
      }
      catch ( MatrixException e ) {
         System.out.println( e ) ;
         e.printStackTrace() ;
      }

      // Print to a file and read it back in

      System.out.println( "Testing input/output..." ) ;

      File file = new File( "test.dat" );
      FileOutputStream fs = new FileOutputStream( file );
      PrintWriter p = new PrintWriter( fs );

      testMatrix.write( p );
      p.flush();
      fs.close();

      FileInputStream is = new FileInputStream( file );
      StreamTokenizer s = new StreamTokenizer( is );
      testMatrix.zero();
      testMatrix.read( s );

      System.out.println( testMatrix.toString() );      
    }
    catch ( Exception e ) {
      e.printStackTrace();
    }


  }

   int size = 0 ;
   protected int[] dimensions ;
   protected int[] multFactor ;
   protected float[] array ;
   protected static NumberFormat format = new DecimalFormat();
}