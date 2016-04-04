/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.map;

import geom.LatLng;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import netscape.javascript.JSObject;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author Alvaro
 */
public class GoogleMap extends VBox implements GoogleMapCallback, GoogleMapObjectCallback{
    private Timeline locationUpdateTimeline;
    private final WebView webView;
    private final WebEngine webEngine;
    private final List<GoogleMapCallback> callbacks_;
    private final List<GoogleMapObjectCallback> objectCallbacks_;
    
    public void addCallback(GoogleMapCallback callback) {
        if (!callbacks_.contains(callback)) {
            callbacks_.add(callback);
            if(callbacks_.size()==1)
            {
                webEngine.executeScript("startListeningLocations();");
            }
        }
    }

    public void removeCallback(GoogleMapCallback callback) {
        if(callbacks_.remove(callback)){
            if(callbacks_.isEmpty())
            {
                webEngine.executeScript("stopListeningLocations();");
            }
        }
    }
    
    public void addObjectCallback(GoogleMapObjectCallback callback) {
        if (!objectCallbacks_.contains(callback)) {
            objectCallbacks_.add(callback);
        }
    }

    public void removeObjectCallback(GoogleMapObjectCallback callback) {
        objectCallbacks_.remove(callback);
    }
    
    @Override
    public void onLocationSelected(double latitude, double longitude) {
        for (GoogleMapCallback callback : callbacks_) {
            callback.onLocationSelected(latitude, longitude);
        }
    }
    
