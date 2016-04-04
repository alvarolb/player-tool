package data.simulation.track;

import data.ObservableObject;
import data.simulation.events.Event;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Alvaro on 4/03/14.
 */
public class TrackManager<T extends ObservableObject>{

    protected static final long serialVersionUID = 1L;

    protected HashMap<String, T> currentTracks;
    protected List<TrackListener> trackListeners;

    public TrackManager()
    {
        this.currentTracks = new HashMap<>();
        this.trackListeners = new CopyOnWriteArrayList();
    }

    public void addListener(final TrackListener listener)
    {
        if(!trackListeners.contains(listener)){
            trackListeners.add(listener);
        }
    }

    public void removeListener(final TrackListener listener)
    {
        if(trackListeners.contains(listener)){
            trackListeners.remove(listener);
        }
    }

    protected void dispatchTrackCreated(final T track, final Event event)
    {
        for(TrackListener listener : trackListeners)
        {
            listener.onTrackCreated(track, event);
        }
    }

    protected void dispatchTrackUpdated(final T track, final Event event)
    {
        for(TrackListener listener : trackListeners)
        {
            listener.onTrackUpdated(track, event);
        }
    }

    protected void dispatchTrackDeleted(final T track, final Event event)
    {
        for(TrackListener listener : trackListeners)
        {
            listener.onTrackDeleted(track, event);
        }
    }

    public ArrayList<T> getCurrentTracks()
    {
        return new ArrayList(currentTracks.values());
    }

    public boolean containsTrack(final T track)
    {
        return currentTracks.containsKey(track.getObjectId());
    }

    public int getTraksCount()
    {
        return currentTracks.size();
    }

    public void manageTrackEvent(final Event event)
    {
        final T track = event.getEventData();
        switch (event.getRawEventAction())
        {
            case Create:
                createTrack(track, event);
                break;
            case Update:
                updateTrack(track, event);
                break;
            case Delete:
                deleteTrack(track, event);
                break;
        }
    }

    protected void createTrack(final T track, final Event event)
    {
        if(!currentTracks.containsKey(track.getObjectId())){
            currentTracks.put(track.getObjectId(), track);
            dispatchTrackCreated(track, event);
        }else
        {
            updateTrack(track, event);
        }
    }

    protected void updateTrack(final T track, final Event event)
    {
        if(currentTracks.containsKey(track.getObjectId()))
        {
            currentTracks.put(track.getObjectId(), track);
            dispatchTrackUpdated(track, event);
        }else
        {
            createTrack(track, event);
        }
    }

    protected void deleteTrack(final T track, final Event event)
    {
        if(currentTracks.containsKey(track.getObjectId()))
        {
            currentTracks.remove(track.getObjectId());
            dispatchTrackDeleted(track, event);
        }
    }

    public void reset() {
        currentTracks.clear();
    }

    public interface TrackListener <T>
    {
        public void onTrackCreated(final T track, final Event event);
        public void onTrackUpdated(final T track, final Event event);
        public void onTrackDeleted(final T track, final Event event);
    }
}
