package data.simulation.events;

import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by alvarolb on 26/02/14.
 */
public class Event {

    private EventType eventType;
    private EventAction eventAction;
    private String eventSource;
    private Date eventDate;
    private Object eventData;

    public enum EventType {
        Track ("Track"),
        TentativeTracking("TentativeTracking"),
        Tracking("Tracking");

        private final String name;

        private EventType(String s) {
            name = s;
        }
        public String toString(){
            return name;
        }
    }

    public <T> T getEventData()
    {
        return (T) eventData;
    }

    public enum EventAction {
        Create ("Create"),
        Update ("Update"),
        Delete ("Delete");

        private final String name;

        private EventAction(String s) {
            name = s;
        }
        public String toString(){
            return name;
        }
    }

    public Event(EventType eventType, EventAction eventAction, String eventSource, Object eventData) {
        this.eventType = eventType;
        this.eventAction = eventAction;
        this.eventSource = eventSource;
        this.eventDate = new Date();
        this.eventData = eventData;
    }

    public String getEventType()
    {
        return eventType.toString();
    }

    public EventType getRawEventType()
    {
        return eventType;
    }
    public EventAction getRawEventAction()
    {
        return eventAction;
    }

    public String getEventAction()
    {
        return eventAction.toString();
    }

    public String getEventSource()
    {
        return eventSource;
    }

    public String getEventDate()
    {
        return eventDate.toString();
    }

    public static List<TableColumn<Event, String>> getColumns(){

        TableColumn<Event, String> eventType = new TableColumn("Type");
        eventType.setCellValueFactory(new PropertyValueFactory<Event,String>("eventType"));

        TableColumn<Event, String> eventAction = new TableColumn("Action");
        eventAction.setCellValueFactory(new PropertyValueFactory<Event,String>("eventAction"));

        TableColumn<Event, String> eventSource = new TableColumn("Source");
        eventSource.setCellValueFactory(new PropertyValueFactory<Event,String>("eventSource"));

        TableColumn<Event, String> eventDate = new TableColumn("Date");
        eventDate.setCellValueFactory(new PropertyValueFactory<Event,String>("eventDate"));

        List<TableColumn<Event, String>> columns = new ArrayList<>();

        columns.add(eventType);
        columns.add(eventAction);
        columns.add(eventSource);
        columns.add(eventDate);

        return columns;
    }
}
