/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package data.simulation;

import data.simulation.events.Event;
import data.simulation.track.SimulatedTrack;
import geom.LatLng;
import geom.Spherical;

import java.util.*;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.text.Text;

/**
 *
 * @author alvarolb
 */
public class RadarSensor extends Sensor{

    private static final long serialVersionUID = 1L;

    public double radarPeriod;
    public double minDistance;
    public double radarReach;
    private double azimuthPrecision;
    private double distancePrecision;
    
    //used in simulation
    public transient double currentAzimuth;
    private transient double simulationTime;
    private transient Set<SimulatedTrack> cycleProcessedTracks;
    private transient Set<SimulatedTrack> currentTracksDrawn;

    public RadarSensor(String sensorId) {
        super(sensorId);
        radarPeriod = 0;
        radarReach = 0;
    }

    @Override
    public List<Node> getConfigurableNodes() {
        List<Node> nodes = super.getConfigurableNodes();

        nodes.add(new Text("Radar details"));
        nodes.add(new Separator());

        final DoubleItemValue periodNode = new DoubleItemValue("Period (ms)", radarPeriod) {
            @Override
            public void onValueChanged(double value) {
                radarPeriod = value;
            }
        };
        nodes.add(periodNode);

        final DoubleItemValue reachNode = new DoubleItemValue("Reach (m)", radarReach) {
            @Override
            public void onValueChanged(double value) {
                radarReach = value;
                setOnMap();
            }
        };
        nodes.add(reachNode);

        final DoubleItemValue minDistanceNode = new DoubleItemValue("Min Distance (m)", minDistance) {
            @Override
            public void onValueChanged(double value) {
                minDistance = value;
                setOnMap();
            }
        };
        nodes.add(minDistanceNode);

        final DoubleItemValue azimuthPrecisionNode = new DoubleItemValue("Azimuth Prec (m)", azimuthPrecision) {
            @Override
            public void onValueChanged(double value) {
                azimuthPrecision = value;
            }
        };
        nodes.add(azimuthPrecisionNode);

        final DoubleItemValue distancePrecisionNode = new DoubleItemValue("Distance Prec (m)", distancePrecision) {
            @Override
            public void onValueChanged(double value) {
                distancePrecision = value;
            }
        };
        nodes.add(distancePrecisionNode);

        nodes.add(new Text("Simulation options"));
        nodes.add(new Separator());

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
    public Map<String, String> getMapRepresentation() {
        Map<String, String> values = new HashMap<>();
        values.put("object_id", getObjectId());
        values.put("type", "object");
        values.put("object_type", "circle");
        values.put("circle_lat", String.valueOf(sensorPosition.lat()));
        values.put("circle_lon", String.valueOf(sensorPosition.lon()));
        values.put("circle_radius", String.valueOf(radarReach));
        return values;
    }

    @Override
    public void resetSimulation() {
        cycleProcessedTracks = new HashSet<>();
        clearDetections();
        currentTracksDrawn = new HashSet<>();
        currentAzimuth = 0;
        simulationTime = 0;
    }

    /**
     * Calculate the azimuth movement for the given time
     *
     * @param milliseconds
     * @return azimuth in degrees
     */
    private double getAzimuthMovement(double milliseconds) {
        return (360.0 / (long) radarPeriod) * milliseconds;
    }

    /**
     * Calculate the required time for complete the given azimuth
     *
     * @param azimuth azimuth in degrees
     * @return time in milliseconds
     */
    private double getRequiredTime(double azimuth) {
        return azimuth / (360.0 / (long) radarPeriod);
    }

    /*
     * un período de radar de 2s equivale a recorrer 0,18º por ms
     * si tenemos un período de simulación de 10ms, equivale a recorrer 1,8º
     * para ver si hemos detectado un blanco, se comprueba
     * 1. que el azimuth del radar es > que el azimuth real del blanco, y que está dentro de la ventana de grados (1,8º)
     * 2. se hace una diferencia de grados
     * 2. se calcula el tiempo que tardaría el radar en recorrer esos grados para generar el timestamp de la detección
     */
    @Override
    public void simulationStep(List<SimulatedTrack> tracks, double simulationStep) {
        double startingAzimuth = currentAzimuth;
        simulationTime += simulationStep;
        //System.out.println("Calling " + getObjectId() + " with " + simulationStep + " -> Radar Sensor: " + simulationTime);

        // increase azimuth corresponding to the simulation step
        if (radarPeriod > 0) {
            currentAzimuth += getAzimuthMovement(simulationStep);
            currentAzimuth = Math.round(currentAzimuth * 10000000.0) / 10000000.0;
        }

        // detect north
        if (currentAzimuth > 360.0) {

            // if starting azimuth is less than 360 process until north
            if (startingAzimuth < 360.0) {
                double northStep = getRequiredTime(360.0 - startingAzimuth);
                northStep = Math.round(northStep * 10000000.0) / 10000000.0;

                // process until 360
                processStep(360.0, tracks, northStep);

                // update simulation step in the last turn
                simulationStep -= northStep;
            }

            // change the current azimuth
            currentAzimuth -= 360.0;

            // clear detected tracks
            cycleProcessedTracks.clear();
        }

        processStep(currentAzimuth, tracks, simulationStep);
    }

    private void processStep(double radarAzimuth, List<SimulatedTrack> tracks, double simulationStep) {
        for (SimulatedTrack track : tracks) {

            // do nothing with the track is not visible
            if(!track.isVisibleInSimulation()){
                if(showDetections) {
                    clearDetection(track);
                }
                setTrackObserved(track, false);
                continue;
            }

            final boolean cycleDetected = cycleProcessedTracks.contains(track);
            if(cycleDetected){
                continue;
            }

            final LatLng trackPosition = track.getPosition();
            // get the shortest distance beetween sensor and track
            double trackDistance = Spherical.getRhumbDistance(sensorPosition, trackPosition);
            boolean inRange = trackDistance <= radarReach;

            if(!inRange)
            {
                setTrackObserved(track, false);
                if(showDetections){
                    clearDetection(track);
                }
                cycleProcessedTracks.add(track);
            }
            else{
                // compute the real direct bearing
                double trackBearing = Spherical.getRhumbBearing(sensorPosition, trackPosition);

                // check that the radar 
                if (radarAzimuth > trackBearing) {
                    double azimuthDiff = Math.abs(radarAzimuth - trackBearing);
                    if (azimuthDiff < getAzimuthMovement(simulationStep)) {
                        /*double azimuthTime = getRequiredTime(azimuthDiff);
                        double detectionTime = simulationTime - azimuthTime;
                        detectionTime = Math.round(detectionTime * 10000000.0) / 10000000.0;*/
                        setTrackObserved(track, true);
                        if(showDetections){
                            drawDetection(track);
                        }
                        cycleProcessedTracks.add(track);
                    }
                }
            }
        }
    }

    public void setRadarPeriod(double radarPeriod) {
        this.radarPeriod = radarPeriod;
    }

    public void setRadarReach(double radarReach) {
        this.radarReach = radarReach;
    }

    public void setMinDistance(double minDistance) {
        this.minDistance = minDistance;
    }

    private void clearDetections() {
        if(currentTracksDrawn !=null && currentTracksDrawn.size()>0){
            for (final SimulatedTrack track : currentTracksDrawn) {
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
            currentTracksDrawn.clear();
        }
    }

    private void clearDetection(final SimulatedTrack track) {
        if(currentTracksDrawn.contains(track)){
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
            currentTracksDrawn.remove(track);
        }
    }

    private void drawDetection(SimulatedTrack track) {
        final Map<String, String> path = new HashMap<>();
        path.put("object_id", getObjectId() + "_" + track.getObjectId());
        path.put("type", "object");
        path.put("object_type", "ellipse");
        if (objectColor != null) {
            path.put("object_color", objectColor);
        }
        path.put("ellipse_lat", String.valueOf(track.getPosition().lat()));
        path.put("ellipse_lon", String.valueOf(track.getPosition().lon()));


        double trackBearing = Spherical.getRhumbBearing(sensorPosition, track.getPosition());
        if (azimuthPrecision >= distancePrecision) {
            path.put("ellipse_major_axis", String.valueOf(azimuthPrecision));
            path.put("ellipse_minor_axis", String.valueOf(distancePrecision));
            path.put("ellipse_angle", String.valueOf(trackBearing + 90));
        } else {
            path.put("ellipse_major_axis", String.valueOf(distancePrecision));
            path.put("ellipse_minor_axis", String.valueOf(azimuthPrecision));
            path.put("ellipse_angle", String.valueOf(trackBearing));
        }

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
        if(!currentTracksDrawn.contains(track)){
            currentTracksDrawn.add(track);
        }
    }

    public Map<String, String> getAzimuthRepresentation() {
        List<LatLng> points = new ArrayList<>();
        points.add(sensorPosition);
        points.add(Spherical.getRhumbDestinationPoint(sensorPosition, radarReach, currentAzimuth));
        Map<String, String> path = new HashMap<>();
        path.put("object_id", '_' + getObjectId());
        path.put("type", "object");
        path.put("object_type", "line");
        if (objectColor != null) {
            path.put("object_color", objectColor);
        }
        path.put("line_path", getPath(points));
        return path;
    }

    private String getPath(List<LatLng> points) {
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
    public boolean setOnMap() {
        if(super.setOnMap()){
            setAzimuthOnMap();
            return true;
        }
        return false;
    }

    @Override
    public void removeFromMap() {
        super.removeFromMap();
        removeAzimuthFromMap();
        clearDetections();
    }

    protected void setAzimuthOnMap() {
        final Map<String, String> pathRepresentation = getAzimuthRepresentation();
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    gMap.setObject(pathRepresentation);
                }
            });
        } else {
            gMap.setObject(pathRepresentation);
        }
    }

    protected void removeAzimuthFromMap() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                gMap.removeObject('_' + getObjectId());
            }
        });
    }

    @Override
    public void onEventReceived(Event event) {

    }
}
