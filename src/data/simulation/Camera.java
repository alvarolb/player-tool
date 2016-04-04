package data.simulation;

import data.items.Polygon;
import data.simulation.events.Event;
import data.simulation.track.SimulatedTrack;
import geom.LatLng;
import geom.Spherical;
import geom.maps.PolyUtil.PolyUtil;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Alvaro on 7/02/14.
 */
public class Camera extends Sensor{

    /* camera model taken from http://www.flir.com/cs/emea/en/view/?id=42060 */

    private static final long serialVersionUID = 1L;

    private double sensorWidthMilliMeters = 9.754565156289354; // mm
    private double sensorHeightMillimeters = 7.284152764787088; // mm

    private double minFocalLengthMillimeters = 22;
    private double maxFocalLengthMillimeters = 275;

    private double currentPanDegrees = 0.0;
    private double currentTiltDegrees = 70.0;
    private double currentFPS = 25.0;
    private double currentFocalLengthMillimeters = minFocalLengthMillimeters; // mm

    private double minHFOVDegrees=2.0;
    private double maxHFOVDegrees=25.0;

    private double minVFOVDegrees=1.5;
    private double maxVFOVDegrees=18.8;

    private transient double incrementCount;

    private transient DoubleItemValue panDegrees;
    private transient DoubleItemValue tiltDegrees;

    public Camera(String sensorId) {
        super(sensorId);
        altitude = 100;
    }

    public double getCurrentHFOVDegrees(){
        return Math.toDegrees(2 * Math.atan(sensorWidthMilliMeters/(2* currentFocalLengthMillimeters)));
    }

    public double getCurrentVFOVDegrees(){
        return Math.toDegrees(2 * Math.atan(sensorHeightMillimeters / (2 * currentFocalLengthMillimeters)));
    }

    public double getMinDistanceViewMeters(){
        return getAltitude() * Math.tan(Math.toRadians(currentTiltDegrees-(getCurrentVFOVDegrees()/2.0)));
    }

    public double getMaxDistanceViewMeters(){
        //System.out.println("Altura: " + getAltitude());
        //System.out.println("Max observable angle:" + (currentTiltDegrees + (getCurrentVFOVDegrees()/2.0)));
        //System.out.println("Distance:" + getAltitude() * Math.tan(Math.toRadians(currentTiltDegrees+(getCurrentVFOVDegrees()/2.0))));
        double maxFov = getAltitude() * Math.tan(Math.toRadians(currentTiltDegrees+(getCurrentVFOVDegrees()/2.0))) ;
        return maxFov < 0 ? 45000 : maxFov;
    }

    public double getMinWidthViewMeters()
    {
        return getMinDistanceViewMeters() * Math.tan((getCurrentHFOVDegrees()/2.0) * (Math.PI / 180));
    }

    public double getMAxWidthViewMeters()
    {
        return getMaxDistanceViewMeters() * Math.tan((getCurrentHFOVDegrees()/2.0) * (Math.PI / 180));
    }

    @Override
    public void resetSimulation() {

    }

    @Override
    public void simulationStep(List<SimulatedTrack> tracks, double simulationStep)
    {
        incrementCount += simulationStep;
        if(incrementCount>(1000/currentFPS)){
            for(SimulatedTrack track : tracks)
            {
                setTrackObserved(track, PolyUtil.containsLocation(track.getPosition(), getFieldOfViewAsPolygon(), true));
            }
            incrementCount++;
        }
    }

    private double getFoVSize(double distance, double fovAngle){
        return 2 * distance * Math.tan(fovAngle/2);
    }

    public double getCurrentPanDegrees()
    {
        return currentPanDegrees;
    }

    public double getCurrentTiltDegrees()
    {
        return currentTiltDegrees;
    }

