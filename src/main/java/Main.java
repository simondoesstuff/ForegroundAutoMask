//import com.oracle.awt.AWTUtils;
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


// QUESTIONS/ANSWERES:
//      BufferedImage.nbsp?
//      @see ColorModel
//      @see Raster    WritableRaster raster;
//      @see WritableRaster
//      java.awt.AlphaComposite documentation.
//      https://en.wikipedia.org/wiki/YUV  Y=Luma  U=BlueProjection  V=RedProjection
//      https://www.flir.com/support-center/iis/machine-vision/knowledge-base/understanding-yuv-data-formats
//      MP4 from fcpx render is 12bits/pixel (bpp) YUV420 ==> Luma-Cb-Cr
//      YUV420 refers to 4:2:0 chroma subsampling.  In 16 and 12 bpp formats, the U and V color values are shared between pixels.
//      As a result, these values are transmitted to the PC image buffer only once for every two pixels, resulting in an average
//      transmission rate of 16 bits per pixel.
//      The bytes are ordered in the image in the following manner:
//      U0 Y0 V0  Y1 U2  Y2 V2  Y3 U4  Y4 V4…

public class Main {
    private long getFrameNumber(FrameGrab fg) {
        if (fg == null)
            return 0;

        return fg.getVideoTrack().getCurFrame();
    } // dumpFrameNumber()


    private void dumpFrameNumber(FrameGrab fg) {
        System.out.println("dumpFrameNumber() " + getFrameNumber(fg));
    }


    private void dumpPicture(Picture p, long frameNumber) {
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


    private String formatPixel(int px) {
        Integer pixel = px;
        return "0x" + Integer.toHexString(pixel);
    }


    private boolean magicFrame(long fmNo) {
      if (fmNo < 10)
        return true;
      else if ((fmNo > 237) && (fmNo < 245))
        return true;
      else if (fmNo > 475)
        return true;
      else
        return false;
    }


    private void dumpBufferedImage(BufferedImage bi) {
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


    private void execTestPattern(String vidFileName) throws IOException, JCodecException {
        System.out.println("VIDEO FILE:  " + vidFileName);

        int   frameCount  = 200000;
        long  frameNo     = 0;

        File file = new File(vidFileName);  // Open Video File

//        Picture picture = FrameGrab.getFrameFromFile(scottWheelerFile, 2);

        FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
        grab.seekToSecondPrecise(0); // Seek to zero second.  At 24 frames/sec we should be around frame 24.
        dumpFrameNumber(grab);
        Picture picture = null;    // Picture is one frame

        while ((picture = grab.getNativeFrame()) != null) {
            frameNo = getFrameNumber(grab);

            if (magicFrame(frameNo)) {
              dumpPicture(picture, frameNo);
              BufferedImage bufIm = AWTUtil.toBufferedImage(picture);  //
              dumpBufferedImage(bufIm);
            }
        }
    } // execTestPattern()


    private void execTestCopy(String vidInFileName,
                              String vidOutFileName) throws IOException, JCodecException {
      System.out.println("VIDEO FILES:  " + vidInFileName + "  " + vidOutFileName);

      int   frameCount  = 200000;
      long  frameNo     = -1;

      SeekableByteChannel fileOut  = null;
      File                fileIn   = new File(vidInFileName);   // Open Video Input File

      if (!fileIn.exists()) {
        System.out.println("Input file does not exist\n" + vidInFileName);
        help();
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
    } // execTestCopy()


    private void help() {
        System.out.println("Video App: Bad arguments\n"
                + "USAGE:\n"
                + "-testPattern\n"
                + "-copyTest\n"
        );
    }

    private void execOtherTasks(String[] args) throws IOException, JCodecException {
      //        Application.launch(args);
      String defaultVideoFile = "P:\\Dad\\VideoSoftware\\TnA_RockVideoOnlyHumanRev9.mp4";
      String videoFile = null;

      ///////////////////////////////////////////////////////////////////
      // You must include one argument for the file path. The video file
      // found will be parsed and the first 100 frames starting after the
      // 1st second will have their screen dimensions displayed according
      // to Simon.
      ///////////////////////////////////////////////////////////////////


      if (args == null || args.length != 1) {
        System.out.println("Video File Not provided.  Trying default video file...");
        videoFile=defaultVideoFile;
//            return;
      }
      else
        videoFile=args[0];
      //        File scottWheelerFile = new File(Main.class.getResource("ScottWheelerProResHQ.mov").getFile());
      //        File scottWheelerFile = new File(Main.class.getResource("ScottWheelerProResHQ.mov").getFile());
    } // execOtherTasks()


    private void commandInterpreter(String[] args) throws IOException, JCodecException {
        if (args==null || (args.length < 1)) {
          help();
          return;
        }

        String arg0 = args[0];

        if (arg0.equalsIgnoreCase("-testPattern")) {
          execTestPattern("P:\\Dad\\VideoSoftware\\TestPattern.mp4");
          return;
        } else if (arg0.equalsIgnoreCase("-copyTest")) {
          execTestCopy("P:\\Dad\\VideoSoftware\\TestPattern.mp4", "P:\\Dad\\VideoSoftware\\TestPatternCopy.mp4");
          return;
        } else {
          help();
          return;
        }
    } // commandInterpreter()


    public static void main(String[] args) throws IOException, JCodecException {
        Main videoApp = new Main(); // Be done with static
        videoApp.commandInterpreter(args);
    }  // main()
} // class
