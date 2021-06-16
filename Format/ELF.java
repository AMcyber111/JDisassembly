package Format;

import swingIO.*;
import swingIO.tree.*;
import javax.swing.tree.*;
import Format.ELFDecode.*;

import core.x86.*;

public class ELF extends Data implements JDEventListener
{
  //Descriptors.

  private Descriptor[][] des = new Descriptor[2][];

  private JDNode root;

  //ELF reader plugins.

  private static Headers header = new Headers();

  JDNode Headers = new JDNode("Headers", 0);

  private static final sec[] Reader = new sec[] { null, null, null, null }; //Section type readers.
  public static final String[] SInfo = new String[]
  {
    //ELF headers.
    "<html>An ELF application Has three headers.<br /><br />" +
    "The ELF header defines the CPU type. The start address of the program after all the headers are read.<br /><br />" +
    "The ELF header defines the location to the \"Program Header\", and \"Section header\".<br /><br />" +
    "The \"program header\" defines the link libraries, and section that must be loaded, or run before calling the start address of the program.<br /><br />" +
    "The \"Section header\" gives every section of the program a name. It defines the rest of the program such as debugging information if any.<br /><br />" +
    "After the \"program header\" sections are executed, and loaded, and all \"section\" placed in memory. Then the programs start address is called.</html>",
    //Code Sections.
    "<html>Note that the program header entires are run before jumping the CPU to the start address of the program.<br /><br />" +
    "The \".init\" section is usually run by the \"program header\" before the \"section header\" maps it as a named section called \".init\".<br /><br />" +
    "The \".text\" section is usually the set program start address defined in the ELF header. Which is run after all headers are read.<br /><br />" +
    "The \".fini\" section is the termination code that is called to exit the program.<br /><br />" +
    "We do not have to call it a \".init\" section. As sections that have runnable processor instructions are defined by flag setting.</html>",
    //Link libraries.
    "<html>Note that the program header entires are run before jumping the CPU to the start address of the program.<br /><br />" +
    "The \".dynamic\" section is usually run by the \"program header\" before the \"section header\" maps it as \".dynamic\".<br /><br />" +
    "Also take note that the section types are identified by type setting, so it could have any name you like if you wanted.</html>",
    //Relocation.
    "<html>Relocation are only used if the ELF sections can not be palaced at set Virtual address locations.</html>",
    //Debug information.
    "<html>Line number information relative to the emitted machine code, and may also contain variable names.</html>",
    //Thread local storage.
    "<html></html>",
    //Sections defined as data only.
    "<html>Some sections are marked as data only. Such sections may be file data, or sections used by external tools.</html>"
  };

  //Read the ELF binary.

  public ELF()
  {
    tree.setEventListener( this );

    file.Events = false;

    //Root node is now the target file.
 
    ((DefaultTreeModel)tree.getModel()).setRoot(null); tree.setRootVisible(true); tree.setShowsRootHandles(true); root = new JDNode( fc.getFileName() + ( fc.getFileName().indexOf(".") > 0 ? "" : ".elf" ) );

    //The ELF header.
  
    JDNode ELFHeader = new JDNode( "ELF Header.h", new long[]{ 0, 0 } );
    JDNode PHeader = new JDNode( "Program Header", new long[]{ 0, 1 } );
    JDNode SECHeader = new JDNode( "Section Header", new long[]{ 1, 0 } );

    code.removeAllChildren();
    for( int i = 0; i < sections.length; i++ ) { if ( sections[i].getChildCount() > 0 ) { sections[i].removeAllChildren(); } }
    data.removeAllChildren();
    
    des[0] = new Descriptor[3];

    try
    {
      des[0][0] = header.readELF(); Headers.add(ELFHeader); 
      if( !Data.error && Data.programHeader != 0 ) { des[0][1] = header.readProgram(PHeader); Headers.add(PHeader); }
      if( !Data.error && Data.Sections != 0 ) { des[1] = header.readSections(SECHeader); Headers.add(SECHeader); }
    }
    catch(Exception e) { Data.error = true; }

    root.add(Headers);

    if( code.getChildCount() > 0 ) { root.add( code ); }
    for( int i = 0; i < sections.length; i++ ) { if ( sections[i].getChildCount() > 0 ) { root.add( sections[i] ); } }
    if( data.getChildCount() > 0 ) { root.add( data ); }

    if( !Data.error )
    {
      //Load processor core type.

      if( coreType == 0x0003 || coreType == 0x003E )
      {
        if( core == null || core.type() != 0 ){ core = new X86( file ); } else { core.setTarget( file ); }

        core.setBit( is64Bit ? X86.x86_64 : X86.x86_32 );
              
        core.setEvent( this::Dis ); coreLoaded = true;
      }
      else { coreLoaded = false; }

      //Machine code start pos.

      root.add( new JDNode( "Program Start (Machine code).h", new long[]{ -1, start } ) );

      //Decode the setup headers.
    
      ((DefaultTreeModel)tree.getModel()).setRoot(root); file.Events = true;

      //Set the default node.

      tree.setSelectionPath( new TreePath( Headers.getPath() ) ); open( new JDEvent( this, "", new long[]{ 0, 0 } ) );
    }
    else{ file.Events = true; }
  }

  public void open( JDEvent e )
  {
    if( e.getArgs().length == 1 ) { info(SInfo[(int)e.getArg(0)]); ds.clear(); }

    else if( e.getArg(0) < 0 )
    {
      if( e.getArg(0) == -1 )
      {
        if( coreLoaded )
        {
          core.locations.clear(); core.data_off.clear(); core.code.clear();

          core.locations.add( e.getArg(1) );

          core.disLoc(0); ds.setDescriptor( core );
        }
        else { try{ file.seekV( e.getArg(1) ); Virtual.setSelected( e.getArg(1), e.getArg(1) ); } catch(Exception er) { } noCore(); }
      }
      else if( e.getArg(0) == -2 )
      {
        try
        {
          file.seekV( e.getArg(2) ); file.seek( e.getArg(1) );
          Offset.setSelected( e.getArg(1), e.getArg(1) + e.getArg(3) - 1 );
          Virtual.setSelected( e.getArg(2), e.getArg(2) + e.getArg(3) - 1 );
          info("<html></html>"); ds.clear();
        }
        catch( Exception er ) { } 
      }
    }
    else if( e.getArgs().length > 1 )
    {
      if( e.getArg(0) >= 2 )
      {
        if( Reader[ (int)e.getArg(0) - 2 ] == null )
        {
          info("<html>There is currently no reader for this section yet.</html>"); ds.clear();
        }
        else
        {
          try{ des[ (int)e.getArg(0) ] = Reader[ (int)e.getArg(0) - 2 ].read( sections[ (int)e.getArg(0) - 2 ] ); } catch( Exception er ) { }
        }

        try
        {
          file.seekV( e.getArg(1) ); Virtual.setSelected( e.getArg(1), e.getArg(1) + e.getArg(2) - 1 );
          Offset.setSelected( file.getFilePointer(), file.getFilePointer() + e.getArg(2) - 1 );
        } catch( Exception er ) { } 

        ds.clear();
      }

      //Section descriptors. Only exist after section is read.

      else { ds.setDescriptor( des[ (int)e.getArg(0) ][ (int)e.getArg(1) ] ); }
    }
    else
    {
      ds.clear();
    }
  }

  public void noCore() { info("<html>The processor core engine is not supported.</html>"); }
}