    public GoogleMap() {
        this.webView = new WebView();
        this.webView.fontSmoothingTypeProperty().setValue(FontSmoothingType.LCD);
        this.webView.setContextMenuEnabled(true);
        this.webEngine = webView.getEngine();
        this.callbacks_ = new CopyOnWriteArrayList<>();
        this.objectCallbacks_ = new CopyOnWriteArrayList<>();

        webEngine.getLoadWorker().stateProperty().addListener(
                new ChangeListener<State>() {
                    @Override
                    public void changed(ObservableValue<? extends State> ov, State oldState, State newState) {
                        if (newState == State.SUCCEEDED) {
                            JSObject window = (JSObject) webEngine.executeScript("window");
                            window.setMember("javacallback", GoogleMap.this);
                        }
                    }
                });

        webEngine.setOnAlert(new EventHandler<WebEvent<String>>() {
            @Override
            public void handle(WebEvent<String> event) {
                System.err.println("Alert: " + event.getData());
            }
        });

        webEngine.load(getClass().getResource("googlemap.html").toString());

        /**
         * TOOL BAR FOR MAP CONTROL
         */
        final ToggleGroup mapTypeGroup = new ToggleGroup();
        final ToggleButton road = new ToggleButton("Road");
        road.setSelected(true);
        road.setToggleGroup(mapTypeGroup);
        final ToggleButton satellite = new ToggleButton("Satellite");
        satellite.setToggleGroup(mapTypeGroup);
        final ToggleButton hybrid = new ToggleButton("Hybrid");
        hybrid.setToggleGroup(mapTypeGroup);
        final ToggleButton terrain = new ToggleButton("Terrain");
        terrain.setToggleGroup(mapTypeGroup);
        mapTypeGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override
            public void changed(ObservableValue<? extends Toggle> observableValue, Toggle toggle, Toggle toggle1) {
                if (road.isSelected()) {
                    webEngine.executeScript("setMapTypeRoad()");
                } else if (satellite.isSelected()) {
                    webEngine.executeScript("setMapTypeSatellite()");
                } else if (hybrid.isSelected()) {
                    webEngine.executeScript("setMapTypeHybrid()");
                } else if (terrain.isSelected()) {
                    webEngine.executeScript("setMapTypeTerrain()");
                }
            }
        });

        final CheckBox showTags = new CheckBox("Show Tags");
        showTags.setSelected(true);
        showTags.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean aBoolean2) {
                if(aBoolean2){
                    webEngine.executeScript("showTags()");
                }else{
                    webEngine.executeScript("hideTags()");
                }
            }
        });


        final TextField followTrack = new TextField("");
        followTrack.setPromptText("SimulatedTrack Object Id");
        followTrack.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String s1) {
                String objectId = followTrack.getText();
                if (objectId.length() > 0) {
                    setTracingObject(objectId);
                } else {
                    removeTracingObject();
                }
            }
        });


        // add search function
        final TextField searchBox = new TextField("");
        searchBox.setPromptText("Search Location");
        searchBox.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String s1) {
                // delay location updates to we don't go too fast file typing
                if (locationUpdateTimeline != null) {
                    locationUpdateTimeline.stop();
                }
                locationUpdateTimeline = new Timeline();
                locationUpdateTimeline.getKeyFrames().add(
                        new KeyFrame(new Duration(400), new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent actionEvent) {
                        webEngine.executeScript("goToLocation(\"" + searchBox.getText() + "\")");
                    }
                }));
                locationUpdateTimeline.play();
            }
        });

        Button zoomIn = new Button("Zoom In");
        zoomIn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                webEngine.executeScript("zoomIn()");
            }
        });

        Button zoomOut = new Button("Zoom Out");
        zoomOut.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                webEngine.executeScript("zoomOut()");
            }
        });
        
        Button snapShot = new Button("Snapshot");
        snapShot.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
               File snapshotFolder = new File("snapshot"); 
               snapshotFolder.mkdirs();
               File destFile = new File(snapshotFolder, "snapshot_" + getDateTime() + ".png");
               WritableImage snapshot = webView.snapshot(new SnapshotParameters(), null);
               RenderedImage renderedImage = SwingFXUtils.fromFXImage(snapshot, null);
                try {
                    ImageIO.write(renderedImage, "png", destFile);
                } catch (IOException ex) {
                    Logger.getLogger(GoogleMap.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        // create toolbar
        ToolBar mapToolbar = new ToolBar();
        //toolBar.getStyleClass().add("map-toolbar");
        mapToolbar.getItems().addAll(
                road, satellite, hybrid, terrain,
                createSpacer(), showTags, createSpacer(),
                followTrack,
                createSpacer(),
                searchBox, zoomIn, zoomOut, createSpacer(), snapShot);


        VBox.setVgrow(webView, Priority.ALWAYS);
        getChildren().addAll(mapToolbar, webView);
    }

    public void clearAllObjects() {
        webEngine.executeScript("removeAllObjects();");
    }

    public void panToObject(String objectId) {
        String command = "panToObject(" + convertObjectId(objectId) + ");";
        runCommand(command);
    }

    public void zoomToObject(String objectId) {
        runCommand("zoomToObject(" + convertObjectId(objectId) + ");");
    }

    public void setTracingObject(String objectId) {
        runCommand("setTracingObject(" + convertObjectId(objectId) + ");");
    }

    public void removeTracingObject() {
        runCommand("removeTracing();");
    }

    public void setObject(Map<String, String> data) {
        data.remove("object_operation");
        data.remove("category");
        String command = "setObject(" + getJSMap(data) + ");";
        runCommand(command);
    }

    public void startEditObject(String objectId) {
        String command = "startEditObject(" + convertObjectId(objectId) + ");";
        runCommand(command);
    }

    public void endEditObject(String objectId) {
        String command = "endEditObject(" + convertObjectId(objectId) + ");";
        runCommand(command);
    }
    
    public List<LatLng> getObjectPath(String objectId) {
        List<LatLng> path = new ArrayList<>();
        
        String command = "getObjectPath(" + convertObjectId(objectId) + ");";
        Object response = webEngine.executeScript(command);
     
        JSObject jsret = (JSObject)response;
        Object len = jsret.getMember("length");
        if (len instanceof Number){
          int n = ((Number)len).intValue();
          for (int i = 0; i < n; ++i){
              String latLng = jsret.getSlot(i).toString();
              double lat = Double.parseDouble(latLng.substring(1, latLng.indexOf(',')));
              double lng = Double.parseDouble(latLng.substring(latLng.indexOf(',')+1, latLng.length()-1));
              path.add(new LatLng(lat, lng));
          }
        }
                    
        return path;
    }
        
    private String convertObjectId(String objectId) {
        if (isNumber(objectId)) {
            return objectId;
        } else {
            return "'" + objectId + "'";
        }
    }
    private void runCommand(String command) {
        //System.out.println(command);
        webEngine.executeScript(command);
    }
    private static final Pattern numberPattern = Pattern.compile("-?\\d+");

    public static boolean isNumber(String string) {
        return string != null && numberPattern.matcher(string).matches();
    }

    public void removeObject(String objectId) {
        String command = "removeObject(" + convertObjectId(objectId) + ");";
        runCommand(command);
    }

    private String getJSMap(Map<String, String> values) {
        StringBuilder strBuffer = new StringBuilder();
        strBuffer.append('{');
        for (Entry<String, String> entry : values.entrySet()) {
            strBuffer.append(entry.getKey());
            strBuffer.append(':');
            try {
                // not is an array
                if (!entry.getValue().startsWith("[") && !entry.getValue().startsWith("{")) {
                    double d = Double.parseDouble(entry.getValue());
                    strBuffer.append(d);
                } else {
                    strBuffer.append(entry.getValue());
                }
            } catch (NumberFormatException nfe) {
                strBuffer.append('\'');
                strBuffer.append(entry.getValue());
                strBuffer.append('\'');
            }
            strBuffer.append(',');
        }
        // remove trailing comma
        strBuffer.deleteCharAt(strBuffer.length() - 1);
        strBuffer.append('}');
        //System.out.println(strBuffer.toString());
        return strBuffer.toString();
    }

    private Node createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
    
    private static String getDateTime()  
    {  
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");  
        df.setTimeZone(TimeZone.getDefault());  
        return df.format(new Date());  
    }

    @Override
    public void onTagSelected(String objectId) {
        for (GoogleMapObjectCallback callback : objectCallbacks_) {
            callback.onTagSelected(objectId);
        }
    }

    @Override
    public void onTagDoubleClick(String objectId) {
        for (GoogleMapObjectCallback callback : objectCallbacks_) {
            callback.onTagDoubleClick(objectId);
        }
    }
}
