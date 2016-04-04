/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package data.items;

import data.ConfigurableObservableObject;
import data.simulation.events.Event;
import geom.LatLng;
import geom.Spherical;
import gui.PromptDistanceAzimuth;
import gui.map.GoogleMapCallback;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author alvarolb
 */
public class HeatMap extends ConfigurableObservableObject implements Serializable {
    public static final String CATEGORY = "HeatMaps";

    private List<LatLng> heatmapPoints;
    private List<LatLng> temporalHeatMapPoints;
    private transient boolean heatmapCreated = false;

    private transient GoogleMapCallback pathCallback;

    private transient ToggleButton addWaypointsNode;
    private transient Button removeWaypointNode;
    private transient Button manualAddWaypointNode;
    private transient ComboBox comboBoxWaypointNode;

    public HeatMap(String polygonId) {
        super(polygonId, CATEGORY);
        heatmapPoints = new ArrayList<>();
        temporalHeatMapPoints = new ArrayList<>();
    }

    private transient List<Node> nodes = null;

    @Override
    protected List<Node> getConfigurableNodes() {
        
        if(nodes!=null){
            return nodes;
        }
        
        nodes = new ArrayList<>();
        nodes.add(new Text("HeatMap points"));
        nodes.add(new Separator());
        
        final Node addIcon = new ImageView(new Image(getClass().getResourceAsStream("/img/add.png")));
        final Node removeIcon = new ImageView(new Image(getClass().getResourceAsStream("/img/remove.png")));
        final Node manualAddIcon = new ImageView(new Image(getClass().getResourceAsStream("/img/manual.png")));

        pathCallback = new GoogleMapCallback() {
            @Override
            public void onLocationSelected(double latitude, double longitude) {
                addHeatPoint(new LatLng(latitude, longitude));
            }
        };

        addWaypointsNode = new ToggleButton();
        addWaypointsNode.setGraphic(addIcon);

        removeWaypointNode = new Button();
        removeWaypointNode.setGraphic(removeIcon);

        manualAddWaypointNode = new Button();
        manualAddWaypointNode.setGraphic(manualAddIcon);

        comboBoxWaypointNode = new ComboBox(FXCollections.observableList(heatmapPoints));
        comboBoxWaypointNode.setPrefWidth(150);

        addWaypointsNode.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                if (t1) {
                    gMap.addCallback(pathCallback);
                } else {
                    gMap.removeCallback(pathCallback);
                }
            }
        });

        removeWaypointNode.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                removeCurrentHeatPoint();
            }
        });

        manualAddWaypointNode.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                if(heatmapPoints.size()>0){
                    new PromptDistanceAzimuth() {
                        @Override
                        public boolean onAccept(double distance, double azimuth) {
                            if (distance > 0 && azimuth >= 0 && azimuth <= 360) {

                                LatLng lastItem = heatmapPoints.get(heatmapPoints.size() - 1);
                                LatLng offset = Spherical.getRhumbDestinationPoint(lastItem, distance, azimuth);

                                addHeatPoint(offset);
                                return true;
                            }
                            return false;
                        }
                    }.show();
                }
            }
        });

        GridPane waypointsNode = new GridPane();
        waypointsNode.add(new Label("Points"), 0, 0);
        waypointsNode.add(addWaypointsNode, 1, 0);
        waypointsNode.add(removeWaypointNode, 2, 0);
        waypointsNode.add(manualAddWaypointNode, 3, 0);
        waypointsNode.setHgap(5);
        
        nodes.add(waypointsNode);
        nodes.add(comboBoxWaypointNode);
        
        nodes.add(new Text("Modify Polyline"));
        nodes.add(new Separator());
        
        final BooleanItem modify = new BooleanItem("Modify points", false) {
            @Override
            public void onValueChanged(boolean value) {
                if (value) {
                    gMap.startEditObject(getObjectId());
                } else {
                    gMap.endEditObject(getObjectId());
                    List<LatLng> objectPath = gMap.getObjectPath(getObjectId());
                    comboBoxWaypointNode.getItems().clear();
                    comboBoxWaypointNode.getItems().addAll(objectPath);
                }
            }
        };
        nodes.add(modify);
        
        return nodes;
    }
    
    public void addHeatPoint(LatLng heatPoint) {
        if(comboBoxWaypointNode!=null) {
            // add waypoint
            comboBoxWaypointNode.getItems().add(heatPoint);

            // set current waypoint
            comboBoxWaypointNode.setValue(heatPoint);
        }else{
            heatmapPoints.add(heatPoint);
        }

        if(heatmapCreated){
            temporalHeatMapPoints.add(heatPoint);
        }

        setOnMap();
    }

    public void removeCurrentHeatPoint() {
        if (comboBoxWaypointNode.getValue() != null) {
            // get current waypoint
            LatLng wayPoint = (LatLng) comboBoxWaypointNode.getValue();

            // remove waypoint
            comboBoxWaypointNode.getItems().remove(wayPoint);
        }
        
        setOnMap();
    }

    private String getPointsArray(List<LatLng> points) {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append('[');
        if (points.size() > 0) {
            for (LatLng point : points) {
                strBuilder.append('[');
                strBuilder.append(point.lat());
                strBuilder.append(',');
                strBuilder.append(point.lon());
                strBuilder.append("],");
            }
            strBuilder.deleteCharAt(strBuilder.length() - 1);
        }
        strBuilder.append(']');
        return strBuilder.toString();
    }

    @Override
    public Map<String, String> getMapRepresentation() {
        if(heatmapPoints.isEmpty()) return null;
        Map<String, String> path = new HashMap<>();
        path.put("object_id", getObjectId());
        path.put("type", "object");
        path.put("object_type", heatmapCreated ? "heatmap_update" : "heatmap");
        path.put("heat_points", getPointsArray(heatmapCreated ? temporalHeatMapPoints : heatmapPoints));
        temporalHeatMapPoints.clear();
        if(!heatmapCreated) heatmapCreated = true;
        return path;
    }

    @Override
    public void onEventReceived(Event event) {

    }

    public List<LatLng> getHeatmapPoints()
    {
        return heatmapPoints;
    }
}
