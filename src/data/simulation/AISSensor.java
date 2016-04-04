/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package data.simulation;

import data.simulation.events.Event;
import data.simulation.track.SimulatedTrack;
import geom.LatLng;
import geom.Spherical;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.text.Text;

/**
 *
 * @author alvarolb
 */
public class AISSensor extends Sensor{

    private static final long serialVersionUID = 1L;

    private double AISReach;
    
    public AISSensor(String sensorId){
        super(sensorId);
    }

    public void setAISReach(double AISReach) {
        this.AISReach = AISReach;
    }
    
    @Override
    public List<Node> getConfigurableNodes() {
        List<Node> nodes = super.getConfigurableNodes();

        nodes.add(new Text("AIS details"));
        nodes.add(new Separator());
        
        final DoubleItemValue reachNode = new DoubleItemValue("Reach (m)", AISReach){
            @Override
            public void onValueChanged(double value) {
                AISReach = value;
                setOnMap();
            }
        };
        nodes.add(reachNode);
        
        final BooleanItem drawDetectionsNode = new BooleanItem("Show detections", showDetections) {
            @Override
            public void onValueChanged(boolean value) {
                if (!value && showDetections) {
                    clearDetections();
                }
                showDetections = value;
            }
        };
        nodes.add(drawDetectionsNode);
        
        return nodes;
    }
    
    @Override
    public Map<String, String> getMapRepresentation(){
        Map<String, String> values = new HashMap<>();
        values.put("object_id", getObjectId());
        values.put("type", "object");
        values.put("object_type", "circle");
        values.put("circle_lat", String.valueOf(sensorPosition.lat()));
        values.put("circle_lon", String.valueOf(sensorPosition.lon()));
        values.put("circle_radius", String.valueOf(AISReach));
        
        return values;
    }

    private double simulationTime=0;
    
    @Override
    public void resetSimulation() {
        simulationTime = 0;
        clearDetections();
        drawDetections = new ArrayList<>();
    }

    @Override
    public void simulationStep(List<SimulatedTrack> tracks, double simulationStep) {
        simulationTime += simulationStep;
        //System.out.println("Calling " + getObjectId() + " with " + simulationStep + " -> AIS   Sensor: " + simulationTime);
        
        for(SimulatedTrack track : tracks){
            // do nothing if the track is not visible in simulation
            if(!track.isVisibleInSimulation()){
                if(showDetections) {
                    clearDetection(track);
                }
                setTrackObserved(track, false);
                continue;
            }
            
            LatLng trackPostion = track.getPosition();
            double trackDistance = Spherical.getGreatCircleDistance(sensorPosition, trackPostion);
            
            if(trackDistance<=AISReach){
                AISMessage message = track.getAISMessage();
                if(message!=null){
                    //System.out.println("AIS[" + (objectTag!=null ? objectTag : getObjectId()) + "] " + message.toString());
                    if(showDetections){
                        drawDetection(track);
                        setTrackObserved(track, true);
                    }
                }
            }else{
                setTrackObserved(track, false);
                if(showDetections){
                    clearDetection(track);
                }
            }
        }
    }
    
    private transient List<SimulatedTrack> drawDetections;
    
    private void drawDetection(SimulatedTrack track) {
        final Map<String, String> path = new HashMap<>();
        path.put("object_id", getObjectId() + "_" + track.getObjectId());
        path.put("type", "object");
        path.put("object_type", "circle");
        if (objectColor != null) {
            path.put("object_color", objectColor);
        }
        path.put("circle_lat", String.valueOf(track.getPosition().lat()));
        path.put("circle_lon", String.valueOf(track.getPosition().lon()));
        path.put("circle_radius", "10");
        
        //path.put("object_history", "15");

        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    gMap.setObject(path);
                }
            });
        } else {
            gMap.setObject(path);
        }
        if(!drawDetections.contains(track)){
            drawDetections.add(track);
        }
    }

    private void clearDetection(final SimulatedTrack track) {
        if(drawDetections.contains(track)){
            if (!Platform.isFxApplicationThread()) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        gMap.removeObject(getObjectId() + "_" + track.getObjectId());
                    }
                });
            } else {
                gMap.removeObject(getObjectId() + "_" + track.getObjectId());
            }
            drawDetections.remove(track);
        }
    }
    
    private void clearDetections() {
        if(drawDetections!=null && drawDetections.size()>0){
            for (final SimulatedTrack track : drawDetections) {
                if (!Platform.isFxApplicationThread()) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            gMap.removeObject(getObjectId() + "_" + track.getObjectId());
                        }
                    });
                } else {
                    gMap.removeObject(getObjectId() + "_" + track.getObjectId());
                }
            }
            drawDetections.clear();
        }
    }
    
   @Override
    public void removeFromMap() {
        super.removeFromMap();
        clearDetections();
    }

    @Override
    public void onEventReceived(Event event) {

    }
}
