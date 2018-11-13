/*
 * WebcamRecorder and it's sister class FaceDetection were taken and adapted from
 * https://github.com/bytedeco/javacv/blob/master/samples/LBFFacemarkExampleWithVideo.java
 * and https://github.com/opencv-java/face-detection
 */

package com.zackatoo.lipsink.record;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.zackatoo.lipsink.Config;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point2fVector;
import org.bytedeco.javacpp.opencv_core.Point2fVectorVector;
import org.bytedeco.javacpp.opencv_core.Scalar;
import static org.bytedeco.javacpp.opencv_face.drawFacemarks;
import org.bytedeco.javacpp.opencv_videoio.VideoCapture;

import javafx.scene.image.ImageView;

public class WebcamRecorder
{
    private static final int WEBCAM_INDEX = 0;

    private VideoCapture webcam;
    private ScheduledExecutorService timer;
    private FaceDetection faceDetector;
    private MouthLandmarkPoints mouthLandmarkPoints;
    private Config config;
    private ImageView imageView;
    private boolean running = false;

    private boolean hideLandmarks = false;

    public WebcamRecorder(ImageView imageView)
    {
        config = Config.getConfig();
        faceDetector = new FaceDetection();
        mouthLandmarkPoints = new MouthLandmarkPoints();
        webcam = new VideoCapture(WEBCAM_INDEX);

        this.imageView = imageView;
    }

    public void start()
    {
        Runnable record = this::recordFrame;

        timer = Executors.newSingleThreadScheduledExecutor();
        timer.scheduleAtFixedRate(record, 0, 1000 / config.frameRate, TimeUnit.MILLISECONDS);
    }

    public void stop()
    {
        if (timer != null && !timer.isShutdown())
        {
            try
            {
                // Waits one frame for the camera to stop recording
                timer.shutdown();
                timer.awaitTermination(1000 / config.frameRate, TimeUnit.MILLISECONDS);
            }
            catch(InterruptedException e)
            {
                System.err.println("Interrupted before able to shutdown webcam");
            }
        }

        if (webcam.isOpened())
        {
            webcam.release();
        }
    }

    public ArrayList<MouthLandmarkPoints.Frame> getPoints()
    {
        return mouthLandmarkPoints.manufacturePoints();
    }

    private boolean hasWidth = false;
    private boolean first = true;
    private void recordFrame()
    {
        // Throw away the first frame because the webcam takes so long to connect that it isn't accurate
        if (first)
        {
            first = false;
            return;
        }

        Mat frame = new Mat();
        webcam.read(frame);

        Point2fVectorVector landmarks = faceDetector.detectFace(frame);

        if (!hasWidth)
        {
            Integer width = frame.cols();
            Integer height = frame.rows();
            mouthLandmarkPoints.setFrameWidth(width);
            mouthLandmarkPoints.setFrameHeight(height);
        }

        mouthLandmarkPoints.add(landmarks);

        if (landmarks != null && !hideLandmarks)
        {
            for (long i = 0; i < landmarks.size(); i++)
            {
                Point2fVector v = landmarks.get(i);
                drawFacemarks(frame, v, interpretColor(config.landmarkColor));
            }
        }

        Image fxImage = mat2Image(frame);
        onFXThread(imageView.imageProperty(), fxImage);
    }
    
    private Scalar interpretColor(String color)
    {
        switch(color)
        {
            case "YELLOW": return Scalar.YELLOW;
            case "RED": return Scalar.RED;
            case "GREEN": return Scalar.GREEN;
            case "BLUE": return Scalar.BLUE;
            default: return Scalar.YELLOW;
        }
    }

    private static Image mat2Image(Mat frame)
	{
		try
		{
			return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
		}
		catch (Exception e)
		{
			System.err.println("Cannot convert the Mat object: " + e);
			return null;
		}
    }
    
    private static <T> void onFXThread(final ObjectProperty<T> property, final T value)
	{
		Platform.runLater(() -> {
			property.set(value);
		});
    }

    // Gets source pixels from the Mat and creates a bufferedImage with the same pixels
    private static BufferedImage matToBufferedImage(Mat original)
	{
		// init
		BufferedImage image = null;
		int width = original.cols(), height = original.rows(), channels = original.channels();
		byte[] sourcePixels = new byte[width * height * channels];
		//original.get(0, 0, sourcePixels);
		original.data().get(sourcePixels);

		if (original.channels() > 1)
		{
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		}
		else
		{
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
		
		return image;
	}

	public void updateHideLandmarksFlag(boolean flag)
    {
        hideLandmarks = flag;
    }
}
