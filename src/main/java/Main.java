//import com.oracle.awt.AWTUtils;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


// QUESIONS:
//      BufferedImage.nbsp?
//      @see ColorModel
//      @see Raster    WritableRaster raster;
//      @see WritableRaster
//      java.awt.AlphaComposite documentation.

public class Main {
    public static void main(String[] args) throws IOException, JCodecException {
//        Application.launch(args);

        if (args == null || args.length != 1) {
            System.out.println("You must include one argument for the file path. The video file found will be parsed and the first 100 frames starting after the 1st second will have their screen dimensions displayed.");
            return;
        }

        int frameNumber = 42;
        int frameCount = 100;

//        File scottWheelerFile = new File(Main.class.getResource("ScottWheelerProResHQ.mov").getFile());
//        File scottWheelerFile = new File(Main.class.getResource("ScottWheelerProResHQ.mov").getFile());
        File file = new File(args[0]);  // Open Video File

//        Picture picture = FrameGrab.getFrameFromFile(scottWheelerFile, 2);

        FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
        grab.seekToSecondPrecise(1);

        for (int i=0;i<frameCount;i++) {
            Picture picture = grab.getNativeFrame();
            BufferedImage bufIm = AWTUtil.toBufferedImage(picture);  // This is new
            System.out.println(picture.getWidth() + "x" + picture.getHeight() + " " + picture.getColor());
        }
    }
}
