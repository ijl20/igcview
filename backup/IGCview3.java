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
//             Config                                                        //
//***************************************************************************//

class Config
{
  // MAP CONFIG
  static boolean draw_thermals = true;
  static boolean draw_thermal_ends = false;
  static float start_xscale = 200;
  static float start_yscale = 400;
  static float start_latitude = 53;
  static float start_longitude = -1;

  // COLOURS
  static Color SECTRACKCOLOUR = Color.green;
  static Color SECAVGCOLOUR = Color.green.darker();
  static Color ALTCOLOUR = Color.blue;              // primary trace in ALT window
  static Color ALTSECCOLOUR = Color.gray;           // secondary traces in ALT window
  static Color TPCOLOUR = Color.blue;               // vertical lines in ALT windows
  static Color TPSECCOLOUR = Color.blue;            // vertical lines in ALT windows
  static Color TASKCOLOUR = Color.red.brighter();   // task drawn on map
  static Color CLIMBCOLOUR = Color.blue;            // climb bars in CLIMB window
  static Color CLIMBSECCOLOUR = Color.green.darker();  // secondary climb bars
  static Color CRUISECOLOUR = Color.blue;            // speed bars in CRUISE window
  static Color CRUISESECCOLOUR = Color.green.darker();  // secondary cruise bars
  static Color MAPBACKGROUND = Color.green.darker();
  static final Color [] TRACKCOLOUR = { Color.blue,
                                        Color.red.darker(),
                                        Color.cyan,
                                        Color.magenta,
                                        Color.blue.brighter(),
                                        Color.orange,
                                        Color.pink,
                                        Color.red,
                                        Color.yellow
                                      };
  static final int PRIMARYCOLOUR = 0; // INDEX of colour in TRACKCOLOUR for primary

  // FILES
  // static String TPFILE = "G:\\src\\java\\igcview\\bga.gdn"; // TP gardown file
  // static String LOGDIR = "G:\\igcfiles\\grl97\\grl1\\";     // LOG directory
     static String TPFILE = "bga.gdn";
     static String LOGDIR = "./";

  // UNIT CONVERSIONS
  static float convert_task_dist = IGCview.NMKM;
}

//***************************************************************************//
//             IGCview                                                       //
//***************************************************************************//

public class IGCview extends Applet {

  static final int   MAXTPS = 6;          // maximum TPs in Task
  static final int   MAXLOGS = 100;        // maximum track logs loaded
  static final int   MAXLOG = 5000;        // maximum log points in TrackLog
  static final int   MAXTHERMALS = 60;      // max number of thermals stored per log

  static final int   MAINWIDTH = 700;       // window default sizes
  static final int   MAINHEIGHT = 500;
  static final int   ALTWIDTH = 600;
  static final int   ALTHEIGHT = 300;
  static final int   SELECTWIDTH = 300;
  static final int   SELECTHEIGHT = 300;
  static final int   BASELINE = 5;         // offset of zero from bottom of frame

  static final int   MAXALT = 40000;       // altitude window 0..MAXALT
  static final int   ALTMAXDIST = 750;     // altitude window 0..MAXALTDIST km
  static final int   MAXCLIMB = 12;        // climb window 0..MAXCLIMB
  static final int   MAXCRUISE = 180;      // cruise window 0..MAXCRUISE

  static final int   CLIMBAVG = 30;        // averaging period for ClimbCanvas
  static final int   CRUISEAVG = 30;       // averaging period for CruiseCanvas
  static final int   CLIMBSPEED = 40;      // point-to-point speed => thermalling

  static final int   CLIMBBOXES = 24;      // number of categories of climb
  static final float   CLIMBBOX = (float) 0.5;  // size of climb category in knots
  static final int   MAXPERCENT = 100;     // max %time in climb profile window
  static final int   LASTTHERMALLIMIT = 120; // ignore 'thermal' within 120secs of end

  static final int MAXCOLOURS = 9;

  static final float TIMEXSCALE = (float) 0.04;  // scale for time x-axis in ALT windows
  static final float DISTXSCALE = (float) 4;  // scale: distance x-axis in ALT windows
  static final float DISTSTART = (float) 50;    // start offset for distance X-axis
 
  static private String appletInfo = 
    "IGC Soaring log file analysis program\nAuthor: Ian Lewis\nDecember 1997";

  static final float PI = (float) 3.1415926;
  static final int TIMEX=1, DISTX=2; // constants giving type of x-axis
  static final int GRAPHALT=1, GRAPHCLIMB=2, GRAPHCRUISE=3, GRAPHCP=4;

  static final float NMKM = (float) 1.852;  // conversion factor Nm -> Km

  static private String usageInfo = "Usage: java IGCfile";

  // APPLICATION GLOBALS
  static TrackLog [] logs = new TrackLog[MAXLOGS+1];   // loaded track logs
  static TrackLogS sec_log = new TrackLogS();          // secondary track log
 
  static int log_count = 0;                      // count of logs loaded
  static int primary_index = 0;                  // log number of primary flight
  static boolean [] secondary = new boolean [MAXLOGS+1]; // whether flight selected for
                                                 // secondary comparison
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

  // main MENU definitions
  private static String [] [] menus = {
    {"File", "Clear Logs", "Exit"},
    {"Zoom", "Zoom In", "Zoom Out", "Zoom Reset"},
    {"Track", "Load Track", "Select Tracks"},
    {"Task", "Define Task"},
    {"Window", "Altitude", "Climb", "Cruise", "Climb Profile"},
    {"Configure", "Colours", "View", "Coordinates", "Units", "Files", "Save Configuration"}
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
      g.setColor(Config.MAPBACKGROUND);
      g.fillRect(0,0, d.width - 1, d.height - 1);
      if (IGCview.task != null) IGCview.task.draw(g);
      for (int i = 1; i <= IGCview.log_count; i++)
          if (IGCview.secondary[i]) IGCview.logs[i].draw(g,i);
      if (IGCview.primary_index>0) 
        {
          IGCview.logs[IGCview.primary_index].draw(g, Config.PRIMARYCOLOUR);
          IGCview.logs[IGCview.primary_index].draw_thermals(g);
          IGCview.logs[IGCview.primary_index].draw_tp_times(g);
        }
    }

  public void actionPerformed (ActionEvent e)
    {
      String s = e.getActionCommand ();
      if (s.equals ("new")) 
        {
          this.repaint ();
        }
      else if (s.equals ("clear logs")) clear_logs();
      else if (s.equals ("load tps")) load_tps();
      else if (s.equals ("define task")) get_task();
      else if (s.equals ("load track")) load_track ();
      else if (s.equals ("select tracks")) select_tracks();
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
      else if (s.equals ("cruise")) view_cruise();
      else if (s.equals ("climb profile")) view_climb_profile();
      else if (s.equals ("exit")) System.exit (0);
    }

