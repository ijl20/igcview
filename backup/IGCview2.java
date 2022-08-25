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
import java.math.*;


//***************************************************************************//
//             IGCview                                                       //
//***************************************************************************//

public class IGCview extends Applet {

  static final float NMKM = (float) 1.852;  // conversion factor Nm -> Km

  static final int   MAXTPS = 6;          // maximum TPs in Task
  static final int   MAXLOGS = 5000;        // maximum log points in TrackLog
  static final int   MAXTHERMALS = 60;      // max number of thermals stored per log

  static final int   MAINWIDTH = 700;       // window default sizes
  static final int   MAINHEIGHT = 500;
  static final int   ALTWIDTH = 600;
  static final int   ALTHEIGHT = 200;

  static final int   MAXALT = 40000;       // altitude window 0..MAXALT
  static final int   MAXCLIMB = 10;        // climb window to +/- MAXCLIMB

  static final int   CLIMBAVG = 30;        // averaging period for ClimbCanvas
  static final int   CLIMBSPEED = 40;      // point-to-point speed => thermalling


  static private String appletInfo = 
    "IGC Soaring log file analysis program\nAuthor: Ian Lewis\nDecember 1997";

  static final float PI = (float) 3.1415926;

  static private String usageInfo = "Usage: java IGCfile";

  static TrackLog [] logs = new TrackLog[100];   // loaded track logs
 
  static int log_count = 0;                      // count of logs loaded
  static int current_log;
  static int time_start = 35000, time_finish = 65000; // boundaries of timebase windows
  
  static TP_db tps;                              // tp database

  static Task task;                              // current task

  public void init () {
    try {
      this.add (new MapFrame(MAINWIDTH,MAINHEIGHT));
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
      try { MapFrame map = new MapFrame(MAINWIDTH,MAINHEIGHT);
            map.show();
          }
      catch (NumberFormatException e) { System.out.println (usageInfo); }
    }

  // distance between two lat/longs in Nautical miles

  static float dec_to_dist(float lat1, float long1, float lat2, float long2)
    {
      float latR1, longR1, latR2, longR2;
      if (lat1 == lat2 && long1 == long2) return (float) 0;
      latR1 = lat1 * PI / 180;
      longR1 = long1 * PI / 180;
      latR2 = lat2 * PI / 180;
      longR2 = long2 * PI / 180;
      return (float) Math.acos(Math.sin(latR1) * Math.sin(latR2) + 
                               Math.cos(latR1) * Math.cos(latR2) * Math.cos(longR1-longR2)) *
                     (float) 60.04042835 * (float) 180 / PI;
    }

  // track angle between two lat/longs in degrees

  static float dec_to_track(float lat1, float long1, float lat2, float long2)
    {
      float offset;
      if (lat1 == lat2 && long1 == long2) return (float) 0;
      offset = (float) Math.atan( dec_to_dist(lat2, long1, lat2, long2) /
                                dec_to_dist(lat1, long1, lat2, long1)) * (float) 180 / PI;
      if (lat2 > lat1)
        if (long2 > long1) return offset; else return (float) 360 - offset;
      else
        if (long2 > long1) return (float) 180 - offset; else return (float) 180 + offset;        
    }
}

//***************************************************************************//
//             MapFrame                                                      //
//***************************************************************************//

class MapFrame extends Frame {

  Zoom zoom;
  DrawingCanvas canvas;

  private static String [] [] menus = {
    {"File", "Quit"},
    {"Zoom", "Zoom In", "Zoom Out", "Zoom Reset"},
    {"Track", "Load Track", "Find Thermals"},
    {"Task", "Load TPs", "Define Task"},
    {"Window", "Altitude", "Climb", "Speed"}
  };    

  private int width, height;

