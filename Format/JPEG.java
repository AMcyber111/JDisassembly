package Format;

import swingIO.*;
import swingIO.tree.*;
import javax.swing.tree.*;

public class JPEG extends Window.Window implements JDEventListener
{
  private java.util.LinkedList<Descriptor> des = new java.util.LinkedList<Descriptor>();
  private int ref = 0;

  private JDNode root;
  private Descriptor markerData;

  private long EOI = 0;

  private static final String pad = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

  //If the image data has bean scanned yet.

  private boolean isScanned = false;
  private boolean readQMat = false;

  //Decoding of huffman table expansion.

  private static java.util.LinkedList<String> huffExpansion = new java.util.LinkedList<String>();
  private static int HuffTable = 0, HuffTables = 0;
  
  private static final int[] bits = new int[]
  {
    0x80000000,0xC0000000,0xE0000000,0xF0000000,0xF8000000,0xFC000000,0xFE000000,0xFF000000,
    0xFF800000,0xFFC00000,0xFFE00000,0xFFF00000,0xFFF80000,0xFFFC0000,0xFFFE0000,0xFFFF0000
  };
  
  private static int[][] HuffmanCodes = new int[4][];

  //Sub sampling is number of 8 by 8 matrixes that are used for each color component.
  //This allows us to forum bigger matrixes like 16x16 using four 8 by 8.

  private int subSamplingY = 0;
  private int subSamplingCr = 0;
  private int subSamplingCb = 0;

  //A simple map for Y in zigzag matrix when showing the DCT matrix.
  //We can easily compute Y, and X fast, but it is a bit faster using a matrix.

  private static final int[] zigzag = new int[]
  {
    0, //Mid point +2.
    0,1,2,1,0, //mid point +2.
    0,1,2,3,4,3,2,1,0, //Mid point +2.
    0,1,2,3,4,5,6,5,4,3,2,1,0, //Mid point +2.
    0,1,2,3,4,5,6,7, //Mid point transition.
    7,6,5,4,3,2,1,2,3,4,5,6,7, //Mid point 1.
    7,6,5,4,3,4,5,6,7, //Mid point +2.
    7,6,5,6,7, //Mid point +2.
    7 //Mid point +2.
  };

  //The quantization matrixes are multiplied by the values for each color component.
  //The bigger the values are in the quotation matrix, then the more values are divided into 0 when rounded off.

  private static int[][] QMat = new int[2][];

  //Image data format. This is determined by the start of frame marker number.

  private int imageType = -1;

  //Picture dimensions.

  private int width = 0, height = 0;
  
  public JPEG() throws java.io.IOException
  {
    //Setup.

    tree.setEventListener( this ); file.Events = false; EOI = file.length();

    ((DefaultTreeModel)tree.getModel()).setRoot(null); tree.setRootVisible(true); tree.setShowsRootHandles(true); root = new JDNode( fc.getFileName(), -1 );
    
    //Set -1 incase invalid JPEG with no start of image marker.

    JDNode h = new JDNode("JPEG Data", -1);

    //Read the jpeg markers. All markers start with a 0xFF = -1 code.

    int MCode = 0, size = 0, type = 0;

    //Read image data in 4k buffer.

    long pos = 0, markerPos = 0; int buf = 0; byte[] b = new byte[4096]; file.read(b);

    //Read all the markers and define the data of known marker types.

    while( ( pos + buf ) < EOI )
    {
      if( buf > 4094 )
      {
        pos = buf + pos; buf = 0;
        
        if( buf != 4096 ) { file.seek(pos); }
        
        file.read(b);
      }

      MCode = b[buf]; type = b[buf + 1] & 0xFF;

      //Check if it is a valid maker.

      if( MCode == -1 && (type > 0x01 && type != 0xFF) )
      {
        if( pos + buf != markerPos )
        {
          JDNode t = new JDNode("Image Data", new long[]{ -3, markerPos, ( pos + buf ) - 1 } ); t.add( new JDNode( pad ) ); h.add( t );
        }

        markerPos = pos + buf; file.seek( markerPos ); //Seek the actual position.

        markerData = new Descriptor(file); des.add(markerData); markerData.setEvent( this::MInfo );
  
        markerData.UINT8("Maker Code"); markerData.UINT8("Marker type");

        //Markers between 0xD0 to 0xD9 have no size.

        if( type >= 0xD0 && type <= 0xD9 )
        {
          //Restart marker

          if( ( type & 0xF8 ) == 0xD0 )
          {
            h.add( new JDNode("Restart #" + ( type & 0x0F ) + ".h", ref++) );
          }

          //Set Start of image as read.

          else if( type == 0xD8 )
          {
            h = new JDNode("JPEG Data", ref++); root.add( h );
          }

          //End of image

          else if( type == 0xD9 ) { h.add( new JDNode("End of Image.h", ref++) ); break; }

          markerPos += 2; buf += 2;
        }

        //Decode maker data types.

        else
        {
          markerData.UINT16("Maker size"); size = ( ((short)markerData.value) & 0xFFFF ) - 2;

          //Decode the marker if it is a known type.

          if( !decodeMarker( type, size, h ) ) { markerData.Other("Maker Data", size); }

          markerPos += size + 4; buf += size + 4; file.skipBytes(size);
        }
      } else { buf += type != 0xFF ? 2 : 1; }
    }

    //Setup headers.
    
    ((DefaultTreeModel)tree.getModel()).setRoot(root); file.Events = true;

    //Set the first node.

    tree.setSelectionPath( new TreePath( h.getPath() ) ); open( new JDEvent( this, "", 0 ) );
  }
  
  //Decode all markers of a particular type. Some parts of the image can not be read without the markers being read.

