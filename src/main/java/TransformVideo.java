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

  public static final int DCM_RED_MASK   = 0x00ff0000; // Stollen from BufferedImage
  public static final int DCM_GREEN_MASK = 0x0000ff00;
  public static final int DCM_BLUE_MASK  = 0x000000ff;
  public static final int DCM_ALPHA_MASK = 0xff000000;


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
    if (pictureIn==null)
      return 0;

    return pictureIn.getWidth();
  }

  protected int getInputHeight() {
    if (pictureIn==null)
      return 0;

    return pictureIn.getHeight();
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
} // class