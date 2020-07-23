

public class frameStats {

///////////////////////////////////////////////////////////
  // Feathering Support via boolean flag array.
  // Flag=true  Background pixel we wish to be transparent
  // Flag=false Foreground pixel we may have to feather
  ///////////////////////////////////////////////////////////
  protected int                 featherSize     = 2;  // Half the size of the feather box
  private   int                 bgMatchRange    = 4;  // Pixel RGB = BG color +/- bgMatchRange to be a bg pixel
  private   int                 fgMatchRange    = 30; // Pixel RGB = BG color distance of at least fgMatchRange
  protected boolean             enhanced = false;     // Enhanced subject aware dynamic range

  protected boolean bgFlagActive[][]    = null; // Pixel is unambiguously a background pixel (frame coordinates)
  protected boolean fgFlagActive[][]    = null; // Pixel is unambiguously a foreground pixel (frame coordinates)
  protected boolean bgFlagNormal[][]    = null; // Pixel is unambiguously a background pixel (frame coordinates)
  protected boolean fgFlagNormal[][]    = null; // Pixel is unambiguously a foreground pixel (frame coordinates)
  protected boolean bgFlagEnhanced[][]  = null; // Pixel is unambiguously a background pixel (frame coordinates)
  protected boolean fgFlagEnhanced[][]  = null; // Pixel is unambiguously a foreground pixel (frame coordinates)
  private   int     subjWidth           = 0;    // Subject width
  private   int     subjCol             = 0;    // Subject location

  protected int     featherArea       = (1 +featherSize +  featherSize) * (1 + featherSize + featherSize);
//  protected boolean featherBox[][]    = null; // Bounding box around selected pixel



  protected void createFrameFgFlagArrays(int width, int height) {
//    System.out.println("TransformVideo.createFrameFgFlagArray() " + width + " " + height);
    bgFlagNormal    = new boolean[width][height];  // Frame coordinates
    fgFlagNormal    = new boolean[width][height];
    bgFlagEnhanced  = new boolean[width][height];
    fgFlagEnhanced  = new boolean[width][height];
    makeActiveNormal();
//    featherBox = new boolean[1+featherSize+featherSize][1+featherSize+featherSize]; // Not actually used at this point
  }

  public void makeActiveNormal() {
    bgFlagActive    = bgFlagNormal;
    fgFlagActive    = fgFlagNormal;
    enhanced        = false;
  }

  public void makeActiveEnhanced() {
    bgFlagActive    = bgFlagEnhanced;
    fgFlagActive    = fgFlagEnhanced;
    enhanced        = true;
  }


//  int FgInRow=0;
//  int FgInCol=0;
//  int BgInRow=0;
//  int BgInCol=0;


  public void setFeatherSize(int newRange) {
    if (newRange > 0)
      featherSize = newRange;
  }


  public void setFgMatchRange(int newRange) {
    if (newRange > 0)
      fgMatchRange = newRange;
  }

  public void setBgMatchRange(int newRange) {
    if (newRange > 0)
      bgMatchRange = newRange;
  }

  public int getBgMatchRange(int col) { // Range varies with x
    if (enhanced) {
      int distanceFromSubject = Math.abs(col - subjCol);

      return bgMatchRange + ((distanceFromSubject)/subjWidth);
    }
    else
      return bgMatchRange;
  }


  public int getFgMatchRange(int col) { // Range varies with x
    if (enhanced) {
      int distanceFromSubject = Math.abs(col - subjCol);

      return fgMatchRange+ ((distanceFromSubject)/subjWidth);
    }
    else
      return fgMatchRange;
  }


  protected void setBgFlag(int x, int y,    // Frame coordinates
                           boolean flag) {  // true = is a BG pixel color
    if (bgFlagActive != null)
      bgFlagActive[x][y]=flag;
  }


  protected void setFgFlag(int x, int y,    // Frame coordinates
                           boolean flag) {  // true == is a FG pixel color
    if (fgFlagActive != null)
      fgFlagActive[x][y]=flag;
  }