  private void openMarkers( int[] m )
  {
    TreePath currentPath = new TreePath( ((JDNode)tree.getLastSelectedPathComponent()).getPath() );
    
    JDNode node, nodes = (JDNode)root.getFirstChild(); long[] a;

    for( int i1 = nodes.getChildCount() - 1; i1 > 0; i1-- )
    {
      for( int i2 = 0; i2 < m.length; i2++ )
      {
        a = ( node = (JDNode)nodes.getChildAt(i1)).getArgs();

        if( m[i2] == ( a.length > 3 ? a[3] : -1 ) )
        {
          tree.setSelectionPath( new TreePath( node.getPath() ) ); open( new JDEvent(this, "", a ) );
        }
      }
    }

    tree.setSelectionPath( currentPath );
  }

  //All of this moves into sub functions when user clicks on a maker.
  //We can also open required markers when needed by type using the "openMarkers" method.

  private boolean decodeMarker( int type, int size, JDNode marker ) throws java.io.IOException
  {
    JDNode n;

    if( ( type & 0xF0 ) == 0xC0 && !( type == 0xC4 || type == 0xC8 || type == 0xCC ) )
    {
      imageType = type - 0xC0;

      //Non-Deferential Huffman coded pictures.

      if( type == 0xC0 ) { n = new JDNode("Start Of Frame (baseline DCT)", new long[]{ ref++, file.getFilePointer(), size, 0 } ); }
      else if( type == 0xC1 ) { n = new JDNode("Start Of Frame (Extended Sequential DCT)", new long[]{ ref++, file.getFilePointer(), size, 0 }); }
      else if( type == 0xC2 ) { n = new JDNode("Start Of Frame (progressive DCT)", new long[]{ ref++, file.getFilePointer(), size, 0 } ); }
      else if( type == 0xC3 ) { n = new JDNode("Start Of Frame (Lossless Sequential)", new long[]{ ref++, file.getFilePointer(), size, 0 } ); }

      //Huffman Deferential codded pictures.
      
      else if( type == 0xC5 ) { n = new JDNode("Start Of Frame (Differential sequential DCT)", new long[]{ ref++, file.getFilePointer(), size, 0 } ); }
      else if( type == 0xC6 ) { n = new JDNode("Start Of Frame (Differential progressive DCT)", new long[]{ ref++, file.getFilePointer(), size, 0 } ); }
      else if( type == 0xC7 ) { n = new JDNode("Start Of Frame (Differential Lossless)", new long[]{ ref++, file.getFilePointer(), size, 0 } ); }

      //Non-Deferential Arithmetic codded pictures.

      else if( type == 0xC9 ) { n = new JDNode("Start Of Frame (Extended Sequential DCT)", new long[]{ ref++, file.getFilePointer(), size, 0 } ); }
      else if( type == 0xCA ) { n = new JDNode("Start Of Frame (Progressive DCT)", new long[]{ ref++, file.getFilePointer(), size, 0 } ); }
      else if( type == 0xCB ) { n = new JDNode("Start Of Frame (Lossless Sequential)", new long[]{ ref++, file.getFilePointer(), size, 0 } ); }

      //Deferential Arithmetic codded pictures.

      else if( type == 0xCD ) { n = new JDNode("Start Of Frame (Differential sequential DCT)", new long[]{ ref++, file.getFilePointer(), size, 0 } ); }
      else if( type == 0xCE ) { n = new JDNode("Start Of Frame (Differential progressive DCT)", new long[]{ ref++, file.getFilePointer(), size, 0 } ); }
      else { n = new JDNode("Start Of Frame (Differential Lossless)", new long[]{ ref++, file.getFilePointer(), size, 0 } ); }
    }
    else if( type == 0xC4 )
    {
      n = new JDNode("Huffman Table", new long[]{ ref++, file.getFilePointer(), size, 1 } );
    }
    else if( type == 0xDA )
    {
      n = new JDNode("Start Of Scan", new long[]{ ref++, file.getFilePointer(), size, 2 } );
    }
    else if( type == 0xDB )
    {
      n = new JDNode("Quantization Table", new long[]{ ref++, file.getFilePointer(), size, 3 } );
    }
    else if( type == 0xDD )
    {
      n = new JDNode("Restart", new long[]{ ref++, file.getFilePointer(), size, 4 } );
    }
    else if( ( type & 0xF0 ) == 0xE0 )
    {
      n = new JDNode("Application (info)", new long[]{ ref++, file.getFilePointer(), size, 5 } );
    }
    else if( type == 0xFE )
    {
      n = new JDNode("Comment", new long[]{ ref++, file.getFilePointer(), size, 6 } );
    }
    else { n = new JDNode("Maker.h", ref++ ); return( false ); }

    n.add( new JDNode(pad) ); marker.add( n ); return( true );
  }

  public void Uninitialize()
  {
    des.clear(); ref = 0;
    HuffmanCodes = new int[4][]; huffExpansion.clear(); HuffTables = 0;
  }

