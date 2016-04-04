package data.simulation.events;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by alvarolb on 26/02/14.
 */
public class EventProducer implements Serializable{

    private List<EventListener> listeners;

    public EventProducer() {
        this.listeners = new CopyOnWriteArrayList<>();
    }

    public void addListener(final EventListener listener)
    {
        if(!this.listeners.contains(listener)){
            this.listeners.add(listener);
        }
    }

    public void removeListener(final EventListener listener)
    {
        if(this.listeners.contains(listener)){
            this.listeners.remove(listener);
        }
    }

    public void dispatchEvent(final Event event)
    {
        for(EventListener listener : listeners)
        {
            listener.onEventReceived(event);
        }
    }
}
