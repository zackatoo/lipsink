/*
 * LipSink Controller
 */

package com.zackatoo.lipsink;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import com.zackatoo.lipsink.record.AudioRecorder;
import com.zackatoo.lipsink.record.MouthLandmarkPoints;
import com.zackatoo.lipsink.record.WebcamRecorder;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 *
 * @author 
 */
public class LipSinkController implements Initializable
{
    private static final String CONNECTING_PATH = "images/connectToWebcam.png";

    private Config config = Config.getConfig();
    private static WebcamRecorder webcamRecorder;
    private AudioRecorder audioRecorder;
    private Image connecting;
    private boolean running = false;

    @FXML private ImageView webcamView;
    @FXML private CheckBox chxBx_hideLandmarks;
    @FXML private Rectangle stopButton;
    @FXML private Polyline startButton;
    
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        connecting = new Image(CONNECTING_PATH);
        stopButton.setVisible(false);
    }

    public void shutdown()
    {
        running = false;
        Config.writeConfig();
        webcamRecorder.stop();
        audioRecorder.finish();
        audioRecorder.cancel(true);
    }

    @FXML private void startWebcam()
    {
        if (!running)
        {
            System.out.println("starting recording");
            startButton.setVisible(false);
            stopButton.setVisible(true);
            running = true;
            webcamView.setImage(connecting);
            webcamRecorder = new WebcamRecorder(webcamView);
            webcamRecorder.start();

            audioRecorder = new AudioRecorder();
            Thread thread = new Thread(audioRecorder);
            thread.start();
        }
    }

    @FXML private void stopWebcam()
    {
        if (running)
        {
            stopButton.setVisible(false);
            startButton.setVisible(true);
            running = false;
            webcamRecorder.stop();
            audioRecorder.finish();
            audioRecorder.cancel(true);
            launchSubStage("Export Animated Mouth", "panels/exportpanel/ExportDocument.fxml");
        }
    }

    @FXML private void handle_hideLandmarksUpdate()
    {
        webcamRecorder.updateHideLandmarksFlag(chxBx_hideLandmarks.isSelected());
    }

    public static ArrayList<MouthLandmarkPoints.Frame> getRecordedFrames()
    {
        return webcamRecorder.getPoints();
    }

    private void launchSubStage(String title, String fxmlDocument)
    {
        Stage subStage = new Stage();
        subStage.setTitle(title);

        Parent root = null;
        FXMLLoader loader = null;
        LipSinkSubControllers controller = null;

        try
        {
            loader = new FXMLLoader(getClass().getResource(fxmlDocument));
            root = loader.load();
            controller = loader.getController();
        }
        catch (Exception e)
        {
            System.err.println("An error occuring trying to load the substage '" + title + "'");
            e.printStackTrace();
            return;
        }

        Scene scene = new Scene(root);
        subStage.setScene(scene);
        controller.setThisStage(subStage, false);
        controller.setParentController(this);
        controller.startup();
        subStage.show();

        controller.setParentStage(null);

    }
}