  public void open( JDEvent e )
  {
    if( e.getID().equals("UInit") ) { Uninitialize(); }

    else if( e.getArg(0) >= 0 )
    {
      tree.expandPath( tree.getLeadSelectionPath() );

      if( e.getArgs().length == 2 ) { HuffTable = (int)e.getArg(1); }

      ds.setDescriptor(des.get((int)e.getArg(0)));
    }

    //Highlight data.

    else if( e.getArg(0) == -2 )
    {
      JDNode n = ((JDNode)tree.getLastSelectedPathComponent());

      try { file.seek( e.getArg(1) ); } catch( Exception er ) { }

      if( n.toString().equals("Image Data") )
      {
        info("<html>" + imageData + "</html>");
      }
      else if( n.toString().startsWith("MCU") )
      {
        info("<html>This is one 8x8 square in the image. Each color can have a max of 64 values, or less.</html>");
      }

      tree.expandPath( tree.getLeadSelectionPath() );
      
      Offset.setSelected( e.getArg(1), e.getArg(2) ); ds.clear();
    }

    //Image data

    else if( e.getArg(0) == -3 )
    {
      JDNode n = ((JDNode)tree.getLastSelectedPathComponent());

      if( !isScanned && imageType == 0 ) { isScanned = true; openMarkers( new int[]{ 1, 0 } ); }

      n.setArgs( new long[]{ -2, n.getArg(1), n.getArg(2) } );

      try
      {
        scanImage( (int)e.getArg(1), (int)e.getArg(2) + 1 );
      }
      catch( Exception er ) { er.printStackTrace(); }

      info("<html>" + imageData + "</html>"); ds.clear();

      tree.expandPath( tree.getLeadSelectionPath() );
      
      Offset.setSelected( e.getArg(1), e.getArg(2) );
    }

    //Read an marker.

    if( e.getArgs().length > 3 )
    {
      JDNode root = (JDNode)tree.getLastSelectedPathComponent(), node = new JDNode("");
      
      if( root.getChildCount() > 0 ){ node = (JDNode)root.getFirstChild(); }

      DefaultTreeModel model = ((DefaultTreeModel)tree.getModel());

      int size = (int)e.getArg(2), type = (int)e.getArg(3);

      file.Events = false; 
      
      try
      {
        file.seek( e.getArg(1) );

        if( type == 0 )
        {
          Descriptor image = new Descriptor(file); des.add(image); image.setEvent( this::StartOfFrame );

          node.setUserObject("Image Information"); node.setArgs( new long[]{ ref++ } );
    
          image.UINT8("Sample Precision");
          image.UINT16("Picture Height"); width = ((short)image.value) & 0xFFFF;
          image.UINT16("Picture Width"); height = ((short)image.value) & 0xFFFF;
    
          image.UINT8("Number of Components in Picture"); int Nf = ((byte)image.value) & 0xFF;
    
          for( int i = 1; i <= Nf; i++ )
          {
            Descriptor imageComp = new Descriptor(file); des.add(imageComp); imageComp.setEvent( this::ComponentInfo );
            
            node.add( new JDNode("Image Component" + i + ".h", ref++) );
            
            imageComp.UINT8("Component Indemnifier");
            imageComp.UINT8("Vertical/Horizontal Sampling factor");

            if( i == 1 ) { subSamplingY = ((byte)imageComp.value); subSamplingY = ( subSamplingY & 0x0F ) * ( ( subSamplingY >> 4 ) & 0x0F ); }
            if( i == 2 ) { subSamplingCb = ((byte)imageComp.value); subSamplingCb = ( subSamplingCb & 0x0F ) * ( ( subSamplingCb >> 4 ) & 0x0F ); }
            if( i == 3 ) { subSamplingCr = ((byte)imageComp.value); subSamplingCr = ( subSamplingCr & 0x0F ) * ( ( subSamplingCr >> 4 ) & 0x0F ); }

            imageComp.UINT8("Quantization table Number");
          }

          ((DefaultTreeModel)tree.getModel()).nodeChanged( (JDNode)tree.getLastSelectedPathComponent() );
        }
        else if( type == 1 )
        {
          //Begin reading Huffman Tables.

          Descriptor nTable = new Descriptor(file); des.add(nTable); nTable.UINT8("Class/Table Number"); nTable.setEvent( this::HTableInfo );

          int classType = (((byte)nTable.value) & 0xF0) >> 4, num = (((byte)nTable.value) & 0x0F);

          node.setUserObject("Huffman Table #" + num + " (Class = " + ( classType > 0 ? "AC" : "DC" ) + ")"); node.setArgs( new long[]{ ref++ } );

          int Sum = 0, TableType = ( num << 1 ) + classType;

          while( size > 0 )
          {
            Descriptor Huff = new Descriptor(file); des.add(Huff); Huff.setEvent( this::HTableCodes );

            JDNode HRow = new JDNode("Huffman codes.h", ref++); node.add( HRow );

            java.util.LinkedList<Integer> codes = new java.util.LinkedList<Integer>();

            int[] bits = new int[16]; int bitPos = 0, code = 0;

            String bitDecode = "<table border=\"1\">";
            bitDecode += "<tr><td>Bits</td><td>Code</td><td>Length</td></tr>";

            for( int i = 1; i <= 16; i++ ) { Huff.UINT8("Bits " + i + ""); Sum += bits[ i - 1 ] = ((byte)Huff.value) & 0xFF; }

            Huff = new Descriptor(file); des.add(Huff); Huff.setEvent( this::HTableData );
            
            long[] arg = new long[2]; arg[0] = ref++; arg[1] = HuffTables++;

            HRow = new JDNode("Data.h", arg ); node.add( HRow );

            for( int i1 = 1; i1 <= 16; i1++ )
            {
              for( int i2 = 0; i2 < bits[ i1 - 1 ]; i2++ )
              {
                Huff.UINT8("Huffman Code " + i1 + " bits"); code = ((byte)Huff.value) & 0xFF;

                //Store the code in the integer with the higher 16 bits as the bit combination.
                //The lower 4 bits as the 0 to 16 bit length next 4 bits as the huffman code.

                codes.add( ( bitPos << ( 16 + ( 16 - i1 ) ) ) + ( code << 4 ) + ( i1 - 1 ) );

                bitDecode += "<tr><td>" + pad( Integer.toBinaryString(bitPos), i1 ) + "</td><td>" + pad( Integer.toHexString( code ), 2 ) + "</td><td>" + ( i1 + ( code & 0x0F ) ) + "</td></tr>";

                bitPos += 1;
              }

              bitPos <<= 1;
            }

            bitDecode += "</table>"; huffExpansion.add( bitDecode );

            HuffmanCodes[TableType] = codes.stream().mapToInt(Integer::intValue).toArray();

            //The tables can be grouped together under one marker.

            size -= 17 + Sum; Sum = 0; if( size > 0 )
            {
              nTable = new Descriptor(file); des.add(nTable); nTable.UINT8("Class/Table Number"); nTable.setEvent( this::HTableInfo );

              classType = (((byte)nTable.value) & 0xF0) >> 4; num = (((byte)nTable.value) & 0x0F); TableType = ( num << 1 ) + classType;

              node = new JDNode("Huffman Table #" + num + " (Class = " + ( classType > 0 ? "AC" : "DC" ) + ")", ref++);

              model.insertNodeInto(node, root, root.getChildCount());
            }
          }
        }
        else if( type == 2 )
        {
          node.setUserObject("Components.h"); node.setArgs( new long[]{ ref++ } );

          Descriptor Scan = new Descriptor(file); des.add(Scan);

          Scan.UINT8("Number of Components"); int Ns = ((byte)Scan.value) & 0xFF;

          for( int c = 1; c <= Ns; c++ )
          {
            Scan.Array("Component #" + c + "", 2);
            Scan.UINT8("Scan component");
            Scan.UINT8("Entropy Table");
          }
    
          Descriptor info = new Descriptor(file); des.add(info);
    
          model.insertNodeInto(new JDNode("Scan info.h", ref++), root, root.getChildCount());
    
          info.UINT8("Start of Spectral");
          info.UINT8("End of Spectral");
          info.UINT8("ah/al");
        }
        else if( type == 3 )
        {
          //Begin reading Quantization Tables.

          Descriptor nTable = new Descriptor(file); des.add(nTable); nTable.UINT8("Precision/Table Number"); nTable.setEvent( this::QTableInfo );

          int Precision = (((byte)nTable.value) & 0xF0) >> 4;

          node.setUserObject("Quantization Table #" + (((byte)nTable.value) & 0x0F) + " (" + ( Precision == 0 ? "8 Bit" : "16 bit" ) + ")"); node.setArgs( new long[]{ ref++ } );

          int eSize = Precision == 0 ? 65 : 129;

          while( size >= eSize )
          {
            for( int i = 1; i <= 8; i++ )
            {
              Descriptor QMat = new Descriptor(file); des.add(QMat);
    
              JDNode matRow = new JDNode("Row #" + i + ".h", ref++); node.add( matRow );
    
              if( Precision == 0 )
              {
                for( int i2 = 1; i2 <= 8; i2++ ) { QMat.UINT8("EL #" + i2 + ""); }
              }
              else
              {
                for( int i2 = 1; i2 <= 8; i2++ ) { QMat.UINT16("EL #" + i2 + ""); }
              }
            }

            //The tables can be grouped together under one marker.

            size -= eSize; if( size >= eSize )
            {
              nTable = new Descriptor(file); des.add(nTable); nTable.UINT8("Precision/Table Number"); nTable.setEvent( this::QTableInfo );

              Precision = (((byte)nTable.value) & 0xF0) >> 4; eSize = Precision == 0 ? 65 : 129;

              node = new JDNode("Quantization Table #" + (((byte)nTable.value) & 0x0F) + " (" + ( Precision == 0 ? "8 Bit" : "16 bit" ) + ")", ref++);

              model.insertNodeInto(node, root, root.getChildCount());
            }
          }
        }
        else if( type == 4 )
        {
          Descriptor r = new Descriptor(file); des.add(r); r.UINT16("Restart interval");

          node.setUserObject("Restart interval.h"); node.setArgs( new long[]{ ref++ } );
        }
        else if( type == 5 )
        {
          Descriptor m = new Descriptor(file); des.add(m);

          m.String8("Type", (byte)0x00); String Type = (String)m.value;

          node.setUserObject( Type + ".h" ); node.setArgs(new long[]{ ref++ });

          if( Type.equals("JFIF") )
          {
            m.setEvent( this::JFIFInfo );
            m.UINT8("Major version");
            m.UINT8("Minor version");
            m.UINT8("Density");
            m.UINT16("Horizontal pixel Density");
            m.UINT16("Vertical pixel Density");
            m.UINT8("Horizontal pixel count");
            m.UINT8("Vertical pixel count");

            if( size - 14 > 0 )
            {
              m.Other("Other Data", size - 14 );
            }
          }
          else { m.Other("Application Data", size - Type.length() - 1 ); m.setEvent( this::AppInfo ); }
        }
        else if( type == 6 )
        {
          Descriptor text = new Descriptor(file); des.add(text); text.String8("Comment", size);

          node.setUserObject("Text.h"); node.setArgs( new long[]{ ref++ } );
        }
        else if( type == 7 )
        {
          if( imageType != 0 ){ info("<html>The start of frame marker type value specifies the image data type.<br /><br />" +
          "Right now Arithmetic encoded JPEG image data is not supported. Only baseline image data is supported.</html>"); ds.clear(); return; }

          long t = file.getFilePointer();

          file.Events = false;

          String out = "<table border=\"1\">";
          out += "<tr><td colspan=\"2\">Huffman table.</td><td colspan=\"2\">RAW Binary Data</td><td rowspan=\"2\">Decoded Value</td></tr>";
          out += "<tr><td>Huff Table</td><td>Huff Code</td><td>Match</td><td>Binary Value</td></tr>";

          boolean match = false, EOB = false;

          int v = 0;
          
          int c = 0;
          
          int bit = 0, code = 0, len = 0, zrl = 0;
          
          int value = 0;

          int bitPos = (int)e.getArg(2) + 32, bytes = 0, bitLen = 0, mp = Integer.MAX_VALUE, mpx = 0;

          int loop = 0;

          int[] HuffTable = HuffmanCodes[ e.getArg(4) == 0 ? 0 : 2 ];

          //Each code has a length for the number of bits is the binary number value.

          int[] DCT = new int[64];

          while( !EOB && loop < 64 )
          {
            //Load in new bytes as needed.

            bytes = bitPos / 8; for( int i = 0; i < bytes; i++ )
            {
              bitPos -= 8; code = file.read();
              
              if( code > 0 )
              {
                v |= code << bitPos; if( code == 255 ){ if( ( file.read() & 0xFF ) == 0 ) { mp = ( bitLen + ( 24 - bitPos ) ) & -8; mpx += 8; } }
              }
            }

            //There is only one DC per 8x8 block. The rest are AC.

            c = 0; while( !match && c < HuffTable.length )
            {
              code = HuffTable[c++]; bit = code & 0xF; if( ( v & bits[bit] ) == ( code & 0xFFFF0000 ) ){ len = ( code >>> 4 ) & 0x0F; zrl = ( code >>> 8 ) & 0x0F; match = true; }
            }

            if( loop == 0 ) { HuffTable = HuffmanCodes[ e.getArg(4) == 0 ? 1 : 3 ]; if( HuffTable == null ){ EOB = true;}  }

            if( match )
            {
              out += "<tr><td>Table #" + e.getArg(4) + " Class " + ( loop == 0 ? "DC" : "AC" ) + "</td><td>" + String.format( "%02X", ( code >>> 4 ) & 0xFF ) + "</td>";

              out += "<td>" + pad( Integer.toBinaryString( code >>> ( 16 + ( 15 - bit ) ) ), bit + 1 ) + "</td>"; v <<= bit + 1;

              bitPos += bit + 1; bitLen += bit + len + 1;

              if( ( len + zrl ) > 0 )
              {
                if( len > 0 )
                {
                  value = ( v & bits[len - 1] ) >>> ( 32 - len );

                  out += "<td>" + pad( Integer.toBinaryString( value ), len );

                  value = value < ( 1 << ( len - 1 ) ) ? value - ( ( 1 << len ) - 1 ) : value;
                  
                  out += "</td><td>" + value + "</td>"; v <<= len; bitPos += len;
                }
              }
              else { out += "<td>EOB</td><td>0</td>"; EOB = loop > 0; }

              out += "</tr>";
            }

            if( bitLen >= mp ) { mp = Integer.MAX_VALUE; bitLen += mpx; mpx = 0; }

            loop += zrl + 1; DCT[ loop - 1 ] = value; value = 0; match = false;
          }

          out += "</table>";

          //The binary and hex string.

          if( ( bitLen & 7 ) != 0 ) { bitLen += 8; }

          //Show the raw binary data.

          file.seek(t); file.read( bitLen >> 3 );

          String bin = ""; for( int i = 0; i < ( bitLen >> 3 ); i++ ) { bin += pad( Integer.toBinaryString( ((int)file.toByte(i)) & 0xFF ), 8 ) + " "; }

          //Decode the DCT matrix.

          String[] row = new String[]{"<tr>","<tr>","<tr>","<tr>","<tr>","<tr>","<tr>","<tr>"}; for( int i = 0; i < zigzag.length; i++ ) { row[zigzag[i]] += "<td>" + DCT[i] + "</td>"; }

          String DCTm = "<table border=\"1\"><tr><td colspan=\"8\">8x8 DCT matrix</td></tr>" + row[0] + "</tr>" + row[1] + "</tr>"+ row[2] + "</tr>"+ row[3] + "</tr>" + row[4] + "</tr>"+ row[5] + "</tr>"+ row[6] + "</tr>"+ row[7] + "</tr></table>"; row = null;

          ds.clear(); info( "<html>The RAW binary data = " + bin + "<br /><br />" + 
          ( e.getArg(2) > 0 ? "The previous 8x8 ended at binary digit " + e.getArg(2) + ", so decoding starts at binary digit " + e.getArg(2) + ".<br /><br />" : "" ) +
          "The binary data is split apart by matching one Huffman combination, and reading a variable in length binary number value.<br /><br />" +
          "The RAW binary data is split apart in 2 columns. The first 2 columns show what the matching Huffman code is for the binary combination. The last column shows what the binary value decodes as.<br /><br />" +
          "Table DC is used for the first value, then the rest use table AC.<br /><br />" +
          "Each huffman code is split into tow values. The first 0 to 15 hex digit tells us how many zero values are used before our color value.<br /><br />" +
          "The Last 0 to 15 hex digit tells us how many binary digits to read for the color value. This allows us to define all 64 values in 8x8 using as little data as possible.<br /><br />" +
          "If we match a Huffman code that is 00 meanings no value. This means there is no more color values for the 8x8, so we terminate early with EOB.<br /><br />" +
          out + "<br /><br />Each of the decoded values are placed into a matrix in a zigzag pattern.<br /><br />" + DCTm + "</html>" );

          out = null; DCTm = null;

          Offset.setSelected( t, t + ( bitLen >> 3 ) - 1 );
          
          file.Events = true; return;
        }

        root.setArgs( new long[]{ root.getArg(0) } );
      }
      catch( Exception er ) { er.printStackTrace(); }

      file.Events = true;
    }
  }

