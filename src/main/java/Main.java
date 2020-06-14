import javafx.application.Application;
import javafx.stage.Stage;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import java.io.File;
import java.io.IOException;

public class Main extends Application {

    public void start(Stage primaryStage) throws Exception {
    }

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
        File file = new File(args[0]);

//        Picture picture = FrameGrab.getFrameFromFile(scottWheelerFile, 2);

        FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
        grab.seekToSecondPrecise(1);

        for (int i=0;i<frameCount;i++) {
            Picture picture = grab.getNativeFrame();
            System.out.println(picture.getWidth() + "x" + picture.getHeight() + " " + picture.getColor());
        }
    }
}
