package com.example.demo2;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class ApplicationStart extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ApplicationStart.class.getResource("main-view.fxml"));
        // Dimensioni fisse minime
        int height = 700;
        int width = 1200;
        Scene scene = new Scene(fxmlLoader.load(), width, height);
        stage.setTitle("Presentazione con slide");
        stage.setScene(scene);
        stage.setMinWidth(700);
        //stage.setMinHeight(height);
        try {
            stage.getIcons().add(
                    new Image(ApplicationStart.class.getResourceAsStream("/icona.png"))
            );
        } catch (Exception e) {
            System.err.println("Icona non trovata: " + e.getMessage());
        }
        stage.show();
    }
}