  //Do A fast optimized full scan of the image data defining each DCT start and end position.
  //When user clicks on a DCT, then the algorithm is run with detailed output for just the one DCT at any position in image.

  private void scanImage( int Start, int End ) throws java.io.IOException
  {
    JDNode node = (JDNode)tree.getLastSelectedPathComponent(); node.removeAllChildren();

    if( imageType != 0 ) { node.add( new JDNode("Unsupported.h", new long[]{-1,0,0,7}) ); return; }

    file.Events = false; file.seek( Start ); file.read( End - Start );

    int size = End - Start, bitSize = ( size << 3 ) - 7, v = 0;

    boolean match = false, EOB = false;
    
    int c = 0;
    
    int bit = 0, code = 0, len = 0, zrl = 0;
    
    int bitPos = 32, bytes = 0, BPos = 0, pos = 0;

    int loop = 0;

    int mp = Integer.MAX_VALUE, mpx = 0;

    int TableNum = 0; int[] HuffTable = HuffmanCodes[TableNum];

    //Color components. Note this should vary based on the start of scan markers.

    int nComps = 2, comp = 0, smp = 0;
    int[] samples = new int[]{ subSamplingY, subSamplingCb, subSamplingCr };
    String[] Colors = new String[]{ " (Y)", " (Cb)", " (Cr)" };

    //Define the first MCU.

    JDNode MCU = new JDNode( pad, new long[]{ 0, 0 } );

    //Each code has a length for the number of bits is the binary number value.

    for( int mcu = 0; pos < bitSize; smp++ )
    {
      if( smp >= samples[comp] ){ smp = 0; comp = comp == nComps ? 0 : comp + 1; }

      //Add DCT matrix node with number, and position.

      TableNum = comp == 0 ? 0 : 2; HuffTable = HuffmanCodes[TableNum];

      if( smp == 0 && comp == 0 )
      {
        MCU.setArgs( new long[]{ -2, MCU.getArg(1), ( Start + ( pos >> 3 ) ) } );
        MCU = new JDNode("MCU #" + mcu + "", new long[]{ -2, ( Start + ( pos >> 3 ) ), 0 } );
        node.add( MCU ); mcu += 1;
      }

      MCU.add( new JDNode("DCT #" + smp + Colors[comp] + ".h", new long[]{ -1, Start + ( pos >> 3 ), pos & 7, 7, TableNum == 0 ? 0 : 1 } ) );

      loop = 0; EOB = false; while( !EOB && loop < 64 )
      {
        //Load in new bytes as needed into integer.

        bytes = bitPos / 8; for( int i = 0; i < bytes; i++ )
        {
          bitPos -= 8; if( BPos < size )
          {
            code = file.toByte( BPos ) & 0xFF; v |= code << bitPos;
                
            //The code 0xFF is used for markers. In some cases we need to use 0xFF as value.
            //If the next byte after 0xFF is 0x00, then 0xFF is used as a value, and 0x00 is skipped.
                            
            if( code == 255 ) { mp = BPos << 3; mpx += 8; BPos += 1; }
          }
                
          BPos += 1;
        }

        //There is only one DC per 8x8 block. The rest are AC.
        
        c = 0; while( !match && c < HuffTable.length )
        {
          code = HuffTable[c++]; bit = code & 0xF; if( ( v & bits[bit] ) == ( code & 0xFFFF0000 ) ){ bit += 1; len = ( code >>> 4 ) & 0x0F; zrl = ( code >>> 8 ) & 0x0F; match = true; }
        }

        if( match ) { bitPos += bit; v <<= bit; if( ( len + zrl ) > 0 ) { v <<= len; bitPos += len; } else { EOB = loop > 0; } }

        if( loop == 0 ) { HuffTable = HuffmanCodes[ TableNum + 1 ]; if( HuffTable == null ){ EOB = true; } }

        pos += bit + len; if( pos > mp ){ mp = Integer.MAX_VALUE; pos += mpx; mpx = 0; }
        
        loop += zrl + 1; match = false;
      }
    }

    file.Events = true;
  }

