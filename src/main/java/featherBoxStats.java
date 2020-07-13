//////////////////////////////////////////////////////////
// Provides feather box statistics that are thread safe
//////////////////////////////////////////////////////////

public class featherBoxStats {
    private int     noBgInBox = 0;            // Also don't confuse feather box with feather bed.
    private int     noFgInBox = 0;            // Feather bed is much more comfortable.

    public featherBoxStats() {
      clear();
    }

    public void clear() {
      noBgInBox = 0;            // Also don't confuse feather box with feather bed.
      noFgInBox = 0;            // Feather bed is much more comfortable.
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
    int area = noBgInBox + noFgInBox;
    float ff;

    if (area == 0) {  // Not supposed to happen
      ff = 1.0f;
    }
    else
      ff = (1.0f * noFgInBox)/(area);

//    System.out.println("TransformVideo.featherFactor() noFgInBox: " + noFgInBox + "  noBgInBox: " + noBgInBox + "  area: " + area + "  ff: " + ff);
    return ff;
  }
} // class
