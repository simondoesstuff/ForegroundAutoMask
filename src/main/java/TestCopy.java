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

public class TestCopy extends TransformVideo {
  private TestCopy(String infile) throws IOException, JCodecException {   // Private to force caller to provide both file names
    super(infile);
  }

  public TestCopy(String infile, String outfile) throws IOException, JCodecException {
    super(infile, outfile);
  }

  ///////////////////////////////////////////////////////////////////
  // This test verifies that an mp4 video can be read in a frame at a
  // time and output/encoded into a .mov file that is effectively
  // identical.  Return true for success and false for error.
  // Helps the caller bring up help() when things go wrong.
  ///////////////////////////////////////////////////////////////////
  @Override
  public boolean execTransform() throws IOException, JCodecException {
    System.out.println("TestCopy.execTransform() VIDEO FILES:  " + vidInFileName + "  " + vidOutFileName);

//    int   frameCount  = 200000;
//    long  frameNo     = -1;

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
  } // execTransform()
} // class