  MapFrame (int width, int height)
    {
      this.setTitle("IGCview");
      this.width = width;
      this.height = height;
	zoom = new Zoom();
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
      float x_adj = ((float) map_frame.getSize().width / (float) rect_w);
      float y_adj = ((float) map_frame.getSize().height / (float) rect_h);
      if (x_adj > y_adj) x_adj = y_adj;
      map_frame.zoom.scale(y_to_lat(rect_y), x_to_long(rect_x), x_adj);
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
      if (IGCview.current_log > 0) 
        IGCview.logs[IGCview.current_log].draw_thermals(g);
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
      else if (s.equals ("find thermals"))
             {
               IGCview.logs[IGCview.current_log].find_thermals();
               paint(getGraphics());
             }
      else if (s.equals ("save")) save ();
      else if (s.equals ("zoom out"))
             { 
               map_frame.zoom.zoom_out();
               paint(getGraphics());
             }
      else if (s.equals ("zoom in"))
             { 
               map_frame.zoom.zoom_in();
               paint(getGraphics());
             }
      else if (s.equals ("zoom reset"))
             { 
               map_frame.zoom.zoom_reset();
               paint(getGraphics());
             }
      else if (s.equals ("altitude")) view_altitude();
      else if (s.equals ("climb")) view_climb();
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
             IGCview.current_log = IGCview.log_count;
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

  private void view_altitude()
    {
      AltFrame window = new AltFrame(IGCview.ALTWIDTH, IGCview.ALTHEIGHT);
      window.setTitle("Altitude");
      window.pack();
      window.show();
    }

  private void view_climb()
    {
      ClimbFrame window = new ClimbFrame(IGCview.ALTWIDTH, IGCview.ALTHEIGHT);
      window.setTitle("Climb");
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
      return (float) x / map_frame.zoom.xscale + map_frame.zoom.longitude;
    } 

  public float y_to_lat(int y)
    {
      return map_frame.zoom.latitude - ((float) y / map_frame.zoom.yscale);
    }

}

//***************************************************************************//
//            TrackLog                                                       //
//***************************************************************************//

class TrackLog
{
  int   [] time      = new int [IGCview.MAXLOGS+1];    // Time of day in seconds, alt in feet
  float [] altitude  = new float [IGCview.MAXLOGS+1];
  float [] latitude  = new float [IGCview.MAXLOGS+1]; 
  float [] longitude = new float [IGCview.MAXLOGS+1];  // West/South = negative, North/East = positive
  int      record_count = 0;               // number of records in log
  MapFrame map_frame;                      // MapFrame creating this TrackLog
  Thermal [] thermal = new Thermal [IGCview.MAXTHERMALS+1];
  int      thermal_count = 0;              // number of thermals in log

  // TrackLog constructor:
  
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

    x1 = long_to_x(longitude[1]);
    y1 = lat_to_y(latitude[1]);
    for (int i = 2; i <= record_count; i++)
    {
        x2 = long_to_x(longitude[i]);
        y2 = lat_to_y(latitude[i]);
        g.drawLine(x1,y1,x2,y2);
        x1 = x2;
        y1 = y2;
    }
  }

  public void draw_alt(AltCanvas alt_canvas, Graphics g)
    {
      int x1,x2,y1,y2,w,h;

      g.setColor(Color.green);                // Draw thermals
      y1 = 0;
      h = alt_canvas.alt_to_y((float) 0);
      for (int i = 1; i <= thermal_count; i++)
        {
          x1 = alt_canvas.time_to_x(time[thermal[i].start_index]);
          w = alt_canvas.time_to_x(time[thermal[i].finish_index]) - x1;
          g.fillRect(x1,y1,w,h);
        }
      g.setColor(Color.black);                // Draw alt trace
      x1 = alt_canvas.time_to_x(time[1]);
      y1 = alt_canvas.alt_to_y(altitude[1]);
      for (int i = 2; i <= record_count; i++)
        {
          x2 = alt_canvas.time_to_x(time[i]);
          y2 = alt_canvas.alt_to_y(altitude[i]);
          g.drawLine(x1,y1,x2,y2);
          x1 = x2;
          y1 = y2;
        }
    }

