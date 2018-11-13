package com.zackatoo.lipsink.record;

import com.zackatoo.lipsink.Config;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.Point2fVectorVector;
import org.bytedeco.javacpp.opencv_core.RectVector;
import org.bytedeco.javacpp.opencv_core.UMat;
import org.bytedeco.javacpp.opencv_face.Facemark;
import org.bytedeco.javacpp.opencv_face.FacemarkLBF;
import static org.bytedeco.javacpp.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.equalizeHist;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;

public class FaceDetection
{
    private CascadeClassifier faceCascade;
    private Facemark landmarker;
    private Config config;
    private int absoluteFaceSize = 0;

    public FaceDetection()
    {
        config = Config.getConfig();

        faceCascade = new CascadeClassifier(config.faceClassifierPath);
        landmarker = FacemarkLBF.create();
        landmarker.loadModel(config.landmarkModelPath);
    }

    // Given an unaltered frame of video it will run a cascade classifier and search for face landmarks
    // Returns a vector of all faces in the video which each contain a vector of points of the landmarks
    // Can return null if no faces are found or unable to find facemarks
    public Point2fVectorVector detectFace(Mat frame)
    {
        // Creates new Frame for manipulation and copies in old frame
        UMat gray = new UMat();
        frame.copyTo(gray);
        // Converts to grayscale and equalizes for better detection
        cvtColor(gray, gray, COLOR_BGR2GRAY);
        equalizeHist(gray, gray);
        // Create vector of rectangles where the face is approximately
        RectVector faces = new RectVector();

        // Search for faces. Uses JavaCV

        if (this.absoluteFaceSize == 0)
        {
            int height = gray.rows();
            if (Math.round(height * 0.2f) > 0)
            {
                this.absoluteFaceSize = Math.round(height * 0.2f);
            }
        }

        faceCascade.detectMultiScale(gray, faces,1.1, 3, 0, new opencv_core.Size(absoluteFaceSize, absoluteFaceSize), new opencv_core.Size());

        if (!faces.empty())
        {
            Point2fVectorVector landmarks = new Point2fVectorVector();
            boolean successful = landmarker.fit(frame, faces, landmarks);
            return successful ? landmarks : null;
        }

        return null;
    }
}