  protected boolean getFgFlag(int x, int y) { // Frame coordinates
    if (fgFlagActive == null)
      return false;
    else
      return fgFlagActive[x][y];
  }


  protected boolean getBgFlag(int x, int y) {   // Frame coordinates
    if (bgFlagActive == null)
      return false;
    else
      return bgFlagActive[x][y];
  }


  protected boolean isFg(int x, int y) {        // Frame coordinates
    return getFgFlag(x, y);
  }


  protected boolean isBg(int x, int y) {        // Frame coordinates
    return getBgFlag(x, y);
  }





  // Generate statistics for # of Fg and Bg pixels in the current
  // column and row.  Intended to be used to help remove unwanted noise.

  private int rowStatsBg[] = null;
  private int rowStatsFg[] = null;
  private int colStatsBg[] = null;
  private int colStatsFg[] = null;

  public void populateFeatherRowCol(int w, int h) {
    if (rowStatsBg==null) {      // Allocate arrays for holding frame statistics.
      rowStatsBg = new int[h];  // Same arrays can be used for ALL frames.
      rowStatsFg = new int[h];
      colStatsBg = new int[w];
      colStatsFg = new int[w];
    }

//    FgInRow=FgInCol=BgInRow=BgInCol=0;

    for (int row=0; row<h; row++) {
      rowStatsBg[row]=rowStatsFg[row]=0;

      for (int x = 0; x < w; x++) { // Walk the row
        if (isBg(x, row))
          rowStatsBg[row]++;

        if (isFg(x, row))
          rowStatsFg[row]++;
      } // for x
    } // for row

    for (int col=0; col<w; col++) {
      colStatsBg[col] = colStatsFg[col] = 0;

      for (int y = 0; y < h; y++) { // Walk the row
        if (isBg(col, y))
          colStatsBg[col]++;

        if (isFg(col, y))
          colStatsFg[col]++;
      } // for y
    } // for col
  } // populateFeatherRowCol()


  private int bgInRow(int row) {
    return rowStatsBg[row];
  }   // Not thread safe yet
  private int fgInRow(int row) {
    return rowStatsFg[row];
  }
  private int bgInCol(int col) {
    return colStatsBg[col];
  }
  private int fgInCol(int col) { return colStatsFg[col]; }

  public void findSubject(int w) {
    int fgPeak  = 0;
    subjCol     = 0;

    // STEP 1: Find the peak where the subject is

    for (int col = 0; col < w; col++) {
      if (fgInCol(col) > fgPeak) {
        fgPeak = fgInCol(col);
        subjCol = col;
      }

//      System.out.println("findSubject(" + col + ":" + w + ")  " + bgInCol(col) + ":" + fgInCol(col));
    } // for

    // STEP 2: Find the width of the subject by figuring out when the stats
    //         drops to .25 of the peak.

    int fgPeakHalf = fgPeak / 4;
    int fgPeakHalfCol = 0;


    for (int col = subjCol + 1; col < w; col++) {
      if (fgInCol(col) < fgPeakHalf) {
        fgPeakHalfCol = col;
        break;
      }
    } // for

    subjWidth = fgPeakHalfCol - subjCol;
    System.out.println("findSubject(SUBJECT) subjCol: " + subjCol + "  fgPeak: " + fgPeak + "  fgPeakHalfCol: " + fgPeakHalfCol + "  subjectWidth: " + subjWidth);
  } // findSubject()


