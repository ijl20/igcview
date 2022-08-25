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
  Config() {}; // constructor

  // WINDOWS
  static final int CURSORWIDTH = 330;
  static final int CURSORHEIGHT = 240;
  static final int ANALYSISWIDTH = 400;
  static final int ANALYSISHEIGHT = 600;
  static final int DATAWIDTH = 780;
  static final int DATAHEIGHT = 500;
  static final int CONFIGWIDTH = 500;
  static final int CONFIGHEIGHT = 500;

  // MAP CONFIG
  static boolean draw_thermals = true;
  static boolean draw_thermal_ends = false;
  static boolean draw_tp_times = true;
  static float start_xscale = 200;
  static float start_yscale = 400;
  static float start_latitude = 53;
  static float start_longitude = -1;

  // COLOURS
  static Color MAPBACKGROUND = Color.green.darker();
  static int MAPBACKGROUND_index = 8, MAPBACKGROUND_shade = 2; // 0 => brighter, 2 => darker
  static Color CURSORCOLOUR = Color.red;
  static int CURSORCOLOR_index = 1, CURSORCOLOUR_shade = 1; // 0 => brighter, 2 => darker
  static Color SECTRACKCOLOUR = Color.green;
  static int SECTRACKCOLOR_index = 8, SECTRACKCOLOUR_shade = 1; // 0 => brighter, 2 => darker
  static Color SECAVGCOLOUR = Color.green.darker();
  static int SECAVGCOLOR_index = 8, SECAVGCOLOUR_shade = 2; // 0 => brighter, 2 => darker
  static Color ALTCOLOUR = Color.blue;              // primary trace in ALT window
  static int ALTCOLOR_index = 0, ALTCOLOUR_shade = 1; // 0 => brighter, 2 => darker
  static Color ALTSECCOLOUR = Color.green.darker(); // secondary traces in ALT window
  static int ALTSECCOLOR_index = 8, ALTSECCOLOUR_shade = 2; // 0 => brighter, 2 => darker
  static Color TPCOLOUR = Color.blue;               // vertical lines in ALT windows
  static int TPCOLOR_index = 0, TPCOLOUR_shade = 1; // 0 => brighter, 2 => darker
  static Color TASKCOLOUR = Color.red.brighter();   // task drawn on map
  static int TASKCOLOR_index = 1, TASKCOLOUR_shade = 0; // 0 => brighter, 2 => darker
  static Color CLIMBCOLOUR = Color.blue;            // climb bars in CLIMB window
  static int CLIMBCOLOR_index = 0, CLIMBCOLOUR_shade = 1; // 0 => brighter, 2 => darker
  static Color CLIMBSECCOLOUR = Color.green.darker();  // secondary climb bars
  static int CLIMBSECCOLOR_index = 8, CLIMBSECCOLOUR_shade = 2; // 0 => brighter, 2 => darker
  static Color CRUISECOLOUR = Color.blue;            // speed bars in CRUISE window
  static int CRUISECOLOR_index = 0, CRUISECOLOUR_shade = 1; // 0 => brighter, 2 => darker
  static Color CRUISESECCOLOUR = Color.green.darker();  // secondary cruise bars
  static int CRUISESECCOLOR_index = 0, CRUISESECCOLOUR_shade = 2; // 0 => brighter, 2 => darker

  static final int MAXCOLOURS = 10;
  static final Color [] TRACKCOLOUR = { Color.blue,    // 0
                                        Color.red,     // 1
                                        Color.white,   // 2
                                        Color.cyan,    // 3
                                        Color.magenta, // 4
                                        Color.black,   // 5
                                        Color.pink,    // 6
                                        Color.yellow,  // 7
                                        Color.green,   // 8
                                        Color.orange   // 9
                                      };

	// FCOLOUR is index pair into TRACKCOLOUR for plane id in maggot race:
  static final int [][] FCOLOUR = {{0,0},{0,0},{1,1},{3,3},{4,4},{5,5},{6,6},
					     {7,7},{0,5},{0,3},{0,7},{1,2},{1,3},
					     {1,7},{2,6},{2,7},{3,5},{3,6},{3,7},
					     {2,5},{2,6},{2,7},{3,5},{0,6},{3,7},
					     {4,5},{4,6},{4,7},{5,0},{5,1},{5,2},{5,3},{5,4},
					     {6,0},{6,1},{6,2},{6,3},{6,4},{6,5},{6,7},
					     {7,0},{7,1},{7,2},{7,3},{7,4},{7,5},{7,6}};

  static final int PRIMARYCOLOUR = 0; // INDEX of colour in TRACKCOLOUR for primary

  // FILES
     //static String TPFILE = "G:\\src\\java\\igcview\\bga.gdn"; // TP gardown file
     //static String LOGDIR = "G:\\igcfiles\\grl97\\grl1\\";     // LOG directory
  static String TPFILE = "bga.gdn";
  static String LOGDIR = "."+System.getProperty("file.separator");

  // MAGGOT RACE PARAMETERS
  static long RACEPAUSE = 100; // pause 100ms between screen updates
  static boolean ALTSTICKS = true; // draw altitude stick below maggots

  // UNIT CONVERSIONS
  static float convert_task_dist = IGCview.NMKM; // adjustment for distance units
  static int time_offset = 1;                     // time zone adjustment
}

//***************************************************************************//
//             IGCview                                                       //
//***************************************************************************//

public class IGCview extends Applet {

  static Config config = new Config();     // configuration
  static Config config_old = config;       // config backup

  static final int   MAXTPS = 6;           // maximum TPs in Task
  static final int   MAXLOGS = 100;        // maximum track logs loaded
  static final int   MAXLOG = 5000;        // maximum log points in TrackLog
  static final int   MAXTHERMALS = 60;      // max number of thermals stored per log

  static final int   MAINWIDTH = 700;       // window default sizes
  static final int   MAINHEIGHT = 500;
  static final int   ALTWIDTH = 600;
  static final int   ALTHEIGHT = 300;
  static final int   SELECTWIDTH = 400;
  static final int   SELECTHEIGHT = 300;
  static final int   BASELINE = 5;         // offset of zero from bottom of frame

  static final int   MAXALT = 40000;       // altitude window 0..MAXALT
  static final int   ALTMAXDIST = 750;     // altitude window 0..MAXALTDIST km
  static final int   MAXCLIMB = 18;        // climb window 0..MAXCLIMB
  static final int   MAXCRUISE = 220;      // cruise window 0..MAXCRUISE

  static final int   CLIMBAVG = 30;        // averaging period for ClimbCanvas
  static final int   CRUISEAVG = 30;       // averaging period for CruiseCanvas
  static final int   CLIMBSPEED = 40;      // point-to-point speed => thermalling

  static final int   CLIMBBOXES = 24;      // number of categories of climb
  static final float   CLIMBBOX = (float) 0.5;  // size of climb category in knots
  static final int   MAXPERCENT = 100;     // max %time in climb profile window
  static final int   LASTTHERMALLIMIT = 120; // ignore 'thermal' within 120secs of end

  static final float TIMEXSCALE = (float) 0.04;  // scale for time x-axis in ALT windows
  static final float DISTXSCALE = (float) 4;  // scale: distance x-axis in ALT windows
  static final float DISTSTART = (float) 50;    // start offset for distance X-axis
 
  static final private String appletInfo = 
    "IGC Soaring log file analysis program\nAuthor: Ian Lewis\nDecember 1997";

  // MAGGOT RACE CONSTANTS
  static final int RACE_TIME_INCREMENT = 30; // timestep increment for maggot race

  // GENERAL CONSTANTS
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

  static int cursor1 = 0, cursor2 = 0;           // cursor indexes into primary log

  // WINDOWS
  static MapFrame map_frame;                     // main map window
  static CursorFrame cursor_window = null;              // window for cursor data
  static AnalysisFrame analysis_window = null;          // window for flight analysis
  static DataFrame data_window = null;                  // window for flight data
  static GraphFrame alt_window, climb_window, cruise_window, cp_window; // graphs
  static ConfigFrame config_window = null;              // window for flight data

  static boolean [] secondary = new boolean [MAXLOGS+1]; // whether flight selected for
                                                 // secondary comparison
  static int time_start = 35000, time_finish = 78000; // boundaries of timebase windows
  
  static TP_db tps;                              // tp database

  static Task task;                              // current task

