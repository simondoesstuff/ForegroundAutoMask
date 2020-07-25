
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.scale.AWTUtil;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class BackGroundAvailable extends TransformVideo {
  /////////////////////////////////////////////////////////////////////
  // This class implements the basic background removal transformation
  // using a provided background video.
  /////////////////////////////////////////////////////////////////////

  private BackGroundAvailable(String infile) throws IOException, JCodecException {   // Private to force caller to provide both file names
    super(infile);
  }


  private BackGroundAvailable(String infile, String outfile) throws IOException, JCodecException {
    super(infile, outfile);
  }


  public BackGroundAvailable(String infile, String outfile, String bgfile) throws IOException, JCodecException {
    super(infile, outfile, bgfile);
  }

  /////////////////////////////////////////////////////////////////////
  // ALGORITHM:
  //    Verify that the foreground primary color for one pixel is
  //    within bgMatchRange ticks of the background primary color.
  /////////////////////////////////////////////////////////////////////

  private boolean primaryBgMatch(int pxPrimary, int bgPrimary, int row, int col) {

    int left = bgPrimary - fstats.getBgMatchRange(row, col);
    int right = bgPrimary + fstats.getBgMatchRange(row, col);

    if ((pxPrimary >= left) && (pxPrimary <= right))
      return true;
    else
      return false;
  } // primaryBgMatch()


  private boolean primaryFgMatch(int pxPrimary, int bgPrimary, int row, int col) {
    int left  = bgPrimary - fstats.getFgMatchRange(row, col);
    int right = bgPrimary + fstats.getFgMatchRange(row, col);

    // The question is, is this pixel a lot different than the Bg color?

    if ((pxPrimary < left) || (pxPrimary > right))
      return true;
    else
      return false;
  } // primaryFgMatch()


  // Return true if the foreground pixel is an
  // approximate match to the background pixel.

  private boolean pixelBgMatch(int px, int bg, int x, int y, int w, int h) {
//    System.out.println("pixelBgMatch()"
//            + "  bgFlag[][]: " + (bgFlag==null ? "NULL" : "NonNUll")
//            + "  px: "  + px
//            + "  bg: "  + bg
//            + "  x: "   + x
//            + "  y: "   + y
//            + "  w: "   + w
//            + "  y: "   + y);

    if (x == 0 || y == 0)     // Edge of the frame?
      return true;            // Force hard core bg on the frame edge

    if (x == (w - 1) || y == (h - 1)) // Other edge of the frame?
      return true;            // Force hard core bg on the frame edge

    int pxRed = getRed(px);   // Get Foreground primary colors
    int pxGrn = getGreen(px);
    int pxBlu = getBlue(px);

    int bgRed = getRed(bg);   // Get Background primary colors
    int bgGrn = getGreen(bg);
    int bgBlu = getBlue(bg);

    if (!primaryBgMatch(pxRed, bgRed, y, x))
      return false;

    if (!primaryBgMatch(pxGrn, bgGrn, y, x))
      return false;

    if (!primaryBgMatch(pxBlu, bgBlu, y, x))
      return false;

    return true;
  } // pixelBgMatch()


  private boolean pixelFgMatch(int px, int bg, int x, int y, int w, int h) {
//    System.out.println("pixelBgMatch()"
//            + "  fgFlag[][]: " + (fgFlag==null ? "NULL" : "NonNUll")
//            + "  px: "  + px
//            + "  bg: "  + bg
//            + "  x: "   + x
//            + "  y: "   + y
//            + "  w: "   + w
//            + "  y: "   + y);

    if (x == 0 || y == 0)       // Edge of the frame is dedicated to BG
      return false;             // Force hard core bg on the frame edge

    if (x == (w - 1) || y == (h - 1))   // Other edge of the frame?
      return false;             // Force hard core bg on the frame edge

    int pxRed = getRed(px);     // Get Foreground primary colors
    int pxGrn = getGreen(px);
    int pxBlu = getBlue(px);

    int bgRed = getRed(bg);     // Get Background primary colors
    int bgGrn = getGreen(bg);
    int bgBlu = getBlue(bg);

    if (primaryFgMatch(pxRed, bgRed, y, x))
      return true;

    if (primaryFgMatch(pxGrn, bgGrn, y, x))
      return true;

    if (primaryFgMatch(pxBlu, bgBlu, y, x))
      return true;

    return false;
  } // pixelFgMatch()


  protected int getFeatheredPixel(int x, int y, float featherFactor) {
    float bgff = 1.0f - featherFactor;  // 100% - featherFactor
    int pxColor = bufImgIn.getRGB(x, y); // pixel(x, y)

    int pxRed = (int) ((featherFactor * getRed(pxColor)) + .5);
    int pxGrn = (int) ((featherFactor * getGreen(pxColor)) + .5);
    int pxBlu = (int) ((featherFactor * getBlue(pxColor)) + .5);

    int bgRed = (int) ((bgff * getRed(transColor)) + .5);
    int bgGrn = (int) ((bgff * getGreen(transColor)) + .5);
    int bgBlu = (int) ((bgff * getBlue(transColor)) + .5);

    return rgbToPixel(bgRed + pxRed, bgGrn + pxGrn, bgBlu + pxBlu);
  }


  // Internal tiny class that is just used to wrap arguments going into the 2 phase methods (for private use only)
  private class FrameSection {
    int     wStart,     // Horizontal first pixel
            wEndPlus1,  // Horizontal last pixel + 1
            hStart,     // Vertical first pixel
            hEndPlus1;  // Vertical last pixel + 1

    public FrameSection(int wStart, int wEnd, int hStart, int hEnd) {
      this.wStart = Math.max(wStart, 0);              // if the start pixel is less than 0, use 0.
      this.wEndPlus1 = Math.min(wEnd, getInputWidth());  // if the last pixel is >= screen width, use screenWidth-1

      this.hStart = Math.max(hStart, 0);              // Same kind of thing vertically
      this.hEndPlus1 = Math.min(hEnd, getInputHeight());
    }
  }


  private void reviseImgOutPhase1(FrameSection s, int frameWidth, int frameHeight) {
    for (int y = s.hStart; y < s.hEndPlus1; y++) {      // Loop through all pixels for step 1 & step 2.
      for (int x = s.wStart; x < s.wEndPlus1; x++) { // Frame coordinates
//        System.out.println("Frame Number: " + frameNo + "  x: " + x + "  y: " + y + "  " + getInputWidth() + "x" + getInputHeight());
        int pixelIN = bufImgIn.getRGB(x, y); // pixel(x, y)
        int pixelBG = bufImgBg.getRGB(x, y);

        /////////////////////////////////////////////////////////////////////////////////
        // ALGORITHM:
        //    STEP1: Walk every pixel in every row and column
        //           If the image pixel RGB value is roughly the background pixel value,
        //           then force the output pixel to be black (green?) and maybe set
        //           alpha.   So far, .mov encoder ignores setting alpha.
        //           Otherwise, we'll do nothing and leave the out video as is.  Since
        //           bufImgOut was initialized to bufImgIn, the foreground is already
        //           there.
        //
        //    STEP2: Also in the same loop, we mark the flag array with information on
        //           which pixels are background.  Important to recognize that at the
        //           moment, background pixels are set black and foreground pixels are
        //           set to the original input and consists of pixels on the edge and
        //           some are deep within the foreground.
        //
        //    STEP3: This step does not alter the bg pixels.  It finds pixels on the
        //           edge and feathers them using a surrounding pixel box.  Loops through
        //           all pixels again.   If the pixel has the bg flag set, ignore.  If
        //           the pixel has the fg flag set, then determine how many pixels
        //           surrounding this pixel are bg and darken proportionally.
        /////////////////////////////////////////////////////////////////////////////////

        if (pixelBgMatch(pixelIN, pixelBG, x, y, frameWidth, frameHeight))
          fstats.setBgFlag(x, y, true);             // Bg pixel
        else
          fstats.setBgFlag(x, y, false);           // Maybe a Fg pixel

        if (pixelFgMatch(pixelIN, pixelBG, x, y, frameWidth, frameHeight))
          fstats.setFgFlag(x, y, true);             // Fg pixel
        else
          fstats.setFgFlag(x, y, false);            // Maybe a Bg pixel

        if (fstats.isBg(x, y) && fstats.isFg(x, y)) {
          System.out.println("FG&BG1!!!! Frame Number: " + frameNo + "  x: " + x + "  y: " + y);
          System.exit(7);
        }
      } // for x
    } // for y
  } // reviseImgOutPhase1()


  @Override
  protected boolean magicPixel(int x, int y) {  // Frame Coordinates
    if (x  <1025 || x > 1035)
      return false;

    if (y  <445 || y > 455)
      return false;

    //System.out.println("magicPixel() " + frameNo + "  x: " + x + "  y: " + y);
    return true;
  }

  private void logIfMagicPixel(int x, int y, String s) {
    return;
//    if (magicPixel(x,y))
//      System.out.println("logIfMagicPixel() " + frameNo + "  x: " + x + "  y: " + y + "  " + s);
  }


  private void reviseImgOutPhase2(FrameSection s) {
    featherBoxStats fbs = new featherBoxStats(getFeatherSize());

    for (int y = s.hStart; y < s.hEndPlus1; y++) {     // Loop through all pixels for step 1 & step 2.
      for (int x = s.wStart; x < s.wEndPlus1; x++) {
        /////////////////////////////////////////////////////////////////////////////////
        //    STEP3: Determine the color based upon the Bg flag array, the Fg flag array,
        //           whether the surrounding pixels contain bg or fb pixes.
        //           If fg, then create the bounding box and
        //           compute the feather to revise the edge pixel color proportionally.
        /////////////////////////////////////////////////////////////////////////////////
//      System.out.println("Frame Number: " + frameNo + "  x: " + x + "  y: " + y + "  " + getInputWidth() + "x" + getInputHeight());

//        if (magicPixel(x, y)) { // Testing to locate region of the frame to investigate deeper
//          bufImgOutSetRGB(x, y, 0xffffffff);  // Force to Black with 0xff Alpha Channel or Green etc.
//          continue;
//        }

        if (fstats.isBg(x, y) && fstats.isFg(x, y)) { // Should never happen
          System.out.println("FG&BG2!!!! Frame Number: " + frameNo + "  x: " + x + "  y: " + y);
          System.exit(0);
        }

        if (fstats.isBg(x, y)) {
          bufImgOutSetRGB(x, y, transColor);     // Synchronized
          logIfMagicPixel(x, y, "BG PIXEL");
        }
        else {
          fstats.populateFeatherBox(fbs, getInputWidth(), getInputHeight(), x, y, magicPixel(x,y));                  // Just computing statistics of the box at this point.

          if (fbs.containsBgPixels() && !fbs.containsFgPixels()) {
//          System.out.println("NonLinear Force to Black");
            bufImgOutSetRGB(x, y, transColor);  // Force to Black with 0xff Alpha Channel or Green etc.

            logIfMagicPixel(x, y, "BG but no FG PIXEL");
          } else {
            if (fstats.isFg(x, y) && !fbs.containsBgPixels()) {                 // FG pixel and No BG pixels
              logIfMagicPixel(x, y, "FG but no BG PIXEL -- NOOP");;
            }
            else if (!fbs.containsFgPixels() && !fbs.containsBgPixels()) {      // Very grey area in between bg and fg colors.   Assume original pixel is good
//              logIfMagicPixel(x, y, "no BG & no FG PIXEL -- NOOP"); xxx                                                      // NOOP yields original pixel color
              bufImgOutSetRGB(x, y, transColor);  // Force to Black with 0xff Alpha Channel or Green etc.
            }
            else if (fstats.muteRow(x, y) || fstats.muteCol(x, y)) {
              bufImgOutSetRGB(x, y, transColor);  // Force to Black with 0xff Alpha Channel or Green etc.
//              logIfMagicPixel(x, y, "Mute row or Mute Col - Make Transparent");
            }
            else {                                                    // Hard case as this box contains fg and bg pixel.  We must feather.
              float ff = fbs.featherFactor();
              int newRGB = getFeatheredPixel(x, y, ff);
//            System.out.println("Feathering  x: " + x + "  y: " + y + "  FeatureFactor: " + ff);
              bufImgOutSetRGB(x, y, newRGB);     // New feathered color
//              logIfMagicPixel(x, y, "FEATHER " + ff);
            } // else
          } // else
        } // else
      } // for x
    } // for y
  }



  private void windowWash(FrameSection s) {
    featherBoxStats fbs = new featherBoxStats(getFeatherSize());

    for (int y = s.hStart; y < s.hEndPlus1; y++) {     // Loop through all pixels for step 1 & step 2.
      for (int x = s.wStart; x < s.wEndPlus1; x++) {
        fstats.populateFeatherBox(fbs, getInputWidth(), getInputHeight(), x, y, magicPixel(x,y));

        if (!fstats.isBg(x, y) && !fstats.isFg(x,y)) {
          if (!fbs.containsFgPixels()) {
          }
        }
      } // for x
    } // for y
  }


  private void waitForAllThreadsToComplete(Future<?>[]  procs, String phaseDescription) throws InterruptedException {
    //System.out.println("\tWAITING for " + phaseDescription + " threads to complete");

    for (Future<?> process : procs) {
      // the following code wait for all processes to complete. There should never be an error thrown, but in the case that it happens, it will be printed instead of thrown.

      try {
        if (process != null)
          process.get();      // this is a blocking call
      } catch (InterruptedException | ExecutionException | CancellationException e) {
        e.printStackTrace();
      }
    } // for

//    Thread.sleep(1000); // Sleep one second temporarily while we debug stuff.   This eliminates suspicion of this wait routine.
  }


  private void schedulePhase1(Future<?>[] processes,
                              int         noThreads,
                              int         width,
                              int         height) throws InterruptedException {
    int shift   = getInputHeight() / noThreads;       // Delta Height.  Uses integer math, expect sections to be a bit too small.
    int prevMax = 0;  // Actually height of the job given to the thread

    for (int i = 0; i < noThreads; i++) {
      int hstart = prevMax;
      int hendPlus1   = prevMax += shift;

      if (i==noThreads-1)   // Last thread.  Fix roundoff error
        hendPlus1 = height;

      FrameSection s = new FrameSection(0, width, hstart, hendPlus1);
//      System.out.println("\tPhase1(" + width + "," + height + ") Threads: " + i + "/" + noThreads
//              + "   FrameSection: " + s.wStart + " " + s.wEndPlus1 + " " + s.hStart + " " + s.hEndPlus1);
      processes[i] = Main.threadPool.submit(() -> reviseImgOutPhase1(s, width, height));         // ignite thread i
    } // for

    //////////////////////////////////////////////////////////////
    // WAIT FOR PHASE 1 COMPLETION
    //////////////////////////////////////////////////////////////

    waitForAllThreadsToComplete(processes, "phase1");
  }


  public void schedulePhase2(Future<?>[] processes,
                             int         noThreads,
                             int         width,
                             int         height) throws InterruptedException {
    int shift   = getInputHeight() / noThreads;       // Delta Height.  Uses integer math, expect sections to be a bit too small.
    int prevMax = 0;

    for (int i = 0; i < noThreads; i++) {
      int hstart = prevMax;
      int hendPlus1   = prevMax += shift;

      if (i==noThreads-1)   // Last thread.  Fix roundoff error
        hendPlus1 = height;

      FrameSection s = new FrameSection(0, width, hstart, hendPlus1);       // this will never exceed the screen size because 'Section' is a safe data-structure.
//      System.out.println("\tPhase2(" + width + "," + height + ") Threads: " + i + "/" + noThreads
//              + "   FrameSection: " + s.wStart + " " + s.wEndPlus1 + " " + s.hStart + " " + s.hEndPlus1);
      processes[i] = Main.threadPool.submit(() -> reviseImgOutPhase2(s));         // ignite thread i
    }

    //////////////////////////////////////////////////////////////
    // WAIT FOR PHASE 2 COMPLETION
    //////////////////////////////////////////////////////////////

    waitForAllThreadsToComplete(processes, "phase2");
  }

  private void reviseImgOutFrame() throws InterruptedException {
    Future<?>[] processes = new Future<?>[Main.NTHREADS];
    int noThreads = Main.NTHREADS;
    int width     = getInputWidth();  // Real frame width
    int height    = getInputHeight(); // Real frame height

    fstats.makeActiveNormal();
    schedulePhase1(processes, noThreads, width, height);

//    System.out.println("\tfstats.populateFeatherRowCol() Not multi-threaded.  Perhaps Rows could be done on one thread and Cols on another?");
    fstats.populateFeatherRowCol(getInputWidth(), getInputHeight());     // Needs to be outside frame section ?????????????????????????????????????????????????????????????????????????????
//    fstats.dumpColStatsInFow(getInputWidth());
//    System.exit(117);
    fstats.findSubject(getInputWidth(), getInputHeight());
    fstats.makeActiveEnhanced();
    schedulePhase1(processes, noThreads, width, height);



//    windowWash(new FrameSection(0, width, 0, height));  // Might be able to run more than once and keep cleaning up the green



    fstats.populateFeatherRowCol(getInputWidth(), getInputHeight());     // Needs to be outside frame section ?????????????????????????????????????????????????????????????????????????????

    schedulePhase2(processes, noThreads, width, height);
  } // reviseImgOutFrame



  @Override
  public boolean execTransform() throws IOException, JCodecException, InterruptedException {
    System.out.println("BackGroundAvailable.execTransform() VIDEO FILES:  " + vidInFileName + "  " + vidOutFileName);

    if (!fileIn.exists()) {
      System.out.println("Input file does not exist\n" + vidInFileName);
      return false;
    } else {
      System.out.println("Input file exists\n" + vidInFileName);
    }

    if (!fileBg.exists()) {
      System.out.println("BG file does not exist\n" + vidBgFileName);
      return false;
    } else {
      System.out.println("BG file exists\n" + vidBgFileName);
    }

    try {
      ///////////////////////////////////////////////////////////////////////////////
      // Setup the input channel just like in the -testPattern code.
      // Setup output channel which is essentially and output file & an encoder since
      // .mov is a compressed video format (not as compressed as mp4).   The output
      // file open and the encoder assignment is done by the superclass constructor.
      //
      // ALGORITHM:
      // Also get a representative frame from the background video.   First frame.
      ///////////////////////////////////////////////////////////////////////////////

      frameNo = getFrameNumber(grabIn);   // Track Frame Number for debugging
      pictureBg = grabBg.getNativeFrame();
      bufImgBg = AWTUtil.toBufferedImage(pictureBg);   // Bg frame
      fstats.createFrameFgFlagArrays(getInputWidth(), getInputHeight());  // Full Frame Size.  Used for feathering

      /////////////////////////////////////////////////////////////////////
      // Loop through all input frames and send to output encoder
      //  Mike McPherson:
      //    15 30 5 Edgy if green, perfect if black
      //    20 40 5 Much better.  Still some bg and jaggy glow around body
      //    25 45 6 Better.  Possible facial deterioration
      //    20 50 6
      /////////////////////////////////////////////////////////////////////

      while ((pictureIn = grabIn.getNativeFrame()) != null) {
        bufImgIn = AWTUtil.toBufferedImage(pictureIn);       // In frame
        bufImgOut = AWTUtil.toBufferedImage(pictureIn);       // Out frame starts as copy of input frame
        frameNo = getFrameNumber(grabIn);

        if (frameNo >= startFrame && frameNo <= stopFrame) {  // Limit range for testing purposes
          System.out.println("Frame Number: " + frameNo);
          reviseImgOutFrame();                                // Magic algorithm
          encoder.encodeImage(bufImgOut);                     // Write the out image to the encoder & out file
        }

        incFramesProcessed();

        if (frameNo > stopFrame)
          break;  // Exit while loop early
      } // while

      encoder.finish();
    } finally {
      NIOUtils.closeQuietly(fileOut);
    }

//    System.out.println("maxDelta: " + frameStats.maxDelta);
    return true;
  } // execTransform()
} // class