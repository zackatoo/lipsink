package com.zackatoo.lipsink.panels.exportpanel;

import com.zackatoo.lipsink.LipSinkSubControllers;
import com.zackatoo.lipsink.render.Render;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

import java.io.File;

public class ExportController extends LipSinkSubControllers
{
    public void startup()
    {
        progressBar.setVisible(false);
        txt_exportDirectory.setDisable(true);
        updateUIIdle();
        updateUIDefaults();
    }

    public void closeout()
    {
        if (renderRunning)
        {
            render.cancel(true);
        }
    }

    private boolean renderRunning = false;
    private Render render;
    private static String exportDirectory = ".";

    @FXML private Button btn_cancel;
    @FXML private Button btn_export;
    @FXML private Button btn_chooseFile;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressMessage;

    @FXML private TextField txt_width;
    @FXML private TextField txt_height;
    @FXML private TextField txt_frameRate;
    @FXML private TextField txt_exportDirectory;

    private static Integer width;
    private static Integer height;
    private static Integer frameRate;

    private void updateUIDefaults()
    {
        txt_width.setText("640");
        txt_height.setText("480");
        txt_frameRate.setText("24");
    }

    private void updateUIRunning()
    {
        progressBar.setProgress(0);
        progressBar.setVisible(true);
        btn_cancel.setDisable(false);
        btn_export.setDisable(true);
        btn_chooseFile.setDisable(true);

        txt_width.setDisable(true);
        txt_height.setDisable(true);
        txt_frameRate.setDisable(true);
    }

    private void updateUIIdle()
    {
        progressBar.setVisible(false);
        btn_cancel.setDisable(true);
        btn_export.setDisable(false);
        btn_chooseFile.setDisable(false);
        progressMessage.setText("");

        txt_width.setDisable(false);
        txt_height.setDisable(false);
        txt_frameRate.setDisable(false);
    }

    @FXML private void export()
    {
        updateUIRunning();

        width = Integer.parseInt(txt_width.getText());
        height = Integer.parseInt(txt_height.getText());
        frameRate = Integer.parseInt(txt_frameRate.getText());

        render = new Render();
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(render.progressProperty());
        progressMessage.textProperty().unbind();
        progressMessage.textProperty().bind(render.messageProperty());

        render.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, event -> done());

        renderRunning = true;
        new Thread(render).start();
    }

    @FXML private void cancel()
    {
        render.cancel(true);
        progressBar.progressProperty().unbind();
        progressMessage.textProperty().unbind();

        updateUIIdle();
        renderRunning = false;
        progressBar.setProgress(0);
    }

    @FXML private void chooseFile()
    {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Pick Export Directory");
        chooser.setInitialDirectory(new File(exportDirectory));

        File selectedDirectory = chooser.showDialog(thisStage);
        if (selectedDirectory == null) return;

        exportDirectory = selectedDirectory.getAbsolutePath();
        txt_exportDirectory.setText(exportDirectory);
    }

    private void done()
    {
        progressMessage.textProperty().unbind();
        updateUIIdle();
        boolean success = render.getValue();

        if (success)
        {
            progressMessage.setText("Finished Rendering");
        }
        else
        {
            progressMessage.setText("Error Rendering");
        }
    }

    public static String getExportDirectory()
    {
        return exportDirectory;
    }

    public static Integer getWidth()
    {
        return width;
    }

    public static Integer getHeight()
    {
        return height;
    }

    public static Integer getFrameRate()
    {
        return frameRate;
    }
}
