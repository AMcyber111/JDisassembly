package Format.RIFFDecode;

import swingIO.Descriptor;

public class Data extends Window.Window
{
  //It is not known how many sections a RIFF file has.

  public static java.util.LinkedList<Descriptor> des = new java.util.LinkedList<Descriptor>();

  //Initial nodes that should be expanded.

  public static java.util.LinkedList<javax.swing.tree.TreePath> initPaths = new java.util.LinkedList<javax.swing.tree.TreePath>();

  //This is used to keep track of the current descriptor.

  public static int ref = 0;
}
