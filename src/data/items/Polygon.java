package data.items;

import data.ConfigurableObservableObject;
import data.simulation.events.Event;
import geom.LatLng;
import geom.Spherical;
import gui.PromptDistanceAzimuth;
import gui.map.GoogleMapCallback;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import util.XmlUtils;

public class Polygon extends ConfigurableObservableObject implements Serializable {
    public static final String CATEGORY = "Polygons";
    
    private List<LatLng> polygonPoints;
     
    private transient GoogleMapCallback pathCallback;
    private transient ToggleButton addWaypointsNode;
    private transient Button removeWaypointNode;
    private transient Button manualAddWaypointNode;
    private transient ComboBox comboBoxWaypointNode;
 
    public Polygon(String polygonId) {
        super(polygonId, CATEGORY);
        polygonPoints = new ArrayList<>();
    }
    
    private transient List<Node> nodes = null;

    @Override
    protected List<Node> getConfigurableNodes() {
        
        if(nodes!=null){
            return nodes;
        }
        
        nodes = new ArrayList<>();
        nodes.add(new Text("Polygon points"));
        nodes.add(new Separator());
        
        final Node addIcon = new ImageView(new Image(getClass().getResourceAsStream("/img/add.png")));
        final Node removeIcon = new ImageView(new Image(getClass().getResourceAsStream("/img/remove.png")));
        final Node manualAddIcon = new ImageView(new Image(getClass().getResourceAsStream("/img/manual.png")));
        
        pathCallback = new GoogleMapCallback() {
            @Override
            public void onLocationSelected(double latitude, double longitude) {
                addWaypoint(new LatLng(latitude, longitude));
            }
        };

        addWaypointsNode = new ToggleButton();
        addWaypointsNode.setGraphic(addIcon);

        removeWaypointNode = new Button();
        removeWaypointNode.setGraphic(removeIcon);

        manualAddWaypointNode = new Button();
        manualAddWaypointNode.setGraphic(manualAddIcon);

        comboBoxWaypointNode = new ComboBox(FXCollections.observableList(polygonPoints));
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
                removeCurrentWaypoint();
            }
        });

        manualAddWaypointNode.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                if(polygonPoints.size()>0){
                    new PromptDistanceAzimuth() {
                        @Override
                        public boolean onAccept(double distance, double azimuth) {
                            if (distance > 0 && azimuth >= 0 && azimuth <= 360) {

                                LatLng lastItem = polygonPoints.get(polygonPoints.size() - 1);
                                LatLng offset = Spherical.getRhumbDestinationPoint(lastItem, distance, azimuth);

                                addWaypoint(offset);
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
        
        nodes.add(new Text("Modify Polygon"));
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
    
    public void addWaypoint(LatLng wayPoint) {
        // add waypoint
        comboBoxWaypointNode.getItems().add(wayPoint);

        // set current waypoint
        comboBoxWaypointNode.setValue(wayPoint);
        
        setOnMap();
    }

    public void removeCurrentWaypoint() {
        if (comboBoxWaypointNode.getValue() != null) {
            // get current waypoint
            LatLng wayPoint = (LatLng) comboBoxWaypointNode.getValue();

            // remove waypoint
            comboBoxWaypointNode.getItems().remove(wayPoint);
        }
        
        setOnMap();
    }

    public static String getPath(List<LatLng> points) {
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
        if(polygonPoints.isEmpty()) return null;

        Map<String, String> path = new HashMap<>();
        path.put("object_id", getObjectId());
        path.put("type", "object");
        path.put("object_type", "poly");
        path.put("poly_path", getPath(polygonPoints));
        return path;
    }
    
    public void addKMLPolygonPlaceMark(Document doc, Element document) {
        Element placeMark = doc.createElement("Placemark");
        placeMark.setAttribute("id", getObjectId());
        document.appendChild(placeMark);

        // add the name (the track id)
        Element name = doc.createElement("name");
        name.appendChild(doc.createTextNode(objectTag != null ? objectTag : getObjectId()));
        placeMark.appendChild(name);

        // add color
        Element style = doc.createElement("Style");
        Element lineStyle = doc.createElement("LineStyle");
        style.appendChild(lineStyle);
        
        Element polyStyle = doc.createElement("PolyStyle");
        style.appendChild(polyStyle);
        
        Element lineColor = doc.createElement("color");
        StringBuilder kmlColor = new StringBuilder();
        //#rrggbb -> aabbggrr
        kmlColor.append("ff");
        if(objectColor!=null)
        {
            kmlColor.append(objectColor.substring(5));
            kmlColor.append(objectColor.substring(3, 5));
            kmlColor.append(objectColor.substring(1, 3));
        }else{
            kmlColor.append("000000");
        }
        
        Element polyColor = doc.createElement("color");
        StringBuilder polyKmlColor = new StringBuilder();
        //#rrggbb -> aabbggrr
        polyKmlColor.append("77");
        if(objectColor!=null)
        {
            polyKmlColor.append(objectColor.substring(5));
            polyKmlColor.append(objectColor.substring(3, 5));
            polyKmlColor.append(objectColor.substring(1, 3));
        }else{
            polyKmlColor.append("000000");
        }
        
        lineColor.appendChild(doc.createTextNode(kmlColor.toString()));
        polyColor.appendChild(doc.createTextNode(polyKmlColor.toString()));
        lineStyle.appendChild(lineColor);
        polyStyle.appendChild(polyColor);
              
        placeMark.appendChild(style);

        Element visibility = doc.createElement("visibility");
        visibility.setTextContent(showOnMap ? "1" : "0");
        placeMark.appendChild(visibility);
        
        
        Element polygon = doc.createElement("Polygon");
        placeMark.appendChild(polygon);
        
        Element outerBoundaryIs = doc.createElement("outerBoundaryIs");
        polygon.appendChild(outerBoundaryIs);
        
        Element linearRing = doc.createElement("LinearRing");
        outerBoundaryIs.appendChild(linearRing);

        Element coordinates = doc.createElement("coordinates");
        linearRing.appendChild(coordinates);
        
        StringBuilder coordStr = new StringBuilder();
        
        if(polygonPoints.size()>0){
            
            // add first point
            polygonPoints.add(polygonPoints.get(0));

            for (LatLng wayPoint : polygonPoints) {
                coordStr.append(wayPoint.lon());
                coordStr.append(',');
                coordStr.append(wayPoint.lat());
                coordStr.append(',');
                coordStr.append(wayPoint.alt());
                coordStr.append(' ');
            }
            
            // remove last point
            polygonPoints.remove(polygonPoints.get(polygonPoints.size()-1));
        }
        coordinates.appendChild(doc.createTextNode(coordStr.toString()));
    }
    
    public static Polygon buildPolygonFromXMLNode(Element node) {
        Polygon polygon = null;

        String name = XmlUtils.getText(XmlUtils.findChild(node, "name"));
        String id = node.getAttribute("id");
        String color = XmlUtils.getText(XmlUtils.findChildByChain(node, new String[]{"Style", "LineStyle", "color"}));
        String visible = XmlUtils.getText(XmlUtils.findChild(node, "visibility"));
        String polygonPoints = XmlUtils.getText(XmlUtils.findChildByChain(node, new String[]{"Polygon", "outerBoundaryIs", "LinearRing", "coordinates"}));

        if(polygonPoints==null || "".equals(polygonPoints))
        {
            return polygon;
        }
        
        polygon = new Polygon(id);
        polygon.objectTag = name;

        if (color != null) {
            StringBuilder kmlColor = new StringBuilder();
            //#rrggbb <- aabbggrr
            kmlColor.append('#');
            kmlColor.append(color.substring(6));
            kmlColor.append(color.substring(4, 6));
            kmlColor.append(color.substring(2, 4));
            polygon.objectColor = kmlColor.toString();
        }

        polygon.showOnMap = false;

        // parse waypoints
        if (polygonPoints != null) {
            Pattern doublePattern = Pattern.compile("([-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)");

            Matcher pointsMatcher = doublePattern.matcher(polygonPoints);
            
            while (pointsMatcher.find()) {
                // in kml the longitude is before latitude
                double longitude = Double.parseDouble(pointsMatcher.group(1));
                pointsMatcher.find();
                double latitude = Double.parseDouble(pointsMatcher.group(1));
                pointsMatcher.find();
                double altitude = Double.parseDouble(pointsMatcher.group(1));
           
                LatLng waypoint = new LatLng(latitude, longitude);
                polygon.polygonPoints.add(waypoint);
                
            }
            
            // remove last waypoint if equal to the first one
            if(polygon.polygonPoints.size()>1)
            {
                LatLng first = polygon.polygonPoints.get(0);
                LatLng last = polygon.polygonPoints.get(polygon.polygonPoints.size()-1);
                if(first.lat() == last.lat() && first.lon() == last.lon())
                {
                    polygon.polygonPoints.remove(last);
                }
            }
        }

        polygon.showOnMap = "1".equals(visible);

        return polygon;
    }

    @Override
    public void onEventReceived(Event event) {

    }
}
