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

  ///////////////////////////////////////////////////////////////////
  // ALGORITHM:
  //    Walk every pixel in every row and column
  ///////////////////////////////////////////////////////////////////

  private void reviseImgOut() {
    int Width   = getInputWidth();
    int Height  = getInputHeight();
    Height=1080;

    for (int row=0; row<Width; row++)
      for (int col=0; col<Height; col++) {
//        System.out.println("Frame Number: " + frameNo + "  Row: " + row + "  Col: " + col + "  " + getInputWidth() + "x" + getInputHeight());
        int pixelIN = bufImgIn.getRGB(row, col);
        int pixelBG = bufImgBg.getRGB(row, col);

        if (pixelIN == pixelBG) {
          bufImgOut.setRGB(row, col,TransformVideo.DCM_ALPHA_MASK); // Black with 0xff Alpha Channel
        }
      }
  } // reviseImgOut()

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

      ///////////////////////////////////////////////////////////
      // Loop through all input frames and send to output encoder
      ///////////////////////////////////////////////////////////

      while ((pictureIn = grabIn.getNativeFrame()) != null) {
        bufImgIn  = AWTUtil.toBufferedImage(pictureIn);   // In frame
        bufImgOut = AWTUtil.toBufferedImage(pictureIn);   // Out frame starts as copy of input frame
        frameNo = getFrameNumber(grabIn);
        System.out.println("Frame Number: " + frameNo);
        reviseImgOut();                                   // Magic algorithm
        encoder.encodeImage(bufImgOut);                   // Write the out image to the encoder & out file
      } // while

      encoder.finish();
    } finally {
      NIOUtils.closeQuietly(fileOut);
    }

    return true;
  } // execTransform()
} // class
