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
    // No Constructor -- Imagine that.

    private void execTestPattern(String vidFileName) throws IOException, JCodecException {
      System.out.println("Main.execTestPattern() START");
      TransformVideo  tv = new TransformVideo();

      if (tv.execTestPattern(vidFileName)) {
        System.out.println("Main.execTestPattern() success");
        return;
      } else {
        System.out.println("Main.execTestPattern() failure");
        help();
        return;
      }
    } // execTestPattern()


    private void execTestCopy(String vidInFileName,
                              String vidOutFileName) throws IOException, JCodecException {
      System.out.println("Main.execTestCopy() START");
      TransformVideo  tv = new TransformVideo();

      if (tv.execTestCopy(vidInFileName, vidOutFileName)) {
        System.out.println("Main.execTestCopy() success");
        return;
      } else {
        System.out.println("Main.execTestCopy() failure");
        help();
        return;
      }
    } // execTestCopy()


    private void help() {
        System.out.println("Video App: Bad arguments\n"
                + "\nUSAGE:\n"
                + "-testPattern\n"
                + "-copyTest\n"
                + "-removeBackground <backgroundVideo> <foregroundVideo>"
        );
    }


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
          execTestCopy("P:\\Dad\\VideoSoftware\\TestPattern.mp4", "P:\\Dad\\VideoSoftware\\TestPatternCopy.mov");
          return;
        } else if (arg0.equalsIgnoreCase("-removeBackground")) {
          if (args.length < 3) {
            System.out.println("Insufficient number of arguments for command.  Num=" + args.length);
            help();
            return;
          }

          execTestCopy("P:\\Dad\\VideoSoftware\\TestPatterxxxn.mp4", "P:\\Dad\\VideoSoftware\\TestPatternCopy.mov");
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