  public void draw_climb(ClimbCanvas climb_canvas, Graphics g)
    {
      int x1,x2,y1,y2, i, w,h;
      float altitude1, altitude2;
      int time1, time2;

      g.setColor(Color.green);                // Draw thermals
      y1 = 0;
      h = climb_canvas.climb_to_y((float) 0);
      for (i=1; i <= thermal_count; i++)
        {
          x1 = climb_canvas.time_to_x(time[thermal[i].start_index]);
          w = climb_canvas.time_to_x(time[thermal[i].finish_index]) - x1;
          g.fillRect(x1,y1,w,h);
        }
      g.setColor(Color.black);                // Draw graph
      time1 = time[1];
      x1 = climb_canvas.time_to_x(time1);
      altitude1 = altitude[1];
      y1 = climb_canvas.climb_to_y((float) 0);
      i = 1;
      while (i < record_count)
        {
          while ( i < record_count && time[++i] - time1 < IGCview.CLIMBAVG) {;} // skip
          if (i == record_count) break;
          time2 = time[i];
          altitude2 = altitude[i];
          x2 = climb_canvas.time_to_x(time2);
          y2 = climb_canvas.climb_to_y((altitude2 - altitude1) / (time2 - time1));
          g.drawLine(x1,y1,x2,y2);
          x1 = x2;
          y1 = y2;
          altitude1 = altitude2;
          time1 = time2;
        }
    }

  public void draw_thermals(Graphics g)
  {
    int x,y,w,h;
    
    for (int i = 1; i <= thermal_count; i++)
      {
        g.setColor(Color.blue);            // draw big box around thermal boundary
        x = long_to_x(thermal[i].long1);
        y = lat_to_y(thermal[i].lat2);
        w = long_to_x(thermal[i].long2) - x;
        h = lat_to_y(thermal[i].lat1) - y;
        g.drawRect(x,y,w,h);
        mark_point(g, Color.white, thermal[i].start_index);
        mark_point(g, Color.blue, thermal[i].finish_index);
    }
  }

  private void mark_point(Graphics g, Color c, int index)
    { 
      int x, y, w ,h;
      g.setColor(c);
      x = long_to_x(longitude[index]) - 2;
      y = lat_to_y(latitude[index]) - 2;
      w = 4;
      h = 4;
      g.drawRect(x,y,w,h);
    }

  private int long_to_x(float longitude)
    {
      return (int) ((longitude - map_frame.zoom.longitude) * map_frame.zoom.xscale);
    }

  private int lat_to_y(float latitude)
    {
      return (int) ((map_frame.zoom.latitude - latitude) * map_frame.zoom.yscale);
    }

  void find_thermals()
    {
      int i = 0, j;
      float lat1, long1, lat2, long2; // boundaries of thermal

      while (++i < record_count && thermal_count < IGCview.MAXTHERMALS)
        {
          if (thermalling(i))
            {
              j = i;
              while (thermalling(++j)) {;} // scan to end of thermal
              lat1 = latitude[j];
              long1 = longitude[j];
              lat2 = lat1;
              long2 = long1;
              for (int k=i; k<j; k++)     // accumulate boundary lat/longs
                {
                  if (latitude[k] < lat1)       lat1 = latitude[k];
                  else {if (latitude[k] > lat2) lat2 = latitude[k];}
                  if (longitude[k] < long1)       long1 = longitude[k];
                  else {if (longitude[k] > long2) long2 = longitude[k];}
                }              
              thermal[++thermal_count] = new Thermal();
              thermal[thermal_count].lat1 = lat1;
              thermal[thermal_count].long1 = long1;
              thermal[thermal_count].lat2 = lat2;
              thermal[thermal_count].long2 = long2;
              thermal[thermal_count].start_index = i;
              thermal[thermal_count].finish_index = j;
              thermal[thermal_count].rate = (altitude[j]-altitude[i]) /
                                            (time[j]-time[i]) * (float) 0.6;
              i = j;
            }
        }
      if (thermal_count == IGCview.MAXTHERMALS) System.out.println("Too many thermals");
      System.out.println(thermal_count + " thermals found.");
    }  

