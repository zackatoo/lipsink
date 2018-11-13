/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zackatoo.lipsink;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application
{
    
    @Override
    public void start(Stage stage) throws Exception
    {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("LipSinkDocument.fxml"));
        Parent root = loader.load();

        LipSinkController controller = loader.getController();
        
        Scene scene = new Scene(root);

        scene.getStylesheets().add("com/zackatoo/lipsink/stylesheets/Base.css");

        stage.setScene(scene);
        stage.setOnCloseRequest(event -> controller.shutdown());
        stage.setTitle("LipSink");
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        launch(args);
    }
    
}
