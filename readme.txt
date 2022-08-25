Return-path: <ian.lewis@cl.cam.ac.uk>
Envelope-to: Ian.Lewis@cl.cam.ac.uk,
 Ian.Lewis@cl.cam.ac.uk
Delivery-date: Fri, 23 Jan 1998 10:34:40 +0000
Received: from ouse.cl.cam.ac.uk (cl.cam.ac.uk) [128.232.33.87] (ijl20)
	by heaton.cl.cam.ac.uk with esmtp (Exim 1.736 #3)
	id 0xvgRb-0002Qb-00; Fri, 23 Jan 1998 10:34:39 +0000
To: hwl@multiweb.nl, la1087@hotmail.com
cc: Ian Lewis <Ian.Lewis@cl.cam.ac.uk>, Ian.Lewis@cl.cam.ac.uk
Subject: Re: Garmin Flight Recorder 
In-reply-to: Your message of "Thu, 22 Jan 1998 18:09:46 +0100."
             <34C77D59.DE89EC3@multiweb.nl> 
Date: Fri, 23 Jan 1998 10:34:36 +0000
From: Ian Lewis <Ian.Lewis@cl.cam.ac.uk>
Message-Id: <E0xvgRb-0002Qb-00@heaton.cl.cam.ac.uk>


Thanks for sending the latest NL TP file.  If I'd seen it before leaving for
home yesterday afternoon I could have saved myself an hour... I loaded your
tracklog files and looked at the tracks to guess the task that had been
assigned, but TP 141 in the file is so far adrift that the flights don't
show as rounding the turnpoint.  Best thing to do is rename your gardown file
"nthrlnds.gdn" and put it in the same directory as my program (IGCview)
and the program will pick up the corrected version.  I've already zipped up
the bad version (with my corrected 141x).

I've put my compiled program and source in:

  http://www.cl.cam.ac.uk/~ijl20/files/IGCVIEW.ZIP

(These notes took longer than I thought... about time I wrote some
  documentation...)

To install:

Create a directory (e.g. igcview) to hold the files, unzip the .ZIP file
into that directory, "cd" to that directory and in a DOS window type
"java IGCview" to start the program.  Don't worry about the DOS messages.

The program uses two data files: "nthrlnds.gdn" which is the TP file, and
"IGCview.cfg" which is a (binary) configuration file.  The TP's are not
loaded UNTIL you define a task, and then you'll see the TP's displayed on the
map.  The "IGCview.cfg" file is loaded when you start the program.  If
you delete it the program just uses defaults, in which the map
is initially centered on Cambridge UK.  When the program starts up, the
map screen is initially blank, which might be a bit disconcerting.

My suggestion to get started is:

  Start the program with "java IGCview".

  With the "Task" menu option, define the task pertinent to the logs you're
  going to load.  The TP keys automatically capitalise, and select from the
  TP file the first TP which starts with the string you've input, so to 
  select TP 141 type "141" but if you want TP "14" enter "14<space>".  Hit
  enter after each one, and the input moves to the next field.  The top
  right field is a <search> field - enter a string in that field and hit
  enter, and the TP's containing that string are displayed in the list box.
  Enter CAPITALS in the search field.  If you click on a TP in the list box
  it will be injected into whichever TP entry field last had focus. 
  Alternatively you can just type the key into the TP entry field.
  I have some minor work to do to get
  the initial focus on the panel to appear in the first TP field.  The
  task entry field shows the leg and total distances, and the tracks
  and bisectors.

  In general, if the task doesn't display on the map (it should do for you as
  I've initialised the coordinates) you can then "Zoom to Task" which will
  center the task on the map.

  Then you can load your tracklog files ("Track" menuitem) and you'll see them
  displayed from above (2D map view), overlaying the task.
  I'd recommend you initially load only two log files. If you drag the
  mouse on the map from the top-left-corner to botton-right-corner of
  the area you're interested in, the map will zoom in to those coordinates.
  To zoom out (or reset the zoom) use the menu options.  If you accidentally
  drag the mouse on a blank area of the map, the program will zoom into that
  area and you won't see *anything* on the map, so just zoom out again or
  'zoom reset'.

  The first loaded track is your "primary" track, displayed in blue.  The
  other loaded tracks are considered "secondary" tracks.  You can pick
  a different loaded log to be the "primary" under the "Track" "Select"
  menu sequence.  In general I'd suggest loading all the tracks you're
  interested in, with you own loaded first.  If you want you can later
  change the primary to someone else if you're interested.

  If you open
  an "Altitude" window (under the "Window" menuitem), you will see the
  baro trace of the primary track, with the secondaries in green.  If you
  click the mouse on the "Altitude" graph, a "cursor info" window will open,
  giving you the data at the cursor position defined by the mouse click.  If
  you *drag* the mouse on the "Altitude" graph, the program defines one
  cursor where you first pressed the mouse button, and a moving second cursor
  wherever you're dragging the mouse to.  If you look carefully at the map
  screen you'll see the lat-long positions corresponding to the cursor
  positions are synchronised on the map also.  So, if you want to examine
  a thermal, drag the mouse from the start of the thermal (on the Altitude
  graph) to the peak of the climb, and the "Cursor Info" window will show
  the height gain, climb rate etc.  On the altitude window you can use
  the menuitem "X-axis" "Distance" to convert the x-axis to distance around
  the task (so long as one has been defined).  If you have more than one
  log loaded, you'll notice that the "time" display obviously has them
  shifted in time, while the "distance" display lines them up.  The
  x-axis can be 'expanded' (doubled) or 'compressed' (halved) as much as
  you like, and the "expanded" "distance" view gives an interesting
  sideways view of the flight.  I'm still working on the scroll bars though.

  You may notice on the map screen that all the thermals during the task
  have been recognised and highlighted.  There are two more 'graph'
  windows with "time" or "distance" on the x-axis, namely "Climb" and
  "Cruise".  The first shows your true avg climb rates in each thermal, 
   while the second shows the avg. cruise speed between thermals.  You
  can see where your competitors were getting better climbs than you, and
  if they were cruising faster/slower.

  The last graphical window is the "Climb Profile" which shows the
  percentage of time spent climbing at different strengths.  The
  divisions along the x-axis are 1 knot climb rates (i.e. approx .5m/s)
  and I haven't got round to converting the units to metric.
  The thick vertical blue line gives the overall average. If you
  have a number of flights loaded, the primary is shown in blue and
  the secondaries are *averaged* to give the green graph.  If you only select
  one secondary then you can compare your flight directly with one other.

  The "Flight Data" window is a text window giving comparative information
  about your flight versus the secondaries.  It would be easiest to view this
  panel first with only one secondary flight, as then the values in
  brackets will be exact for that secondary flight.  Otherwise the
  secondary information in brackets is an average of all the
  secondaries selected.  When you've understood this and have got the
  hang of the "Track"-"Select Tracks" menu option on the main map
  window you will be able to load many logs and change between them.

  On the main map window menu and the "Altitude" menu there is an option
  "Replay".  This gives a "maggot-race" rerun of the logs.  "Synchro start"
  synchronises the logs to a common start time, while "Real time" just
  uses tha absolute time in the logs, so you see people starting at
  different times.  I haven't yet provided a way of interrupting the
  maggot race so you have to wait until it completes.  Selecting
  "Replay" "Cancel" gets you back the normal tracklog map display mode
  (so does the "Track" "Select" menu option).

  Finally (more-or-less), the "File" "Configuration" menuitem allows
  you to customise the program.  When the window opens it is initialised
  with the current program parameters, and you can change them (e.g.
  map background colour) and then select "Use Current Settings".  If
  you want to save the current configuration (*after* you have
  selected "Use Current Settings" is you want to save the ones you put
  into the window), you use the "Save Configuration" menuitem. IF you 
  save the configuration as "IGCview.cfg", that configuration is
  automatically loaded at startup.  My advice is save your config as
  something else first if you want to experiment.

  This program is very much still in development... I await your
  feedback with interest.  Let me know when you get it loaded.

Ian.



  
