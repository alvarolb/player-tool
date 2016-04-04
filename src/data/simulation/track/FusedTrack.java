package data.simulation.track;

import data.simulation.events.Event;
import javafx.scene.Node;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Alvaro on 4/03/14.
 */
public class FusedTrack extends TrackDetection{
    private static final long serialVersionUID = 1L;

    public static String CATEGORY = "Fused Tracks";

    private Set<String> sources;

    public FusedTrack(SimulatedTrack simulatedTrack)
    {
        super(simulatedTrack, "Fusion", true);
        this.sources = new HashSet<>();
    }

    public Set<String> getSources()
    {
        return sources;
    }

    public int getSourcesCount()
    {
        return sources.size();
    }

    public boolean isEmpty()
    {
        return sources.isEmpty();
    }

    public boolean containsSource(String source)
    {
        return sources.contains(source);
    }

    public void addSource(String source)
    {
        if(!sources.contains(source))
        {
            sources.add(source);
        }
    }

    public void removeSource(String source)
    {
        if(sources.contains(source))
        {
            sources.remove(source);
        }
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

    @Override
    public String toString() {
        return getObjectId() + ": " + sources.toString();
    }
}