  private boolean thermalling(int i)  // true if thermalling at log index i
    {
      int j = i;
      float dist;       // distance travelled
      int time_taken;   // time taken between samples

      while (j < record_count && time[++j] - time[i] < IGCview.CLIMBAVG) {;}
      if (j == record_count) return false;
      dist = IGCview.dec_to_dist(latitude[i], longitude[i], latitude[j], longitude[j]);
      time_taken = time[j] - time[i];
      return (dist / time_taken * 3600 < IGCview.CLIMBSPEED);
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
  private TextField [] tp_trigraph = new TextField [IGCview.MAXTPS+1];
  private Label [] tp_fullname = new Label [IGCview.MAXTPS+1];
  private Label [] tp_dist = new Label [IGCview.MAXTPS+1];
  private Label [] tp_track = new Label [IGCview.MAXTPS+1];
  private Label [] tp_bisector = new Label [IGCview.MAXTPS+1];
  private Label tp_length;                              // label to hold task length
  private int [] tp_index = new int [IGCview.MAXTPS+1];            // size is MAXTPS
  private List tp_list;
  private Button ok_button, cancel_button;
  private int current_trigraph = 1;              // text field with focus
  private MapFrame map_frame;                    // frame to draw on
  private float [] dist = new float [7];         // array [MAXTPS+1] to hold leg lengths
  private float [] track = new float [7];        // array [MAXTPS+1] to hold leg tracks
  private float [] bisector = new float [7];     // array [MAXTPS+1] to hold leg tracks
  private float length;                          // total task length       

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

      for (int i=1; i<=IGCview.MAXTPS; i++) tp_index[i] = 0;

      for (int i=1;i <= IGCview.tps.tp_count; i++)
        {
          tp_list.addItem(IGCview.tps.tp[i].trigraph + " " + IGCview.tps.tp[i].full_name);
        }
      tp_list.addItemListener(this);

      GridBagLayout gridbag = new GridBagLayout();
      GridBagConstraints c = new GridBagConstraints();
 
      setFont(new Font("Helvetica", Font.PLAIN, 14));
      setLayout(gridbag);
   
      c.insets = new Insets(3,3,3,3);                 // space 3 pixels around fields
      c.fill = GridBagConstraints.BOTH;
      c.gridx = 1;
      make_label("Trigraph:", gridbag, c);            //-- LABEL: Trigraphs
      c.gridx = GridBagConstraints.RELATIVE;
      c.weightx = 1.0;
      make_label("Full name:", gridbag, c);           //-- LABEL: Full name
      c.weightx = 0.0;
      make_label("Distance:", gridbag, c);            //-- LABEL: Distance
      make_label("Track:", gridbag, c);               //-- LABEL: Track
      make_label("Bisector:", gridbag, c);            //-- LABEL: Bisector
      c.weightx = 1.0;
      c.gridwidth = GridBagConstraints.REMAINDER;
      make_label("TP list:", gridbag, c);             //-- LABEL: TP list
      c.weightx = 0.0;
      c.gridwidth = 1;
      make_label("1",gridbag,c);                      //-- LABEL: tp number
      tp_trigraph[1] = make_entry(gridbag, c);        //-- TEXT:  trigraph
      c.weightx = 1.0;
      tp_fullname[1] = make_tp_label(gridbag,c);      //-- LABEL: tp_fullname[1]
      c.weightx = 0.0;
      tp_dist[1] = make_tp_label(gridbag,c);          //-- LABEL: tp_dist[1]
      tp_track[1] = make_tp_label(gridbag,c);         //-- LABEL: tp_track[1]
      tp_bisector[1] = make_tp_label(gridbag,c);      //-- LABEL: tp_bisector[1]
      c.weightx = 1.0;
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.gridheight = GridBagConstraints.RELATIVE;
      gridbag.setConstraints(tp_list, c);
      add(tp_list);                                   //-- LIST: tp list
      c.gridheight = 1;
      c.gridwidth = 1;
      for (int i=2; i<=IGCview.MAXTPS; i++)
        { c.gridx = 0;
          c.weightx = 0.0;
          make_label(String.valueOf(i),gridbag,c);    //-- LABEL: tp number
          c.gridx = 1;
          tp_trigraph[i] = make_entry(gridbag, c);    //-- TEXT:  trigraph
          c.gridx = 2;
          c.weightx = 1.0;
          tp_fullname[i] = make_tp_label(gridbag,c);  //-- LABEL: tp_fullname[i]
          c.weightx = 0.0;
          c.gridx = 3;
          tp_dist[i] = make_tp_label(gridbag,c);      //-- LABEL: tp_dist[i]
          c.gridx = 4;
          tp_track[i] = make_tp_label(gridbag,c);     //-- LABEL: tp_track[i]
          c.gridx = 5;
          tp_bisector[i] = make_tp_label(gridbag,c);  //-- LABEL: tp_bisector[i]
        }
      c.gridx = 1;
      c.weightx = 0.0;
      cancel_button = make_button("Cancel", gridbag,c);  //-- BUTTON: Cancel
      c.gridx = 2;
      ok_button = make_button("OK", gridbag,c);       //-- BUTTON: OK
      c.gridx = 3;
      tp_length = make_tp_label(gridbag,c);           //-- LABEL: tp_length
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
             tp_fullname[current_trigraph].setText(IGCview.tps.tp[i].full_name);
             calc_length();
             if (current_trigraph < IGCview.MAXTPS)
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
      for (int i=1; i<= IGCview.MAXTPS; i++)  // <return> hit in trigraph field ?
        {
         if ((TextField) e.getSource() == tp_trigraph[i])
           { String full_name = "Bad TP";
             String upper_key = tp_trigraph[i].getText().toUpperCase();
             current_trigraph = i;
             tp_trigraph[current_trigraph].setText(upper_key);
             int current_tp = IGCview.tps.lookup(upper_key);
             if (current_tp > 0) 
               { full_name = IGCview.tps.tp[current_tp].full_name;
                 tp_index[current_trigraph] = current_tp;
                 if (i < IGCview.MAXTPS) tp_trigraph[++current_trigraph].requestFocus();
               }
             else 
               tp_index[current_trigraph] = 0;
             tp_fullname[i].setText(full_name);
             calc_length();
             return;
	   }
        }
      System.out.println("Unknown action event " + e);
    }

