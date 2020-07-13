public class frameStats {

///////////////////////////////////////////////////////////
  // Feathering Support via boolean flag array.
  // Flag=true  Background pixel we wish to be transparent
  // Flag=false Foreground pixel we may have to feather
  ///////////////////////////////////////////////////////////
  protected int                 featherSize     = 2;  // Half the size of the feather box
  protected int                 bgMatchRange    = 4;  // Pixel RGB = BG color +/- bgMatchRange to be a bg pixel
  protected int                 fgMatchRange    = 30; // Pixel RGB = BG color distance of at least fgMatchRange

  protected boolean bgFlag[][]        = null; // Pixel is unambiguously a background pixel (frame coordinates)
  protected boolean fgFlag[][]        = null; // Pixel is unambiguously a foreground pixel (frame coordinates)

  protected int     featherArea       = (1 +featherSize +  featherSize) * (1 + featherSize + featherSize);
//  protected boolean featherBox[][]    = null; // Bounding box around selected pixel











  protected void createFrameFgFlagArray(int width, int height) {
//    System.out.println("TransformVideo.createFrameFgFlagArray() " + width + " " + height);
    bgFlag = new boolean[width][height];  // Frame coordinates
    fgFlag = new boolean[width][height];  // Frame coordinates
//    featherBox = new boolean[1+featherSize+featherSize][1+featherSize+featherSize]; // Not actually used at this point
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


  protected void setBgFlag(int x, int y,    // Frame coordinates
                           boolean flag) {  // true = is a BG pixel color
    if (bgFlag != null)
      bgFlag[x][y]=flag;
  }


  protected void setFgFlag(int x, int y,    // Frame coordinates
                           boolean flag) {  // true == is a FG pixel color
    if (fgFlag != null)
      fgFlag[x][y]=flag;
  }


  protected boolean getFgFlag(int x, int y) { // Frame coordinates
    if (fgFlag == null)
      return false;
    else
      return fgFlag[x][y];
  }


  protected boolean getBgFlag(int x, int y) {   // Frame coordinates
    if (bgFlag == null)
      return false;
    else
      return bgFlag[x][y];
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
  private int fgInCol(int col) {
    return colStatsFg[col];
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
                                    int             ycenter) {
    int flagx;      // x & y adjusted by sanity checks to that we do not
    int flagy;      // index into feather box out of bounds.
    fbs.clear();    // Number of Bg & Fb pixes in the feather box

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