  //Pad string with 0.

  private String pad( String s, int size ) { for( int i = s.length(); i < size; i++, s = "0" + s ); return( s ); }

  private static String markerTypes = "<table border='1'>" +
  "<tr><td>Type</td><td>Format</td><td>Marker Defines.</td></tr>" +
  "<tr><td>192</td><td>Start of Frame</td><td>Image data is Baseline DCT.</td></tr>" +
  "<tr><td>193</td><td>Start of Frame</td><td>Image data is Extended Sequential DCT.</td></tr>" +
  "<tr><td>194</td><td>Start of Frame</td><td>Image data is Progressive DCT.</td></tr>" +
  "<tr><td>195</td><td>Start of Frame</td><td>Image data is Lossless (sequential).</td></tr>" +
  "<tr><td>196</td><td>Define Huffman Table</td><td></td></tr>" +
  "<tr><td>197</td><td>Start of Frame</td><td>Image data is Differential sequential DCT.</td></tr>" +
  "<tr><td>198</td><td>Start of Frame</td><td>Image data is Differential progressive DCT.</td></tr>" +
  "<tr><td>199</td><td>Start of Frame</td><td>Image data is Differential lossless (sequential).</td></tr>" +
  "<tr><td>201</td><td>Start of Frame</td><td>Image data is Extended sequential DCT, Arithmetic coding.</td></tr>" +
  "<tr><td>202</td><td>Start of Frame</td><td>Image data is Progressive DCT, Arithmetic coding.</td></tr>" +
  "<tr><td>203</td><td>Start of Frame</td><td>Image data is Lossless (sequential), Arithmetic coding.</td></tr>" +
  "<tr><td>204</td><td>Define Arithmetic Coding</td><td></td></tr>" +
  "<tr><td>205</td><td>Start of Frame</td><td>Image data is Differential sequential DCT, Arithmetic coding.</td></tr>" +
  "<tr><td>206</td><td>Start of Frame</td><td>Image data is Differential progressive DCT, Arithmetic coding.</td></tr>" +
  "<tr><td>207</td><td>Start of Frame</td><td>Image data is Differential lossless (sequential), Arithmetic coding.</td></tr>" +
  "<tr><td>208 to 215</td><td>Restart Marker</td><td>Restart Markers 0 to 7.</td></tr>" +
  "<tr><td>216</td><td>Start of Image</td><td>Image must start with this marker.</td></tr>" +
  "<tr><td>217</td><td>End of Image</td><td>Image must end with this marker.</td></tr>" +
  "<tr><td>218</td><td>Start of Scan</td><td></td></tr>" +
  "<tr><td>219</td><td>Define Quantization Table</td><td></td></tr>" +
  "<tr><td>220</td><td>Define Number of Lines</td><td>(Not common)</td></tr>" +
  "<tr><td>221</td><td>Define Restart Interval</td><td></td></tr>" +
  "<tr><td>222</td><td>Define Hierarchical Progression</td><td>(Not common)</td></tr>" +
  "<tr><td>223</td><td>Expand Reference Component</td><td>(Not common)</td></tr>" +
  "<tr><td>224 to 239</td><td>App Marker</td><td>Picture Application info only 0 to 15.</td></tr>" +
  "<tr><td>254</td><td></td><td>Textural based comment.</td></tr>" +
  "</table>";

