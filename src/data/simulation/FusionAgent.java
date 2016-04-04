package data.simulation;

import data.simulation.events.Event;
import data.simulation.track.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Alvaro on 4/03/14.
 */
public class FusionAgent extends Agent {

    private static final long serialVersionUID = 1L;

    private transient TrackManager<SimulatedTrack> trackManager;
    private transient TrackManager.TrackListener<TrackDetection> trackListener;
    private transient HashMap<String, FusedTrack> fusedTracks;

    public FusionAgent(String objectId, String objectCategory) {
        super(objectId, objectCategory);

        this.trackManager = new TrackManager();
        this.trackListener = new TrackListenerImpl();
        this.trackManager.addListener(this.trackListener);
        this.fusedTracks = new HashMap<>();
    }

    // used in serialization for initialize transient objects
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // initialize non-gui transient objects
        this.trackManager = new TrackManager();
        this.trackListener = new TrackListenerImpl();
        this.trackManager.addListener(this.trackListener);
        this.fusedTracks = new HashMap<>();
    }

    private class TrackListenerImpl implements TrackManager.TrackListener<TrackDetection>
    {

        @Override
        public void onTrackCreated(final TrackDetection track, final  Event event) {
            FusedTrack fusedTrack = fusedTracks.get(track.getSimulatedTrackId());
            final boolean newFused = fusedTrack == null;

            if(newFused){
                fusedTrack = new FusedTrack(track.getSimulatedTrack());
                fusedTracks.put(track.getSimulatedTrackId(), fusedTrack);
            }

            if(!fusedTrack.containsSource(track.getObjectId())){
                fusedTrack.addSource(track.getObjectId());
            }

            if(newFused) {
                Event createFusedTrack = new Event(Event.EventType.Track, Event.EventAction.Create, getObjectId(), fusedTrack);
                dispatchEvent(createFusedTrack);
            }
        }

        @Override
        public void onTrackUpdated(final TrackDetection track, final  Event event) {
            FusedTrack fusedTrack = fusedTracks.get(track.getSimulatedTrackId());
            if(fusedTrack!=null) {
                if(!fusedTrack.containsSource(track.getObjectId())) {
                    fusedTrack.addSource(track.getObjectId());
                }
                fusedTrack.update();
                Event updateFusedTrack = new Event(Event.EventType.Track, Event.EventAction.Update, getObjectId(), fusedTrack);
                dispatchEvent(updateFusedTrack);
            }else{
                onTrackCreated(track, event);
            }
        }

        @Override
        public void onTrackDeleted(final TrackDetection track, final  Event event) {
            FusedTrack fusedTrack = fusedTracks.get(track.getSimulatedTrackId());
            if(fusedTrack!=null) {
                fusedTrack.removeSource(track.getObjectId());
                if (fusedTrack.isEmpty()) {
                    fusedTracks.remove(track.getSimulatedTrackId());
                    Event deleteFusedTrack = new Event(Event.EventType.Track, Event.EventAction.Delete, getObjectId(), fusedTrack);
                    dispatchEvent(deleteFusedTrack);
                }
            }
        }
    }

    @Override
    protected void onSensorAttached(Sensor sensor) {
        sensor.addListener(this);
    }

    @Override
    protected void onSensorDetached(Sensor sensor) {
        sensor.removeListener(this);
    }

    @Override
    protected void onAgentAttached(Agent agent) {
        agent.addListener(this);
    }

    @Override
    protected void onAgentDetached(Agent agent) {
        agent.removeListener(this);
    }

    @Override
    public void simulationStep(List<SimulatedTrack> tracks, double simulationStep) {

    }

    @Override
    public void resetSimulation() {

    }

    @Override
    public void onEventReceived(final Event event) {
        // call super to draw events no gui table
        super.onEventReceived(event);

        switch (event.getRawEventType())
        {
            case Track:
                trackManager.manageTrackEvent(event);
                break;
        }
    }
}
