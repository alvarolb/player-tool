/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package data.items;

import data.ConfigurableObservableObject;
import data.ObservableObject;
import data.PairedValues;
import data.simulation.events.Event;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Alvaro
 */
public class ObjectData extends ConfigurableObservableObject {
    @Override
    public void onEventReceived(Event event) {

    }

    public static enum ObjectOperation{ SET, REMOVE, UNDEF };
    
    protected ObjectOperation operation;
    protected PairedValues values;
    
    public ObjectData(PairedValues values) {
        super(values.getString("object_id"), values.getString("category"), !values.containsKey("visible") ? true : values.getBoolean("visible"));
       
        switch(values.getString("object_operation")){
            case "set":
                this.operation = ObjectOperation.SET;
                break;
            case "remove":
                this.operation = ObjectOperation.REMOVE;
                break;
            default:
                this.operation = ObjectOperation.UNDEF;
                break;
        }
        
        this.values = values;
    }
    
    @Override
    public void update(ObservableObject object)
    {
        super.update(object);
        ObjectData updateObject = (ObjectData) object;
        this.operation = updateObject.operation;
        this.values = updateObject.values;
    }
    
    public ObjectOperation getOperation(){
        return this.operation;
    }
   
    @Override
    public Map<String, String> getMapRepresentation() {
        return values.getValues();
    }
    
    @Override
    public String getObjectTag()
    {
        return values.getString("object_tag");
    }
    
    @Override
    protected List<Node> getConfigurableNodes() {
        List<Node> nodes = new ArrayList<>();
        return nodes;
    }
    
}
    