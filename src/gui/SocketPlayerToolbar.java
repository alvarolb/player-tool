/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import data.input.DataFeeder;
import data.input.DataListener;
import data.input.DataSocketFeeder;
import data.items.InfoData;
import data.items.ObjectData;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 *
 * @author Alvaro
 */
public class SocketPlayerToolbar extends ToolBar implements DataListener{
    private final Button start;
    private final Button stop;
    private final Label status;
    private final TextField portBox;
    private final CheckBox checkBox;
    private final CheckBox checkBoxClear;
    private DataFeeder dataFeed;
    private final DataListener listener;
    
    private void setStatusDown(){
        status.setStyle("-fx-text-fill: red;");
        status.setText("DOWN");
    }
    
    private void setStatusUp(){
        status.setStyle("-fx-text-fill: green;");
        status.setText("UP");
    }
    
    private void setStatusConnected(){
        status.setStyle("-fx-text-fill: blue;");
        status.setText("CONNECTED");
    }
    
    public SocketPlayerToolbar(DataListener listener){
        this.listener = listener;
        portBox = new TextField("25500");
        checkBox = new CheckBox("Save to file");
        checkBoxClear = new CheckBox("Clear on start");
        checkBoxClear.setSelected(true);
        start = new Button("Start");
        stop = new Button("Stop");
        start.setDisable(false);
        stop.setDisable(true);
        status = new Label();
        status.setPrefWidth(80);
        status.setAlignment(Pos.CENTER);
        
        setStatusDown();
        
        start.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent){
                if(dataFeed==null){
                    dataFeed = new DataSocketFeeder(SocketPlayerToolbar.this, Integer.parseInt(portBox.getText()), checkBox.isSelected());                    
                }
                dataFeed.start();
                setStatusUp();
                start.setDisable(true);
                stop.setDisable(false);
                portBox.setDisable(true);
                checkBox.setDisable(true);
            }
        });
        
        stop.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if(dataFeed!=null){
                    dataFeed.stop();
                    dataFeed = null;
                    setStatusDown();
                    start.setDisable(false);
                    stop.setDisable(true);
                    portBox.setDisable(false);
                    checkBox.setDisable(false);
                }
            }
        });
        
        getItems().addAll(new Label("Net Player: " ), portBox, start, stop, createSpacer(), checkBox, createSpacer(), checkBoxClear, status);
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
        setStatusConnected();
        listener.onStart(checkBoxClear.isSelected());
    }
    
    @Override
    public void onResume() {
        // should not be called
        listener.onResume();
    }

    @Override
    public void onPause() {
        // should not be called
        listener.onPause();
    }
    
    @Override
    public void onStop() {
        setStatusUp();
        listener.onStop();
    }
    
    private Node createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
}