  private void clear_logs()
    {
      IGCview.log_count = 0;
      IGCview.primary_index = 0;
      for (int log=1; log<=IGCview.MAXLOGS; log++) IGCview.secondary[log] = false;
      paint(getGraphics());
    }

  private void load_track()
   {
     FileDialog fd = new FileDialog (map_frame, "Load Track Log", FileDialog.LOAD);
     fd.setDirectory(Config.LOGDIR);
     fd.show ();
     String filename = fd.getFile ();
     Config.LOGDIR = fd.getDirectory();
     if (filename != null) {
       try { TrackLog log =  new TrackLog(Config.LOGDIR+filename, map_frame);
             IGCview.logs[IGCview.log_count] = log;
             paint(getGraphics());
           } catch (Exception e) {System.out.println (e);}
     }
   }

  private void select_tracks()
    {
      SelectFrame window = new SelectFrame(IGCview.SELECTWIDTH, IGCview.SELECTHEIGHT);
      window.setTitle("Select Tracks");
      window.pack();
      window.show();
    }

  private void load_tps()
   {
     try { IGCview.tps = new TP_db(Config.TPFILE);
           System.out.println("Loaded " + IGCview.tps.tp_count + " TPs");
         } catch (Exception e) {System.out.println (e);}
   }

  private void get_task()
    {
      if (IGCview.tps==null) load_tps();
      TaskWindow window = new TaskWindow(map_frame);
      window.setTitle("Define Task");
      window.pack();
      window.show();
    }

  private void view_altitude()
    {
      GraphFrame window = new GraphFrame(IGCview.GRAPHALT,
                                     IGCview.logs[IGCview.primary_index],
                                     IGCview.ALTWIDTH,
                                     IGCview.ALTHEIGHT);
      window.pack();
      window.show();
    }

  private void view_climb()
    {
      GraphFrame window = new GraphFrame(IGCview.GRAPHCLIMB,
                                     IGCview.logs[IGCview.primary_index],
                                     IGCview.ALTWIDTH,
                                     IGCview.ALTHEIGHT);
      window.pack();
      window.show();
    }

  private void view_cruise()
    {
      GraphFrame window = new GraphFrame(IGCview.GRAPHCRUISE,
                                     IGCview.logs[IGCview.primary_index],
                                     IGCview.ALTWIDTH,
                                     IGCview.ALTHEIGHT);
      window.setTitle("Cruise speed");
      window.pack();
      window.show();
    }