  // handle focus events for trigraph text entry fields

  public void focusGained(FocusEvent e)
    {
      for (int i=1; i<=IGCview.MAXTPS; i++)
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

      for (int i=1; i<= IGCview.MAXTPS; i++)
        {
          if (tp_index[i] > 0) task.add(IGCview.tps.tp[tp_index[i]], 
                                        dist[i], 
                                        track[i],
                                        bisector[i]);
        }
      task.length = length;
      IGCview.task = task;
    }

  void calc_length()  // sets and returns length (km) and tracks of task
    {
      int prev_tp, current_tp;
      float lat1, long1, lat2, long2;

      length = 0;
      // clear task distance fields
      for (int i=2; i<=IGCview.MAXTPS; i++)
        {
          tp_dist[i].setText("");     
          tp_track[i].setText("");
          tp_bisector[i].setText("");
        }
      tp_length.setText("");

      prev_tp = tp_index[1];
      if (prev_tp == 0) return;
      lat1 = IGCview.tps.tp[prev_tp].latitude;
      long1 = IGCview.tps.tp[prev_tp].longitude; 
      for (int i=2; i<=IGCview.MAXTPS; i++)   // calculate leg lengths and tracks
        {
          current_tp = tp_index[i];
          if (current_tp == 0) break;
          lat2 = IGCview.tps.tp[current_tp].latitude;
          long2 = IGCview.tps.tp[current_tp].longitude;
          dist[i] = IGCview.dec_to_dist(lat1, long1, lat2, long2) * IGCview.NMKM;
          track[i] = IGCview.dec_to_track(lat1, long1, lat2, long2);
          tp_dist[i].setText(places(dist[i],1));
          tp_track[i].setText(places(track[i],0));
          length = length + dist[i];
          tp_length.setText(places(length,1));
          lat1 = lat2;
          long1 = long2;
        }
      for (int i=2; i<IGCview.MAXTPS; i++)   // calculate bisectors
        {
          if (tp_index[i+1] == 0) break;
          bisector[i] = calc_bisector(track[i], track[i+1]);
          tp_bisector[i].setText(places(bisector[i],0));
        }
      return;
    }

