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


    private void execTestPattern(String[] args) throws IOException, JCodecException {
      System.out.println("Main.execTestPattern() START");

      if (args.length < 1) {
        System.out.println("Insufficient number of arguments for command testpattern.  Num=" + args.length);
        help();
        return;
      }

      String vidInFileName = args[0];

      System.out.println("Main.Background() \n"
              + "FG  '" + vidInFileName   + "'\n");

      tv = new TestPattern(vidInFileName);
      execTransform();  // Execute the specific transform for this derived class
    } // execTestPattern()


    private void execTestCopy(String vidInFileName,
                              String vidOutFileName) throws IOException, JCodecException {
      System.out.println("Main.execTestCopy() START");
      tv = new TestCopy(vidInFileName, vidOutFileName);
      execTransform();  // Execute the specific transform for this derived class
    } // execTestCopy()


    private void execBackground(String[] args) throws IOException, JCodecException {
      if (args.length < 3) {
        System.out.println("Insufficient number of arguments for command background.  Num=" + args.length);
        help();
        return;
      }

      String vidInFileName  = args[0];
      String vidOutFileName = args[1];
      String vidBgFileName  = args[2];

      System.out.println("Main.Background() \n"
              + "FG  '" + vidInFileName   + "'\n"
              + "OUT '" + vidOutFileName  + "'\n"
              + "BG  '" + vidBgFileName   + "'");

      tv = new BackGroundAvailable(vidInFileName, vidOutFileName, vidBgFileName);


      String matchRange     = "";
      String featherRange   = "";

      try {
        int fmStart   = -1;
        int fmStop    = -1;

        if (args.length >= 4) {
          fmStart = Integer.parseInt(args[3]);

          if (args.length >= 5)
            fmStop = Integer.parseInt(args[4]);
          else
            fmStop = Integer.MAX_VALUE;

          tv.setFirstLastFame(fmStart, fmStop);
        }

        if (args.length >= 6) {
          tv.setMatchRange(Integer.parseInt(args[5]));
        }

        if (args.length >= 7) {
          tv.setFeatherSize(Integer.parseInt(args[6]));
        }

      } catch (Exception e) {
        System.out.println("execBackground() Illegal integer in command line arguments");
        return;
      }

      System.out.println("Main.Background() \n"
              + "Frame.Start:\t"  + tv.startFrame   + "\n"
              + "Frame.Stop:\t\t"   + tv.stopFrame    + "\n"
              + "Match.Range:\t"  + tv.matchRange   + "\n"
              + "Feather.Size:\t" + tv.featherSize  + "\n");

      execTransform();  // Execute the specific transform for this derived class
    } // execBackground()


    private void help() {
        System.out.println("Video App: Bad arguments\n"
                + "\nUSAGE:\n"
                + "-testpattern\n"
                + "-copytest\n"
                + "-background <foregroundMP4> <outputMov> <backgroundMP4> <StartFrame> <EndFrame> <MatchRange> <FeatherSize> "
        );
    }


    private void commandInterpreter(String[] args) throws IOException, JCodecException {
        System.out.println("commandInterpreter() START");

        if (args==null || (args.length < 1)) {
          help();
          return;
        }

        String cmdHandle = args[0];
        String[] cmdArgs = new String[args.length - 1];       // Allocate a smaller string array
        System.arraycopy(args, 1, cmdArgs,0, cmdArgs.length);// Put back in original array

        switch (cmdHandle.toLowerCase()) {
            case ("-testpattern"):
                execTestPattern(cmdArgs);
                break;
            case ("-copytest"):
                execTestCopy("P:\\Dad\\VideoSoftware\\TestPattern.mp4", "P:\\Dad\\VideoSoftware\\TestPatternCopy.mov");
                break;
            case ("-background"):
                execBackground(cmdArgs);
                break;
            default:
                help();
                break;
        }  // switch

        System.out.println("commandInterpreter() DONE");
    } // commandInterpreter()


    public static void main(String[] args) throws IOException, JCodecException {
      System.out.println("Main() START");
      Main videoApp = new Main(); // Be done with static
      videoApp.commandInterpreter(args);
    }  // main()
} // class