  public void init () {
    try { map_frame = new MapFrame(MAINWIDTH,MAINHEIGHT);
            map_frame.show();
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
      try { map_frame = new MapFrame(MAINWIDTH,MAINHEIGHT);
            map_frame.show();
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

  String places(int i, int d)
    {
      return places((float)i, d);
    }

  static String places(float f, int d)
    {
       if (d == 0)
         return String.valueOf( (int) (f+(float)0.5) );
       else if ( d == 1)
         return String.valueOf( (float) ((int)(f*10+(float)0.5)) / (float) 10);
       else if ( d == 2)
         return String.valueOf( (float) ((int)(f*100+(float)0.5)) / (float) 100);
       else return "????";
    }

  static String format_clock(int time)
    {
      if (time!=0) return format_time(time+Config.time_offset*3600);
      return "00:00:00";
    }

  static String format_time(int time)
    {
      int h = time / 3600;
      int m = (time-(h*3600)) / 60;
      int s = time - (h*3600) - (m*60);
      String hh = two_digits(h);
      String mm = two_digits(m);
      String ss = two_digits(s);
      return hh+":"+mm+":"+ss;
    }

  static private String two_digits(int i)
    {
      String cc = "00"+String.valueOf(i);
      return cc.substring(cc.length()-2);
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
    {"Zoom", "Zoom In", "Zoom Out", "Zoom to Task", "Zoom Reset"},
    {"Track", "Load Track", "Select Tracks"},
    {"Task", "Define Task"},
    {"Maggots", "Synchro Start", "Real Time"},
    {"Window", "Altitude", "Climb", "Cruise", "Climb Profile", 
     "Cursor Data", "Flight Data", "Flight Analysis"},
    {"Configure", "Modify Configuration", "Load Configuration", "Save Configuration"}
  };    

  int width, height;

  MapFrame (int width, int height)
    {
      this.setTitle("IGCview");
      this.width = width;
      this.height = height;
      zoom = new Zoom();
      ScrollPane pane = new ScrollPane ();
      canvas = new DrawingCanvas (this, 2 * width, 2 * height);
      pane.add (canvas);
      this.add(pane);

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

  public Dimension getPreferredSize ()
    {
      return new Dimension (width, height);
    }

  int long_to_x(float longitude)
    {
      return (int) ((longitude - zoom.longitude) * zoom.xscale);
    }

  int lat_to_y(float latitude)
    {
      return (int) ((zoom.latitude - latitude) * zoom.yscale);
    }


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

  boolean racing = false;

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
      if (rect_w<5 || rect_h<5) {paint(getGraphics()); return;}
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

  public Dimension getMinimumSize () 
    {
      return new Dimension (100,100);
    }

  public void update (Graphics g)
    {
	g.setColor(Config.MAPBACKGROUND);
      g.setXORMode(Color.white);
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
	if (racing) return;
      for (int i = 1; i <= IGCview.log_count; i++)
          if (IGCview.secondary[i]) IGCview.logs[i].draw(g,i);
      if (IGCview.primary_index>0) 
        {
          IGCview.logs[IGCview.primary_index].draw(g, Config.PRIMARYCOLOUR);
          IGCview.logs[IGCview.primary_index].draw_thermals(g);
          IGCview.logs[IGCview.primary_index].draw_tp_times(g);
          draw_cursors(g);
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
      else if (s.equals ("zoom to task"))
             { 
               map_frame.zoom.zoom_to_task();
               paint(getGraphics());
             }
      else if (s.equals ("zoom reset"))
             { 
               map_frame.zoom.zoom_reset();
               paint(getGraphics());
             }
      else if (s.equals ("synchro start")) maggot_synchro();
      else if (s.equals ("real time")) maggot_real_time();
      else if (s.equals ("altitude")) view_altitude();
      else if (s.equals ("climb")) view_climb();
      else if (s.equals ("cruise")) view_cruise();
      else if (s.equals ("climb profile")) view_climb_profile();
      else if (s.equals ("cursor data")) view_cursor_data();
      else if (s.equals ("flight data")) view_flight_data();
      else if (s.equals ("flight analysis")) view_flight_analysis();
      else if (s.equals ("modify configuration")) modify_config();
      else if (s.equals ("load configuration")) load_config();
      else if (s.equals ("save configuration")) save_config();
      else if (s.equals ("exit")) System.exit (0);
    }

  private void clear_logs()
    {
      IGCview.cursor1 = 1;
      IGCview.cursor2 = 1;
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
	racing = false;
      SelectFrame window = new SelectFrame(map_frame,
                                           IGCview.SELECTWIDTH,
                                           IGCview.SELECTHEIGHT);
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
      window.init();  // set focus on UNIX to TP 1 (seems ok on Win95)
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
      window.pack();
      window.show();
    }

  private void view_climb_profile()
    {
      GraphFrame window = new GraphFrame(IGCview.GRAPHCP,
                                     IGCview.logs[IGCview.primary_index],
                                     IGCview.ALTWIDTH,
                                     IGCview.ALTHEIGHT);
      window.pack();
      window.show();
    }

  private void view_cursor_data()
    {
      IGCview.cursor_window = new CursorFrame();
      IGCview.cursor_window.pack();
      IGCview.cursor_window.show();
    }

  private void view_flight_data()
    {
      if (IGCview.data_window!=null) return;
      IGCview.data_window = new DataFrame(IGCview.logs[IGCview.primary_index]);
      IGCview.data_window.pack();
      IGCview.data_window.show();
    }

  private void view_flight_analysis()
    {
      if (IGCview.analysis_window!=null) return;
      IGCview.analysis_window = new AnalysisFrame(IGCview.logs[IGCview.primary_index]);
      IGCview.analysis_window.pack();
      IGCview.analysis_window.show();
    }

  private void modify_config()
    {
      if (IGCview.analysis_window!=null) return;
      IGCview.config_window = new ConfigFrame();
      IGCview.config_window.pack();
      IGCview.config_window.show();
    }

  private void save_config()
   {
    FileDialog fd = new FileDialog (map_frame, "Save configuration", FileDialog.SAVE);
    fd.show ();
    String f = fd.getFile ();
    if (f != null) {
      try {
	FileOutputStream s = new FileOutputStream (f);
	ObjectOutputStream o = new ObjectOutputStream (s);
	o.writeObject(IGCview.config);
	o.flush ();
	o.close ();
      }
      catch (Exception e) {System.out.println (e);};
    }
  }

  private void load_config()
    {
      FileDialog fd = new FileDialog (map_frame, "Save configuration", FileDialog.LOAD);
      fd.show ();
      String f = fd.getFile ();
      if (f != null)
        {     
          try
            {
	      FileInputStream s = new FileInputStream (f);
	      ObjectInputStream o = new ObjectInputStream (s);
            IGCview.config_old = IGCview.config;
	      IGCview.config = (Config) o.readObject ();
	      o.close ();
            }
          catch (Exception e)
            {
	        IGCview.config = IGCview.config_old;
              System.out.println (e);
            }
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

  void draw_cursors(Graphics g)
    {
      if (IGCview.cursor1==0) return;
      draw_cursor1(g, IGCview.cursor1);
      if (IGCview.cursor1!=IGCview.cursor2) draw_cursor2(g, IGCview.cursor2);
    }

  void draw_cursor1(Graphics g, int i) // i is index into primary track log
    {
      int x = map_frame.long_to_x(IGCview.logs[IGCview.primary_index].longitude[i]);
      int y = map_frame.lat_to_y(IGCview.logs[IGCview.primary_index].latitude[i]);

      g.setColor(Config.MAPBACKGROUND);
      g.setXORMode(Config.CURSORCOLOUR);
      g.drawLine(x-14,y,x-4,y);
      g.drawLine(x+4,y,x+14,y);
      g.drawLine(x,y-14,x,y-4);
      g.drawLine(x,y+4,x,y+14);
      g.setPaintMode();
    }

  void draw_cursor2(Graphics g, int i) // i is index into primary track log
    {
      int x = map_frame.long_to_x(IGCview.logs[IGCview.primary_index].longitude[i]);
      int y = map_frame.lat_to_y(IGCview.logs[IGCview.primary_index].latitude[i]);

      g.setColor(Config.MAPBACKGROUND);
      g.setXORMode(Config.CURSORCOLOUR);
      g.drawLine(x-10,y-10,x-2,y-2);  // NW
      g.drawLine(x-10,y+10,x-2,y+2);  // SW
      g.drawLine(x+2,y-2,x+10,y-10);  // NE
      g.drawLine(x+2,y+2,x+10,y+10);  // SE
      g.setPaintMode();
    }

  void move_cursor1(int i)
    {
      Graphics g = getGraphics();
      if (IGCview.cursor1!=0) draw_cursor1(g, IGCview.cursor1); // remove old cursor1
      draw_cursor1(g, i);                                       // draw new cursor1
      IGCview.cursor1 = i;
      if (IGCview.cursor_window!=null) IGCview.cursor_window.update();
    }

  void move_cursor2(int i)
    {
      Graphics g = getGraphics();
      if (IGCview.cursor2!=0) draw_cursor2(g, IGCview.cursor2); // remove old cursor2
      draw_cursor2(g, i);                                       // draw new cursor2
      IGCview.cursor2 = i;
      if (IGCview.cursor_window!=null) IGCview.cursor_window.update();
    }

  void maggot_synchro()
    {
	Graphics g = getGraphics();
	racing = true;
      paint(g);
	MaggotRacer m = new MaggotRacer(g,true);
	m.race();
    }

  void maggot_real_time()
    {
	Graphics g = getGraphics();
	racing = true;
      paint(g);
	MaggotRacer m = new MaggotRacer(g,false);
	m.race();
    }

  void mark_plane(Graphics g, int log_num, int x, int y, int h) // draw plane at x,y
                                                                // with stick length h
    {
	Color c = Config.TRACKCOLOUR[Config.FCOLOUR[log_num][0]];
        g.setColor(Config.MAPBACKGROUND);
        g.setXORMode(c);
        if (Config.ALTSTICKS)
          {
            g.drawLine(x,y-h,x,y);
            g.fillOval(x-4,y-h-8,8,8);
            g.setColor(c);
            g.setXORMode(Config.TRACKCOLOUR[Config.FCOLOUR[log_num][1]]);
            g.fillOval(x-2,y-h-6,4,4);
          }
        else
          {
            g.fillOval(x-4,y-4,8,8);
            g.setColor(c);
            g.setXORMode(Config.TRACKCOLOUR[Config.FCOLOUR[log_num][1]]);
            g.fillOval(x-2,y-2,4,4);
          }
    }

  void draw_plane(Graphics g, int log_num, int i) // draw plane at log index i
    { 
      int x = map_frame.long_to_x(IGCview.logs[log_num].longitude[i]);
      int y = map_frame.lat_to_y(IGCview.logs[log_num].latitude[i]);
      int h = (int)(IGCview.logs[log_num].altitude[i] / 200);
      mark_plane(g, log_num, x, y, h);
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
  float climb_avg = (float) 0.0; // DEBUG
  float task_climb_avg = (float) 0.0;
  float task_climb_height = (float) 0.0;
  int   task_climb_time = 0;
  float task_climb_percent = (float) 0.0;
  int   task_time = 0;
  float task_distance = (float) 0.0;
  float task_speed = (float) 0.0;
  float task_cruise_avg = (float) 0.0;

  int   [] tp_index  = new int [IGCview.MAXTPS+1];     // log point at each TP
  int   [] tp_time  = new int [IGCview.MAXTPS+1];      // time rounded each TP
  float [] tp_altitude = new float [IGCview.MAXTPS+1]; // height rounded each TP
  int   [] leg_time = new int [IGCview.MAXTPS];        // duration for each leg
  float [] leg_speed = new float [IGCview.MAXTPS];       // speed for each leg
  float [] leg_climb_avg = new float [IGCview.MAXTPS];      // average climb for each leg
  float [] leg_climb_percent = new float [IGCview.MAXTPS];  // percent time climbing for each leg
  int   [] leg_climb_count = new int [IGCview.MAXTPS];  // percent time climbing for each leg
  float [] leg_cruise_avg = new float [IGCview.MAXTPS];     // average cruise speed for each leg

  int tps_rounded = 0;                                 // TPs successfully rounded
  String name;

  // TrackLog constructor:
  
  TrackLog (String filename, MapFrame f)
  {
    int i,j;
    map_frame = f;
    try { BufferedReader in = new BufferedReader(new FileReader(filename));
          String buf;

          while ((buf = in.readLine()) != null) 
          { if (buf.charAt(0) == 'B')
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
          j = filename.lastIndexOf(".");
          if (j==-1) j = filename.length();
          name = filename.substring(i+1,j);
          in.close();
          IGCview.log_count++;
          if (IGCview.log_count==1)
            IGCview.primary_index = 1;
	  else 
            IGCview.secondary[IGCview.log_count] = true;
	  find_thermals();
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
    g.setColor(Config.TRACKCOLOUR[num % Config.MAXCOLOURS]);

    while (latitude[++i]==(float)0.0) {;} // skip bad gps records
    x1 = map_frame.long_to_x(longitude[i]);
    y1 = map_frame.lat_to_y(latitude[i]);
    for (int j = i+1; j <= record_count; j++)
    {
        if (latitude[j]==(float)0.0) continue;
        x2 = map_frame.long_to_x(longitude[j]);
        y2 = map_frame.lat_to_y(latitude[j]);
        g.drawLine(x1,y1,x2,y2);
        x1 = x2;
        y1 = y2;
    }
  }

  public void draw_alt(AltCanvas canvas, Graphics g, boolean primary)
    {
      if (primary) draw_alt_tp_lines(canvas, g);           // Draw TP lines
      if (!baro_ok) return;
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
      y1 = canvas.get_y(altitude[1]);
      for (int i = 2; i <= record_count; i++)
        {
          x2 = canvas.time_to_x(time[i]);
          y2 = canvas.get_y(altitude[i]);
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
      y1 = canvas.get_y(altitude[1]);
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
          y2 = canvas.get_y(altitude[i]);
          g.drawLine(x1,y1,x2,y2);
          x1 = x2;
          y1 = y2;
        }
    }

  private void draw_alt_tp_lines(GraphCanvas canvas, Graphics g)
    {
      int x, h = canvas.get_y((float) 0), tp_max;
      float tp_dist = (float) 0;

      g.setColor(Config.TPCOLOUR);
      if (tps_rounded==0) return;                     // Draw TP lines
      tp_max = (canvas.x_axis==IGCview.TIMEX) ? tps_rounded : IGCview.task.tp_count;
      for (int tp=1; tp<=tp_max; tp++)
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

  public void draw_climb(GraphCanvas canvas, Graphics g, boolean primary)
    {
      find_thermals();
      if (primary) draw_alt_tp_lines(canvas, g);           // Draw TP lines
      g.setColor(primary ? Config.CLIMBCOLOUR : Config.CLIMBSECCOLOUR);
      if (canvas.x_axis==IGCview.TIMEX)                // Draw alt trace
        draw_time_climb(canvas, g, primary);
      else
        draw_dist_climb(canvas, g, primary);
    }

  public void draw_time_climb(GraphCanvas canvas, Graphics g, boolean primary)
    {
      int x,y,w,h,i;

      h = primary ? 5 : 4;
      for (i=1; i<=thermal_count; i++)
        {
          if (tps_rounded>0)
            if (thermal[i].finish_index < tp_index[1]) continue;  // not yet started task
            else if (tps_rounded==IGCview.task.tp_count &&        // completed task
                     thermal[i].finish_index > tp_index[tps_rounded]) break;
          x = canvas.time_to_x(time[thermal[i].start_index]);
          w = canvas.time_to_x(time[thermal[i].finish_index]) - x;
          y = canvas.get_y(thermal[i].rate())-2;
          g.fillRect(x,y,w,h);
        }
    }

  public void draw_dist_climb(GraphCanvas canvas, Graphics g, boolean primary)
    {
      int x,y,w,h,i,next_tp=1;
      float dist, tp_lat, tp_long, tp_dist = (float) 0.0;

      if (tps_rounded==0) return;
      h = primary ? 5 : 4;
      tp_lat = IGCview.task.tp[1].latitude;
      tp_long = IGCview.task.tp[1].longitude;
      for (i=1; i<=thermal_count; i++)
        {
          if (tps_rounded>0)
            if (thermal[i].finish_index < tp_index[1]) continue;  // not yet started task
            else if (tps_rounded==IGCview.task.tp_count &&        // completed task
                     thermal[i].finish_index > tp_index[tps_rounded]) break;
          if (thermal[i].start_index>=tp_index[next_tp] && next_tp<IGCview.task.tp_count) 
            {
              next_tp++;
              tp_dist += IGCview.task.dist[next_tp];
              tp_lat = IGCview.task.tp[next_tp].latitude;
              tp_long = IGCview.task.tp[next_tp].longitude;
            }
          dist = IGCview.dec_to_dist(latitude[thermal[i].start_index],
                                     longitude[thermal[i].start_index],
                                     tp_lat, tp_long);
          x = canvas.dist_to_x(tp_dist-dist);
          y = canvas.get_y(thermal[i].rate())-2;
          dist = IGCview.dec_to_dist(latitude[thermal[i].finish_index],
                                     longitude[thermal[i].finish_index],
                                     tp_lat, tp_long);
          w = canvas.dist_to_x(tp_dist-dist) - x;
          if (w<0)  // some corrections as you can move backwards while thermalling
            w=-w;
	  else if (w==0) // or you might stand still
            w=1;
          g.fillRect(x,y,w,h);
        }
    }

  public void draw_cruise(GraphCanvas canvas, Graphics g, boolean primary)
    {
      if (!gps_ok) return;
      find_thermals();
      if (primary) draw_alt_tp_lines(canvas, g);           // Draw TP lines
      g.setColor(primary ? Config.CRUISECOLOUR : Config.CRUISESECCOLOUR);
      if (canvas.x_axis==IGCview.TIMEX)                // Draw alt trace
        draw_time_cruise(canvas, g, primary);
      else
        draw_dist_cruise(canvas, g, primary);
    }

  public void draw_time_cruise(GraphCanvas canvas, Graphics g, boolean primary)
    {
      float dist;
      int x,y,w,h,i, i1, i2;

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
          x = canvas.time_to_x(time[i1]);
          w = canvas.time_to_x(time[i2]) - x;
          y = canvas.get_y( dist / (time[i2]-time[i1]) * 3600)-2;
          g.fillRect(x,y,w,h);
        } 
    }

  public void draw_dist_cruise(GraphCanvas canvas, Graphics g, boolean primary)
    {
      int x,y,w,h,i, i1, i2, next_tp=1;
      float dist, tp_lat, tp_long, tp_dist = (float) 0.0;

      if (tps_rounded==0) return;

      h = primary ? 5 : 4;
      tp_lat = IGCview.task.tp[1].latitude;
      tp_long = IGCview.task.tp[1].longitude;
      for (i=1; i<=thermal_count; i++)
        {
          i1 = thermal[i].finish_index;
          i2 = (i<thermal_count) ? thermal[i+1].start_index : record_count; 
          if (tps_rounded>0)
            if (i1 < tp_index[1]) continue;  // not yet started task
            else if (tps_rounded==IGCview.task.tp_count)          // completed task
                   if (i1 > tp_index[tps_rounded]) break;
                   else if (i2 > tp_index[tps_rounded]) i2 = tp_index[tps_rounded];
          if (i1>=tp_index[next_tp] && next_tp<IGCview.task.tp_count) 
            {
              next_tp++;
              tp_dist += IGCview.task.dist[next_tp];
              tp_lat = IGCview.task.tp[next_tp].latitude;
              tp_long = IGCview.task.tp[next_tp].longitude;
            }
          dist = IGCview.dec_to_dist(latitude[i1], longitude[i1], tp_lat, tp_long);
          x = canvas.dist_to_x(tp_dist-dist);
          dist = IGCview.dec_to_dist(latitude[i2], longitude[i2], tp_lat, tp_long);
          w = canvas.dist_to_x(tp_dist-dist) - x;
          if (w<0)  // maybe he's flying AWAY from the TP
            w=-w;
	  else if (w==0) // or somehow in a turn
            w=1;
          dist = IGCview.dec_to_dist(latitude[i1], longitude[i1],
                                     latitude[i2], longitude[i2]);
          y = canvas.get_y( dist / (time[i2]-time[i1]) * 3600)-2;
          g.fillRect(x,y,w,h);
        } 
    }

  public void draw_climb_profile(ClimbProfileCanvas cp_canvas, Graphics g, boolean primary)
    {
      int x,y, i, box;
      Polygon p = new Polygon();
      Color c = primary ? Config.TRACKCOLOUR[Config.PRIMARYCOLOUR] : Config.SECTRACKCOLOUR;

      if (!baro_ok) return;
      find_climb_profile();
      x = cp_canvas.climb_to_x((float) 0);
      y = cp_canvas.get_y((float) 0);
      p.addPoint(x,y);
      y = cp_canvas.get_y(climb_percent[1]);
      p.addPoint(x,y);
      for (box=1; box<=IGCview.CLIMBBOXES; box++)
        { 
          x = cp_canvas.box_to_x(box);
          y = cp_canvas.get_y(climb_percent[box]);
          p.addPoint(x,y);
        }
      x = cp_canvas.box_to_x(IGCview.CLIMBBOXES);
      y = cp_canvas.get_y((float) 0);
      p.addPoint(x,y);
      x = cp_canvas.climb_to_x(climb_avg);
      g.setColor(c);                               // Draw graph
      g.drawPolygon(p);
      if (primary)                                 // draw average climb line
        {
          g.drawLine(x,0,x,cp_canvas.get_y((float) 0));
          g.drawLine(x+1,0,x+1,cp_canvas.get_y((float) 0));
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
        x = map_frame.long_to_x(thermal[i].long1);
        y = map_frame.lat_to_y(thermal[i].lat2);
        w = map_frame.long_to_x(thermal[i].long2) - x;
        h = map_frame.lat_to_y(thermal[i].lat1) - y;
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
      if (!Config.draw_tp_times || !gps_ok) return;
      for (int i = 1; i <= tps_rounded; i++) mark_point(g, Color.yellow, tp_index[i]);
    }

  private void mark_point(Graphics g, Color c, int index)
    { 
      int x, y, w ,h;
      g.setColor(c);
      x = map_frame.long_to_x(longitude[index]) - 3;
      y = map_frame.lat_to_y(latitude[index]) - 3;
      w = 6;
      h = 6;
      g.drawRect(x,y,w,h);
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

      if (tps_rounded!=0 || !gps_ok || IGCview.task==null) return;

      for (i=2; i<=record_count; i++) // scan forwards for all TPs except start
        if (IGCview.task.in_sector(latitude[i], longitude[i], current_tp))
          {
		tp_time[current_tp] = time[i];
		tp_altitude[current_tp] = altitude[i];
            tp_index[current_tp++] = i;
            if (current_tp > IGCview.task.tp_count) break;
          }
      for (i=tp_index[2]; i>1; i--)  // scan backwards from TP2 to find start
        if (IGCview.task.in_sector(latitude[i], longitude[i], 1))
          {
		tp_time[1] = time[i];
		tp_altitude[1] = altitude[i];
            tp_index[1] = i;
            tps_rounded = current_tp-1;
	    find_task_distance();
	    if (tps_rounded==IGCview.task.tp_count)
	      task_time = time[tp_index[tps_rounded]]-time[tp_index[1]];
	    else
	      task_time = time[record_count]-time[tp_index[1]];
	    task_speed = task_distance / task_time * 3600;
            return;
          }
      tps_rounded = 0; // start not found
    }

  void find_task_distance() // returns distance flown around task in nautical miles
    {
      int next_tp=1;
      float dist = (float) 0.0;
      if (tps_rounded==IGCview.task.tp_count)
	{
	  task_distance = IGCview.task.length;
	  return;
	}
      while (next_tp<=tps_rounded) dist += IGCview.task.dist[++next_tp];
      task_distance = dist - IGCview.dec_to_dist(latitude[record_count],
				  	         longitude[record_count],
			 		         IGCview.task.tp[tps_rounded+1].latitude,
					         IGCview.task.tp[tps_rounded+1].longitude);

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

  int time_to_index(int t) // do binary split to convert time to index
    {
      int i=record_count/2, i_min=1, i_max=record_count;
      while (time[i]>t || time[i+1]<t)
        {
          i = (i_min + i_max) / 2;
          if (i_max-i_min<=1) break;
          if (time[i]<t)
            i_min = i;
          else
            i_max = i;
        }
      return i;
    }

  int dist_to_index(float d)
    {
      int next_tp = 1, i, i_min, i_max;
      int index=1;                   // current index with minimum distance
      float index_dist=(float)999,   // value of current minimum distance
            dist = (float) 0.0,
	    tp_dist = (float) 0.0,
            tp_lat,
            tp_long;

      if (IGCview.task==null) return 0;
      while (d>tp_dist && next_tp<IGCview.task.tp_count)
	tp_dist += IGCview.task.dist[++next_tp];
      tp_lat = IGCview.task.tp[next_tp].latitude;
      tp_long = IGCview.task.tp[next_tp].longitude;
      i_min = (next_tp==0) ? 1 : tp_index[next_tp-1];
      i_max = tp_index[next_tp];
      for (i=i_min; i<=i_max; i++)
        {
	  if (latitude[i]==(float)0.0) continue;
	  dist = tp_dist-IGCview.dec_to_dist(latitude[i], longitude[i], tp_lat, tp_long)-d;
          if (dist<0) dist = -dist;
	  if (dist<index_dist)
	    {
	      index = i;
	      index_dist = dist;
            }
        }
      return index;
    }

  void move_cursor1_time(int t)
    {
      int i = time_to_index(t);
      while (i<record_count && latitude[i]==(float)0.0) i++; // skip bad gps records
      map_frame.canvas.move_cursor1(i);
    }

  void move_cursor2_time(int t)
    {
      int i = time_to_index(t);
      while (i<record_count && latitude[i]==(float)0.0) i++; // skip bad gps records
      map_frame.canvas.move_cursor2(i);
    }

  void move_cursor1_dist(float d)
    {
      int i = dist_to_index(d);
      map_frame.canvas.move_cursor1(i);
    }

  void move_cursor2_dist(float d)
    {
      int i = dist_to_index(d);
      map_frame.canvas.move_cursor2(i);
    }

  void calc_flight_data()
    {
      int i;
      find_thermals();
      find_tp_times();
      for (i=1; i<tps_rounded; i++)
	{
	  leg_time[i] = time[tp_index[i+1]]-time[tp_index[i]];
	  leg_speed[i] = IGCview.task.dist[i+1] / leg_time[i] * 3600;
	}
      calc_leg_climb_data();
      calc_leg_cruise_data();
    }

  void calc_leg_climb_data()  // calculate leg_average_climb[i], leg_climb_percent[i],
                              // task_climb_height, task_climb_avg, task_climb_time, task_climb_percent
    {
      int leg=1;
      float leg_climb_height = (float) 0.0;
      int leg_climb_time = 0;

      task_climb_height = (float) 0.0;
      task_climb_time = 0;
	leg_climb_count[1] = 0;
      for (int i=1; i<=thermal_count+1; i++)
	{
	  if (i<=thermal_count && thermal[i].finish_index<tp_index[leg]) continue;
	  if (i==thermal_count+1 || thermal[i].start_index>tp_index[leg+1])
	    {
	      leg_climb_avg[leg] = leg_climb_height / leg_climb_time * (float) 0.6;
	      leg_climb_percent[leg] = (float) leg_climb_time / leg_time[leg] * 100;
  	      leg_climb_height = (float) 0.0;
	      leg_climb_time = 0;
	      if (++leg==tps_rounded)
	        break;
		leg_climb_count[leg] = 0;
	    }
	  task_climb_height += thermal[i].height();
	  task_climb_time += thermal[i].duration();
	  leg_climb_height += thermal[i].height();
	  leg_climb_time += thermal[i].duration();
	  leg_climb_count[leg]++;
	}
      task_climb_avg = task_climb_height / task_climb_time * (float) 0.6;
      task_climb_percent = (float) task_climb_time / task_time * 100;
    }

  void calc_leg_cruise_data() {};
     
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
      // TOP LINE
      c.gridx = 1;
      make_label("Trigraph:", gridbag, c);            //-- LABEL: "Trigraphs:"
      c.gridx = GridBagConstraints.RELATIVE;
      c.weightx = 1.0;
      make_label("Full name:                   ", gridbag, c);  //-- LABEL: "Full name:"
      c.weightx = 0.0;
      make_label("Distance:", gridbag, c);            //-- LABEL: "Distance:"
      make_label("Track:", gridbag, c);               //-- LABEL: "Track:"
      make_label("Bisector:", gridbag, c);            //-- LABEL: "Bisector:"
      c.weightx = 1.0;
      c.gridwidth = GridBagConstraints.REMAINDER;
      make_label("Search:", gridbag, c);             //-- LABEL: "Search:"
      // SECOND LINE
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

  void init()
    {
      tp_trigraph[1].requestFocus();
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
	  map_frame.canvas.paint(map_frame.canvas.getGraphics());
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
               {
                 tp_index[current_trigraph] = 0;
                 if (upper_key.equals("")) full_name = "";
               }
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
      task.length = length / IGCview.NMKM;
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
          tp_dist[i].setText(IGCview.places(dist[i],1));
          tp_track[i].setText(IGCview.places(track[i],0));
          length = length + dist[i];
          tp_length.setText(IGCview.places(length,1));
          lat1 = lat2;
          long1 = long2;
        }
      for (int i=2; i<IGCview.MAXTPS; i++)   // calculate bisectors
        {
          if (tp_index[i+1] == 0) break;
          bisector[i] = calc_bisector(track[i], track[i+1]);
          tp_bisector[i].setText(IGCview.places(bisector[i],0));
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
}

//***************************************************************************//
//            Task                                                           //
//***************************************************************************//

class Task
{
  int tp_count = 0;
  TP [] tp = new TP [IGCview.MAXTPS+1];
  float [] dist = new float [IGCview.MAXTPS+1]; // length of leg in nautical miles
  float [] track = new float [IGCview.MAXTPS+1];
  float [] bisector = new float [IGCview.MAXTPS+1];
  float length = 0; // length of task in nautical miles

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
      x1 = map_frame.long_to_x(tp[1].longitude);
      y1 = map_frame.lat_to_y(tp[1].latitude);
      for (int i = 2; i <= tp_count; i++)
        {
          x2 = map_frame.long_to_x(tp[i].longitude);
          y2 = map_frame.lat_to_y(tp[i].latitude);
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

  void zoom_to_task()   // zoom such that map displays task
    {
	float lat_min, long_min, lat_max, long_max, tp_lat, tp_long;

	if (IGCview.task==null) return;

	lat_min = IGCview.task.tp[1].latitude;       // accumulate task bounding box
	long_min = IGCview.task.tp[1].longitude;
	lat_max = lat_min+(float)0.1;
	long_max = long_min+(float)0.1;
	for (int tp=2; tp<=IGCview.task.tp_count; tp++)
	  {
	    tp_lat = IGCview.task.tp[tp].latitude;
	    tp_long = IGCview.task.tp[tp].longitude;
 	    if (tp_lat<lat_min)
		lat_min = tp_lat;
 	    else if (tp_lat>lat_max)
		lat_max = tp_lat;
 	    if (tp_long<long_min)
		long_min = tp_long;
 	    else if (tp_long>long_max)
		long_max = tp_long;
        }
      xscale = (float) IGCview.map_frame.width / (long_max-long_min) * (float) 0.45;
      yscale = (float) IGCview.map_frame.height / (lat_max-lat_min) * (float) 0.45;
      latitude = lat_max + (float) 50 / yscale;
      longitude = long_min - (float) 50 / xscale;
      current = 1;
      in_limit = 1;
      out_limit = 1;
      x_hist[1] = xscale;
      y_hist[1] = yscale;
      lat_hist[1] = latitude;
      long_hist[1] = longitude;
    }

  void zoom_out()
    {
      if (current == out_limit)
        {
          scale(latitude+(float)300/yscale, longitude-(float)300/xscale, OUTRATIO);
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
      return height() / duration() * (float) 0.6;
    }

  float height()
    {
      return track_log.altitude[finish_index]-track_log.altitude[start_index];
    }
}

//***************************************************************************//
//             GraphFrame                                                    //
//***************************************************************************//

class GraphFrame extends Frame {

  int graph_type;
  GraphCanvas canvas;

  TrackLog log;

  private String [] [] menus = {
    {"File", "Close", "Exit"},
    {"X-axis", "Time", "Distance", "Expand", "Compress"}
  };    

  private static String [] [] cp_menu = {
    {"File", "Close", "Exit"},
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
      set_canvas();
      pane.add(canvas);

      if (graph_type==IGCview.GRAPHCP) menus=cp_menu;
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
              i.addActionListener(canvas);
  	    }
        }
      }
      this.pack ();
      pane.doLayout();
      pane.setScrollPosition(0,pane.getVAdjustable().getMaximum());
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

  void set_canvas()
    {
      switch (graph_type)
        {
          case IGCview.GRAPHALT:
                 canvas = new AltCanvas (this, 6*width, 3*height, 
                                             IGCview.MAXALT, 5000, 1000);
                 break;
	  case IGCview.GRAPHCLIMB:
                 canvas = new ClimbCanvas (this, 6*width, 2*height,
                                             IGCview.MAXCLIMB, 5, 1);
                 break;
          case IGCview.GRAPHCRUISE :
                 canvas = new CruiseCanvas (this, 6*width, 2*height,
                                             IGCview.MAXCRUISE, 50, 10);
                 break;
          case IGCview.GRAPHCP :
                 canvas = new ClimbProfileCanvas (this, 2*width, 2*height);
                 break;
        }      

    }

  public Dimension getPreferredSize () {
    return new Dimension (width, height);
  };

}

//***************************************************************************//
//            GraphCanvas                                                    //
//***************************************************************************//

class GraphCanvas extends Canvas implements ActionListener,
					    MouseListener,
                                            MouseMotionListener
{
  int width, height;
  GraphFrame frame;
  float xscale = IGCview.TIMEXSCALE, yscale;
  int x_axis = IGCview.TIMEX;
  int  max_y, major_incr_y, minor_incr_y;

  int cursor1_x = 0, cursor1_time = 0;                     // current cursor position
  float cursor1_dist = (float) 0.0;
  int cursor2_x = 0, cursor2_time = 0;                     // current cursor position
  float cursor2_dist = (float) 0.0;
  
  public Dimension getPreferredSize () 
    {
      return new Dimension (width, height);
    }

  public void paint (Graphics g)
    { Dimension d = getSize();
      g.setColor(Color.white);
      g.fillRect(0,0, d.width - 1, d.height - 1);
      draw_grid(g);                                        // draw grid
      cursor1_x = (x_axis==IGCview.TIMEX) ? time_to_x(cursor1_time) : dist_to_x(cursor1_dist);
      cursor2_x = (x_axis==IGCview.TIMEX) ? time_to_x(cursor2_time) : dist_to_x(cursor2_dist);
      draw_cursors(g); // draw cursors
    }

  public void actionPerformed (ActionEvent e)
    {
      String s = e.getActionCommand ();
      if (s.equals ("close")) frame.dispose();
      else if (s.equals ("exit")) System.exit (0);
      else if (s.equals ("time")) set_time_x_axis();
      else if (s.equals ("distance")) set_dist_x_axis();
      else if (s.equals ("expand")) { xscale *= 2; this.repaint(); }
      else if (s.equals ("compress")) { xscale /= 2; this.repaint(); }
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
      for (int i=0; i<max_y; i+=major_incr_y)  // draw horizontal lines
        {
          line_y = get_y(i);
          g.drawLine(0, line_y, width, line_y);
          g.drawLine(0, line_y+1, width, line_y+1); // double lines for major increments
	  for (int j=minor_incr_y; j<major_incr_y; j+=minor_incr_y)
            {
              line_y = get_y(i+j);
              g.drawLine(0, line_y, width, line_y);
            }
        }
      if (x_axis==IGCview.TIMEX)    // draw vertical lines
        draw_time_contours(g);
      else
        draw_dist_contours(g);
      g.setColor(Color.black);
      line_y = get_y((float) 0);           // draw baseline
      g.drawLine(0, line_y, width, line_y);
      g.drawLine(0, line_y+1, width, line_y+1);
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

  public int get_y(float y_value)
    {
      return height - (int)(y_value * yscale) - IGCview.BASELINE;
    }

  public int x_to_time(int x)
    {
      return (int) ((float) x / xscale  + IGCview.time_start);
    } 

  public float x_to_dist(int x)
    {
      return ((float) x / xscale - IGCview.DISTSTART);
    } 

  // MOUSE EVENT HANDLERS: MouseListener

  public void mousePressed (MouseEvent e)
    {
      int time;
      float dist;
      int x = e.getX();
      Graphics g = getGraphics();
      draw_cursor(g, cursor1_x); // erase previous cursors
      if (cursor2_x!=cursor1_x) draw_cursor(g, cursor2_x); 
      cursor1_x = x;
      cursor2_x = x+1;
      draw_cursor(g, cursor1_x); // draw new cursors
      draw_cursor(g, cursor2_x); 
      if (x_axis==IGCview.TIMEX)
        {
          cursor1_time = x_to_time(x);
          cursor2_time = cursor1_time;
          frame.log.move_cursor1_time(cursor1_time);
          frame.log.move_cursor2_time(cursor2_time);
        }
      else
        {
	  cursor1_dist = x_to_dist(x);
	  cursor2_dist = cursor1_dist;
	  frame.log.move_cursor1_dist(cursor1_dist);
	  frame.log.move_cursor2_dist(cursor2_dist);
        }
    }

  public void mouseReleased (MouseEvent e) {};
  public void mouseClicked(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {};
  public void mouseExited(MouseEvent e) {};

  // MOUSE EVENT HANDLERS: MouseMotionListener

  public void mouseDragged (MouseEvent e)
    {
      int time;
      float dist;
      int x = e.getX();
      Graphics g = getGraphics();
      if (cursor2_x!=cursor1_x) draw_cursor(g, cursor2_x); // erase previous cursor2
      if (x!=cursor1_x)  draw_cursor(g, x);                // draw new cursor2
      cursor2_x = x;
      if (x_axis==IGCview.TIMEX)
        {
          cursor2_time = x_to_time(x);
          frame.log.move_cursor2_time(cursor2_time);
        }
      else
        {
	  cursor2_dist = x_to_dist(x);
	  frame.log.move_cursor2_dist(cursor2_dist);
        }
    }
 
  public void mouseMoved(MouseEvent e) {};


  void draw_cursors(Graphics g)
    {
      draw_cursor(g, cursor1_x);
      if (cursor1_x!=cursor2_x) draw_cursor(g, cursor2_x);
    }

  void draw_cursor(Graphics g, int x)
    {
      g.setColor(Color.white);
      g.setXORMode(Config.CURSORCOLOUR);
      g.drawLine(x,0,x,height); // draw new cursor
      g.setPaintMode();
    }

}

//***************************************************************************//
//            AltCanvas                                                      //
//***************************************************************************//

class AltCanvas extends GraphCanvas
{
  AltCanvas (GraphFrame frame, int width, int height,
               int max_y, int major_incr_y, int minor_incr_y)
    {
      this.frame = frame;
      this.width = width;
      this.height = height;
      this.max_y = max_y;
      this.major_incr_y = major_incr_y;
      this.minor_incr_y = minor_incr_y;
      yscale = (float) (height-IGCview.BASELINE) / max_y;
      this.addMouseListener (this);
      this.addMouseMotionListener (this);
    }

  public void paint (Graphics g)
    { 
      super.paint(g);
      frame.log.draw_alt(this, g, true);         // draw primary
      IGCview.sec_log.draw_alt(this,g);              // draw secondaries
    }
}

//***************************************************************************//
//            ClimbCanvas                                                    //
//***************************************************************************//

class ClimbCanvas extends GraphCanvas
{
  ClimbCanvas (GraphFrame frame, int width, int height,
               int max_y, int major_incr_y, int minor_incr_y)
    {
      this.frame = frame;
      this.width = width;
      this.height = height;
      this.max_y = max_y;
      this.major_incr_y = major_incr_y;
      this.minor_incr_y = minor_incr_y;
      yscale = (float) (height-IGCview.BASELINE) / max_y;
      this.addMouseListener (this);
      this.addMouseMotionListener (this);
    }

  public void paint (Graphics g)
    { 
      super.paint(g);
      IGCview.sec_log.draw_climb(this, g);
      frame.log.draw_climb(this, g, true);
    }
}

//***************************************************************************//
//            CruiseCanvas                                                   //
//***************************************************************************//

class CruiseCanvas extends GraphCanvas
{
  CruiseCanvas (GraphFrame frame, int width, int height,
                int max_y, int major_incr_y, int minor_incr_y)
    {
      this.frame = frame;
      this.width = width;
      this.height = height;
      this.max_y = max_y;
      this.major_incr_y = major_incr_y;
      this.minor_incr_y = minor_incr_y;
      yscale = (float) (height-IGCview.BASELINE) / max_y;
      this.addMouseListener (this);
      this.addMouseMotionListener (this);
    }

  public void paint (Graphics g)
    {
      super.paint(g);
      IGCview.sec_log.draw_cruise(this,g);           // draw secondaries
      frame.log.draw_cruise(this, g, true);   // draw primary
    }
}

//***************************************************************************//
//            ClimbProfileCanvas                                             //
//***************************************************************************//

class ClimbProfileCanvas extends GraphCanvas implements ActionListener
{
  ClimbProfileCanvas (GraphFrame frame, int width, int height)
    {
      this.frame = frame;
      this.width = width;
      this.height = height;
      xscale = ((float) (width - IGCview.BASELINE)) / IGCview.CLIMBBOXES;
                                                         // xscale in pixels/box
      yscale = ((float) (height - IGCview.BASELINE)) / IGCview.MAXPERCENT;
                                                         // yscale in pixels/(%time)
    }

  public void paint (Graphics g)
    {
      super.paint(g);
      IGCview.sec_log.draw_climb_profile(this,g);       // draw secondary
      frame.log.draw_climb_profile(this, g, true);      // draw primary
    }

  public void actionPerformed (ActionEvent e)
    {
      String s = e.getActionCommand ();
      if (s.equals ("close")) frame.dispose();
      else if (s.equals ("exit")) System.exit (0);
    }

  void draw_grid(Graphics g)
    {
      int line_x, line_y;

      g.setColor(Color.black);
      for (int i=10; i<=IGCview.MAXPERCENT; i+=10) // draw %time contours
        {
          line_y = get_y((float) i);
          g.drawLine(0, line_y, width, line_y);
        }
                                  // draw vertical grid lines
      for (float climb=0; climb<=IGCview.CLIMBBOX*IGCview.CLIMBBOXES; climb++)
        {
          line_x = climb_to_x(climb);
          g.drawLine(line_x, 0, line_x, height);
        }
      line_y = get_y((float) 0);           // draw baseline
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
}

//***************************************************************************//
//             SelectFrame                                                   //
//***************************************************************************//

class SelectFrame extends Frame {

  SelectWindow window;
  MapFrame frame;

  private static String [] [] menus = {
    {"File", "Close", "Exit"}
  };    

  private int width, height;

  SelectFrame (MapFrame f, int width, int height)
    {
      setSize(width, height);
      frame = f;
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
	String flags;

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
	    flags = (IGCview.logs[i].baro_ok) ? "B" : "";
	    if (IGCview.logs[i].gps_ok) flags = flags+"G";
          secondary[i] = new Checkbox(flags);
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
	  select_frame.frame.canvas.paint(select_frame.frame.canvas.getGraphics());
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
  float task_climb_avg = (float) 0.0;
  float task_climb_height = (float) 0.0;
  int   task_climb_time = 0;
  float task_climb_percent = (float) 0.0;
  int   task_time = 0;
  float task_distance = (float) 0.0;
  float task_speed = (float) 0.0;
  float task_cruise_avg = (float) 0.0;

  int   [] tp_time  = new int [IGCview.MAXTPS+1];      // time rounded each TP
  float [] tp_altitude  = new float [IGCview.MAXTPS+1]; // height rounded each TP
  int   [] leg_time = new int [IGCview.MAXTPS];        // duration for each leg
  float [] leg_speed = new float [IGCview.MAXTPS];       // speed for each leg
  float [] leg_climb_avg = new float [IGCview.MAXTPS];      // average climb for each leg
  float [] leg_climb_percent = new float [IGCview.MAXTPS];  // percent time climbing for each leg
  int   [] leg_climb_count = new int [IGCview.MAXTPS];  // percent time climbing for each leg
  float [] leg_cruise_avg = new float [IGCview.MAXTPS];     // average cruise speed for each leg

  int tps_rounded = 0;                                 // TPs successfully rounded

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
      y = cp_canvas.get_y((float) 0);
      p.addPoint(x,y);
      y = cp_canvas.get_y(climb_percent[1]);
      p.addPoint(x,y);
      for (box=1; box<=IGCview.CLIMBBOXES; box++)
        { 
          x = cp_canvas.box_to_x(box);
          y = cp_canvas.get_y(climb_percent[box]);
          p.addPoint(x,y);
        }
      x = cp_canvas.box_to_x(IGCview.CLIMBBOXES);
      y = cp_canvas.get_y((float) 0);
      p.addPoint(x,y);
      x = cp_canvas.climb_to_x(climb_avg);
      g.setColor(Config.SECAVGCOLOUR);                           // Fill graph
      g.fillPolygon(p);
      g.drawLine(x,0,x,cp_canvas.get_y((float) 0));  // Line for avg climb
      g.drawLine(x+1,0,x+1,cp_canvas.get_y((float) 0));
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

  void calc_flight_data()
    {
	int i,log_num;
	TrackLog log;
	int count_task_climb_avg = 0,
	    count_task_climb_height = 0,
	    count_task_climb_time = 0,
	    count_task_climb_percent = 0,
	    count_task_time = 0,
	    count_task_distance = 0,
	    count_task_speed = 0,
	    count_task_cruise_avg = 0;
	int [] count_tp_time = new int [IGCview.MAXTPS+1];
	int [] count_tp_altitude = new int [IGCview.MAXTPS+1];
	int [] count_leg_time = new int [IGCview.MAXTPS];
	int [] count_leg_speed = new int [IGCview.MAXTPS];
      int [] count_leg_climb_avg = new int [IGCview.MAXTPS];
	int [] count_leg_climb_percent = new int [IGCview.MAXTPS];
      int [] count_leg_climb_count = new int [IGCview.MAXTPS];
	int [] count_leg_cruise_avg = new int [IGCview.MAXTPS];

	task_climb_avg = (float) 0.0;
	task_climb_height = (float) 0.0;
	task_climb_time = 0;
	task_climb_percent = (float) 0.0;
	task_time = 0;
	task_distance = (float) 0.0;
	task_speed = (float) 0.0;
	task_cruise_avg = (float) 0.0;

	for (i=1; i<=IGCview.MAXTPS; i++)
        {
	    count_tp_time[i] = 0;
          tp_time[i] = 0;
	    count_tp_altitude[i] = 0;
          tp_altitude[i] = 0;
        }
	for (i=1; i<IGCview.MAXTPS; i++)
	  {
	    leg_time[i] = 0;
	    leg_speed[i] = (float) 0.0;
          leg_climb_avg[i] = (float) 0.0;
	    leg_climb_percent[i] = (float) 0.0;
          leg_climb_count[i] = 0;
	    leg_cruise_avg[i] = (float) 0.0;	    
	    count_leg_time[i] = 0;
	    count_leg_speed[i] = 0;
          count_leg_climb_avg[i] = 0;
	    count_leg_climb_percent[i] = 0;
          count_leg_climb_count[i] = 0;
	    count_leg_cruise_avg[i] = 0;
	  }
	
      for (log_num=1; log_num<=IGCview.MAXLOGS; log_num++)
        if (IGCview.secondary[log_num])
	    {
		log = IGCview.logs[log_num];
		if (!log.gps_ok) continue; // skip log if no gps
		log.calc_flight_data();
		if (log.task_climb_avg!=(float)0.0)
		  {
                task_climb_avg += log.task_climb_avg;
		    count_task_climb_avg++;
              }
		if (log.task_climb_height!=(float)0.0)
		  {
                task_climb_height += log.task_climb_height;
		    count_task_climb_height++;
              }
		if (log.task_climb_time!=0)
		  {
                task_climb_time += log.task_climb_time;
		    count_task_climb_time++;
              }
		if (log.task_climb_percent!=(float)0.0)
		  {
                task_climb_percent += log.task_climb_percent;
		    count_task_climb_percent++;
              }
		if (log.task_time!=0)
		  {
                task_time += log.task_time;
		    count_task_time++;
              }
		if (log.task_distance!=(float)0.0)
		  {
                task_distance += log.task_distance;
		    count_task_distance++;
              }
		if (log.task_speed!=(float)0.0)
		  {
                task_speed += log.task_speed;
		    count_task_speed++;
              }
		if (log.task_cruise_avg!=(float)0.0)
		  {
                task_cruise_avg += log.task_cruise_avg;
		    count_task_cruise_avg++;
              }
   	      for (i=1; i<=IGCview.MAXTPS; i++)
		  {
		    count_tp_time[i]++;
		    tp_time[i] += log.tp_time[i];
		    if (log.baro_ok)
                  {
		        count_tp_altitude[i]++;
		        tp_altitude[i] += log.tp_altitude[i];
                  }
              }
	      for (i=1; i<IGCview.MAXTPS; i++)
	        {
	          if (log.tps_rounded>i)
			{
			  count_leg_time[i]++;
			  leg_time[i] += log.leg_time[i];
			  count_leg_speed[i]++;
			  leg_speed[i] += log.leg_speed[i];
			  count_leg_climb_count[i]++;
			  leg_climb_count[i] += log.leg_climb_count[i];
			  count_leg_cruise_avg[i]++;
			  leg_cruise_avg[i] += log.leg_cruise_avg[i];
			  count_leg_climb_percent[i]++;
			  leg_climb_percent[i] += log.leg_climb_percent[i];
                  }
	          if (log.tps_rounded>i && log.task_climb_avg!=(float)0.0)
			{
			  count_leg_climb_avg[i]++;
			  leg_climb_avg[i] += log.leg_climb_avg[i];
                  }
	        }
	    }
	if (count_task_climb_avg!=0) task_climb_avg /= count_task_climb_avg;
	if (count_task_climb_height!=0) task_climb_height /= count_task_climb_height;
	if (count_task_climb_time!=0) task_climb_time /= count_task_climb_time;
	if (count_task_climb_percent!=0) task_climb_percent /= count_task_climb_percent;
	if (count_task_time!=0) task_time /= count_task_time;
	if (count_task_distance!=0) task_distance /= count_task_distance;
	if (count_task_speed!=0) task_speed /= count_task_speed;
	if (count_task_cruise_avg!=0) task_cruise_avg /= count_task_cruise_avg;
      for (i=1; i<=IGCview.MAXTPS; i++)
        {
	    if (count_tp_time[i]!=0) tp_time[i] /= count_tp_time[i];
	    if (count_tp_altitude[i]!=0) tp_altitude[i] /= count_tp_altitude[i];
        }    
	for (i=1; i<IGCview.MAXTPS; i++)
	  {
	    if (count_leg_time[i]!=0) leg_time[i] /= count_leg_time[i];
	    if (count_leg_speed[i]!=0) leg_speed[i] /= count_leg_speed[i];
	    if (count_leg_climb_avg[i]!=0) leg_climb_avg[i] /= count_leg_climb_avg[i];
	    if (count_leg_climb_percent[i]!=0) leg_climb_percent[i] /= count_leg_climb_percent[i];
	    if (count_leg_climb_count[i]!=0) leg_climb_count[i] /= count_leg_climb_count[i];
	    if (count_leg_cruise_avg[i]!=0) leg_cruise_avg[i] /= count_leg_cruise_avg[i];
	  }
    }
}

//***************************************************************************//
//            CursorFrame                                                    //
//***************************************************************************//

class CursorFrame extends Frame {

  CursorWindow window;

  private static String [] [] menus = {
    {"File", "Close", "Exit"}
  };    

  private int width, height;

  CursorFrame ()
    {
      this.setTitle("Cursor Data");
      setSize(Config.CURSORWIDTH, Config.CURSORHEIGHT);
      this.width = Config.CURSORWIDTH;
      this.height = Config.CURSORHEIGHT;
      ScrollPane pane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
      this.add (pane, "Center");
      window = new CursorWindow(this);
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

  void update()
    {
      window.update();
    }
}

//***************************************************************************//
//            CursorWindow                                                   //
//***************************************************************************//

class CursorWindow extends Panel implements ActionListener
{
  private CursorFrame frame;              // frame to draw on

  private GridBagLayout gridbag;
  private GridBagConstraints c;

  private Label time_heading = new Label("Time");
  private int time1=0;
  private Label time1_label = new Label("00:00:00");
  private int time2=0;
  private Label time2_label = new Label("00:00:00");
  private int time_diff=0;
  private Label time_diff_label = new Label("00:00:00");

  private Label alt_heading = new Label("Alt");
  private float alt1=(float)0.0;
  private Label alt1_label = new Label("00000");
  private float alt2=(float)0.0;
  private Label alt2_label = new Label("00000");
  private float alt_diff=(float)0.0;
  private Label alt_diff_label = new Label("00000");

  private Label climb_heading = new Label("Climb");
  private float climb=(float)0.0;
  private Label climb_label = new Label("00.0");

  private Label dist_heading = new Label("Dist");
  private float dist=(float)0.0;
  private Label dist_label = new Label("000.0");

  private Label speed_knots_heading = new Label("Speed (kts)");
  private float speed_knots =(float)0.0;
  private Label speed_knots_label = new Label("000.0");

  private Label speed_kmh_heading = new Label("Speed (kmh)");
  private Label speed_kmh_label = new Label("000.0");

  private Label l_d_heading = new Label("L/D");
  private float l_d=(float)0.0;
  private Label l_d_label = new Label("000.0");


  protected void l_add(Label l)
    {
        gridbag.setConstraints(l, c);
        add(l);
    }

  CursorWindow(CursorFrame frame)
    {
      this.frame = frame;
      gridbag = new GridBagLayout();
      c = new GridBagConstraints();
      setFont(new Font("Helvetica", Font.PLAIN, 14));
      setLayout(gridbag);
      c.fill = GridBagConstraints.HORIZONTAL;
      time_heading.setAlignment(Label.LEFT);
      l_add(time_heading);
      time1_label.setAlignment(Label.RIGHT);
      l_add(time1_label);
      time2_label.setAlignment(Label.RIGHT);
      l_add(time2_label);
      c.gridwidth = GridBagConstraints.REMAINDER;
      time_diff_label.setAlignment(Label.RIGHT);
      l_add(time_diff_label);

      c.gridwidth = 1;
      alt_heading.setAlignment(Label.LEFT);
      l_add(alt_heading);
      alt1_label.setAlignment(Label.RIGHT);
      l_add(alt1_label);
      alt2_label.setAlignment(Label.RIGHT);
      l_add(alt2_label);
      c.gridwidth = GridBagConstraints.REMAINDER;
      alt_diff_label.setAlignment(Label.RIGHT);
      l_add(alt_diff_label);

      c.gridwidth = 1;
      climb_heading.setAlignment(Label.LEFT);
      l_add(climb_heading);
      c.gridwidth = GridBagConstraints.REMAINDER;
      climb_label.setAlignment(Label.RIGHT);
      l_add(climb_label);

      c.gridwidth = 1;
      dist_heading.setAlignment(Label.LEFT);
      l_add(dist_heading);
      c.gridwidth = GridBagConstraints.REMAINDER;
      dist_label.setAlignment(Label.RIGHT);
      l_add(dist_label);

      c.gridwidth = 1;
      speed_knots_heading.setAlignment(Label.LEFT);
      l_add(speed_knots_heading);
      c.gridwidth = GridBagConstraints.REMAINDER;
      speed_knots_label.setAlignment(Label.RIGHT);
      l_add(speed_knots_label);

      c.gridwidth = 1;
      speed_kmh_heading.setAlignment(Label.LEFT);
      l_add(speed_kmh_heading);
      c.gridwidth = GridBagConstraints.REMAINDER;
      speed_kmh_label.setAlignment(Label.RIGHT);
      l_add(speed_kmh_label);

      c.gridwidth = 1;
      l_d_heading.setAlignment(Label.LEFT);
      l_add(l_d_heading);
      c.gridwidth = GridBagConstraints.REMAINDER;
      l_d_label.setAlignment(Label.RIGHT);
      l_add(l_d_label);
    }

  public void actionPerformed(ActionEvent e)
    {
      String label = e.getActionCommand();
      if (label.equals ("close"))
        { IGCview.cursor_window=null; 
          frame.dispose();
          return;
	}
      else if (label.equals ("exit"))
        System.exit (0);
      System.out.println("Unknown action event " + e);
    }

  public void update()
    {
      calc_values();
      time1_label.setText(IGCview.format_clock(time1));
      time2_label.setText(IGCview.format_clock(time2));
      time_diff_label.setText(IGCview.format_time(time_diff));
      alt1_label.setText(IGCview.places(alt1,0));
      alt2_label.setText(IGCview.places(alt2,0));
      alt_diff_label.setText(IGCview.places(alt_diff,0));
      climb_label.setText(IGCview.places(climb,1));
      dist_label.setText(IGCview.places(dist*Config.convert_task_dist,1));
      speed_knots_label.setText(IGCview.places(speed_knots,1));
      speed_kmh_label.setText(IGCview.places(speed_knots*IGCview.NMKM,1));
      l_d_label.setText(IGCview.places(l_d,1));
    }

  void calc_values()
    {
      TrackLog l = IGCview.logs[IGCview.primary_index];
      int i1 = IGCview.cursor1, i2 = IGCview.cursor2;

      if (IGCview.primary_index==0) return;
      time1 = l.time[i1];
      time2 = l.time[i2];
      time_diff = (time1>time2) ? time1-time2 : time2-time1;
      alt1 = l.altitude[i1];
      alt2 = l.altitude[i2];
      alt_diff = (alt1>alt2) ? alt1-alt2 : alt2-alt1;
      dist = IGCview.dec_to_dist(l.latitude[i1], l.longitude[i1],
				 l.latitude[i2], l.longitude[i2]);
      if (i1==i2)
	{
	  climb = (float)0.0;
	  speed_knots = (float)0.0;
	  l_d = (float)0.0;
	}
      else
	{
          climb = alt_diff / time_diff * (float) 0.6;
          speed_knots = dist / time_diff * 3600;
          l_d = dist * 6080 / alt_diff;
	}
    }

}

//***************************************************************************//
//            AnalysisFrame                                                  //
//***************************************************************************//

class AnalysisFrame extends Frame implements ActionListener
{

  private static String [] [] menus = {
    {"File", "Close", "Exit"}
  };    

  int width, height;
  TextArea text = new TextArea();

  TrackLog log;

  AnalysisFrame() {};

  AnalysisFrame (TrackLog log)
    {
      setTitle("Flight Analysis: "+log.name);
      this.log = log;
      setSize(Config.ANALYSISWIDTH, Config.ANALYSISHEIGHT);
      this.width = Config.ANALYSISWIDTH;
      this.height = Config.ANALYSISHEIGHT;
      initialise();
    }

  void initialise()
    {
      text.setEditable(false);
      add(text);

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
	      i.addActionListener (this);
  	    }
        }
      }
      refresh();
      this.pack ();
    }

  public Dimension getPreferredSize ()
    {
      return new Dimension (width, height);
    }

  public void actionPerformed(ActionEvent e)
    {
      String label = e.getActionCommand();
      if (label.equals ("close"))
        { dispose();
	  IGCview.analysis_window = null;
          return;
	}
      else if (label.equals ("exit"))
        System.exit (0);
      System.out.println("Unknown action event " + e);
    }

  void refresh()
    {
      for (int i=1; i<25; i++) write(String.valueOf(i)+"...\n");
    }

  void write(String s)
    {
      text.append(s);
    }
}

//***************************************************************************//
//            DataFrame                                                      //
//***************************************************************************//

class DataFrame extends AnalysisFrame
{
  TrackLogS s = IGCview.sec_log;

  DataFrame (TrackLog log)
    {
	String title = "Flight Data: "+log.name+" (vs. ";
	boolean no_secondaries = true;
	for (int i=1; i<=IGCview.MAXLOGS; i++)
	  if (IGCview.secondary[i])
          {
            title = title + IGCview.logs[i].name + ", ";
            no_secondaries = false;
          }
	title = no_secondaries ? 
			title.substring(0,title.length()-5) : 
			title.substring(0,title.length()-2)+")";
      setTitle(title);
      this.log = log;
      setSize(Config.DATAWIDTH, Config.DATAHEIGHT);
      this.width = Config.DATAWIDTH;
      this.height = Config.DATAHEIGHT;
      initialise();
    }

  public void actionPerformed(ActionEvent e)
    {
      String label = e.getActionCommand();
      if (label.equals ("close"))
        { dispose();
	  IGCview.data_window = null;
          return;
	}
      else if (label.equals ("exit"))
        System.exit (0);
      System.out.println("Unknown action event " + e);
    }

  void refresh()
    {
      if (IGCview.task==null)
        {
          text.append("No task defined.\n");
          return;
        }
	else if (!log.gps_ok)
        {
          text.append("Primary log ("+log.name+") has no GPS data.\n");
          return;
        }
      log.calc_flight_data();
	IGCview.sec_log.calc_flight_data();
      print_task_data();
      print_tp_data();
      print_leg_data();
    }

  void print_task_data()
    {
      write("\nTask:\t");
      for (int i=1;i<IGCview.task.tp_count; i++) write(IGCview.task.tp[i].trigraph+" - ");
      write(IGCview.task.tp[IGCview.task.tp_count].trigraph + "  " +
            IGCview.places(IGCview.task.length*IGCview.NMKM,1) + "km\n\n");
      write("Start\t\t\tFinish\t\t\tTask\n");
      write("Time:\t\t\tTime:\t\t\tTime:\n");
      write(IGCview.format_clock(log.tp_time[1])+"\t"+
            "("+IGCview.format_clock(s.tp_time[1])+")\t"+
	    IGCview.format_clock(log.tp_time[IGCview.task.tp_count])+"\t"+
            "("+IGCview.format_clock(s.tp_time[IGCview.task.tp_count])+")\t"+
            IGCview.format_time(log.task_time)+"\t"+
            "("+IGCview.format_time(s.task_time)+")\n\n");
	write("Speed\t\tAvgClimb\t\t%Climb\t\tAvgCruise\n");
	write("(kmh)\t\t(knots)\t\t\t\t(knots)\n");
      write(IGCview.places(log.task_speed*IGCview.NMKM,1)+"\t"+
            "("+IGCview.places(s.task_speed*IGCview.NMKM,1)+")\t"+
            IGCview.places(log.task_climb_avg,1)+"\t"+
            "("+IGCview.places(s.task_climb_avg,1)+")\t"+
            IGCview.places(log.task_climb_percent,0)+"%\t"+
            "("+IGCview.places(s.task_climb_percent,0)+"%)\t"+
            IGCview.places(log.task_cruise_avg,1)+"\t"+
            "("+IGCview.places(s.task_cruise_avg,1)+")\n\n");
    }

  void print_tp_data()
    {
      int i;
      write("TP Information:\n\n");
      for (i=1; i<=IGCview.task.tp_count; i++)
        write("\t"+IGCview.task.tp[i].trigraph+"\t\t");
      write("\n");
      write("Time:\t");
      for (i=1; i<=IGCview.task.tp_count; i++)
	  write(IGCview.format_clock(log.tp_time[i])+"\t"+
              "("+IGCview.format_clock(s.tp_time[i])+")\t");
      write("\n");
      write("Height:\t");
      for (i=1; i<=IGCview.task.tp_count; i++)
  	  write(IGCview.places(log.tp_altitude[i],0)+"\t"+
              "("+IGCview.places(s.tp_altitude[i],0)+")\t\t");
      write("\n\n");
    }

  void print_leg_data()
    {
      int leg;
      write("Task Leg Information:\n\n");
      for (leg=1; leg<IGCview.task.tp_count; leg++)
        write("\t\t"+IGCview.task.tp[leg].trigraph+"-"+IGCview.task.tp[leg+1].trigraph+"\t");
      write("\n");
      write("Time:\t\t");
      for (leg=1; leg<IGCview.task.tp_count; leg++)
        write(IGCview.format_time(log.leg_time[leg])+"\t"+
              "("+IGCview.format_time(s.leg_time[leg])+")\t");
      write("\n");
      write("Speed(kmh):\t");
      for (leg=1; leg<IGCview.task.tp_count; leg++)
        write(IGCview.places(log.leg_speed[leg]*IGCview.NMKM,1)+"\t"+
              "("+IGCview.places(s.leg_speed[leg]*IGCview.NMKM,1)+")\t\t");
      write("\n");
      write("AvgClimb(knots):\t");
      for (leg=1; leg<IGCview.task.tp_count; leg++)
        write(IGCview.places(log.leg_climb_avg[leg],1)+"\t"+
              "("+IGCview.places(s.leg_climb_avg[leg],1)+")\t\t");
      write("\n");
      write("%Climb:\t\t");
      for (leg=1; leg<IGCview.task.tp_count; leg++)
        write(IGCview.places(log.leg_climb_percent[leg],0)+"%\t"+
              "("+IGCview.places(s.leg_climb_percent[leg],0)+"%)\t\t");
      write("\n");
      write("#Climbs:\t\t");
      for (leg=1; leg<IGCview.task.tp_count; leg++)
        write(String.valueOf(log.leg_climb_count[leg])+"\t"+
              "("+String.valueOf(s.leg_climb_count[leg])+")\t\t");
      write("\n");
      write("AvgCruise(knots):\t");
      for (leg=1; leg<IGCview.task.tp_count; leg++)
        write(IGCview.places(log.leg_cruise_avg[leg],1)+"\t"+
              "("+IGCview.places(s.leg_cruise_avg[leg],1)+")\t\t");
      write("\n");
    }
}

//***************************************************************************//
//            MaggotRacer                                                    //
//***************************************************************************//

class MaggotRacer extends Thread
{
  static Graphics g;
  static boolean synchro_start;
  static DrawingCanvas c = IGCview.map_frame.canvas;
  static int [] time_offset = new int [IGCview.MAXLOGS+1]; // time offsets for synchro
  int first_log_start=90000, first_task_start=90000;
  static int [] index = new int [IGCview.MAXLOGS+1]; // index of current point in log

  MaggotRacer(Graphics g, boolean synchro_start)
    {
	int i, legend_y = 10;
	TrackLog log;
      this.g = g;
      this.synchro_start = synchro_start;
      for (i=1; i<=IGCview.log_count; i++)
	{
	  if (i==IGCview.primary_index || IGCview.secondary[i])
	      {
	    	log = IGCview.logs[i];
	        time_offset[i] = 0;
		index[i]=1;
		c.mark_plane(g,i,5,legend_y,0);
		g.setColor(Color.black);
		g.setPaintMode();
		g.drawString(log.name, 10, legend_y+4);
		legend_y += 12;
	    	if (log.time[1]<first_log_start) first_log_start = log.time[1];
	    	if (log.tps_rounded>0 && log.tp_time[1]<first_task_start)
		  first_task_start = log.tp_time[1];
	      }
	}
      if (synchro_start)
	for (i=1; i<=IGCview.log_count; i++)
	  {
	    if (i==IGCview.primary_index || IGCview.secondary[i])
	      {
		log = IGCview.logs[i];
		if (log.tps_rounded>0) time_offset[i] = log.tp_time[1] - first_task_start;
              }
          }
      System.out.println("Log start: "+IGCview.format_clock(first_log_start)+
                         ". Task start: "+IGCview.format_clock(first_task_start));
    }

  void race()
    {
      if (synchro_start)
	  race_loop(first_task_start - 1200);  // start maggot race start - 20mins
	else
	  race_loop(first_log_start);
    }

  void race_loop(int start_time)
    {
	int time = start_time;
	boolean cont = true; // continue flag
	TrackLog log;

	while (cont)
	  {
	    try {sleep(Config.RACEPAUSE);} catch (InterruptedException e){};
	    cont = false;
	    for (int i=1; i<=IGCview.log_count; i++)
		{
	    	  if (i==IGCview.primary_index || IGCview.secondary[i])
		    {
			c.draw_plane(g,i,index[i]); // erase old plane
		  	log = IGCview.logs[i];
			while (index[i]<=log.record_count &&
                         log.time[index[i]]-time_offset[i]<=time) {index[i]++;};
			if (index[i]<log.record_count) cont = true;
			c.draw_plane(g,i,index[i]); // draw new plane
		    }
		}
  	    time += IGCview.RACE_TIME_INCREMENT;
	  }
	System.out.println("Race over");
    }
}

//***************************************************************************//
//             ConfigFrame                                                   //
//***************************************************************************//

class ConfigFrame extends Frame {

  ConfigWindow window;

  private static String [] [] menus = {
    {"File", "Close", "Exit"}
  };    

  private int width, height;

  ConfigFrame ()
    {
      width = Config.CONFIGWIDTH;
      height = Config.CONFIGHEIGHT;
      setSize(width, height);
      ScrollPane pane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
      this.add (pane, "Center");
      window = new ConfigWindow(this);
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
//            ConfigWindow                                                   //
//***************************************************************************//

class ConfigWindow extends Panel implements ActionListener
{
  // MAP CONFIG
  private Checkbox draw_thermals = new Checkbox("Box around thermals");
  private Checkbox draw_thermal_ends = new Checkbox("Mark thermal entry/exit");
  private Checkbox draw_tp_times = new Checkbox("Mark sector entry");
  
  private TextField start_xscale = new TextField(String.valueOf(Config.start_xscale));

  private TextField start_latitude = new TextField(String.valueOf(Config.start_latitude));

  private TextField start_longitude = new TextField(String.valueOf(Config.start_longitude));

  // WINDOWS
  private TextField CURSORWIDTH = new TextField(String.valueOf(Config.CURSORWIDTH));
  private TextField CURSORHEIGHT = new TextField(String.valueOf(Config.CURSORHEIGHT));
  private TextField ANALYSISWIDTH = new TextField(String.valueOf(Config.ANALYSISWIDTH));
  private TextField ANALYSISHEIGHT = new TextField(String.valueOf(Config.ANALYSISHEIGHT));
  private TextField DATAWIDTH = new TextField(String.valueOf(Config.DATAWIDTH));
  private TextField DATAHEIGHT = new TextField(String.valueOf(Config.DATAHEIGHT));

  // COLOURS
  private ColourChoice MAPBACKGROUND = new ColourChoice();
  private Checkbox [] MAPBACKGROUND_shade;

  private ColourChoice CURSORCOLOUR = new ColourChoice();
  private Checkbox [] CURSORCOLOUR_shade;

  private ColourChoice SECAVGCOLOUR = new ColourChoice();
  private Checkbox [] SECAVGCOLOUR_shade;

  private ColourChoice ALTCOLOUR = new ColourChoice();
  private Checkbox [] ALTCOLOUR_shade;

  private ColourChoice ALTSECCOLOUR = new ColourChoice();
  private Checkbox [] ALTSECCOLOUR_shade;

  private ColourChoice TPCOLOUR = new ColourChoice();
  private Checkbox [] TPCOLOUR_shade;

  private ColourChoice TASKCOLOUR = new ColourChoice();
  private Checkbox [] TASKCOLOUR_shade;

  private ColourChoice CLIMBCOLOUR = new ColourChoice();
  private Checkbox [] CLIMBCOLOUR_shade;

  private ColourChoice CLIMBSECCOLOUR = new ColourChoice();
  private Checkbox [] CLIMBSECCOLOUR_shade;

  private ColourChoice CRUISECOLOUR = new ColourChoice();
  private Checkbox [] CRUISECOLOUR_shade;

  private ColourChoice CRUISESECCOLOUR = new ColourChoice();
  private Checkbox [] CRUISESECCOLOUR_shade;

  // FILES
  private TextField TPFILE = new TextField(String.valueOf(Config.TPFILE));

  private TextField LOGDIR = new TextField(String.valueOf(Config.LOGDIR));

  // MAGGOT RACE PARAMETERS
  private TextField RACEPAUSE = new TextField(String.valueOf(Config.RACEPAUSE));

  private Checkbox ALTSTICKS = new Checkbox("Altitude sticks below planes");

  // UNIT CONVERSIONS
  private CheckboxGroup convert_task_dist_group = new CheckboxGroup();
  private Checkbox convert_task_dist_km = new Checkbox("Task distance in km", false,convert_task_dist_group);
  private Checkbox convert_task_dist_nm = new Checkbox("Task distance in nautical miles",
                                                       false, convert_task_dist_group);
  private Checkbox convert_task_dist_mi = new Checkbox("Task distance in statute miles",
                                                       false, convert_task_dist_group);

  private TextField time_offset = new TextField(String.valueOf(Config.time_offset));
  private Label time_offset_label = new Label("Local time offset from UTC");

  private Button ok_button, cancel_button;
  private ConfigFrame config_frame;              // frame to draw on

  GridBagLayout gridbag;
  GridBagConstraints c;

  Button make_button(String name)
    {
      Button b = new Button(name);
      gridbag.setConstraints(b, c);
      b.addActionListener(this);
      add(b);
      return b;
    }

  void set_text(TextField t, String s)
    {
	c.gridx = 3;
      Label l = new Label(s);
      c.gridwidth = GridBagConstraints.RELATIVE;
      gridbag.setConstraints(t, c);
      t.addActionListener(this);
      //t.addFocusListener(this);
      add(t);
	c.gridx = GridBagConstraints.RELATIVE;
      c.gridwidth = GridBagConstraints.REMAINDER;
      gridbag.setConstraints(l,c);
      add(l);
    }

  void set_checkbox(Checkbox cb)
    {
	c.gridx = 3;
      c.gridwidth = GridBagConstraints.REMAINDER;
      gridbag.setConstraints(cb, c);
      add(cb);
    }

  void set_new_line()
    {
      Label l = new Label("");
      c.gridwidth = GridBagConstraints.REMAINDER;
      gridbag.setConstraints(l,c);
      add(l);
    }

  void set_colour_headings()
    {
      Label l;
      l = new Label("Light");
      c.gridx = 1;
      c.gridwidth = 1;
      gridbag.setConstraints(l, c);
      add(l);
      l = new Label("Dark");
      c.gridx = 3;
      c.gridwidth = GridBagConstraints.REMAINDER;
      gridbag.setConstraints(l,c);
      add(l);
    }

  Checkbox [] set_colour_choice(ColourChoice colours, String s)
    {
      CheckboxGroup shade_group = new CheckboxGroup();
      Checkbox [] shade = { new Checkbox("",false,shade_group),
                            new Checkbox("",true,shade_group),
                            new Checkbox("",false,shade_group)};
      Label l = new Label(s);

      c.gridx = GridBagConstraints.RELATIVE;
      c.gridwidth = 1;
      gridbag.setConstraints(colours.choice, c);
      add(colours.choice);
	c.gridx = GridBagConstraints.RELATIVE;
      gridbag.setConstraints(shade[0],c);
      add(shade[0]);
      gridbag.setConstraints(shade[1],c);
      add(shade[1]);
      c.gridwidth = GridBagConstraints.RELATIVE;
      gridbag.setConstraints(shade[2],c);
      add(shade[2]);
      c.gridwidth = GridBagConstraints.REMAINDER;
      gridbag.setConstraints(l,c);
      add(l);
      return shade;
    }

  ConfigWindow(ConfigFrame config_frame)
    {
      this.config_frame = config_frame;
 
      gridbag = new GridBagLayout();
      c = new GridBagConstraints();
 
      setFont(new Font("Helvetica", Font.PLAIN, 14));
      setLayout(gridbag);
   
      c.insets = new Insets(3,3,3,3);                 // space 3 pixels around fields
      c.weightx = 0.0;
	c.anchor = GridBagConstraints.WEST;

      set_checkbox(draw_thermals);
      set_checkbox(draw_thermal_ends);
      set_checkbox(draw_tp_times);
      set_checkbox(ALTSTICKS);

      set_text(start_xscale, "Startup scale (pixels/degree) e.g. 200");
      set_text(start_latitude, "Startup latitude e.g. 53.5 = 53 deg 30 mins N");
      set_text(start_longitude, "Startup longitude e.g. -1.1 = 1 deg 6 mins W");
      set_text(CURSORWIDTH, "Width of cursor window (e.g. 330 pixels)");
      set_text(CURSORHEIGHT, "Height of cursor window (e.g. 400 pixels)");
      set_text(ANALYSISWIDTH, "Width of Flight Analysis window e.g. 400");
      set_text(ANALYSISHEIGHT, "Height of Flight Analysis window e.g. 600");
      set_text(DATAWIDTH, "Width of Flight Data window e.g. 780");
      set_text(DATAHEIGHT, "Height of Flight Data window e.g. 500");
      set_text(TPFILE, "Turnpoint file, e.g. bga.gdn");
      set_text(LOGDIR, "Log file directory, e.g. c:\\logfiles\\");
      set_text(RACEPAUSE, "Pause (milliseconds) between maggot race updates e.g. 100");

      set_new_line();
      set_colour_headings();
      MAPBACKGROUND_shade = set_colour_choice(MAPBACKGROUND, "Map background");
      CURSORCOLOUR_shade = set_colour_choice(CURSORCOLOUR, "Map cross-hair cursors");
      SECAVGCOLOUR_shade = set_colour_choice(SECAVGCOLOUR, "Secondary in Climb Profile");
      ALTCOLOUR_shade = set_colour_choice(ALTCOLOUR, "Primary in Altitude window");
      ALTSECCOLOUR_shade = set_colour_choice(ALTSECCOLOUR, "Secondary in Altitude window");
      TPCOLOUR_shade = set_colour_choice(TPCOLOUR, "Vertical lines in Alt/Climb/Cruise windows marking TPs");
      TASKCOLOUR_shade = set_colour_choice(TASKCOLOUR, "Task drawn on map");
      CLIMBCOLOUR_shade = set_colour_choice(CLIMBCOLOUR, "Primary climb bars in Climb window");
      CLIMBSECCOLOUR_shade = set_colour_choice(CLIMBSECCOLOUR, "Secondary climb bars in Climb window");
      CRUISECOLOUR_shade = set_colour_choice(CRUISECOLOUR, "Primary cruise bars in Cruise window");
      CRUISESECCOLOUR_shade = set_colour_choice(CRUISESECCOLOUR, "Secondary cruise bars in Cruise window");

      c.weightx = 0.0;
      c.gridwidth = GridBagConstraints.RELATIVE;
      cancel_button = make_button("Cancel");  //-- BUTTON: Cancel
      ok_button = make_button("OK");       //-- BUTTON: OK
    }

  public void actionPerformed(ActionEvent e)
    {
      String label = e.getActionCommand();
      if (label.equals("OK"))
        { 
          ok_hit();
          config_frame.dispose();
          return;
        }
      else if (label.equals("Cancel"))
  	  {
          config_frame.dispose();
          return;
        }
      else if (label.equals ("close")) { config_frame.dispose(); return;}
      else if (label.equals ("exit")) System.exit (0);
      System.out.println("Unknown action event " + e);
    }

  void ok_hit()
    { 
      int i;
    }
}

//***************************************************************************//
//            ColourChoice                                                   //
//***************************************************************************//


class ColourChoice implements ItemListener 
{
  Choice choice; //pop-up list of choices

  ColourChoice()
    {
      choice = new Choice();
      choice.addItem("blue");
      choice.addItem("red");
      choice.addItem("white");
      choice.addItem("cyan");
      choice.addItem("magenta");
      choice.addItem("black");
      choice.addItem("pink");
      choice.addItem("yellow");
      choice.addItem("green");
      choice.addItem("orange");

      choice.addItemListener(this);
    }

  int selected()
    {
      return choice.getSelectedIndex();
    }

  public void itemStateChanged(ItemEvent e) {};

}