  public static String markerRule = "Every marker must start with 255, and must not have a maker type with 255, 0, or 1.<br /><br />" +
  "Marker codes do not have to start one after another. Invalid makers are skipped as padding is allowed.";

  public static final String imageData = "The huffman tables are used to decode the image data.<br /><br />" +
  "<table border=\"1\">" +
  "<tr><td>Table</td><td>Usage</td></tr>" +
  "<tr><td>Huffman table #0 (Class = DC)</td><td>Used for DC component of Luminance (Y).</td></tr>" +
  "<tr><td>Huffman table #1 (Class = DC)</td><td>Used for DC component of Chrominance (Cb & Cr).</td></tr>" +
  "<tr><td>Huffman table #0 (Class = AC)</td><td>Used for AC component of Luminance (Y).</td></tr>" +
  "<tr><td>Huffman table #1 (Class = AC)</td><td>Used for AC component of Chrominance (Cb & Cr).</td></tr>" +
  "</table><br />" +
  "JPEG pictures use variable length numbers for each Y, Cb, Cr color in the image data.<br /><br />" +
  "Each color uses one DC value that is added to the 8 by 8 quantized matrix. The AC huffman table is for individual values in the 8 by 8 matrix.<br /><br />" +
  "If we chose not to filter out anything, then we would have 1 DC following 63 AC values for each 8x8.<br /><br />" +
  "A huffman table can say binary digits 01 uses the next 4 binary digits as a number.<br /><br />" +
  "And also that binary digits 11 uses the next 13 in length binary number.<br /><br />" +
  "A huffman table cen specify 111 for a 0 in length number value. The 0 in length codes are used to set a end point for AC values.<br /><br />" +
  "This way we use as little data as possible for image color data. An optimized huffman table is the most used bit combinations as 1 to 2 bit combinations.";

