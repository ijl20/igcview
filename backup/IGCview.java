// Main class: IGCview
// Project type: console
// Arguments: 
// Compile command: javac -deprecation

/** (*) IGCview.java
 *  Soaring log file analysis
 *  @author Ian Lewis
 *  @version December 1997
 */

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

//***************************************************************************//
//             IGCview                                                       //
//***************************************************************************//

public class IGCview extends Applet {

  static private String appletInfo = 
    "IGC Soaring log file analysis program\nAuthor: Ian Lewis\nDecember 1997";

  static private String usageInfo = "Usage: java IGCfile";

  static TrackLog [] logs = new TrackLog[100];   // loaded track logs
 
  static int log_count = 0;                      // count of logs loaded

  static TP_db tps;                              // tp database

  static Task task;                              // current task

  public void init () {
    try {
      this.add (new MapFrame(600,400));
    }
    catch (NumberFormatException e) {
      this.add (new TextField (appletInfo));
    };
  };

  public String getAppletInfo () {
    return appletInfo;
  };

  public void paint (Graphics g) {
    g.drawString ("Use the other frame!", 20, 30);
  };

  public static void main (String [] args) 
    {
      try { MapFrame map = new MapFrame(700,500);
            map.show();
          }
      catch (NumberFormatException e) { System.out.println (usageInfo); }
    }
}

//***************************************************************************//
//             MapFrame                                                      //
//***************************************************************************//

class MapFrame extends Frame {

  public float xscale = 200; // pixels per degree
  public float yscale = 400;
  public float origin_lat = 53;
  public float origin_long = -1;

  DrawingCanvas canvas;

  private static String [] [] menus = {
    {"File", "Load Track", "Save", null, "Quit"},
    {"Task", "Load TPs", "Define Task"},
    {"Colour", "Black", "Blue", "Red", "Green"}
  };    

  private int width, height;

  MapFrame (int width, int height)
    {
      this.setTitle("IGCview");
      this.width = width;
      this.height = height;

      ScrollPane pane = new ScrollPane ();
      this.add (pane, "Center");
      canvas = new DrawingCanvas (this, 2 * width, 2 * height);
      pane.add (canvas);

      MenuBar menubar = new MenuBar ();
      this.setMenuBar (menubar);

      for (int c = 0; c < menus.length; c++) {
        Menu m = new Menu (menus [c][0]);
        menubar.add (m);
        for (int r = 1; r < menus [c] .length; r++) {
  	  if (menus [c][r] == null)
              m.addSeparator ();
	  else
            {
	      MenuItem i = new MenuItem (menus [c][r]);
	      m.add (i);
	      i.setActionCommand (menus [c][r] .toLowerCase ());
	      i.addActionListener (canvas);
  	    }
        }
      }
      this.pack ();
    }

  public Dimension getPreferredSize () {
    return new Dimension (width, height);
  };

};

//***************************************************************************//
//            DrawingCanvas                                                  //
//***************************************************************************//

