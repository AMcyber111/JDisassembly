package Format.ELFDecode;

public class Data extends Window.Window
{
  //An error occurred while read the ELF binary file.
  
  public static boolean error = false;

  //Weather the byte felids are little or big endian.

  public static boolean isLittle = true;

  //File is 64 bits.

  public static boolean is64Bit = false;

  //The start of the program.

  public static long start = 0;

  //Program information.

  public static long programHeader = 0;
  public static int elPrSize = 0;
  public static int prSize = 0;

  //The sections location.

  public static long Sections = 0;
  public static int elSecSize = 0;
  public static int secSize = 0;
  public static int namesEl = 0;

  //The machine core type.

  public static int coreType = 0;

  //Check if core type is loaded.

  public static boolean coreLoaded = false;

  //Sections can be separated by the data they contain.

  public static swingIO.tree.JDNode code = new swingIO.tree.JDNode("Code Sections"); //Sections that are executable.
  public static swingIO.tree.JDNode data = new swingIO.tree.JDNode("Data Sections"); //Sections that are straight data.
  public static swingIO.tree.JDNode lnk = new swingIO.tree.JDNode("Link libraries Sections"); //Linked libraries method sections.
}