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

  private boolean primaryBgMatch(int pxPrimary, int bgPrimary) {

    int left = bgPrimary - bgMatchRange;
    int right = bgPrimary + bgMatchRange;

    if ((pxPrimary >= left) && (pxPrimary <= right))
      return true;
    else
      return false;
  } // primaryBgMatch()


  private boolean primaryFgMatch(int pxPrimary, int bgPrimary) {
    int left = bgPrimary - fgMatchRange;
    int right = bgPrimary + fgMatchRange;

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

    if (x == 0 || y == 0)         // Edge of the frame?
      return true;            // Force hard core bg on the frame edge

    if (x == (w - 1) || y == (h - 1)) // Other edge of the frame?
      return true;            // Force hard core bg on the frame edge

    int pxRed = getRed(px);   // Get Foreground primary colors
    int pxGrn = getGreen(px);
    int pxBlu = getBlue(px);

    int bgRed = getRed(bg);   // Get Background primary colors
    int bgGrn = getGreen(bg);
    int bgBlu = getBlue(bg);

    if (!primaryBgMatch(pxRed, bgRed))
      return false;

    if (!primaryBgMatch(pxGrn, bgGrn))
      return false;

    if (!primaryBgMatch(pxBlu, bgBlu))
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

    if (x == 0 || y == 0)           // Edge of the frame is dedicated to BG
      return false;             // Force hard core bg on the frame edge

    if (x == (w - 1) || y == (h - 1))   // Other edge of the frame?
      return false;             // Force hard core bg on the frame edge

    int pxRed = getRed(px);     // Get Foreground primary colors
    int pxGrn = getGreen(px);
    int pxBlu = getBlue(px);

    int bgRed = getRed(bg);   // Get Background primary colors
    int bgGrn = getGreen(bg);
    int bgBlu = getBlue(bg);

    if (primaryFgMatch(pxRed, bgRed))
      return true;

    if (primaryFgMatch(pxGrn, bgGrn))
      return true;

    if (primaryFgMatch(pxBlu, bgBlu))
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

  // a tiny class that is just used to wrap arguments going into the 2 phase methods (for private use only)
  private class FrameSection {
    int wStart, wEnd, hStart, hEnd;

    public FrameSection(int wStart, int wEnd, int hStart, int hEnd) {
      this.wStart = Math.max(wStart, 0);      // if the minimum width is less than 0, use 0.
      this.wEnd = Math.min(wEnd, getInputWidth());        // if the maximum width is > screen width, use screenWidth.

      this.hStart = Math.max(hStart, 0);
      this.hEnd = Math.min(hEnd, getInputWidth());
    }
  }

  private void reviseImgOutPhase1(FrameSection s) {
    int Width = s.wEnd;
    int Height = s.hEnd;

    for (int y = s.hStart; y < Height; y++) {      // Loop through all pixels for step 1 & step 2.
      for (int x = s.wStart; x < Width; x++) { // Frame coordinates
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

        if (pixelBgMatch(pixelIN, pixelBG, x, y, Width, Height))
          setBgFlag(x, y, true);             // Bg pixel
        else
          setBgFlag(x, y, false);           // Maybe a Fg pixel

        if (pixelFgMatch(pixelIN, pixelBG, x, y, Width, Height))
          setFgFlag(x, y, true);             // Fg pixel
        else
          setFgFlag(x, y, false);            // Maybe a Bg pixel

        if (isBg(x, y) && isFg(x, y))
          System.out.println("FG&BG1!!!! Frame Number: " + frameNo + "  x: " + x + "  y: " + y);
        if (isBg(x, y) && isFg(x, y))
          System.exit(0);
      } // for x
    } // for y
  } // reviseImgOutPhase1()

  private void reviseImgOutPhase2(FrameSection s) {
    int Width = s.wEnd;
    int Height = s.hEnd;

    for (int y = s.hStart; y < Height; y++) {     // Loop through all pixels for step 1 & step 2.
      for (int x = s.wStart; x < Width; x++) {
        /////////////////////////////////////////////////////////////////////////////////
        //    STEP3: Determine the color based upon the Bg flag array, the Fg flag array,
        //           whether the surrounding pixels contain bg or fb pixes.
        //           If fg, then create the bounding box and
        //           compute the feather to revise the edge pixel color proportionally.
        /////////////////////////////////////////////////////////////////////////////////
//      System.out.println("Frame Number: " + frameNo + "  x: " + x + "  y: " + y + "  " + getInputWidth() + "x" + getInputHeight());

        if (isBg(x, y) && isFg(x, y)) {
          System.out.println("FG&BG2!!!! Frame Number: " + frameNo + "  x: " + x + "  y: " + y);
          System.exit(0);
        }

//
        if (isBg(x, y))
          bufImgOut.setRGB(x, y, transColor);  // Simple case.  Do not compute feather box.  Set to black
        else {
          populateFeatherBox(Width, Height, x, y);                  // Just computing statistics of the box at this point.
          populateFeatherRowCol(Width, Height, x, y);

          if (containsBgPixels() && !containsFgPixels()) {
//            System.out.println("NonLinear Force to Black");
            bufImgOut.setRGB(x, y, transColor);  // Force to Black with 0xff Alpha Channel or Green etc.
          } else {
            if (isFg(x, y) && !containsBgPixels())                  // FG pixel and No BG pixels
              ;                                                       // Let's see the video fg how about
            else if (!containsFgPixels() && !containsBgPixels())      // Very grey area in between bg and fg colors.   Assume original pixel is good
              ;                                                       // NOOP yields original pixel color
            else if (muteRow(x, y) || muteCol(x, y))
              bufImgOut.setRGB(x, y, transColor);  // Force to Black with 0xff Alpha Channel or Green etc.
            else {                                                    // Hard case as this box contains fg and bg pixel.  We must feather.
              float ff = featherFactor();
              int newRGB = getFeatheredPixel(x, y, ff);
//              System.out.println("Feathering  x: " + x + "  y: " + y + "  FeatureFactor: " + ff);
              bufImgOut.setRGB(x, y, newRGB);     // New feathered color
            } // else
          } // else
        } // else
      } // for x
    } // for y
  }

  private void reviseImgOutFrame() {
    Future<?>[] processes = new Future<?>[Main.NTHREADS];

    int width = getInputWidth();
    int shift = getInputHeight() / Main.NTHREADS;       // uses integer math, expect sections to be a bit too small.

    int prevMax = 0;

    for (int i = 0; i < Main.NTHREADS; i++) {
      // the section can be the prevMax and the prevMax + shift because the beginning of the range is inclusive and the end of exclusive by the nature of a for loop
      FrameSection s = new FrameSection(0, width, prevMax, prevMax += shift);       // this will never exceed the screen size because 'Section' is a safe data-structure.
      processes[i] = Main.threadPool.submit(() -> reviseImgOutPhase1(s));         // ignite thread i
    }

    ////////////////////////////////////////////////////////////// WAIT FOR PHASE 1 COMPLETION

    for (Future<?> process : processes) {
      // the following code wait for all processes to complete. There should never be an error thrown, but in the case that it happens, it will be printed instead of thrown.

      try {
        process.get();      // this is a blocking call
      } catch (InterruptedException | ExecutionException | CancellationException e) {
        e.printStackTrace();
      }
    }

    ////////////////////////////////////////////////////////////// BEGIN PHASE 2

    prevMax = 0;

    for (int i = 0; i < Main.NTHREADS; i++) {
      FrameSection s = new FrameSection(0, width, prevMax, prevMax += shift);       // this will never exceed the screen size because 'Section' is a safe data-structure.
      processes[i] = Main.threadPool.submit(() -> reviseImgOutPhase2(s));         // ignite thread i
    }
  } // reviseImgOutFrame


  ///////////////////////////////////////////////////////////////////
  // This test verifies that an mp4 video can be read in a frame at a
  // time and output/encoded into a .mov file that is effectively
  // identical.  Return true for success and false for error.
  // Helps the caller bring up help() when things go wrong.
  ///////////////////////////////////////////////////////////////////
  @Override
  public boolean execTransform() throws IOException, JCodecException {
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
      createFrameFgFlagArray(getInputWidth(), getInputHeight());  // Used for feathering

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

        if (frameNo > stopFrame)
          break;  // Exit while loop early
      } // while

      encoder.finish();
    } finally {
      NIOUtils.closeQuietly(fileOut);
    }

    return true;
  } // execTransform()
} // class