class DrawingCanvas extends Canvas implements ActionListener,
                                              MouseListener,
					      MouseMotionListener
{
  private MapFrame map_frame;
  private int width, height;

  Color color = Color.white;
  int rect_x = 0, rect_y =0;      // x,y of mouse press
  int rect_w = 0, rect_h = 0;     // dimensions of current rectangle
  int rect_w1 = 0, rect_h1 = 0;   // dimensions of old rectangle
 
  DrawingCanvas (MapFrame frame, int width, int height)
    {
      map_frame = frame;
      this.width = width;
      this.height = height;

      this.addMouseListener (this);
      this.addMouseMotionListener (this);
    }

  public void mousePressed (MouseEvent e)
    {
      rect_x = e.getX();
      rect_y = e.getY();
    }

  public void mouseReleased (MouseEvent e)
    { 
      map_frame.origin_long = x_to_long(rect_x);
      map_frame.origin_lat = y_to_lat(rect_y);
      float x_adj = ((float) map_frame.getSize().width / (float) rect_w);
      float y_adj = ((float) map_frame.getSize().height / (float) rect_h);
      if (x_adj < y_adj) 
        y_adj = x_adj;
      else
        x_adj = y_adj;
      map_frame.xscale = x_adj * map_frame.xscale;
      map_frame.yscale = y_adj * map_frame.yscale;
      rect_w = 0;
      rect_h = 0;
      rect_w1 = 0;
      rect_h1 = 0;
      paint(getGraphics());
    }

  public void mouseDragged (MouseEvent e)
    {
      rect_w = e.getX() - rect_x;
      rect_h = e.getY() - rect_y;
      repaint();
    }
 
  public void mouseClicked(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {};
  public void mouseExited(MouseEvent e) {};
  public void mouseMoved(MouseEvent e) {};

  public Dimension getPreferredSize () 
    {
      return new Dimension (width, height);
    }

  public void update (Graphics g)
    {
      g.setXORMode(color);
      if (rect_w > 0) 
        {
          g.drawRect(rect_x, rect_y, rect_w1, rect_h1);  // remove old rectangle
          g.drawRect(rect_x, rect_y, rect_w, rect_h);    // draw new rectangle
          rect_w1 = rect_w;
          rect_h1 = rect_h;  // save dimensions of drawn rectangle
        }
    }

  public void paint (Graphics g)
    { Dimension d = getSize();
      g.setColor(Color.green);
      g.fillRect(0,0, d.width - 1, d.height - 1);
      if (IGCview.task != null) IGCview.task.draw(g);
      g.setColor(Color.black);
      for (int i = 1; i <= IGCview.log_count; i++)
        {
          IGCview.logs[i].draw(g);
        }
    }

  public void actionPerformed (ActionEvent e)
    {
      String s = e.getActionCommand ();
      if (s.equals ("new")) 
        {
          this.repaint ();
        }
      else if (s.equals ("load tps")) load_tps();
      else if (s.equals ("define task")) get_task();
      else if (s.equals ("load track")) load_track ();
      else if (s.equals ("save")) save ();
      else if (s.equals ("quit")) System.exit (0);
      else if (s.equals ("black")) color = Color.black;
      else if (s.equals ("blue")) color = Color.blue;
      else if (s.equals ("red")) color = Color.red;
      else if (s.equals ("green")) color = Color.green;
    }

  private void load_tps()
   {
     FileDialog fd = new FileDialog (map_frame, "Load TP database", FileDialog.LOAD);
     fd.show ();
     String filename = fd.getFile ();
     if (filename != null)
       {
         try { IGCview.tps = new TP_db(filename);
               System.out.println("Loaded " + IGCview.tps.tp_count + " TPs");
             } catch (Exception e) {System.out.println (e);}
       }
   }

  private void load_track()
   {
     FileDialog fd = new FileDialog (map_frame, "Load Track Log", FileDialog.LOAD);
     fd.show ();
     String filename = fd.getFile ();
     if (filename != null) {
       try { IGCview.log_count += 1;
             IGCview.logs[IGCview.log_count] = new TrackLog(filename, map_frame);
             paint(getGraphics());
           } catch (Exception e) {System.out.println (e);}
     }
   }

  private void get_task()
    {
      TaskWindow window = new TaskWindow(map_frame);
      window.setTitle("Define Task");
      window.pack();
      window.show();
    }

  private void save () {
    FileDialog fd = new FileDialog (map_frame, "Save drawing", FileDialog.SAVE);
    fd.show ();
    String f = fd.getFile ();
    if (f != null) {
      try {
	FileOutputStream s = new FileOutputStream (f);
	ObjectOutputStream o = new ObjectOutputStream (s);
	// o.writeObject (drawing);
	o.flush ();
	o.close ();
      }
      catch (Exception e) {System.out.println (e);};
    }
  }

  public float x_to_long(int x)
    {
      return (float) x / map_frame.xscale + map_frame.origin_long;
    } 

  public float y_to_lat(int y)
    {
      return map_frame.origin_lat - ((float) y / map_frame.yscale);
    }

}

//***************************************************************************//
//            TrackLog                                                       //
//***************************************************************************//

class TrackLog
{
  int   [] time      = new int[5000];
  float [] altitude  = new float [5000];          // Time of day in seconds, alt in feet
  float [] latitude  = new float [5000]; 
  float [] longitude = new float [5000];   // West/South = negative, North/East = positive
  int      record_count = 0;               // number of records in log
  MapFrame map_frame;                      // MapFrame creating this TrackLog
  
  TrackLog (String filename, MapFrame f)
  {
    map_frame = f;
    try { BufferedReader in = new BufferedReader(new FileReader(filename));
          String buf;

          while ((buf = in.readLine()) != null) 
          { if ((buf.charAt(0) == 'B') && (buf.charAt(35) == 'V'))
            { record_count += 1;
              longitude[record_count] = decimal_latlong(buf.substring(15,18),
                                                        buf.substring(18,23),
                                                        buf.charAt(23));
              latitude[record_count] = decimal_latlong(buf.substring(7,9),
                                                       buf.substring(9,14),
                                                       buf.charAt(14));
              time[record_count] = Integer.valueOf(buf.substring(1,3)).intValue()*3600 +
                                   Integer.valueOf(buf.substring(3,5)).intValue()*60 + 
                                   Integer.valueOf(buf.substring(5,7)).intValue();
              altitude[record_count] = Float.valueOf(buf.substring(25,30)).floatValue() *
                                         (float) 3.2808 ;
	    }
          }
          in.close();
        } catch (FileNotFoundException e) {
            System.err.println("TrackLog: " + e);
          } catch (IOException e) {
              System.err.println("TrackLog: " + e);
            }
  }

  private float decimal_latlong(String degs, String mins, char EWNS)
  {
    float deg = Float.valueOf(degs).floatValue();
    float min = Float.valueOf(mins).floatValue();
    if ((EWNS == 'W') || (EWNS == 'S')) 
      return -(deg + (min / (float) 60000));
    else 
      return (deg + (min / (float) 60000));
  }

  public void draw(Graphics g)
  {
    int x1,x2,y1,y2;

    x1 = long_to_x(1);
    y1 = lat_to_y(1);
    for (int i = 2; i <= record_count; i++)
    {
        x2 = long_to_x(i);
        y2 = lat_to_y(i);
        g.drawLine(x1,y1,x2,y2);
        x1 = x2;
        y1 = y2;
    }
  }

  private int long_to_x(int index)
  {
    return (int) ((longitude[index] - map_frame.origin_long) * map_frame.xscale);
  }

  private int lat_to_y(int index)
  {
    return (int) ((map_frame.origin_lat - latitude[index]) * map_frame.yscale);
  }

}

//***************************************************************************//
//            TP_db                                                          //
//***************************************************************************//

class TP_db
{
  int tp_count = 0;
  TP [] tp = new TP [5000];

  TP_db(String filename)
    {
     try { BufferedReader in = new BufferedReader(new FileReader(filename));
          String buf;
          while ((buf = in.readLine()) != null) 
            if (buf.charAt(0) == 'W') tp[++tp_count] = new TP(buf);
          in.close();
          } catch (FileNotFoundException e) {
              System.err.println("TrackLog: " + e);
            } catch (IOException e) {
                System.err.println("TrackLog: " + e);
              }
     
    } 

  int lookup(String key)
  { int i;
    for (i=1; i<=tp_count; i++) if (tp[i].trigraph.equals(key)) return i;
    return -1;
  }

}

//***************************************************************************//
//            TP                                                             //
//***************************************************************************//

class TP
{
  String trigraph, full_name;
  float latitude, longitude;

  TP(String gdn_rec)
    {
      trigraph = gdn_rec.substring(3,6).toUpperCase();
      full_name = gdn_rec.substring(60,80);
      latitude = decimal_latlong(gdn_rec.substring(11,13),
                                 gdn_rec.substring(14,21),
                                 gdn_rec.charAt(10));
      longitude = decimal_latlong(gdn_rec.substring(23,26),
                                  gdn_rec.substring(27,34),
                                  gdn_rec.charAt(22));
    }

  TP() {;}

  private float decimal_latlong(String degs, String mins, char EWNS)
    {
      float deg = Float.valueOf(degs).floatValue();
      float min = Float.valueOf(mins).floatValue();
      if ((EWNS == 'W') || (EWNS == 'S')) 
        return -(deg + (min / (float) 60));
      else 
        return (deg + (min / (float) 60));
    }

}


//***************************************************************************//
//            TaskWindow                                                     //
//***************************************************************************//

class TaskWindow extends Frame implements ItemListener, 
                                          ActionListener,
                                          FocusListener
{
  int MAXTASK = 6;                       // max number of TPs in task
  TextField [] tp_trigraph = new TextField [7];     // array 0..MAXTASK 
  Label [] tp_label = new Label [7];     // array 0..MAXTASK 
  int [] tp_index = {0,0,0,0,0,0,0};     // size is MAXTASK
  List tp_list;
  Button ok_button, cancel_button;
  int current_trigraph = 1;              // text field with focus
  MapFrame map_frame;                    // frame to draw on

  Button make_button(String name, GridBagLayout gridbag, GridBagConstraints c) 
    {
      Button b = new Button(name);
      gridbag.setConstraints(b, c);
      b.addActionListener(this);
      add(b);
      return b;
    }

  void make_label(String text, GridBagLayout gridbag, GridBagConstraints c) 
    {
      Label l = new Label();
      l.setText(text);
      gridbag.setConstraints(l, c);
      add(l);
    }

  Label make_tp_label(GridBagLayout gridbag, GridBagConstraints c) 
    {
      Label l = new Label();
      gridbag.setConstraints(l, c);
      add(l);
      return l;
    }

  TextField make_entry(GridBagLayout gridbag, GridBagConstraints c) 
    {
      TextField t = new TextField(3);
      gridbag.setConstraints(t, c);
      t.addActionListener(this);
      t.addFocusListener(this);
      add(t);
      return t;
    }

  TaskWindow(MapFrame map_frame)
    {
      tp_list = new List();
      this.map_frame = map_frame;

      for (int i=1;i <= IGCview.tps.tp_count; i++)
        {
          tp_list.addItem(IGCview.tps.tp[i].trigraph + " " + IGCview.tps.tp[i].full_name);
        }
      tp_list.addItemListener(this);

      GridBagLayout gridbag = new GridBagLayout();
      GridBagConstraints c = new GridBagConstraints();
 
      setFont(new Font("Helvetica", Font.PLAIN, 14));
      setLayout(gridbag);
   
      c.fill = GridBagConstraints.BOTH;
      c.gridx = 1;
      make_label("Trigraph:", gridbag, c);            //-- LABEL: Trigraphs
      c.gridx = GridBagConstraints.RELATIVE;
      c.weightx = 1.0;
      make_label("Full name:", gridbag, c);           //-- LABEL: Full name
      c.gridwidth = GridBagConstraints.REMAINDER;
      make_label("TP list:", gridbag, c);             //-- LABEL: TP list
      c.weightx = 0.0;
      c.gridwidth = 1;
      make_label("1",gridbag,c);                      //-- LABEL: tp number
      tp_trigraph[1] = make_entry(gridbag, c);        //-- TEXT:  trigraph
      c.weightx = 1.0;
      tp_label[1] = make_tp_label(gridbag,c);         //-- LABEL: full name
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.gridheight = GridBagConstraints.RELATIVE;
      gridbag.setConstraints(tp_list, c);
      add(tp_list);                                   //-- LIST: tp list
      c.gridheight = 1;
      c.gridwidth = 1;
      for (int i=2; i<=MAXTASK; i++)
        { c.gridx = 0;
          c.weightx = 0.0;
     	  make_label(String.valueOf(i),gridbag,c);    //-- LABEL: tp number
          c.gridx = 1;
          tp_trigraph[i] = make_entry(gridbag, c);    //-- TEXT:  trigraph
          c.gridx = 2;
          c.weightx = 1.0;
          tp_label[i] = make_tp_label(gridbag,c);     //-- LABEL: full name
        }
      c.gridx = 1;
      c.weightx = 0.0;
      c.insets = new Insets(3,3,3,3);
      cancel_button = make_button("Cancel", gridbag,c);  //-- BUTTON: Cancel
      c.gridx = 2;
      ok_button = make_button("OK", gridbag,c);          //-- BUTTON: OK
    }

  // Handle SELECT events from TP listbox

  public void itemStateChanged(ItemEvent e)
    {
     if ((List) e.getItemSelectable() == tp_list)
       {
         if (e.getStateChange() == ItemEvent.SELECTED)
           {
             int i = tp_list.getSelectedIndex() + 1;
             tp_index[current_trigraph] = i;
             tp_trigraph[current_trigraph].setText(IGCview.tps.tp[i].trigraph);
             tp_label[current_trigraph].setText(IGCview.tps.tp[i].full_name);
             if (current_trigraph < MAXTASK)
                  tp_trigraph[++current_trigraph].requestFocus();
           }
       }
    }

  // Handle <Return> key hit in trigraph text fields, and OK/CANCEL

  public void actionPerformed(ActionEvent e)
    {
      String button_label = e.getActionCommand();
      if (button_label.equals("OK"))
        { 
          ok_hit();
          dispose();
          return;
        }
      else if (button_label.equals("Cancel"))
	{
          dispose();
          return;
        }
      for (int i=1; i<= MAXTASK; i++)
        {
         if ((TextField) e.getSource() == tp_trigraph[i])
           { String full_name = "Bad TP";
             String upper_key = tp_trigraph[i].getText().toUpperCase();
             tp_trigraph[i].setText(upper_key);
             int current_tp = IGCview.tps.lookup(upper_key);
             current_trigraph = i;
             if (current_tp > 0) 
               { full_name = IGCview.tps.tp[current_tp].full_name;
                 tp_index[current_trigraph] = current_tp;
                 if (i < MAXTASK) tp_trigraph[++current_trigraph].requestFocus();
               }
             tp_label[i].setText(full_name);
             return;
	   }
        }
      System.out.println("Unknown action event " + e);
    }

  // handle focus events for trigraph text entry fields

  public void focusGained(FocusEvent e)
    {
      for (int i=1; i<= MAXTASK; i++)
        {
         if ((TextField) e.getSource() == tp_trigraph[i])
           { current_trigraph = i;
             return;
           }
        }
    }  

  public void focusLost(FocusEvent e) {;}

  void ok_hit()
    { Task task = new Task(map_frame);
      System.out.println("OK hit");
      for (int i=1; i<= MAXTASK; i++)
        {
          if (tp_index[i] > 0) task.add(IGCview.tps.tp[tp_index[i]]);
        }
      IGCview.task = task;
    }
}

//***************************************************************************//
//            Task                                                           //
//***************************************************************************//

class Task
{
  int tp_count = 0;
  TP [] tp = new TP [7];   // MAXTASK + 1
  MapFrame map_frame;

  Task(MapFrame map_frame)
    {
      this.map_frame = map_frame;
    }

  void add(TP new_tp)
    {
      tp[++tp_count] = new_tp;
    }

  public void draw(Graphics g)
    {
      int x1,x2,y1,y2;
      g.setColor(Color.red);
      x1 = long_to_x(tp[1].longitude);
      y1 = lat_to_y(tp[1].latitude);
      for (int i = 2; i <= tp_count; i++)
        {
          x2 = long_to_x(tp[i].longitude);
          y2 = lat_to_y(tp[i].latitude);
          g.drawLine(x1,y1,x2,y2);
          x1 = x2;
          y1 = y2;
        }
    }

  private int long_to_x(float longitude)
    {
      return (int) ((longitude - map_frame.origin_long) * map_frame.xscale);
    }

  private int lat_to_y(float latitude)
    {
      return (int) ((map_frame.origin_lat - latitude) * map_frame.yscale);
    }

}