  float calc_bisector(float track_in, float track_out)
    {
      float bisector, bisector_offset;
      bisector = (track_in + track_out) / 2 + 90;
      bisector_offset = (bisector > track_in) ? bisector - track_in : track_in - bisector;
      if (bisector_offset > 90 && bisector_offset < 270) bisector += 180;
      if (bisector >= 360) bisector -= 360;
      return bisector;
    }

  private String places(float f, int d)
    {
       if (d == 0)
         return String.valueOf( (int) f );
       else if ( d == 1)
         return String.valueOf( (float) ((int)(f * 10)) / (float) 10);
       else if ( d == 2)
         return String.valueOf( (float) ((int)(f * 100)) / (float) 100);
       else return "????";
    }
}

//***************************************************************************//
//            Task                                                           //
//***************************************************************************//

class Task
{
  int tp_count = 0;
  TP [] tp = new TP [IGCview.MAXTPS+1];
  float [] dist = new float [IGCview.MAXTPS+1];
  float [] track = new float [IGCview.MAXTPS+1];
  float [] bisector = new float [IGCview.MAXTPS+1];
  float length = 0;

  MapFrame map_frame;

  Task(MapFrame map_frame)
    {
      this.map_frame = map_frame;
    }

  void add(TP new_tp, float dist, float track, float bisector)
    {
      tp[++tp_count] = new_tp;
      this.track[tp_count] = track;
      this.dist[tp_count] = dist;
      this.bisector[tp_count] = bisector;
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

  boolean in_sector(float latitude, float longitude, int tp_index)
    {
      float bearing, margin, max_margin, bisector1;

      bearing = IGCview.dec_to_track(tp[tp_index].latitude,
                                     tp[tp_index].longitude,
                                     latitude,
                                     longitude);
      if (tp_index == 1)
        {
          bisector1 = track[2] + 180;
          if (bisector1 > 360) bisector1 -= 360;
          max_margin = 90;
        }
      else if (tp_index == tp_count)
        {
          bisector1 = track[tp_index];
          max_margin = 90;
	}
      else 
        {
          bisector1 = bisector[tp_index];
          max_margin = 45;
	}
      margin =  (bearing > bisector1) ? bearing - bisector1 : bisector1 - bearing;
      if (margin >= 180) margin = (float) 360 - margin;
      return (margin <= max_margin);
    }

  private int long_to_x(float longitude)
    {
      return (int) ((longitude - map_frame.zoom.longitude) * map_frame.zoom.xscale);
    }

  private int lat_to_y(float latitude)
    {
      return (int) ((map_frame.zoom.latitude - latitude) * map_frame.zoom.yscale);
    }

}

//***************************************************************************//
//            Zoom                                                           //
//***************************************************************************//
//
//  Implements zoom history as circular buffer of size ZOOMHISTORY
//  Current scale in 'zoom.xscale' and 'zoom.yscale'
//

class Zoom
{
  // public class variables:
  public float xscale, yscale, latitude, longitude;

  // constants:
  private static final int ZOOMHISTORY = 20;
  private static final float INRATIO = (float) 2, OUTRATIO = (float) 0.5;
  // circular zoom buffer:
  private float [] x_hist = new float [21];  // ZOOMHISTORY + 1
  private float [] y_hist = new float [21];  // ZOOMHISTORY + 1
  private float [] lat_hist = new float [21];  // ZOOMHISTORY + 1
  private float [] long_hist = new float [21];  // ZOOMHISTORY + 1

  private int current, in_limit, out_limit;