  public void dumpColStatsInFow(int w) {
//    int fgPeak=0;
//    int fgPeakCol=0;

    // STEP 1: Find the peak where the subject is

    for (int col=0; col < w; col++) {
//      if (fgInCol(col) > fgPeak) {
//        fgPeak = fgInCol(col);
//        fgPeakCol=col;
//      }

      System.out.println("dumpColStatsInRow(" + col + ":" + w + ")  " + bgInCol(col) + ":" + fgInCol(col));
    } // for

    // STEP 2: Find the width of the subject by figuring out when the peak
    //         drops to .25 of the peak.

//    int fgPeakHalf=fgPeak/4;
//    int fgPeakHalfCol=0;
//    int subjWidth = 0;  // Subject width
//
//    for (int col=fgPeakCol+1; col < w; col++) {
//      if (fgInCol(col) < fgPeakHalf) {
//        fgPeakHalfCol = col;
//        break;
//      }
//    } // for
//
//    subjWidth = fgPeakHalfCol - fgPeakCol;
//    System.out.println("dumpColStatsInRow(SUBJECT)  col: " + fgPeakCol + "  fgPeak: " + fgPeak + "  fgPeakHalfCol: " + fgPeakHalfCol + "  subjectWidth: " + subjWidth);
  }


  boolean muteRow(int x, int y) {                           // Not thread safe yet
    if (isFg(x,y))
      return false;                          // Don't mute Fg pixel
    else if (bgInRow(y) > 100 && fgInRow(y) <20)
      return true;
    else
      return false;
  }

  boolean muteCol(int x, int y) {                           // Not thread safe yet
    if (isFg(x,y))
      return false;                          // Don't mute Fg pixel
    else if (bgInCol(x) > 100 && fgInCol(x) <20)
      return true;
    else
      return false;
  }


  protected void populateFeatherBox(featherBoxStats fbs,      // Thread safe because it operates on a thread unique fbs
                                    int             w,        // Frame width  needed if box exceeds frame dimensions at the edge
                                    int             h,        // Frame Height needed if box exceeds frame dimensions at the edge
                                    int             xcenter,  // Center of the box in frame coordinates
                                    int             ycenter,
                                    boolean         enableLogging) {
    int flagx;      // x & y adjusted by sanity checks to that we do not
    int flagy;      // index into feather box out of bounds.
    fbs.clear();    // Number of Bg & Fb pixes in the feather box
    fbs.setLogging(enableLogging);

    for (int ybox=-featherSize; ybox<=featherSize; ybox++) {      // Loop box coordinates
      flagy = ycenter + ybox;     // Translate box coordinates to flag array coordinates
      if (flagy < 0) flagy=0;     // Lower sanity check
      if (flagy >= h) flagy=h-1;  // Upper sanity check

      for (int xbox=-featherSize; xbox <= featherSize; xbox++) {  // Loop box coordinates
        flagx=xcenter + xbox;     // Translate box coordinates to flag array coordinates
        if (flagx < 0) flagx=0;   // Lower sanity check
        if (flagx >= w) flagx=w-1;// Upper sanity check

        if (isBg(flagx, flagy)) {
//          featherBox[featherSize+xbox][featherSize+ybox] = true;  // Index translated as it cannot be negative
          fbs.incBgInBox();
        } else
//          featherBox[featherSize+xbox][featherSize+ybox] = false;

        if (isFg(flagx, flagy)) {
//          feath?erBox[fe?atherSize+xbox][featherSize+ybox] = true;  // Index translated as it cannot be negative
          fbs.incFgInBox();
        }
//        else
//          featherBox[featherSize+xbox][featherSize+ybox] = false;
      } // for x
    } // for y
  } // populateFeatherBox()






  public boolean primaryBgMatch(int pxPrimary, int bgPrimary) {

    int left  = bgPrimary - bgMatchRange;
    int right = bgPrimary + bgMatchRange;

    if ((pxPrimary >=left) && (pxPrimary<=right))
      return true;
    else
      return false;
  } // primaryBgMatch()


  public boolean primaryFgMatch(int pxPrimary, int bgPrimary) {
    int left  = bgPrimary - fgMatchRange;
    int right = bgPrimary + fgMatchRange;

    // The question is, is this pixel a lot different than the Bg color?

    if ((pxPrimary <left) || (pxPrimary>right))
      return true;
    else
      return false;
  } // primaryFgMatch()
} // class