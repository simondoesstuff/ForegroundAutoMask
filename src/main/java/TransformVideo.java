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
import java.io.IOException;

public class TransformVideo {

  public TransformVideo() {   // Constructor
    // constructor
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


  private static void dumpPicture(Picture p, long frameNumber) {
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


  private static boolean magicFrame(long fmNo) {  // Used with the test below
    if (fmNo < 10)
      return true;
    else if ((fmNo > 237) && (fmNo < 245))
      return true;
    else if (fmNo > 475)
      return true;
    else
      return false;
  }


  //////////////////////////////////////////////////////////////////
  // This test is used to verify how pixes data is formatted within
  // a BufferedImage data structure.
  //////////////////////////////////////////////////////////////////

  public static boolean execTestPattern(String vidFileName) throws IOException, JCodecException {
    System.out.println("VIDEO FILE:  " + vidFileName);

    int   frameCount  = 200000;
    long  frameNo     = 0;

    File file = new File(vidFileName);  // Open Video File

//        Picture picture = FrameGrab.getFrameFromFile(scottWheelerFile, 2);

    FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
    grab.seekToSecondPrecise(0); // Seek to zero second.  At 24 frames/sec we should be around frame 24.
    TransformVideo.dumpFrameNumber(grab);
    Picture picture = null;    // Picture is one frame

    while ((picture = grab.getNativeFrame()) != null) {
      frameNo = TransformVideo.getFrameNumber(grab);

      if (TransformVideo.magicFrame(frameNo)) {
        dumpPicture(picture, frameNo);
        BufferedImage bufIm = AWTUtil.toBufferedImage(picture);  //
        dumpBufferedImage(bufIm);
      }
    }

    return true;
  } // execTestPattern()


  ///////////////////////////////////////////////////////////////////
  // This test verifies that an mp4 video can be read in a frame at a
  // time and output/encoded into a .mov file that is effectively
  // identical.  Return true for success and false for error.
  // Helps the caller bring up help() when things go wrong.
  ///////////////////////////////////////////////////////////////////

  public boolean execTestCopy(String vidInFileName,
                            String vidOutFileName) throws IOException, JCodecException {
    System.out.println("VIDEO FILES:  " + vidInFileName + "  " + vidOutFileName);

    int   frameCount  = 200000;
    long  frameNo     = -1;

    SeekableByteChannel fileOut  = null;
    File fileIn   = new File(vidInFileName);   // Open Video Input File

    if (!fileIn.exists()) {
      System.out.println("Input file does not exist\n" + vidInFileName);
      return false;
    } else {
      System.out.println("Input file exists\n" + vidInFileName);
    }

    try {
      ///////////////////////////////////////////////////////////////
      // Setup output channel which is essentially and output file
      // and an encoder since mp4 is a compressed video format.
      ///////////////////////////////////////////////////////////////

      fileOut = NIOUtils.writableFileChannel(vidOutFileName); // Open Video Output File
      AWTSequenceEncoder encoder = new AWTSequenceEncoder(fileOut, Rational.R(24, 1));

      ///////////////////////////////////////////////////////////////
      // Setup the input channel just like in the -testPattern code.
      ///////////////////////////////////////////////////////////////

      FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(fileIn)); // Helper for inFile
      Picture picture = null;           // Picture is one frame
      frameNo = getFrameNumber(grab);   // Track Frame Number for debugging

      ///////////////////////////////////////////////////////////
      // Loop through all input frams and send to output encoder
      ///////////////////////////////////////////////////////////

      while ((picture = grab.getNativeFrame()) != null) {
        //dumpPicture(picture, frameNo);
        BufferedImage bufIm = AWTUtil.toBufferedImage(picture);  // Get frame
        //dumpBufferedImage(bufIm);
        frameNo = getFrameNumber(grab);
        encoder.encodeImage(bufIm);
      } // while

      encoder.finish();
    } finally {
      NIOUtils.closeQuietly(fileOut);
    }

    return true;
  } // execTestCopy()

} // class