  Zoom()
    {
      zoom_reset();
    }

  void zoom_reset()
    {
      xscale = 200;
      yscale = 400;
      latitude = 53;
      longitude = -1;
      current = 1;
      in_limit = 1;
      out_limit = 1;
      x_hist[1] = xscale;
      y_hist[1] = yscale;
      lat_hist[1] = latitude;
      long_hist[1] = longitude;
    }

  void scale(float o_lat, float o_long, float factor)   // adjust scale by 'factor'
    {
      xscale *= factor;
      yscale *= factor;
      latitude = o_lat;
      longitude = o_long;
      if (factor > (float) 1)
        { // zooming IN
          if (current++ == ZOOMHISTORY) current = 1;
          in_limit = current;
        }
      else
        { // zooming OUT
          if (current-- == 1) current = ZOOMHISTORY;
          out_limit = current;
        }
      x_hist[current] = xscale;
      y_hist[current] = yscale;
      lat_hist[current] = latitude;
      long_hist[current] = longitude;
    }

  void zoom_out()
    {
      if (current == out_limit)
        {
          scale(latitude, longitude, OUTRATIO);
          return;
        }
      if (current-- == 1) current = ZOOMHISTORY;
      xscale = x_hist[current];
      yscale = y_hist[current];
      latitude = lat_hist[current];
      longitude = long_hist[current];
    }

  void zoom_in()
    {
      if (current == in_limit)
        {
          scale(latitude, longitude, INRATIO);
          return;
        }
      if (current++ == ZOOMHISTORY) current = 1;
      xscale = x_hist[current];
      yscale = y_hist[current];
      latitude = lat_hist[current];
      longitude = long_hist[current];
    }
}

//***************************************************************************//
//             AltFrame                                                      //
//***************************************************************************//

class AltFrame extends Frame {

  AltCanvas canvas;

  private static String [] [] menus = {
    {"File", "Close", "Exit"}
  };    

  private int width, height;