  public static final String[] markers = new String[]
  {
    "<html>This Must always be 255.</html>",
    "<html>" + markerRule + "<br /><br />" +
    "Marker types 208 to 223 do not have a marker size number after marker type.<br /><br /><hr /><br />" +
    "All JPEG pictures start with a start of image marker type = 216. The maker does not contain a size after it as it is in the maker range 208 to 223.<br /><br />" +
    "Lastly \"Start of frame\" defines the picture width and height. There is a lot of \"start of frame\" makers, but they are all read in the same format.<br /><br />" +
    "Because of the \"Start of frame\" marker we have to define a marker format column, and an extended description of what the maker implies the image data is by type.<br /><br />" +
    markerTypes + "</html>",
    "<html>This is the size of the marker. The two bytes that are read for the size are included as part of the marker size.<br /><br />Markers types 208 to 223 do not have a size.</html>",
    "<html>Unknown marker data. This happens when a unknown maker type is used.</html>",
  };

  public static final String[] AppInfo = new String[]
  {
    "<html>Each application marker has a 00 byte terminated text string. This is earthier the application name, or URL.<br /><br />" +
    "The application markers define information specific to the application used to create, or make the JPEG.<br /><br />" +
    "The information defined in these markers are not necessary to draw the picture.</html>",
    "<html>This is the application specific data.</html>"
  };

  public static final String[] JFIFInfo = new String[]
  {
    "<html>Each application marker has a 00 byte terminated text string. This is the JFIF header.</html>",
    "<html>This is the major version.<br /><br />" +
    "If major version is 7, and minor version is 2 we when up with version number 7.2v.</html>",
    "<html>This is the major version.<br /><br />" +
    "If major version is 7, and minor version is 2 we when up with version number 7.2v.</html>",
    "<html>Image density.</html>",
    "<html>Horizontal pixel Density.</html>",
    "<html>Vertical pixel Density.</html>",
    "<html>Horizontal pixel count.</html>",
    "<html>Vertical pixel count.</html>",
    "<html>Extended JFIF picture information.</html>"
  };

  public static final String[] StartOfFrame = new String[]
  {
    "<html>This should always be 8 by 8 sample size.</html>",
    "<html>Picture Height in pixels.</html>",
    "<html>Picture Width in pixels.</html>",
    "<html>Number of component's. Usually 3, for Y, Cb, Cr.<br /><br />" +
    "Each component uses an 8 by 8 quantization table. You can change the table number each component uses if you like." +
    "You can also modify the 8 by 8 quantization matrix an component number uses if you like.</html>"
  };

