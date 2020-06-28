import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.jcodec.scale.AWTUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

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
  //    within matchRange ticks of the background primary color.
  /////////////////////////////////////////////////////////////////////



  private boolean primaryMatch(int fgPrimary, int bgPrimary) {

    int left  = bgPrimary - matchRange;
    int right = bgPrimary + matchRange;

    if ((fgPrimary >=left) && (fgPrimary<=right))
      return true;
    else
      return false;
  }


  // Return true if the foreground pixel is an
  // approximate match to the background pixel.

  private boolean pixelMatch(int fg, int bg) {
    int fgRed = getRed(fg);   // Get Foreground primary colors
    int fgGrn = getGreen(fg);
    int fgBlu = getBlue(fg);

    int bgRed = getRed(bg);   // Get Background primary colors
    int bgGrn = getGreen(bg);
    int bgBlu = getBlue(bg);

    if (!primaryMatch(fgRed, bgRed))
      return false;

    if (!primaryMatch(fgGrn, bgGrn))
      return false;

    if (!primaryMatch(fgBlu, bgBlu))
      return false;

    return true;
  } // pixelMatch()


  private void reviseFgF() {
    createFrameFgFlagArray(getInputWidth(), getInputHeight());
  }


  protected int getFeatheredPixel(int x, int y, float featherFactor) {
    int originalFg = bufImgIn.getRGB(x, y); // pixel(x, y)

    int newRed = (int)((featherFactor * getRed(originalFg))   +.5);
    int newGrn = (int)((featherFactor * getGreen(originalFg)) +.5);
    int newBlu = (int)((featherFactor * getBlue(originalFg))  +.5);

    return rgbToPixel(newRed, newGrn, newBlu);
  }


  private void reviseImgOutFrame() {
    int Width   = getInputWidth();
    int Height  = getInputHeight();

    for (int y=0; y<Height; y++) {      // Loop through all pixels for step 1 & step 2.
      for (int x = 0; x < Width; x++) {
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

        if (pixelMatch(pixelIN, pixelBG)) {
          setFgFlag(x, y, true);              // Bg pixel
          bufImgOut.setRGB(x, y, 0 /*TransformVideo.DCM_ALPHA_MASK*/); // Black with 0xff Alpha Channel or Green etc.
        } else {
          setFgFlag(x, y, false);             // Fg pixel
        }
      } // for x
    } // for y

    for (int y=0; y<Height; y++) {     // Loop through all pixels for step 1 & step 2.
      for (int x = 0; x < Width; x++) {
        /////////////////////////////////////////////////////////////////////////////////
        //    STEP3: Look only at the flag.  If fg, then create the bounding box and
        //           compute the feather to revise the edge pixel color proportionally.
        /////////////////////////////////////////////////////////////////////////////////
//      System.out.println("Frame Number: " + frameNo + "  x: " + x + "  y: " + y + "  " + getInputWidth() + "x" + getInputHeight());

        if (isFg(x, y)) { // Look only at the flag identified fg pixels
          populateFeatherBox(Width, Height, x, y);

          if (pixelOnEdge()) {
            int newRGB = getFeatheredPixel(x, y, featherFactor());
            bufImgOut.setRGB(x, y, newRGB);     // New feathered color
          } // pixedOnEdge
        } // ifFg
      } // for x
    } // for y
  } // reviseImgOutFrame()


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
      bufImgBg  = AWTUtil.toBufferedImage(pictureBg);   // Bg frame
      createFrameFgFlagArray(getInputWidth(), getInputHeight());  // Used for feathering

      ///////////////////////////////////////////////////////////
      // Loop through all input frames and send to output encoder
      ///////////////////////////////////////////////////////////

      while ((pictureIn = grabIn.getNativeFrame()) != null) {
        bufImgIn  = AWTUtil.toBufferedImage(pictureIn);       // In frame
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
