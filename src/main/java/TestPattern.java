import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TestPattern extends TransformVideo {
  public TestPattern(String infile) throws IOException, JCodecException { // Constructor
    super(infile);
  }


  @Override
  protected boolean magicFrame(long fmNo) {  // Used with the test below
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
  @Override
  public boolean execTransform() throws IOException, JCodecException {
    System.out.println("TestPattern.execTransform() VIDEO FILE:  " + vidInFileName);

    frameNo     = 0;

    grabIn = FrameGrab.createFrameGrab(NIOUtils.readableChannel(fileIn));
    grabIn.seekToSecondPrecise(0); // Seek to zero second.  At 24 frames/sec we should be around frame 24.
    TransformVideo.dumpFrameNumber(grabIn);

    while ((pictureIn = grabIn.getNativeFrame()) != null) {
      frameNo = TransformVideo.getFrameNumber(grabIn);

      if (magicFrame(frameNo)) {
        TransformVideo.dumpPicture(pictureIn, frameNo);
        bufImgIn = AWTUtil.toBufferedImage(pictureIn);  //
        dumpBufferedImage(bufImgIn);
      }

      incFramesProcessed();
    }

    return true;
  } // execTransform()
} // class