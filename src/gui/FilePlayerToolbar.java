/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import data.input.DataFeeder;
import data.input.DataFileFeeder;
import data.input.DataListener;
import data.items.InfoData;
import data.items.ObjectData;
import java.io.File;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;

/**
 *
 * @author Alvaro
 */
public class FilePlayerToolbar extends ToolBar implements DataListener{
    private final Button play;
    private final Button pause;
    private final Button stop;
    private final CheckBox checkBoxClear;
    private DataFeeder dataFeed;
    private final DataListener listener;
    
    public FilePlayerToolbar(DataListener listener){
        this.listener = listener;
        final TextField fileBox = new TextField("");
        final Button selectFile = new Button("...");
        final Slider speed = new Slider(1, 100, 1);
        checkBoxClear = new CheckBox("Clear on start");
        checkBoxClear.setSelected(true);
        play = new Button("Play");
        pause = new Button("Pause");
        stop = new Button("Stop");
        play.setDisable(false);
        pause.setDisable(true);
        stop.setDisable(true);
        
        selectFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) { 
              FileChooser fileChooser = new FileChooser();
              // try to select log file as the initial file directory
              File log = new File("log");
              if(log.exists()){
                fileChooser.setInitialDirectory(log);
              }
              
              //Set extension filter
              FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Log files (*.log)", "*.log");
              fileChooser.getExtensionFilters().add(extFilter);
             
              //Show open file dialog
              File file = fileChooser.showOpenDialog(null);
              if(file!=null)
              {
                fileBox.setText(file.getPath());
              }
            }
        });
        
        play.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent){
                if(dataFeed==null){
                    dataFeed = new DataFileFeeder(FilePlayerToolbar.this, fileBox.getText());
                    dataFeed.setSpeed((int)speed.getValue());
                }    
                dataFeed.start();
            }
        });
        
        pause.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) { 
                dataFeed.pause();
            }
        });
        
        stop.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if(dataFeed!=null){
                    dataFeed.stop();
                    dataFeed = null;
                }
            }
        });
        
        final Label speedLabel = new Label("1x");
         
        speed.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
                int speedValue = new_val.intValue();
                speedLabel.setText(speedValue + "x");
                
                if(dataFeed!=null){
                    dataFeed.setSpeed(speedValue);
                }
            }
        });
        
        getItems().addAll(new Label("File Player: " ), fileBox, selectFile, createSpacer(), play, pause, stop, createSpacer(), checkBoxClear, createSpacer(), new Label("Speed: " ), speed, speedLabel);
    }

    @Override
    public void onInfoDataReceived(InfoData infoData) {
        listener.onInfoDataReceived(infoData);
    }

    @Override
    public void onObjectDataReceived(ObjectData objectData) {
        listener.onObjectDataReceived(objectData);
    }

    @Override
    public void onStart(boolean reset) {
        play.setDisable(true);
        pause.setDisable(false);
        stop.setDisable(false);
        listener.onStart(checkBoxClear.isSelected());
    }
    
    @Override
    public void onResume() {
        play.setDisable(true);
        pause.setDisable(false);
        stop.setDisable(false);
        listener.onResume();
    }

    @Override
    public void onPause() {
        play.setDisable(false);
        pause.setDisable(true);
        stop.setDisable(false);
        listener.onPause();
    }
    
    @Override
    public void onStop() {
        play.setDisable(false);
        pause.setDisable(true);
        stop.setDisable(true);
        dataFeed = null;
        listener.onStop();
    }
    
    private Node createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

}