  AltFrame (int width, int height)
    {
      this.setTitle("Altitude view");
      this.width = width;
      this.height = height;
      ScrollPane pane = new ScrollPane ();
      this.add (pane, "Center");
      canvas = new AltCanvas (this,2 * width, 5 * height);
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
//            AltCanvas                                                      //
//***************************************************************************//

class AltCanvas extends Canvas implements ActionListener
{
  private int width, height;
  private float xscale, yscale;
  private AltFrame alt_frame;
  Color color = Color.white;
 
  AltCanvas (AltFrame alt_frame, int width, int height)
    {
      this.alt_frame = alt_frame;
      this.width = width;
      this.height = height;
    }

  public Dimension getPreferredSize () 
    {
      return new Dimension (width, height);
    }

  public void paint (Graphics g)
    { Dimension d = getSize();
      g.setColor(Color.white);
      g.fillRect(0,0, d.width - 1, d.height - 1);
      xscale = (float) d.width / (IGCview.time_finish - IGCview.time_start);
                                                   // xscale in pixels / foot
      yscale = (float) d.height / IGCview.MAXALT;  // yscale in pixels / second
      g.setColor(Color.black);
      draw_grid(g);
      g.setColor(Color.black);
      IGCview.logs[IGCview.current_log].draw_alt(this, g);
    }

  public void actionPerformed (ActionEvent e)
    {
      String s = e.getActionCommand ();
      if (s.equals ("new")) 
        {
          this.repaint ();
        }
      else if (s.equals ("close")) alt_frame.dispose();
      else if (s.equals ("exit")) System.exit (0);
      else if (s.equals ("black")) color = Color.black;
    }

  void draw_grid(Graphics g)
    {
      int line_x, line_y;

      g.setColor(Color.black);
      for (int i=0; i<IGCview.MAXALT; i += 1000)  // draw height contours
        {
          line_y = alt_to_y(i);
          g.drawLine(0, line_y, width, line_y);
        }
      for (int i=IGCview.time_start; i < IGCview.time_finish; i += 3600)
        {
          line_x = time_to_x(i);
          g.drawLine(line_x, 0, line_x, height);
          line_x +=2;
          g.drawLine(line_x, 0, line_x, height); // double line for hours
          for (int j=600; j<=3000; j+=600)        // draw 10 min lines
            { line_x = time_to_x(i+j);
              g.drawLine(line_x, 0, line_x, height);
            }
        }
    }

  public int time_to_x(int time)
    {
      return (int) ((float) (time-IGCview.time_start) * xscale);
    } 

  public int alt_to_y(float altitude)
    {
      return height - (int)(altitude * yscale);
    }

}

//***************************************************************************//
//             ClimbFrame                                                    //
//***************************************************************************//

class ClimbFrame extends Frame {

  ClimbCanvas canvas;

  private static String [] [] menus = {
    {"File", "Close", "Exit"}
  };    

  private int width, height;

  ClimbFrame (int width, int height)
    {
      this.setTitle("Climb view");
      this.width = width;
      this.height = height;
      ScrollPane pane = new ScrollPane ();
      this.add (pane, "Center");
      canvas = new ClimbCanvas (this, 2 * width, 2 * height);
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
//            ClimbCanvas                                                    //
//***************************************************************************//

class ClimbCanvas extends Canvas implements ActionListener
{
  private int width, height;
  private float xscale, yscale;
  private ClimbFrame climb_frame;
 
  Color color = Color.white;
 
  ClimbCanvas (ClimbFrame climb_frame, int width, int height)
    {
      this.climb_frame = climb_frame;
      this.width = width;
      this.height = height;
    }

  public Dimension getPreferredSize () 
    {
      return new Dimension (width, height);
    }

  public void paint (Graphics g)
    { Dimension d = getSize();
      g.setColor(Color.white);
      g.fillRect(0,0, d.width - 1, d.height - 1);
      xscale = (float) d.width / (IGCview.time_finish - IGCview.time_start);
                                                         // xscale in pixels / second
      yscale = (float) d.height / IGCview.MAXCLIMB / 2;  // yscale in pixels/knot
      g.setColor(Color.black);
      draw_grid(g);
      g.setColor(Color.red);
      IGCview.logs[IGCview.current_log].draw_climb(this, g);
    }

  public void actionPerformed (ActionEvent e)
    {
      String s = e.getActionCommand ();
      if (s.equals ("new")) 
        {
          this.repaint ();
        }
      else if (s.equals ("close")) climb_frame.dispose();
      else if (s.equals ("exit")) System.exit (0);
      else if (s.equals ("black")) color = Color.black;
    }

  void draw_grid(Graphics g)
    {
      int line_x, line_y;

      for (int i= -(IGCview.MAXCLIMB); i<IGCview.MAXCLIMB * 2; i++)  // draw climb contours
        {
          line_y = climb_to_y(i);
          g.drawLine(0, line_y, width, line_y);
        }
      for (int i=IGCview.time_start; i < IGCview.time_finish; i += 3600)
        {
          line_x = time_to_x(i);
          g.drawLine(line_x, 0, line_x, height);
          line_x +=2;
          g.drawLine(line_x, 0, line_x, height); // double line for hours
          for (int j=600; j<=3000; j+=600)        // draw 10 min lines
            { line_x = time_to_x(i+j);
              g.drawLine(line_x, 0, line_x, height);
            }
        }
      line_y = climb_to_y((float) 0);
      g.drawLine(0, line_y, width, line_y);
      line_y += 2;
      g.drawLine(0, line_y, width, line_y);
    }

  public int time_to_x(int time)
    {
      return (int) ((float) (time-IGCview.time_start) * xscale);
    } 

  public int climb_to_y(float climb)
    { 
      return height / 2 - (int)(climb * yscale);
    }

}

//***************************************************************************//
//            Thermal                                                        //
//***************************************************************************//

class Thermal
{
  float lat1, long1, lat2, long2;  // boundaries of circling flight
  float rate;                      // climb rate
  int start_index, finish_index;   // first and last log point

  Thermal() {;}
}
