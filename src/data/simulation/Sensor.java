package data.simulation;

import data.ConfigurableObservableObject;
import data.simulation.events.Event;
import data.simulation.track.SimulatedTrack;
import data.simulation.track.TrackDetection;
import geom.LatLng;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.text.Text;
import org.w3c.dom.Element;
import util.XmlUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author alvarolb
 */
public abstract class Sensor extends ConfigurableObservableObject{

    private static final long serialVersionUID = 1L;

    public static final String CATEGORY = "Sensor";
    protected LatLng sensorPosition;
    protected double altitude;
    protected boolean showDetections;
    protected transient Map<String, SimulatedTrack> observedTracksMap;
    protected transient ObservableList<SimulatedTrack> observedTracks;

    public Sensor(String sensorId){
        super(sensorId, CATEGORY);
        this.sensorPosition = new LatLng();
        this.observedTracksMap = new HashMap<>();
        this.observedTracks = FXCollections.observableArrayList();
    }

    // used in serialization for initialize transient objects
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // initialize non-gui transient objects
        this.observedTracksMap = new HashMap<>();
        this.observedTracks = FXCollections.observableArrayList();
    }

    public Map<String, SimulatedTrack> getObservedTracksMap()
    {
        return observedTracksMap;
    }

    public ObservableList<SimulatedTrack> getObservedTracks()
    {
        return observedTracks;
    }

    @Override
    public List<Node> getConfigurableNodes() {
        List<Node> nodes = new ArrayList<>();
        
        nodes.add(new Text("Sensor Location"));
        nodes.add(new Separator());

        final PositionItem positionNode = new PositionItem(sensorPosition) {

            @Override
            public void onLocationChanged(LatLng latlon) {
                sensorPosition = latlon;
                setOnMap();
            }
            
        };
        nodes.add(positionNode);
        
        /*
        // add edit shape button
        final ToggleButton editObjectNode = new ToggleButton("Edit Object");
        editObjectNode.selectedProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                GoogleMap gMap = WebDataPlayer.getGoogleMap();
                if(t1){
                    gMap.startEditObject(getObjectId());
                }else{
                    gMap.endEditObject(getObjectId());
                }
            }
        });
        nodes.add(editObjectNode);*/
        
        
        // add altitude
        final DoubleItemValue altnode = new DoubleItemValue("Altitude (m)", altitude){
            @Override
            public void onValueChanged(double value) {
                altitude = value;
                setOnMap();
            }
        };
        nodes.add(altnode);

        nodes.add(new Text("Detected tracks"));
        nodes.add(new Separator());

        ListView listviewDetectedTracks = new ListView<>(observedTracks);
        listviewDetectedTracks.setMaxHeight(150);
        nodes.add(listviewDetectedTracks);
        
        return nodes;  
    }
    
    @Override
    public boolean setOnMap() {
        if(sensorPosition.isValid()) {
            return super.setOnMap();
        }
        return false;
    }
    
    public abstract void resetSimulation();
    public abstract void simulationStep(List<SimulatedTrack> tracks, double simulationStep);

    public void setTrackObserved(final SimulatedTrack track, final boolean observed)
    {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    setTrakcObservedGUI(track, observed);
                }
            });
        } else {
            setTrakcObservedGUI(track, observed);
        }
    }

    private void setTrakcObservedGUI(final SimulatedTrack track, final boolean observed)
    {
        if(observedTracksMap.containsKey(track.getObjectId())){
            final TrackDetection trackDetection = new TrackDetection(track, getObjectId(), false);
            if(!observed){
                observedTracksMap.remove(track.getObjectId());
                observedTracks.remove(track);
                dispatchEvent(new Event(Event.EventType.Track, Event.EventAction.Delete, getObjectId(), trackDetection));
            }else{
                dispatchEvent(new Event(Event.EventType.Track, Event.EventAction.Update, getObjectId(), trackDetection));
            }
        }else if(observed){
            final TrackDetection trackDetection = new TrackDetection(track, getObjectId(), false);
            observedTracksMap.put(track.getObjectId(), track);
            observedTracks.add(track);
            dispatchEvent(new Event(Event.EventType.Track, Event.EventAction.Create, getObjectId(), trackDetection));
        }
    }
    
    private static Pattern locationPattern = Pattern.compile("(\\d+\\.?\\d*) (\\d+\\.?\\d*) (\\d+\\.?\\d*) ([NSEW]+)");
    
    // convert from dd mm ss NSEW to latlng
    private static double getDegrees(String coord){
        Matcher matcher = locationPattern.matcher(coord);
        if(matcher.find()){
            double dd = Double.parseDouble(matcher.group(1));
            double mm = Double.parseDouble(matcher.group(2));
            double ss = Double.parseDouble(matcher.group(3));
            double value = dd + mm/60 + ss/3600;
            if("W".equals(matcher.group(4)) || "S".equals(matcher.group(4))){
                value *= -1;
            }
            return value;
        }
        return 0;
    }
    
    public static Sensor buildSensorFromXMLNode(Element node){
        Sensor sensor = null;
        
        String name = XmlUtils.getText(XmlUtils.findChild(node, "name"));
        String id = XmlUtils.getText(XmlUtils.findChild(node, "id"));
        String latitude = XmlUtils.getText(XmlUtils.findChild(node, "latitude"));
        String longitude = XmlUtils.getText(XmlUtils.findChild(node, "longitude"));
        double altitude = XmlUtils.getDouble(XmlUtils.findChild(node, "altitude"));
        double delay = XmlUtils.getDouble(XmlUtils.findChild(node, "delay"));
        
        String sensorType = node.getAttribute("type");
        switch(sensorType.toLowerCase()){
            case "radar":
            {
                double period = XmlUtils.getDouble(XmlUtils.findChild(node, "period"))*1000;
                double minDist = XmlUtils.getDouble(XmlUtils.findChild(node, "min_dist"));
                double maxReach = XmlUtils.getDouble(XmlUtils.findChild(node, "max_reach"));
                sensor = new RadarSensor(id);
                ((RadarSensor)sensor).setMinDistance(minDist);
                ((RadarSensor)sensor).setRadarReach(maxReach);
                ((RadarSensor)sensor).setRadarPeriod(period);
            }
                break;
            case "ais":
            {
                double maxReach = XmlUtils.getDouble(XmlUtils.findChild(node, "max_reach"));
                sensor = new AISSensor(id);
                ((AISSensor)sensor).setAISReach(maxReach);
            }
               break;
        }
        
        if(sensor!=null){
            sensor.setObjectTag(name);
            sensor.sensorPosition = new LatLng(getDegrees(latitude), getDegrees(longitude));
            sensor.altitude = altitude;
            sensor.showOnMap = true;
        }
        
        return sensor;
    }

    public double getAltitude()
    {
        return this.altitude;
    }

    public LatLng getSensorPosition(){
        return this.sensorPosition;
    }
    
}
