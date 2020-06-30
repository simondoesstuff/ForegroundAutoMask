import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;
import org.jcodec.scale.AWTUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TransformVideo {
  protected String              vidInFileName   = null; // Input video filename containing FG & BG (mp4)
  protected String              vidOutFileName  = null; // Output video filename (mov)
  protected String              vidBgFileName   = null; // BG video filename w/o FG (mp4)
  protected File                fileIn          = null; // Input video file handle
  protected File                fileBg          = null;  // Background video file handle
  protected SeekableByteChannel fileOut         = null; // Output video file handle
  protected AWTSequenceEncoder  encoder         = null; // .mov encoder
  protected FrameGrab           grabIn          = null; // Frame Grabber class
  protected FrameGrab           grabBg          = null; // Frame Grabber class
  protected Picture             pictureIn       = null; // R/W frame image
  protected Picture             pictureBg       = null; // R/W frame image
  protected BufferedImage       bufImgIn        = null; // R/W frame image
  protected BufferedImage       bufImgBg        = null; // R/W frame image
  protected BufferedImage       bufImgOut       = null; // R/W frame image
  protected long                frameNo         = -1;   // Frame Number
  protected int                 startFrame      = 0;
  protected int                 stopFrame       = Integer.MAX_VALUE;
  protected int                 bgMatchRange    = 4;  // Pixel RGB = BG color +/- bgMatchRange to be a bg pixel
  protected int                 fgMatchRange    = 30; // Pixel RGB = BG color distance of at least fgMatchRange
  protected int                 featherSize     = 2;  // Half the size of the feather box

  public static final int DCM_RED_MASK    = 0x00ff0000; // Stollen from BufferedImage
  public static final int DCM_GREEN_MASK  = 0x0000ff00;
  public static final int DCM_BLUE_MASK   = 0x000000ff;
  public static final int DCM_ALPHA_MASK  = 0xff000000;
  public static final int DCM_RED_SHIFT   = 16;
  public static final int DCM_GREEN_SHIFT = 8;
  public static final int DCM_BLUE_SHIFT  = 0;

  public static int getRed(int pixel) {
    return (pixel & DCM_RED_MASK)>>DCM_RED_SHIFT;
  }

  public static int getGreen(int pixel) {
    return (pixel & DCM_GREEN_MASK)>>DCM_GREEN_SHIFT;
  }

  public static int getBlue(int pixel) {
    return (pixel & DCM_BLUE_MASK);
  }

  public static int rgbToPixel(int r, int g, int b) {
    return DCM_ALPHA_MASK|(r<<DCM_RED_SHIFT)|(g<<DCM_GREEN_SHIFT)|b;
  }

  private void openInFile() throws IOException, JCodecException {
    fileIn   = new File(vidInFileName);   // Open Video Input File
    grabIn = FrameGrab.createFrameGrab(NIOUtils.readableChannel(fileIn)); // Helper for inFile
  }

  private void openBgFile() throws IOException, JCodecException {
    fileBg = new File(vidBgFileName);   // Open Video Input File
    grabBg = FrameGrab.createFrameGrab(NIOUtils.readableChannel(fileBg)); // Helper for BgFile
  }

  private void openOutFile() throws IOException {
    ///////////////////////////////////////////////////////////////
    // Setup output channel which is essentially and output file
    // and an encoder since mp4 is a compressed video format.
    ///////////////////////////////////////////////////////////////

    fileOut = NIOUtils.writableFileChannel(vidOutFileName); // Open Video Output File
    encoder = new AWTSequenceEncoder(fileOut, Rational.R(24, 1));
//    new AWTSequenceEncoder();
  }


  protected TransformVideo(String infile) throws IOException, JCodecException {                    // Constructor
    vidInFileName = infile;
    openInFile();
  }

  protected TransformVideo(String infile, String outfile) throws IOException, JCodecException {    // Constructor
    vidInFileName   = infile;
    vidOutFileName  = outfile;
    openInFile();
    openOutFile();
  }

  protected TransformVideo(String infile, String outfile, String bgfile) throws IOException, JCodecException {    // Constructor
    vidInFileName   = infile;
    vidOutFileName  = outfile;
    vidBgFileName   = bgfile;
    openInFile();
    openOutFile();
    openBgFile();
  }

  ///////////////////////////////////////////////////////
  // UTILITIES (Mostly Static)
  ///////////////////////////////////////////////////////

  public void setFirstLastFame(int start, int stop) {
    if (stop < start)   // Sanity check
      return;

    if (start < 0)      // Sanity check
      return;

    startFrame  = start;
    stopFrame   = stop;
  }

  public void setBgMatchRange(int newRange) {
    if (newRange > 0)
      bgMatchRange = newRange;
  }


  public void setFgMatchRange(int newRange) {
    if (newRange > 0)
      fgMatchRange = newRange;
  }


  public void setFeatherSize(int newRange) {
    if (newRange > 0)
      featherSize = newRange;
  }

  public static long getFrameNumber(FrameGrab fg) {
    if (fg == null)
      return 0;

    return fg.getVideoTrack().getCurFrame();
  } // dumpFrameNumber()


  public static void dumpFrameNumber(FrameGrab fg) {
    System.out.println("dumpFrameNumber() " + getFrameNumber(fg));
  }


  public static String formatPixel(int px) {
    Integer pixel = px;
    return "0x" + Integer.toHexString(pixel);
  }


  protected int getInputWidth() {
    if (pictureIn!=null)
      return pictureIn.getWidth();

    if (pictureBg!=null)
      return pictureBg.getWidth();

    return 0;
  }


  protected int getInputHeight() {
    int h = 0;

    if (pictureIn!=null)
      h = pictureIn.getHeight();
    else if (pictureBg!=null)
      h = pictureBg.getHeight();

    if (h > 1080) // Workaround library errors
      return 1080;
    else
      return h;
  }


  protected static void dumpPicture(Picture p, long frameNumber) {
    if (p == null)
      return;

    ColorSpace cs = p.getColor();

    System.out.println("\nFrameNumber: " + frameNumber + " "  +    p.getWidth() + "x" + p.getHeight()
            + "  ColorSpace.bitsPerPixel:"  + cs.bitsPerPixel
            + "  ColorSpace.name:"          + cs.toString()
            + "  ColorSpace.ncomp:"         + cs.nComp
            + "  LowBitsNum:"               + p.getLowBitsNum()
            + "  LowBits:"                  + (p.getLowBits() == null ? "NULL" : "NonNull")
    );
  } // dumpPicture()


  public static void dumpBufferedImage(BufferedImage bi) {
    if (bi == null)
      return;

    String[] propNames = bi.getPropertyNames();

    int pixelBlack    = bi.getRGB(240,  272); // Rectangle in test pattern where we expect black
    int pixelRed      = bi.getRGB(720,  272);
    int pixelGreen    = bi.getRGB(1200, 272);
    int pixelBlue     = bi.getRGB(1680, 272);
    int pixelWhite    = bi.getRGB(240,  816); // Rectangle in test pattern where we expect white
    int pixelYellow   = bi.getRGB(720,  816);
    int pixelCyan     = bi.getRGB(1200, 816);
    int pixelMagenta  = bi.getRGB(1680, 816);

    // The code below has identified where in the integer pixel each primary color resides.
    // Unused: 0xff000000  Red: 0x00ff0000  Green: 0x0000ff00  Blue: 0x000000ff
    // Could be that the upper byte is used for transparency when implemented

    System.out.println("BufImg PropertyNames: " + (propNames == null ? "NULL" : "NonNull")
            + "  " + bi.getWidth() + "x" + bi.getHeight()
            + "  " + bi.getColorModel().toString() + "\n"
            + "Black: "     + formatPixel(pixelBlack)
            + "  Red: "     + formatPixel(pixelRed)
            + "  Green: "   + formatPixel(pixelGreen)
            + "  Blue: "    + formatPixel(pixelBlue)
            + "  White: "   + formatPixel(pixelWhite)
            + "  Yellow: "  + formatPixel(pixelYellow)
            + "  Cyan: "    + formatPixel(pixelCyan)
            + "  Magenta: " + formatPixel(pixelMagenta)
    );
  } // dumpBufferedImage()


  public boolean execTransform() throws IOException, JCodecException {
    System.out.println("Oops! Super class TransformVideo.execTransform() called instead of derived class method");
    return false;
  }

  ///////////////////////////////////////////////////////////
  // Feathering Support via boolean flag array.
  // Flag=true  Background pixel we wish to be transparent
  // Flag=false Foreground pixel we may have to feather
  ///////////////////////////////////////////////////////////

  protected boolean bgFlag[][]        = null; // Pixel is unambiguously a background pixel (frame coordinates)
  protected boolean fgFlag[][]        = null; // Pixel is unambiguously a foreground pixel (frame coordinates)

  protected int     featherArea       = (1 +featherSize +  featherSize) * (1 + featherSize + featherSize);
  protected boolean featherBox[][]    = null; // Bounding box around selected pixel
  protected int     noBgInBox = 0;            // Also don't confuse feather box with feather bed.
  protected int     noFgInBox = 0;            // Feather bed is much more comfortable.

  protected void createFrameFgFlagArray(int width, int height) {
//    System.out.println("TransformVideo.createFrameFgFlagArray() " + width + " " + height);
    bgFlag = new boolean[width][height];  // Frame coordinates
    fgFlag = new boolean[width][height];  // Frame coordinates
    featherBox = new boolean[1+featherSize+featherSize][1+featherSize+featherSize]; // Not actually used at this point
  }

  protected void populateFeatherBox(int w, int h, int xcenter, int ycenter) {
    int flagx;      // x & y adjusted by sanity checks to that we do not
    int flagy;      // index into feather box out of bounds.
    noBgInBox = 0;  // Number of Bg pixes in the feather box
    noFgInBox = 0;

    for (int ybox=-featherSize; ybox<=featherSize; ybox++) {      // Loop box coordinates
      flagy = ycenter + ybox;     // Translate box coordinates to flag array coordinates
      if (flagy < 0) flagy=0;     // Lower sanity check
      if (flagy >= h) flagy=h-1;  // Upper sanity check

      for (int xbox=-featherSize; xbox <= featherSize; xbox++) {  // Loop box coordinates
        flagx=xcenter + xbox;     // Translate box coordinates to flag array coordinates
        if (flagx < 0) flagx=0;   // Lower sanity check
        if (flagx >= w) flagx=w-1;// Upper sanity check

        if (isBg(flagx, flagy)) {
          featherBox[featherSize+xbox][featherSize+ybox] = true;  // Index translated as it cannot be negative
          noBgInBox++;
        } else
          featherBox[featherSize+xbox][featherSize+ybox] = false;

        if (isFg(flagx, flagy)) {
//          feath?erBox[fe?atherSize+xbox][featherSize+ybox] = true;  // Index translated as it cannot be negative
          noFgInBox++;
        }
//        else
//          featherBox[featherSize+xbox][featherSize+ybox] = false;
      } // for x
    } // for y
  } // populateFeatherBox()


//  protected boolean pixelOnEdge() {
//    if (noBgInBox==0)
//      return false;
//    else
//      return true;
//  }


  protected float featherFactor() {
    int area = noBgInBox + noFgInBox;

    if (area == 0)  // Not supposed to happen
      return 1.0f;

    return ((noFgInBox)/(area));
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

  protected boolean containsFgPixels() {
    return  (noFgInBox > 0);
  }

  protected boolean containsBgPixels() {
    return  (noBgInBox > 0);
  }
} // class