package data.simulation;

import data.ConfigurableObservableObject;
import data.ObjectRepository;
import data.ObservableObject;
import data.simulation.events.Event;
import data.simulation.track.SimulatedTrack;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.text.Text;
import javafx.util.Callback;

import java.io.IOException;
import java.util.*;

/**
 * Created by alvarolb on 12/02/14.
 */
public abstract class Agent extends ConfigurableObservableObject {

    private static final long serialVersionUID = 1L;
    public static final String CATEGORY ="Agent";


    protected transient ObjectRepository repository;

    protected Set<Sensor> monitoringSensors;
    protected Set<Agent> associatedAgents;

    protected transient List<Node> configurableNodes;
    protected transient EventTable eventTable;
    protected transient double simulationDuration;

    public Agent(String objectId, String objectCategory) {
        super(objectId, objectCategory);
        this.repository = ObjectRepository.getInstance();
        this.monitoringSensors = new HashSet<>();
        this.associatedAgents = new HashSet<>();
    }

    // used in serialization for initialize transient objects
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // initialize non-gui transient objects
        this.repository = ObjectRepository.getInstance();
    }

    @Override
    protected List<Node> getConfigurableNodes() {

        if(configurableNodes!=null){
            return configurableNodes;
        }

        configurableNodes = new ArrayList<>();

        configurableNodes.add(new Text("Associated Sensors"));
        configurableNodes.add(new Separator());

        ObservableList<ObservableObject> sensors = FXCollections.observableList(repository.getObjectsByCategory(Sensor.CATEGORY));
        ListView listViewSensors = new ListView<>(sensors);
        listViewSensors.setMaxHeight(150);


        Callback<ObservableObject, ObservableValue<Boolean>> getSensorProperty = new Callback<ObservableObject, ObservableValue<Boolean>>(){
            @Override
            public ObservableValue<Boolean> call(final ObservableObject observableObject) {
                SimpleBooleanProperty booleanProperty = new SimpleBooleanProperty(monitoringSensors.contains(observableObject));
                booleanProperty.addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                        if (t1) {
                            monitoringSensors.add((Sensor) observableObject);
                            observableObject.addListener(Agent.this);
                            onSensorAttached((Sensor) observableObject);
                        } else {
                            monitoringSensors.remove(observableObject);
                            observableObject.removeListener(Agent.this);
                            onSensorDetached((Sensor) observableObject);
                        }
                    }
                });
                return booleanProperty;
            }
        };
        Callback<ListView<ObservableObject>, ListCell<ObservableObject>> forListView = CheckBoxListCell.forListView(getSensorProperty);

        listViewSensors.setCellFactory(forListView);
        configurableNodes.add(listViewSensors);


        configurableNodes.add(new Text("Associated Agents"));
        configurableNodes.add(new Separator());

        ObservableList<ObservableObject> agents = FXCollections.observableList(repository.getObjectsByCategory(Agent.CATEGORY));
        ListView listViewAgents = new ListView<>(agents);
        listViewAgents.setMaxHeight(150);
        Callback<ObservableObject, ObservableValue<Boolean>> getAgentProperty = new Callback<ObservableObject, ObservableValue<Boolean>>(){
            @Override
            public ObservableValue<Boolean> call(final ObservableObject observableObject) {
                SimpleBooleanProperty booleanProperty = new SimpleBooleanProperty(associatedAgents.contains(observableObject));
                booleanProperty.addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                        if (t1) {
                            associatedAgents.add((Agent) observableObject);
                            onAgentAttached((Agent) observableObject);
                        } else {
                            associatedAgents.remove(observableObject);
                            onAgentDetached((Agent) observableObject);
                        }
                    }
                });
                return booleanProperty;
            }
        };
        Callback<ListView<ObservableObject>, ListCell<ObservableObject>> forListViewAgents = CheckBoxListCell.forListView(getAgentProperty);
        listViewAgents.setCellFactory(forListViewAgents);
        configurableNodes.add(listViewAgents);

        configurableNodes.add(new Text("Incoming perceptions"));
        configurableNodes.add(new Separator());
        eventTable = new EventTable();
        configurableNodes.add(eventTable);

        return configurableNodes;
    }

    @Override
    public Map<String, String> getMapRepresentation() {
        return null;
    }

    @Override
    public void onEventReceived(Event event) {
        if(eventTable!=null){
            eventTable.addEvent(event);
        }
    }

    protected abstract void onSensorAttached(Sensor sensor);
    protected abstract void onSensorDetached(Sensor sensor);
    protected abstract void onAgentAttached(Agent agent);
    protected abstract void onAgentDetached(Agent agent);
    public void simulationStep(List<SimulatedTrack> tracks, double simulationStep)
    {
        simulationDuration += simulationStep;
    }

    public void resetSimulation()
    {
        if(eventTable!=null)
        {
            eventTable.clear();
        }
        simulationDuration = 0;
    }

}
