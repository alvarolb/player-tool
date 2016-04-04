/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 *
 * @author alvarolb
 */
public abstract class PromptDistanceAzimuth extends Stage {

        public abstract boolean onAccept(double distance, double azimuth);

        public PromptDistanceAzimuth(String... types) {
            final GridPane gridPane = new GridPane();
            gridPane.setPadding(new Insets(3, 3, 3, 3));
            gridPane.setVgap(5);
            gridPane.setHgap(5);

            final TextField distance = new TextField();
            gridPane.add(new Text("Distance (m)"), 0, 0);
            gridPane.add(distance, 1, 0);
            
            final TextField azimuth = new TextField();
            gridPane.add(new Text("Azimuth (º)"), 0, 1);
            gridPane.add(azimuth, 1, 1);

            final Button accept = new Button("Accept");
            final Button cancel = new Button("Cancel");
            gridPane.add(accept, 0, 2);
            gridPane.add(cancel, 1, 2);

            accept.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    try{
                        double distanceValue = Double.parseDouble(distance.getText());
                        double azimuthValue = Double.parseDouble(azimuth.getText());
                        if (onAccept(distanceValue, azimuthValue)) {
                            close();
                        }
                    }catch(Exception e){
                        
                    }
                }
            });
            
            cancel.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    close();
                }
            });

            VBox rootBox = new VBox();
            Label title = new Label("Set distance and azimuth");
            rootBox.getChildren().addAll(title, new Separator(), gridPane);

            Scene scene = new Scene(rootBox, 222, 100);
            setScene(scene);
            
            // the request focus should be called when the node is part of a Scene
            distance.requestFocus();
        }
    }