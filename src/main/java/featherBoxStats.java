//////////////////////////////////////////////////////////
// Provides feather box statistics that are thread safe
//////////////////////////////////////////////////////////

public class featherBoxStats {
    private   int     noBgInBox   = 0;      // Also don't confuse feather box with feather bed.
    private   int     noFgInBox   = 0;      // Feather bed is much more comfortable.
    protected int     featherSize = 2;      // Half the size of the feather box
    private   int     mgPercent   = 100;    // How much weight to give middle ground pixels
    private   boolean logging     = false;

    public featherBoxStats(int fsize) {
      featherSize = fsize;
      clear();
    }

    public void clear() {
      noBgInBox = 0;            // Also don't confuse feather box with feather bed.
      noFgInBox = 0;            // Feather bed is much more comfortable.
    }

    public void setLogging(boolean enableLogging) {
      logging = enableLogging;
    }

    public void incBgInBox() {
      noBgInBox++;
    }

    public void incFgInBox() {
      noFgInBox++;
    }

    public int noFg() { return noFgInBox; } // Getter functions
    public int noBg() { return noBgInBox; }

    public boolean containsFgPixels() {
      return  (noFgInBox > 0);
    }

    public boolean containsBgPixels() {
      return  (noBgInBox > 0);
    }

    public float featherFactor() {
      int fullFeatherBoxArea = (1 + featherSize + featherSize) * (1 + featherSize + featherSize);
      int noMG = fullFeatherBoxArea - noBgInBox - noFgInBox;  // Number Middle Ground

      /////////////////////////////////////////////////////////////////////////////////////////////////////////
      // So the question is, do we calculate the feather factor based upon only FG and BG or do we consider
      // the MG too.   If so, do MG pixes have the same weight?   Seems that making MG pixes equal weight
      // eliminates the edge on the subject.   Not sure, so make it configurable via mgPercent.
      /////////////////////////////////////////////////////////////////////////////////////////////////////////

    int effectiveArea = noBgInBox + noFgInBox + ((noMG * mgPercent)/100);
    float ff;

    if (effectiveArea == 0) {  // Not supposed to happen
      ff = 1.0f;
    }
    else
      ff = (1.0f * noFgInBox)/(effectiveArea);

//    if (logging)
//      System.out.println("TransformVideo.featherFactor()"
//                              + " noFgInBox: "  + noFgInBox
//                              + "  noBgInBox: " + noBgInBox
//                              + "  noMgInBox:"  + noMG
//                              + "  mgPercent:"  + mgPercent
//                              + "  fullArea: "  + fullFeatherBoxArea
//                              + "  feathSize: " + featherSize
//                              + "  effArea:"    + effectiveArea
//                              + "  ff: "        + ff);

    return ff;
  }
} // class