  private void view_climb_profile()
    {
      GraphFrame window = new GraphFrame(IGCview.GRAPHCP,
                                     IGCview.logs[IGCview.primary_index],
                                     IGCview.ALTWIDTH,
                                     IGCview.ALTHEIGHT);
      window.setTitle("Climb Profile: "+IGCview.logs[IGCview.primary_index].name);
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
  boolean baro_ok=false, gps_ok=false;
  int   [] time      = new int [IGCview.MAXLOG+1];    // Time of day in seconds, alt in feet
  float [] altitude  = new float [IGCview.MAXLOG+1];
  float [] latitude  = new float [IGCview.MAXLOG+1]; 
  float [] longitude = new float [IGCview.MAXLOG+1];  // W/S = negative, N/E = positive
  int   record_count = 0;                              // number of records in log
  MapFrame map_frame;                                  // MapFrame creating this TrackLog
  Climb [] thermal = new Climb [IGCview.MAXTHERMALS+1];
  int  thermal_count = 0;                              // number of thermals in log
  float [] climb_percent = new float [IGCview.CLIMBBOXES+1];
  float climb_avg = (float) 0.0;

  int   [] tp_index  = new int [IGCview.MAXTPS+1];     // log point at each TP
  int tps_rounded = 0;                                 // TPs successfully rounded
  String name;

  // TrackLog constructor:
  
  TrackLog (String filename, MapFrame f)
  {
    int i;
    map_frame = f;
    try { BufferedReader in = new BufferedReader(new FileReader(filename));
          String buf;

          while ((buf = in.readLine()) != null) 
          { if ((buf.charAt(0) == 'B') &&
                (buf.charAt(35) == 'V'))
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
		if (latitude[record_count]!=(float)0.0) gps_ok=true;
		if (altitude[record_count]!=(float)0.0) baro_ok=true;
	        }
          }
          i = filename.lastIndexOf("/");
	  if (i==-1) i = filename.lastIndexOf("\\");
          name = (i<0) ? filename : filename.substring(i);
          in.close();
          IGCview.log_count++;
          if (IGCview.log_count==1)
            IGCview.primary_index = 1;
	  else 
            IGCview.secondary[IGCview.log_count] = true;
          find_tp_times();
          System.out.println("Log "+name+" baro_ok="+baro_ok+", gps_ok="+gps_ok);
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

  public void draw(Graphics g, int num)
  {
    int x1,x2,y1,y2, i=0;

    if (!gps_ok) return;
    g.setColor(Config.TRACKCOLOUR[num % IGCview.MAXCOLOURS]);

    while (latitude[++i]==(float)0.0) {;} // skip bad gps records
    x1 = long_to_x(longitude[i]);
    y1 = lat_to_y(latitude[i]);
    for (int j = i+1; j <= record_count; j++)
    {
        if (latitude[j]==(float)0.0) continue;
        x2 = long_to_x(longitude[j]);
        y2 = lat_to_y(latitude[j]);
        g.drawLine(x1,y1,x2,y2);
        x1 = x2;
        y1 = y2;
    }
  }

  public void draw_alt(AltCanvas canvas, Graphics g, boolean primary)
    {
      if (!baro_ok) return;
      if (primary) draw_alt_tp_lines(canvas, g);           // Draw TP lines
      g.setColor(primary ? Config.ALTCOLOUR : Config.ALTSECCOLOUR);
      if (canvas.x_axis==IGCview.TIMEX)                // Draw alt trace
        draw_time_alt(canvas, g);
      else
        draw_dist_alt(canvas, g);
    }

  private void draw_time_alt(AltCanvas canvas, Graphics g)
    {
      int x1,x2,y1,y2;

      x1 = canvas.time_to_x(time[1]);
      y1 = canvas.alt_to_y(altitude[1]);
      for (int i = 2; i <= record_count; i++)
        {
          x2 = canvas.time_to_x(time[i]);
          y2 = canvas.alt_to_y(altitude[i]);
          g.drawLine(x1,y1,x2,y2);
          x1 = x2;
          y1 = y2;
        }
    }

  private void draw_dist_alt(AltCanvas canvas, Graphics g)
    {
      int x1,x2,y1,y2, next_tp=1;
      float dist, tp_lat, tp_long, tp_dist = (float) 0.0;

      if (tps_rounded==0) return;
      tp_lat = IGCview.task.tp[1].latitude;
      tp_long = IGCview.task.tp[1].longitude;
      x1 = canvas.dist_to_x(0);
      y1 = canvas.alt_to_y(altitude[1]);
      for (int i = 1; i <= record_count; i++)
        {
	  if (latitude[i]==(float)0.0) continue;
          if (i==tp_index[next_tp] && next_tp<IGCview.task.tp_count) 
            {
              next_tp++;
              tp_dist += IGCview.task.dist[next_tp];
              tp_lat = IGCview.task.tp[next_tp].latitude;
              tp_long = IGCview.task.tp[next_tp].longitude;
            }
          dist = IGCview.dec_to_dist(latitude[i], longitude[i], tp_lat, tp_long);
          x2 = canvas.dist_to_x(tp_dist-dist);
          y2 = canvas.alt_to_y(altitude[i]);
          g.drawLine(x1,y1,x2,y2);
          x1 = x2;
          y1 = y2;
        }
    }

  private void draw_alt_tp_lines(AltCanvas canvas, Graphics g)
    {
      int x, h = canvas.alt_to_y((float) 0);
      float tp_dist = (float) 0;

      g.setColor(Config.TPCOLOUR);
      if (tps_rounded==0) return;                     // Draw TP lines
      for (int tp=1; tp<=tps_rounded; tp++)
        {
          if (canvas.x_axis==IGCview.TIMEX)
              x = canvas.time_to_x(time[tp_index[tp]]);
          else
            { tp_dist += IGCview.task.dist[tp];
              x = canvas.dist_to_x(tp_dist);
            }
          g.drawLine(x,0,x,h);
          x++;
          g.drawLine(x,0,x,h);
        }
    }

  public void draw_climb(ClimbCanvas climb_canvas, Graphics g, boolean primary)
    {
      int x,y,w,h,i;

      if (!baro_ok) return;
      find_thermals();
      h = climb_canvas.climb_to_y((float) 0);
      g.setColor(Config.TPCOLOUR);      
      if (primary && tps_rounded>0)                     // Draw TP lines
        for (int tp=1; tp<=tps_rounded; tp++)
          {
            x = climb_canvas.time_to_x(time[tp_index[tp]]);
            g.drawLine(x,0,x,h);
            x++;
            g.drawLine(x,0,x,h);
          }
      g.setColor(primary ? Config.CLIMBCOLOUR : Config.CLIMBSECCOLOUR);
      h = primary ? 5 : 4;
      for (i=1; i<=thermal_count; i++)
        {
          if (tps_rounded>0)
            if (thermal[i].finish_index < tp_index[1]) continue;  // not yet started task
            else if (tps_rounded==IGCview.task.tp_count &&       // completed task
                     thermal[i].finish_index > tp_index[tps_rounded]) break;
          x = climb_canvas.time_to_x(time[thermal[i].start_index]);
          w = climb_canvas.time_to_x(time[thermal[i].finish_index]) - x;
          y = climb_canvas.climb_to_y(thermal[i].rate());
          g.fillRect(x,y,w,h);
        }
    }

  public void draw_cruise(CruiseCanvas cruise_canvas, Graphics g, boolean primary)
    {
      float dist;
      int x,y,w,h,i, i1, i2;

      if (!baro_ok) return;
      find_thermals();
      h = cruise_canvas.speed_to_y((float) 0);
      g.setColor(Config.TPCOLOUR);      
      if (primary && tps_rounded>0)                     // Draw TP lines
        for (int tp=1; tp<=tps_rounded; tp++)
          {
            x = cruise_canvas.time_to_x(time[tp_index[tp]]);
            g.drawLine(x,0,x,h);
            x++;
            g.drawLine(x,0,x,h);
          }
      g.setColor(primary ? Config.CRUISECOLOUR : Config.CRUISESECCOLOUR);
      h = primary ? 5 : 4;
      for (i=1; i<=thermal_count; i++)
        {
          i1 = thermal[i].finish_index;
          i2 = (i<thermal_count) ? thermal[i+1].start_index : record_count; 
          if (tps_rounded>0)
            if (thermal[i].finish_index < tp_index[1]) continue;  // not yet started task
            else if (tps_rounded==IGCview.task.tp_count)          // completed task
                   if (i1 > tp_index[tps_rounded]) break;
                   else if (i2 > tp_index[tps_rounded]) i2 = tp_index[tps_rounded];
          dist = IGCview.dec_to_dist(latitude[i1], longitude[i1],
                                     latitude[i2], longitude[i2]);
          x = cruise_canvas.time_to_x(time[i1]);
          w = cruise_canvas.time_to_x(time[i2]) - x;
          y = cruise_canvas.speed_to_y( dist / (time[i2]-time[i1]) * 3600);
          g.fillRect(x,y,w,h);
        } // what about final glide????
    }

  public void draw_climb_profile(ClimbProfileCanvas cp_canvas, Graphics g, boolean primary)
    {
      int x,y, i, box;
      Polygon p = new Polygon();
      Color c = primary ? Config.TRACKCOLOUR[Config.PRIMARYCOLOUR] : Config.SECTRACKCOLOUR;

      if (!baro_ok) return;
      find_climb_profile();
      x = cp_canvas.climb_to_x((float) 0);
      y = cp_canvas.time_percent_to_y((float) 0);
      p.addPoint(x,y);
      y = cp_canvas.time_percent_to_y(climb_percent[1]);
      p.addPoint(x,y);
      for (box=1; box<=IGCview.CLIMBBOXES; box++)
        { 
          x = cp_canvas.box_to_x(box);
          y = cp_canvas.time_percent_to_y(climb_percent[box]);
          p.addPoint(x,y);
        }
      x = cp_canvas.box_to_x(IGCview.CLIMBBOXES);
      y = cp_canvas.time_percent_to_y((float) 0);
      p.addPoint(x,y);
      x = cp_canvas.climb_to_x(climb_avg);
      g.setColor(c);                               // Draw graph
      g.drawPolygon(p);
      if (primary)                                 // draw average climb line
        {
          g.drawLine(x,0,x,cp_canvas.time_percent_to_y((float) 0));
          g.drawLine(x+1,0,x+1,cp_canvas.time_percent_to_y((float) 0));
        }
    }

  public void draw_thermals(Graphics g)
  {
    int i,x,y,w,h;

    if (!gps_ok) return;
    if (!Config.draw_thermals) return;    
    g.setColor(Color.green);            // draw big box around thermal boundary
    for (i=1; i<=thermal_count; i++)
      {
        if (tps_rounded>0 && thermal[i].finish_index < tp_index[1]) continue;
        x = long_to_x(thermal[i].long1);
        y = lat_to_y(thermal[i].lat2);
        w = long_to_x(thermal[i].long2) - x;
        h = lat_to_y(thermal[i].lat1) - y;
        g.drawRect(x,y,w,h);
      }
    if (Config.draw_thermal_ends)          // mark thermal entry and exit points
      for (i=1; i<=thermal_count; i++)
        { 
          if (tps_rounded>0 && thermal[i].finish_index < tp_index[1]) continue;
          mark_point(g, Color.green, thermal[i].start_index);
          mark_point(g, Color.red, thermal[i].finish_index);
        }
  }

  void draw_tp_times(Graphics g)
    {
      if (!gps_ok) return;
      for (int i = 1; i <= tps_rounded; i++) mark_point(g, Color.yellow, tp_index[i]);
    }

  private void mark_point(Graphics g, Color c, int index)
    { 
      int x, y, w ,h;
      g.setColor(c);
      x = long_to_x(longitude[index]) - 3;
      y = lat_to_y(latitude[index]) - 3;
      w = 6;
      h = 6;
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
      int i = 0, i_max, j;
      float lat1, long1, lat2, long2; // boundaries of thermal

      if (!gps_ok || thermal_count != 0) return;
      i_max = record_count;
      if (tps_rounded > 0)
         { i = tp_index[1] - 1; // if TP times found then use start time
           if (tps_rounded == IGCview.task.tp_count)
              i_max = tp_index[tps_rounded]; // if all TPs rounded use finish time
         }
      while (++i < i_max && thermal_count < IGCview.MAXTHERMALS)
        {
	  if (latitude[i]==(float)0.0) continue; // skip bad gps records
          if (thermalling(i))
            {
              j = i;
              while (thermalling(++j)) {;} // scan to end of thermal
                                           // skip last "thermal"
              if (time[record_count]-time[j] <= IGCview.LASTTHERMALLIMIT) break;
              lat1 = latitude[j];
              long1 = longitude[j];
              lat2 = lat1;
              long2 = long1;
              for (int k=i; k<j; k++)     // accumulate boundary lat/longs
                {
		  if (latitude[k]==(float)0.0) continue; // skip bad gps records
                  if (latitude[k] < lat1)       lat1 = latitude[k];
                  else {if (latitude[k] > lat2) lat2 = latitude[k];}
                  if (longitude[k] < long1)       long1 = longitude[k];
                  else {if (longitude[k] > long2) long2 = longitude[k];}
                }              
              thermal[++thermal_count] = new Climb(this);
              thermal[thermal_count].lat1 = lat1;
              thermal[thermal_count].long1 = long1;
              thermal[thermal_count].lat2 = lat2;
              thermal[thermal_count].long2 = long2;
              thermal[thermal_count].start_index = i;
              thermal[thermal_count].finish_index = j;
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

      if (latitude[i]==(float)0.0) return false;
      while (j < record_count && 
             time[++j] - time[i] < IGCview.CLIMBAVG &&
             latitude[j] != (float)0.0) {;}
      if (j == record_count) return false;
      dist = IGCview.dec_to_dist(latitude[i], longitude[i], latitude[j], longitude[j]);
      time_taken = time[j] - time[i];
      return (dist / time_taken * 3600 < IGCview.CLIMBSPEED);
    }

  void find_tp_times()
    {
      int i;
      int current_tp = 1;

      if (!gps_ok || IGCview.task==null) return;
      climb_avg = (float) 0.0; // invalidate climb average so
                               // it is recalculated when needed
      for (i=2; i<=record_count; i++) // scan forwards for all TPs except start
        if (IGCview.task.in_sector(latitude[i], longitude[i], current_tp))
          {
            tp_index[current_tp++] = i;
            if (current_tp > IGCview.task.tp_count) break;
          }
      for (i=tp_index[2]; i>1; i--)  // scan backwards from TP2 to find start
        if (IGCview.task.in_sector(latitude[i], longitude[i], 1))
          {
            tp_index[1] = i;
            tps_rounded = current_tp-1;
            return;
          }
      tps_rounded = 0; // start not found
    }

  void find_climb_profile()
    {
      int i, box, total_time = 0;
      float climb_acc = 0;  // accumulated climb to calculate average

      if (!gps_ok || !baro_ok || climb_avg != (float) 0.0) return;
      int [] climb_time = new int [IGCview.CLIMBBOXES+1];
      find_thermals();
      for (box=1; box<=IGCview.CLIMBBOXES; box++) climb_time[box] = 0;
      for (i=1; i<=thermal_count; i++)
        {
          if (tps_rounded>0)
            if (thermal[i].finish_index < tp_index[1]) continue;  // not yet started task
            else if (tps_rounded==IGCview.task.tp_count &&       // completed task
                     thermal[i].finish_index > tp_index[tps_rounded]) break;
          total_time += thermal[i].duration();
          for (box=1; box<=IGCview.CLIMBBOXES; box++)
            {
              if (thermal[i].rate()<IGCview.CLIMBBOX* box)
                {
                  climb_time[box] += thermal[i].duration();
                  break;
                }
            }
        }
      for (box=1; box<=IGCview.CLIMBBOXES; box++) // calculate percentages
        climb_percent[box] = ((float) climb_time[box]) / total_time * 100;
      for (box=1; box<=IGCview.CLIMBBOXES; box++) // accumulate climb_acc
        climb_acc += IGCview.CLIMBBOX*((float)box-(float)0.5)*climb_percent[box];
      climb_avg = climb_acc / 100;
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
  private float [] dist = new float [IGCview.MAXTPS+1];
  private float [] track = new float [IGCview.MAXTPS+1];
  private float [] bisector = new float [IGCview.MAXTPS+1];
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
/*  COMMENTED OUT LIST BOX FILL
      for (int i=1;i <= IGCview.tps.tp_count; i++)
        {
          tp_list.addItem(IGCview.tps.tp[i].trigraph + " " + IGCview.tps.tp[i].full_name);
        }
*/
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
    { int i;
      Task task = new Task(map_frame);

      for (i=1; i<= IGCview.MAXTPS; i++)
        {
          if (tp_index[i] > 0) task.add(IGCview.tps.tp[tp_index[i]], 
                                        dist[i] / IGCview.NMKM, 
                                        track[i],
                                        bisector[i]);
        }
      task.dist[1] = (float) 0;
      task.length = length;
      IGCview.task = task;
      for (i=1; i<=IGCview.log_count; i++) IGCview.logs[i].find_tp_times();      
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
      g.setColor(Config.TASKCOLOUR);
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

      if (latitude==(float)0.0) return false;  // cater for bad gps records
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
      xscale = Config.start_xscale;
      yscale = Config.start_yscale;
      latitude = Config.start_latitude;
      longitude = Config.start_longitude;
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
//             GraphFrame                                                    //
//***************************************************************************//

class GraphFrame extends Frame {

  int graph_type;
  AltCanvas alt_canvas;
  ClimbCanvas climb_canvas;
  CruiseCanvas cruise_canvas;
  ClimbProfileCanvas cp_canvas;

  TrackLog log;

  private static String [] [] menus = {
    {"File", "Close", "Exit"},
    {"X-axis", "Time", "Distance"}
  };    

  private int width, height;

  GraphFrame (int graph_type, TrackLog log, int width, int height)
    {
      this.graph_type = graph_type;
      this.log = log;
      setSize(width,height);
      set_title();
      this.width = width;
      this.height = height;
      ScrollPane pane = new ScrollPane ();
      this.add (pane, "Center");
      set_canvas(pane);

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
	      set_listener(i);
  	    }
        }
      }
      this.pack ();
      pane.doLayout();
      pane.setScrollPosition(0,10000);
      repaint();
    }

  void set_title()
    {
      switch (graph_type)
        {
          case IGCview.GRAPHALT :    this.setTitle("Altitude view: "+log.name);
                                     break;
          case IGCview.GRAPHCLIMB :  this.setTitle("Climb view: "+log.name);
                                     break;
          case IGCview.GRAPHCRUISE : this.setTitle("Cruise view: "+log.name);
                                     break;
          case IGCview.GRAPHCP :     this.setTitle("Climb Profile: "+log.name);
                                     break;
        }      

    }

  void set_canvas(ScrollPane pane)
    {
      switch (graph_type)
        {
          case IGCview.GRAPHALT:
                 alt_canvas = new AltCanvas (this, 2*width, 5*height);
                 pane.add(alt_canvas);
                 break;
	  case IGCview.GRAPHCLIMB:
                 climb_canvas = new ClimbCanvas (this, 2*width, 2*height);
                 pane.add(climb_canvas);
                 break;
          case IGCview.GRAPHCRUISE :
                 cruise_canvas = new CruiseCanvas (this, 2*width, 2*height);
                 pane.add(cruise_canvas);
                 break;
          case IGCview.GRAPHCP :
                 cp_canvas = new ClimbProfileCanvas (this, 2*width, 2*height);
                 pane.add(cp_canvas);
                 break;
        }      

    }

  void set_listener(MenuItem i)
    {
      switch (graph_type)
        {
          case IGCview.GRAPHALT:
                 i.addActionListener(alt_canvas);
                 break;
	  case IGCview.GRAPHCLIMB:
                 i.addActionListener(climb_canvas);
                 break;
          case IGCview.GRAPHCRUISE :
                 i.addActionListener(cruise_canvas);
                 break;
          case IGCview.GRAPHCP :
                 i.addActionListener(cp_canvas);
                 break;
        }      

    }

  public Dimension getPreferredSize () {
    return new Dimension (width, height);
  };

}

//***************************************************************************//
//             AltFrame                                                      //
//***************************************************************************//

class AltFrame extends Frame {

  AltCanvas canvas;
  TrackLog log;

  private static String [] [] menus = {
    {"File", "Close", "Exit"},
    {"X-axis", "Time", "Distance"}
  };    

  private int width, height;

  AltFrame (TrackLog log, int width, int height)
    {
      setSize(width,height);
      this.setTitle("Altitude view");
      this.width = width;
      this.height = height;
      this.log = log;
      ScrollPane pane = new ScrollPane ();
      this.add (pane, "Center");
//      canvas = new AltCanvas (this,2 * width, 5 * height);
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
      pane.doLayout();
      pane.setScrollPosition(0,10000);
      repaint();
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
  private GraphFrame alt_frame;
  private float xscale = IGCview.TIMEXSCALE, yscale;
  int x_axis = IGCview.TIMEX;
  
  AltCanvas (GraphFrame alt_frame, int width, int height)
    {
      this.alt_frame = alt_frame;
      this.width = width;
      this.height = height;
      yscale = (float) (height-IGCview.BASELINE) / IGCview.MAXALT;
                                                   // yscale in pixels / second
    }

  public Dimension getPreferredSize () 
    {
      return new Dimension (width, height);
    }

  public void paint (Graphics g)
    { Dimension d = getSize();
      g.setColor(Color.white);
      g.fillRect(0,0, d.width - 1, d.height - 1);
      draw_grid(g);                                  // draw grid
      alt_frame.log.draw_alt(this, g, true);         // draw primary
      IGCview.sec_log.draw_alt(this,g);              // draw secondaries
    }

  public void actionPerformed (ActionEvent e)
    {
      String s = e.getActionCommand ();
      if (s.equals ("close")) alt_frame.dispose();
      else if (s.equals ("exit")) System.exit (0);
      else if (s.equals ("time")) set_time_x_axis();
      else if (s.equals ("distance")) set_dist_x_axis();
    }

  void set_time_x_axis()
        {
          x_axis = IGCview.TIMEX;
          xscale = IGCview.TIMEXSCALE;
          this.repaint ();
        }

  void set_dist_x_axis()
        {
          x_axis = IGCview.DISTX;
          xscale = IGCview.DISTXSCALE;
          this.repaint ();
        }

  void draw_grid(Graphics g)
    {
      int line_x, line_y;

      g.setColor(Color.gray);
      for (int i=1; i<IGCview.MAXALT; i += 1000)  // draw height contours
        {
          line_y = alt_to_y(i);
          g.drawLine(0, line_y, width, line_y);
        }
      if (x_axis==IGCview.TIMEX) 
        draw_time_contours(g);
      else
        draw_dist_contours(g);
      g.setColor(Color.black);
      line_y = alt_to_y((float) 0);           // draw baseline
      g.drawLine(0, line_y, width, line_y);
      line_y += 1;
      g.drawLine(0, line_y, width, line_y);
    }

  void draw_time_contours(Graphics g)
    {
      int line_x, line_y;
      for (int i=IGCview.time_start; i < IGCview.time_finish; i += 3600)
        {
          line_x = time_to_x(i);
          g.drawLine(line_x, 0, line_x, height);
          line_x +=1;
          g.drawLine(line_x, 0, line_x, height); // double line for hours
          for (int j=600; j<=3000; j+=600)        // draw 10 min lines
            { line_x = time_to_x(i+j);
              g.drawLine(line_x, 0, line_x, height);
            }
        }
    }

  void draw_dist_contours(Graphics g)
    {
      int line_x, line_y;
      for (int i=0; i<IGCview.ALTMAXDIST; i += 100)
        {
          line_x = dist_to_x((float)i/Config.convert_task_dist);
          g.drawLine(line_x, 0, line_x, height);
          line_x +=1;
          g.drawLine(line_x, 0, line_x, height); // double line for 100km
          for (int j=10; j<=90; j+=10)           // draw 10 km lines
            { line_x = dist_to_x((float)(i+j)/Config.convert_task_dist);
              g.drawLine(line_x, 0, line_x, height);
            }
        }
    }

  public int time_to_x(int time)
    {
      return (int) ((float) (time-IGCview.time_start) * xscale);
    } 

  public int dist_to_x(float dist)
    {
      return (int) ((dist+IGCview.DISTSTART) * xscale);
    } 

  public int alt_to_y(float altitude)
    {
      return height - (int)(altitude * yscale) - IGCview.BASELINE;
    }

}

//***************************************************************************//
//             ClimbFrame                                                    //
//***************************************************************************//

class ClimbFrame extends Frame {

  ClimbCanvas canvas;
  TrackLog log;

  private static String [] [] menus = {
    {"File", "Close", "Exit"}
  };    

  private int width, height;

  ClimbFrame (TrackLog log, int width, int height)
    {
      setSize(width,height);
      this.setTitle("Climb view");
      this.width = width;
      this.height = height;
      this.log = log;
      ScrollPane pane = new ScrollPane ();
      this.add (pane, "Center");
      //      canvas = new ClimbCanvas (this, 2 * width, 2 * height);
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
      pane.doLayout();
      pane.setScrollPosition(0,10000);
      repaint();
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
  private GraphFrame climb_frame;
 
  ClimbCanvas (GraphFrame climb_frame, int width, int height)
    {
      this.climb_frame = climb_frame;
      this.width = width;
      this.height = height;
      xscale = (float) width / (IGCview.time_finish - IGCview.time_start);
                                                         // xscale in pixels / second
      yscale = (float) (height - IGCview.BASELINE) / IGCview.MAXCLIMB;
                                                         // yscale in pixels/knot
    }

  public Dimension getPreferredSize () 
    {
      return new Dimension (width, height);
    }

  public void paint (Graphics g)
    { Dimension d = getSize();
      g.setColor(Color.white);
      g.fillRect(0,0, d.width - 1, d.height - 1);
      IGCview.sec_log.draw_climb(this, g);
      climb_frame.log.draw_climb(this, g, true);
      draw_grid(g);
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
    }

  void draw_grid(Graphics g)
    {
      int line_x, line_y;

      g.setColor(Color.gray);
      for (int i=1; i<IGCview.MAXCLIMB; i++)  // draw climb contours
        {
          line_y = climb_to_y(i);
          g.drawLine(0, line_y, width, line_y);
          if (i%5==0) g.drawLine(0, line_y+1, width, line_y+1); // double line at 5 knots
        }
      for (int i=IGCview.time_start; i < IGCview.time_finish; i += 3600)
        {
          line_x = time_to_x(i);
          g.drawLine(line_x, 0, line_x, height);
          line_x +=1;
          g.drawLine(line_x, 0, line_x, height); // double line for hours
          for (int j=600; j<=3000; j+=600)        // draw 10 min lines
            { line_x = time_to_x(i+j);
              g.drawLine(line_x, 0, line_x, height);
            }
        }
      g.setColor(Color.black);
      line_y = climb_to_y((float) 0);           // draw baseline
      g.drawLine(0, line_y, width, line_y);
      line_y += 1;
      g.drawLine(0, line_y, width, line_y);
    }

  public int time_to_x(int time)
    {
      return (int) ((float) (time-IGCview.time_start) * xscale);
    } 

  public int climb_to_y(float climb)
    { 
      return height - (int)(climb * yscale) - IGCview.BASELINE;
    }

}

//***************************************************************************//
//            Climb                                                          //
//***************************************************************************//

class Climb
{
  float lat1, long1, lat2, long2;  // boundaries of circling flight
  int start_index, finish_index;   // first and last log point
  TrackLog track_log;

  Climb(TrackLog t)
    {
      track_log = t;
    }

  int duration()
    {
      return track_log.time[finish_index] - track_log.time[start_index];
    }

  float rate()
    {
      return (track_log.altitude[finish_index]-track_log.altitude[start_index]) / 
                duration() * (float) 0.6;
    }
}

//***************************************************************************//
//             CruiseFrame                                                   //
//***************************************************************************//

class CruiseFrame extends Frame {

  CruiseCanvas canvas;
  TrackLog log;

  private static String [] [] menus = {
    {"File", "Close", "Exit"},
    {"X-axis","Time","Distance"}
  };    

  private int width, height;

  CruiseFrame (TrackLog log, int width, int height)
    {
      setSize(width,height);
      this.setTitle("Cruise view");
      this.width = width;
      this.height = height;
      this.log = log;
      ScrollPane pane = new ScrollPane ();
      this.add (pane, "Center");
      //      canvas = new CruiseCanvas (this, 2 * width, 2 * height);
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
      pane.doLayout();
      pane.setScrollPosition(0,10000);
      repaint();
    }

  public Dimension getPreferredSize () {
    return new Dimension (width, height);
  };

};

//***************************************************************************//
//            CruiseCanvas                                                   //
//***************************************************************************//

class CruiseCanvas extends Canvas implements ActionListener
{
  private int width, height;             // size of window
  private float xscale = IGCview.TIMEXSCALE, yscale;
  private GraphFrame cruise_frame;
  int x_axis = IGCview.TIMEX;
 
  CruiseCanvas (GraphFrame cruise_frame, int width, int height)
    {
      this.cruise_frame = cruise_frame;
      this.width = width;
      this.height = height;
      yscale = (float) (height - IGCview.BASELINE) / IGCview.MAXCRUISE;
    }

  public Dimension getPreferredSize () 
    {
      return new Dimension (width, height);
    }

  public void paint (Graphics g)
    { Dimension d = getSize();
      g.setColor(Color.white);
      g.fillRect(0,0, d.width - 1, d.height - 1);
      draw_grid(g);                                  // draw grid
      IGCview.sec_log.draw_cruise(this,g);           // draw secondaries
      cruise_frame.log.draw_cruise(this, g, true);   // draw primary
    }

  public void actionPerformed (ActionEvent e)
    {
      String s = e.getActionCommand ();
      if (s.equals ("close")) cruise_frame.dispose();
      else if (s.equals ("exit")) System.exit (0);
      else if (s.equals ("time")) set_time_x_axis();
      else if (s.equals ("distance")) set_dist_x_axis();
    }

  void set_time_x_axis()
        {
          x_axis = IGCview.TIMEX;
          xscale = IGCview.TIMEXSCALE;
          this.repaint ();
        }

  void set_dist_x_axis()
        {
          x_axis = IGCview.DISTX;
          xscale = IGCview.DISTXSCALE;
          this.repaint ();
        }

  void draw_grid(Graphics g)
    {
      int line_x, line_y;

      g.setColor(Color.gray);
      for (int i=1; i<IGCview.MAXCRUISE; i+=10)  // draw speed contours
        {
          line_y = speed_to_y(i);
          g.drawLine(0, line_y, width, line_y);
          if (i%50==0) g.drawLine(0, line_y+1, width, line_y+1); // double at 50 knots
        }
      for (int i=IGCview.time_start; i < IGCview.time_finish; i += 3600)
        {
          line_x = time_to_x(i);
          g.drawLine(line_x, 0, line_x, height);
          line_x +=1;
          g.drawLine(line_x, 0, line_x, height); // double line for hours
          for (int j=600; j<=3000; j+=600)        // draw 10 min lines
            { line_x = time_to_x(i+j);
              g.drawLine(line_x, 0, line_x, height);
            }
        }
      g.setColor(Color.black);
      line_y = speed_to_y((float) 0);           // draw baseline
      g.drawLine(0, line_y, width, line_y);
      line_y += 1;
      g.drawLine(0, line_y, width, line_y);
    }

  public int time_to_x(int time)
    {
      return (int) ((float) (time-IGCview.time_start) * xscale);
    } 

  public int speed_to_y(float speed)
    { 
      return height - (int)(speed * yscale) - IGCview.BASELINE;
    }
}

//***************************************************************************//
//             ClimbProfileFrame                                             //
//***************************************************************************//

class ClimbProfileFrame extends Frame {

  TrackLog log;
  ClimbProfileCanvas canvas;

  private static String [] [] menus = {
    {"File", "Close", "Exit"}
  };    

  private int width, height;

  ClimbProfileFrame (TrackLog log, int width, int height)
    {
      setSize(width, height);
      this.width = width;
      this.height = height;
      this.log = log;
      ScrollPane pane = new ScrollPane ();
      this.add (pane, "Center");
      //      canvas = new ClimbProfileCanvas (this, 2 * width, 2 * height);
      pane.add(canvas);

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
      pane.doLayout();
      pane.setScrollPosition(0,10000);
      repaint();
    }

  public Dimension getPreferredSize ()
    {
      return new Dimension (width, height);
    }

};

//***************************************************************************//
//            ClimbProfileCanvas                                             //
//***************************************************************************//

class ClimbProfileCanvas extends Canvas implements ActionListener
{
  private int width, height;
  private float xscale, yscale;
  private GraphFrame cp_frame;
 
  ClimbProfileCanvas (GraphFrame cp_frame, int width, int height)
    {
      this.cp_frame = cp_frame;
      this.width = width;
      this.height = height;
      xscale = ((float) (width - IGCview.BASELINE)) / IGCview.CLIMBBOXES;
                                                         // xscale in pixels/box
      yscale = ((float) (height - IGCview.BASELINE)) / IGCview.MAXPERCENT;
                                                         // yscale in pixels/(%time)
    }

  public Dimension getPreferredSize () 
    {
      return new Dimension (width, height);
    }

  public void paint (Graphics g)
    { Dimension d = getSize();
      g.setColor(Color.white);
      g.fillRect(0,0, d.width - 1, d.height - 1);
      draw_grid(g);                                     // draw grid
      IGCview.sec_log.draw_climb_profile(this,g);       // draw secondary
      cp_frame.log.draw_climb_profile(this, g, true);      // draw primary
    }

  public void actionPerformed (ActionEvent e)
    {
      String s = e.getActionCommand ();
      if (s.equals ("new")) 
        {
          this.repaint ();
        }
      else if (s.equals ("close")) cp_frame.dispose();
      else if (s.equals ("exit")) System.exit (0);
    }

  void draw_grid(Graphics g)
    {
      int line_x, line_y;

      g.setColor(Color.black);
      for (int i=10; i<=IGCview.MAXPERCENT; i+=10) // draw %time contours
        {
          line_y = time_percent_to_y((float) i);
          g.drawLine(0, line_y, width, line_y);
        }
                                  // draw vertical grid lines
      for (float climb=0; climb<=IGCview.CLIMBBOX*IGCview.CLIMBBOXES; climb++)
        {
          line_x = climb_to_x(climb);
          g.drawLine(line_x, 0, line_x, height);
        }
      line_y = time_percent_to_y((float) 0);           // draw baseline
      g.drawLine(0, line_y, width, line_y);
      line_y += 1;
      g.drawLine(0, line_y, width, line_y);
    }

  public int box_to_x(int box)
    {
      return climb_to_x((float) IGCview.CLIMBBOX * (box - (float) 0.5));
    } 

  public int climb_to_x(float climb)
    {
      return (int) (climb * xscale + IGCview.BASELINE);
    }

  public int time_percent_to_y(float time_percent)
    { 
      return height - (int)(time_percent * yscale) - IGCview.BASELINE;
    }
}

//***************************************************************************//
//             SelectFrame                                                   //
//***************************************************************************//

class SelectFrame extends Frame {

  SelectWindow window;

  private static String [] [] menus = {
    {"File", "Close", "Exit"}
  };    

  private int width, height;

  SelectFrame (int width, int height)
    {
      setSize(width, height);
      this.width = width;
      this.height = height;
      ScrollPane pane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
      this.add (pane, "Center");
      window = new SelectWindow(this);
      pane.add(window);

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
	      i.addActionListener (window);
  	    }
        }
      }
      this.pack ();
    }

  public Dimension getPreferredSize ()
    {
      return new Dimension (width, height);
    }

}

//***************************************************************************//
//            SelectWindow                                                   //
//***************************************************************************//

class SelectWindow extends Panel implements ActionListener
{
  private Checkbox [] primary = new Checkbox [IGCview.log_count+1];
  private CheckboxGroup primary_group = new CheckboxGroup();
  private Checkbox [] secondary = new Checkbox [IGCview.log_count+1];
  private Button ok_button, cancel_button;
  private SelectFrame select_frame;              // frame to draw on

  Button make_button(String name, GridBagLayout gridbag, GridBagConstraints c) 
    {
      Button b = new Button(name);
      gridbag.setConstraints(b, c);
      b.addActionListener(this);
      add(b);
      return b;
    }

  SelectWindow(SelectFrame select_frame)
    {
      Label l;
      this.select_frame = select_frame;
 
      GridBagLayout gridbag = new GridBagLayout();
      GridBagConstraints c = new GridBagConstraints();
 
      setFont(new Font("Helvetica", Font.PLAIN, 14));
      setLayout(gridbag);
   
      c.insets = new Insets(3,3,3,3);                 // space 3 pixels around fields
      // c.fill = GridBagConstraints.BOTH;
      c.weightx = 0.0;
      l = new Label("Primary");
      gridbag.setConstraints(l,c);
      add(l);
      l = new Label("Secondary");
      c.gridwidth = GridBagConstraints.RELATIVE;
      gridbag.setConstraints(l,c);
      add(l);
      c.weightx = 1.0;
      l = new Label("Flight");
      c.gridwidth = GridBagConstraints.REMAINDER;
      gridbag.setConstraints(l,c);
      add(l);
      for (int i=1; i<=IGCview.log_count; i++)
        {
          c.weightx = 0.0;
          c.gridwidth = 1;
          primary[i] = new Checkbox("", false, primary_group);
          gridbag.setConstraints(primary[i],c);
          add(primary[i]);
          if (i==IGCview.primary_index) primary[i].setState(true);
          secondary[i] = new Checkbox();
          c.gridwidth = GridBagConstraints.RELATIVE;
          gridbag.setConstraints(secondary[i],c);
          add(secondary[i]);
          secondary[i].setState(IGCview.secondary[i]);
          c.weightx = 1.0;
          l = new Label(IGCview.logs[i].name);
          c.gridwidth = GridBagConstraints.REMAINDER;
          gridbag.setConstraints(l,c);
          add(l);
        }
      c.weightx = 0.0;
      c.gridwidth = GridBagConstraints.RELATIVE;
      cancel_button = make_button("Cancel", gridbag,c);  //-- BUTTON: Cancel
      ok_button = make_button("OK", gridbag,c);       //-- BUTTON: OK
    }

  public void actionPerformed(ActionEvent e)
    {
      String label = e.getActionCommand();
      if (label.equals("OK"))
        { 
          ok_hit();
          select_frame.dispose();
          return;
        }
      else if (label.equals("Cancel"))
  	  {
          select_frame.dispose();
          return;
        }
      else if (label.equals ("close")) { select_frame.dispose(); return;}
      else if (label.equals ("exit")) System.exit (0);
      System.out.println("Unknown action event " + e);
    }

  void ok_hit()
    { 
      int i;
      for (i=1; i<=IGCview.log_count; i++)
        {
          IGCview.secondary[i] = false;
          if (primary[i].getState()) 
            {
              IGCview.primary_index = i;
            }
          else if (secondary[i].getState())
            {
              IGCview.secondary[i] = true;
            }
        }
      IGCview.sec_log.climb_avg = (float) 0.0; // reset secondary track log
    }
}

//***************************************************************************//
//            Secondary TrackLog: TrackLogS                                  //
//***************************************************************************//

class TrackLogS
{
  float [] climb_percent = new float [IGCview.CLIMBBOXES+1];
  float climb_avg = (float) 0.0;

  private float climb_acc = (float) 0.0;

  // TrackLogS constructor:
  
  TrackLogS()
    {;}

  public void draw_climb_profile(ClimbProfileCanvas cp_canvas, Graphics g)
    {
      int sec_log_count=0,x,y, log_num, box;
      Polygon p = new Polygon();
      Color c = Config.SECAVGCOLOUR;
      log_num = 1;
      while (log_num <= IGCview.MAXLOGS && !IGCview.secondary[log_num++]) {;}
      if (log_num>IGCview.MAXLOGS) return;
      if (climb_avg == (float) 0.0)
        {
          for (box=1; box<=IGCview.CLIMBBOXES; box++) climb_percent[box] = (float) 0.0;
          for (log_num=1; log_num<=IGCview.MAXLOGS; log_num++) // accumulate time_percents
                                                               //  in boxes
            if (IGCview.secondary[log_num] &&
                IGCview.logs[log_num].baro_ok &&
		IGCview.logs[log_num].gps_ok)
              {
                sec_log_count++;
                IGCview.logs[log_num].find_climb_profile();
                for (box=1; box<=IGCview.CLIMBBOXES; box++)
                  climb_percent[box] += IGCview.logs[log_num].climb_percent[box];
              }
          climb_acc = (float) 0.0;
          for (box=1; box<=IGCview.CLIMBBOXES; box++) // divide boxes by sec_log_count
            {                                                   //   to get averages
              climb_percent[box] /= sec_log_count;
              climb_acc += IGCview.CLIMBBOX*((float)box-(float)0.5)*climb_percent[box];
            }
          climb_avg = climb_acc / 100;
        }
      x = cp_canvas.climb_to_x((float) 0);                  // draw average profile
      y = cp_canvas.time_percent_to_y((float) 0);
      p.addPoint(x,y);
      y = cp_canvas.time_percent_to_y(climb_percent[1]);
      p.addPoint(x,y);
      for (box=1; box<=IGCview.CLIMBBOXES; box++)
        { 
          x = cp_canvas.box_to_x(box);
          y = cp_canvas.time_percent_to_y(climb_percent[box]);
          p.addPoint(x,y);
        }
      x = cp_canvas.box_to_x(IGCview.CLIMBBOXES);
      y = cp_canvas.time_percent_to_y((float) 0);
      p.addPoint(x,y);
      x = cp_canvas.climb_to_x(climb_avg);
      g.setColor(Config.SECAVGCOLOUR);                           // Fill graph
      g.fillPolygon(p);
      g.drawLine(x,0,x,cp_canvas.time_percent_to_y((float) 0));  // Line for avg climb
      g.drawLine(x+1,0,x+1,cp_canvas.time_percent_to_y((float) 0));
      for (log_num=1; log_num<=IGCview.MAXLOGS; log_num++)       // draw secondary profiles
        if (IGCview.secondary[log_num])
            IGCview.logs[log_num].draw_climb_profile(cp_canvas,g,false);
    }

  public void draw_alt(AltCanvas alt_canvas, Graphics g)
    {
      for (int log_num=1; log_num<=IGCview.MAXLOGS; log_num++)   // draw secondary profiles
        if (IGCview.secondary[log_num]) IGCview.logs[log_num].draw_alt(alt_canvas,g,false);
 
    }

  public void draw_climb(ClimbCanvas climb_canvas, Graphics g)
    {
      for (int log_num=1; log_num<=IGCview.MAXLOGS; log_num++)   // draw secondary climbs
        if (IGCview.secondary[log_num])
          IGCview.logs[log_num].draw_climb(climb_canvas,g,false);
    }

  public void draw_cruise(CruiseCanvas canvas, Graphics g)
    {
      for (int log_num=1; log_num<=IGCview.MAXLOGS; log_num++)   // draw secondary speeds
        if (IGCview.secondary[log_num])
          IGCview.logs[log_num].draw_cruise(canvas,g,false);
    }

}