  public static final String[] DefineComponents = new String[]
  {
    "<html>This is the assigned component number. Each \"start of scan\" marker uses the component number.<br /><br />" +
    "You can switch component numbers if you like. You can set component 1 to 2, and component 2 to 1.</html>",
    "<html>The first hex digit is Vertical 0 to 15, and the last hex digit is 0 to 15 horizontal.</html>",
    "<html>This is the quantization matrix that will be used with this component number.<br /><br />" +
    "You can modify the quantization matrix this component uses, or set it to a different one defined in this image.</html>"
  };

  public void MInfo( int el )
  {
    if( el < 0 ) { info("<html>" + markerRule + "</html>"); }
    else
    {
      info( markers[el] );
    }
  }

  public void JFIFInfo( int el )
  {
    if( el < 0 ) { info("<html>Picture application only information.</html>"); }
    else
    {
      info( JFIFInfo[el] );
    }
  }

  public void AppInfo( int el )
  {
    if( el < 0 ) { info("<html>Picture application only information.</html>"); }
    else
    {
      info( AppInfo[el] );
    }
  }

  public void StartOfFrame( int el )
  {
    if( el < 0 )
    {
      info("The start of frame defines the width and height of the JPEG picture.<br /><br />" +
      "The frame also usually specifies 3 image components, Y, Cb, and Cr.<br /><br />" +
      "The image components specify a quantization table number to use. An 8 by 8 matrix is shaded and blended together with three image components using the image data and quantization matrix in 8 by 8 pixel squares.<br /><br />" +
      "This allows JPEG pictures to be much smaller in size, but can only approximate the color in each 8x8.");
    }
    else
    {
      info( StartOfFrame[el] );
    }
  }

  public void ComponentInfo( int el )
  {
    if( el < 0 )
    {
      info("Defines the image components which are used by each start of scan maker preceding the image data.");
    }
    else
    {
      info( DefineComponents[el] );
    }
  }

  public void QTableInfo( int el )
  {
    if( el < 0 )
    {
      info("<html>The quantitation tables are used with the start of frame marker which defines the color components that will be used with the quantization table number.<br /><br />" +
      "JPEG pictures are compressed into luminous data that present 8 by 8 tiles of the image. Image color data is approximated using the quantitation matrix.</html>");
    }
    else
    {
      info("<html>The first 0 to F digit is the 0 to 15 precision. The last digit 0 to F is 0 to 15 table number.<br /><br />" +
      "If precision is 1 then each point is 16 bits instead of 8 bits.</html>");
    }
  }

  public void HTableInfo( int el )
  {
    if( el < 0 )
    {
      info("<html>" + imageData + "</html>");
    }
    else
    {
      info("<html>The first 0 to F digit is the 0 to 15 is the Class type 1 = AC, and 0 = DC. The last digit 0 to F is 0 to 15 table number.</html>");
    }
  }

  public void HTableCodes( int el )
  {
    if( el < 0 )
    {
      info("<html>A Huffman table specifies the number of codes that use a set bit combination length 1 to 16.<br /><br />" +
      "Say bit length 3 has 3 codes. Then we count from 000 binary going 000 = ?, 001 = ?, 010 = ?.<br /><br />" +
      "We add one more time to the 3-bit combination before moving to the next bit combination 010 + 1 = 011.<br /><br />" +
      "Now say bit length 5 has 2 values. We then make our current three-bit combination into 5 by moving to the left 2 times, making 011 into 011 00.<br /><br />" +
      "The next 2 codes are then 01100 = ?, 01101 = ? as we continue the counting sequence.<br /><br />" +
      "After this last 5 bit combination we then must not forget to add +1 before moving to the next combination length.<br /><br />" +
      "The question marks are filled in with the bytes that are read after the 16 bytes.<br /><br />" +
      "The counting sequence can also be graphed out as a binary tree using 0 to 1 nodes. Which for some makes it easier to map the code combinations.</html>");
    }
    else
    {
      info("<html>Number of codes existent under bit combination.</html>");
    }
  }

  public void HTableData( int el )
  {
    if( el < 0 )
    {
      info("<html>To get a general understanding of Huffman binary tree expansion, see the \"Huffman codes\" section.<br /><br />" +
      "The bit combinations are the codes that appear in the image data. After the code is the color value.<br /><br />" +
      "A Huffman code that is 73 would mean the next 7 color values are zero [0,0,0,0,0,0,0,?] then the 8th value is our number value in 8x8.<br /><br />" +
      "The last hex digit is 3 meaning the next 3 binary digits is the color value.<br /><br />" +
      "Some JPEG programs do not optimize the Huffman table to compact as much data as possible.<br /><br />" +
      "Some programs use already made Huffman tables which it pick combinations following the color value giving reasonable compression.<br /><br />" +
      "This is because optimized Huffman tables can sometimes take a while to generate.<br /><br />" +
      "It also requires you to set optimizations when creating the JPEG.<br /><br />" +
      huffExpansion.get( HuffTable ) + "</html>");
    }
    else
    {
      info("<html>Each byte is the Huffman code for a bit combination. The code is broken into tow 4 bit numbers.<br /><br />" +
      "The first 0 to 15 hex digit is the number of 0 values in the 8x8 matrix before the value.<br /><br />" +
      "The Last 0 to 15 hex digit is the length for the number of binary digits to read as the number value.<br /><br />" +
      "A Huffman code that is 73 would mean the next 7 color values are [0,0,0,0,0,0,0,?] then the 8th value is our number value in 8x8.<br /><br />" +
      "The last hex digit is 3 meaning the next 3 binary digits us used for our value.</html>");
    }
  }
}