    public List<LatLng> getFieldOfViewAsPolygon()
    {
        List<LatLng> polygon = new ArrayList<>();

        // a single line is the polygon
        if(getCurrentHFOVDegrees() == 0)
        {
            LatLng fovStart = Spherical.getDestinationPoint(sensorPosition, getMinDistanceViewMeters(), getCurrentPanDegrees());
            //LatLng fovEnd = Spherical.getDestinationPoint(sensorPosition, getMaxDistanceViewMeters(), getCurrentPanDegrees());
            LatLng fovEnd = Spherical.getDestinationPoint(sensorPosition, lookingAtDistance + 150, getCurrentPanDegrees());
            polygon.add(fovStart);
            polygon.add(fovEnd);
        }else{
            LatLng fovStartLeft = Spherical.getDestinationPoint(sensorPosition, getMinDistanceViewMeters(), getCurrentPanDegrees()-getCurrentHFOVDegrees()/2.0);
            LatLng fovStartRight = Spherical.getDestinationPoint(sensorPosition, getMinDistanceViewMeters(), getCurrentPanDegrees()+getCurrentHFOVDegrees()/2.0);
            //LatLng fovEndLeft = Spherical.getDestinationPoint(sensorPosition, getMaxDistanceViewMeters(), getCurrentPanDegrees()-getCurrentHFOVDegrees()/2.0);
            //LatLng fovEndRight = Spherical.getDestinationPoint(sensorPosition, getMaxDistanceViewMeters(), getCurrentPanDegrees()+getCurrentHFOVDegrees()/2.0);
            LatLng fovEndLeft = Spherical.getDestinationPoint(sensorPosition, lookingAtDistance + 150, getCurrentPanDegrees()-getCurrentHFOVDegrees()/2.0);
            LatLng fovEndRight = Spherical.getDestinationPoint(sensorPosition, lookingAtDistance + 150, getCurrentPanDegrees()+getCurrentHFOVDegrees()/2.0);

            polygon.add(fovStartLeft);
            polygon.add(fovStartRight);
            polygon.add(fovEndRight);
            polygon.add(fovEndLeft);
            polygon.add(fovStartLeft);
        }
        return polygon;
    }

    @Override
    public Map<String, String> getMapRepresentation() {
        Map<String, String> path = new HashMap<>();
        path.put("object_id", getObjectId());
        path.put("type", "object");
        path.put("object_type", "poly");
        path.put("poly_path", Polygon.getPath(getFieldOfViewAsPolygon()));
        return path;
    }

    private transient double lookingAtDistance = 0;
    private transient LatLng lookingAtPoint = null;

    public boolean lookAt(LatLng position)
    {
        lookingAtPoint = position;
        currentPanDegrees = Spherical.getRhumbBearing(sensorPosition, position);
        lookingAtDistance = Spherical.getGreatCircleDistance(sensorPosition, position);
        currentTiltDegrees = Math.toDegrees(Math.atan(lookingAtDistance/getAltitude()));
        if(panDegrees!=null && tiltDegrees!=null){
            panDegrees.setValue(currentPanDegrees);
            tiltDegrees.setValue(currentTiltDegrees);
        }
        return true;
    }

    public LatLng getLookingAtPoint()
    {
        return lookingAtPoint;
    }

