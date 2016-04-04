package data.simulation.track;

import data.simulation.events.Event;
import geom.LatLng;
import javafx.scene.Node;

import java.util.List;
import java.util.Map;

/**
 * Created by alvarolb on 17/03/14.
 */
public class TrackDetection extends AbstractTrack{

    private LatLng position;
    private String sourceId;
    private String source;
    private boolean fromFusion;
    private double speed;
    private double cog;
    private long detectionTime;
    private SimulatedTrack sourceSimulated;

    public TrackDetection(SimulatedTrack track, String source, boolean fromFusion){
        super(source + "_" + track.getObjectId(), "DETECTION", TrackType.FusedTrack);
        this.sourceId = track.getObjectId();
        this.source = source;
        this.position = track.getCurrentPosition();
        this.speed = track.getSpeed();
        this.fromFusion = fromFusion;
        this.detectionTime = System.currentTimeMillis();
        this.sourceSimulated = track;
    }

    public SimulatedTrack getSimulatedTrack()
    {
        return sourceSimulated;
    }

    public void update()
    {
        this.position = sourceSimulated.getCurrentPosition();
        this.speed = sourceSimulated.getSpeed();
        this.detectionTime = System.currentTimeMillis();
    }

    @Override
    public boolean isFused() {
        return fromFusion;
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public double getSpeed() {
        return speed;
    }

    public String getSimulatedTrackId() {
        return sourceId;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    protected List<Node> getConfigurableNodes() {
        return null;
    }

    @Override
    public Map<String, String> getMapRepresentation() {
        return null;
    }

    @Override
    public void onEventReceived(Event event) {

    }

}
