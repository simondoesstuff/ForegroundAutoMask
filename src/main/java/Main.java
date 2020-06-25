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
  private TransformVideo  tv = null;

    // No Constructor -- Imagine that.

    private void execTransform() throws IOException, JCodecException {
      if (tv.execTransform()) {   // Implement method in derived class
        System.out.println("Main.execTestPattern() success");
        return;
      } else {
        System.out.println("Main.execTestPattern() failure");
        help();
        return;
      }
    } // execTransform()


    private void execTestPattern(String vidFileName) throws IOException, JCodecException {
      System.out.println("Main.execTestPattern() START");
      tv = new TestPattern(vidFileName);
      execTransform();  // Execute the specific transform for this derived class
    } // execTestPattern()


    private void execTestCopy(String vidInFileName,
                              String vidOutFileName) throws IOException, JCodecException {
      System.out.println("Main.execTestCopy() START");
      tv = new TestCopy(vidInFileName, vidOutFileName);
      execTransform();  // Execute the specific transform for this derived class
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

        String cmdHandle = args[0];
        String[] cmdArgs = new String[args.length - 1];                                     // this line should be the original arguments except for arg0. It is useful because it can be passed into the method
                                                                                            //... so that it can access the command-specific arguments directly instead of handling it in the switch statement
                                                                                            //... which will add bulk.
        System.arraycopy(args, 1, cmdArgs,0, cmdArgs.length);

        switch (cmdHandle.toLowerCase()) {
            case ("-testpattern"):
                execTestPattern("P:\\Dad\\VideoSoftware\\TestPattern.mp4");
                break;
            case ("-copytest"):
                execTestCopy("P:\\Dad\\VideoSoftware\\TestPattern.mp4", "P:\\Dad\\VideoSoftware\\TestPatternCopy.mov");
                break;
            case ("-removebackground"):
                if (cmdArgs.length != 2) {
                    System.out.println("Insufficient number of arguments for command.  Num=" + cmdArgs.length);
                    help();
                    return;
                }
                break;
            default:
                help();
                break;
        }
    } // commandInterpreter()


    public static void main(String[] args) throws IOException, JCodecException {
        Main videoApp = new Main(); // Be done with static
        videoApp.commandInterpreter(args);
    }  // main()
} // class