    @Override
    public List<Node> getConfigurableNodes() {
        List<Node> nodes = super.getConfigurableNodes();

        nodes.add(new Text("Camera Control"));
        nodes.add(new Separator());

        final DoubleItemValue horizontalFOV = new DoubleItemValue("HFOV (º)", getCurrentHFOVDegrees()) {
            @Override
            public void onValueChanged(double value) {
            }
        };
        horizontalFOV.setEditable(false);

        final DoubleItemValue verticalFOV = new DoubleItemValue("VFOV (º)", getCurrentVFOVDegrees()) {
            @Override
            public void onValueChanged(double value) {
            }
        };
        verticalFOV.setEditable(false);

        final DoubleItemValue sensorWidth = new DoubleItemValue("Sensor Width (mm)", sensorWidthMilliMeters) {
            @Override
            public void onValueChanged(double value) {
                sensorWidthMilliMeters = value;
            }
        };
        sensorWidth.setEditable(false);

        final DoubleItemValue sensorHeight = new DoubleItemValue("Sensor Height (mm)", sensorHeightMillimeters) {
            @Override
            public void onValueChanged(double value) {
                sensorHeightMillimeters = value;
            }
        };
        sensorHeight.setEditable(false);

        final DoubleItemValue focalLenght = new DoubleItemValue("Focal Lenght (mm)", currentFocalLengthMillimeters) {
            @Override
            public void onValueChanged(double value) {
                currentFocalLengthMillimeters = value;
                horizontalFOV.setValue(getCurrentHFOVDegrees());
                verticalFOV.setValue(getCurrentVFOVDegrees());
                setOnMap();
            }
        };
        nodes.add(focalLenght);

        panDegrees = new DoubleItemValue("Pan (º)", currentPanDegrees) {
            @Override
            public void onValueChanged(double value) {
                currentPanDegrees = value;
                setOnMap();
            }
        };
        nodes.add(panDegrees);

        tiltDegrees = new DoubleItemValue("Tilt (º)", currentTiltDegrees) {
            @Override
            public void onValueChanged(double value) {
                currentTiltDegrees = value;
                setOnMap();
            }
        };
        nodes.add(tiltDegrees);

        final DoubleItemValue currentFPSItem = new DoubleItemValue("FPS", currentFPS) {
            @Override
            public void onValueChanged(double value) {
                currentFPS = value;
            }
        };
        nodes.add(currentFPSItem);


        nodes.add(new Text("Look at"));
        nodes.add(new Separator());

        final PositionItem positionNode = new PositionItem(new LatLng()) {

            @Override
            public void onLocationChanged(LatLng latlon) {
                lookAt(latlon);
            }
        };
        nodes.add(positionNode);


        nodes.add(new Text("Camera Parameters"));
        nodes.add(new Separator());

        final DoubleItemValue minFocalLength = new DoubleItemValue("Min Focal Length (mm)", minFocalLengthMillimeters) {
            @Override
            public void onValueChanged(double value) {
                minFocalLengthMillimeters = value;
                setOnMap();
            }
        };
        nodes.add(minFocalLength);

        final DoubleItemValue maxFocalLength = new DoubleItemValue("Max Focal Length (mm)", maxFocalLengthMillimeters) {
            @Override
            public void onValueChanged(double value) {
                maxFocalLengthMillimeters = value;
                setOnMap();
            }
        };
        nodes.add(maxFocalLength);


        final DoubleItemValue minHFOV = new DoubleItemValue("Min HFOV (º)", minHFOVDegrees) {
            @Override
            public void onValueChanged(double value) {
                minHFOVDegrees = value;
                double sensorWidthValue = 2*maxFocalLengthMillimeters * Math.tan(Math.toRadians(minHFOVDegrees/2.0));
                sensorWidth.setValue(sensorWidthValue);
                setOnMap();
            }
        };
        nodes.add(minHFOV);

        final DoubleItemValue maxHFOV = new DoubleItemValue("Max HFOV (º)", maxHFOVDegrees) {
            @Override
            public void onValueChanged(double value) {
                maxHFOVDegrees = value;
                double sensorWidthValue = 2*minFocalLengthMillimeters * Math.tan(Math.toRadians(maxHFOVDegrees/2.0));
                sensorWidth.setValue(sensorWidthValue);
                setOnMap();
            }
        };
        nodes.add(maxHFOV);

        final DoubleItemValue minVFOV = new DoubleItemValue("Min VFOV (º)", minVFOVDegrees) {
            @Override
            public void onValueChanged(double value) {
                minVFOVDegrees = value;
                double sensorHeightValue = 2*maxFocalLengthMillimeters * Math.tan(Math.toRadians(minVFOVDegrees/2.0));
                sensorHeight.setValue(sensorHeightValue);
                setOnMap();
            }
        };
        nodes.add(minVFOV);

        final DoubleItemValue maxVFOV = new DoubleItemValue("Max VFOV (º)", maxVFOVDegrees) {
            @Override
            public void onValueChanged(double value) {
                maxVFOVDegrees = value;
                double sensorHeightValue = 2*minFocalLengthMillimeters * Math.tan(Math.toRadians(maxVFOVDegrees/2.0));
                sensorHeight.setValue(sensorHeightValue);
                setOnMap();
            }
        };
        nodes.add(maxVFOV);

        nodes.add(new Text("Camera Status"));
        nodes.add(new Separator());

        nodes.add(sensorWidth);
        nodes.add(sensorHeight);

        nodes.add(horizontalFOV);
        nodes.add(verticalFOV);

        return nodes;
    }

    @Override
    public void onEventReceived(Event event) {

    }
}
