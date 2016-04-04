/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package data.items;

import data.ObservableObject;
import data.PairedValues;
import data.simulation.events.Event;

import java.util.Map;

/**
 *
 * @author alvarolb
 */
public class InfoData extends ObservableObject {
    
    public InfoData(PairedValues values){
        super(values.getString("info_id"), values.getString("info_category"));
    }

    @Override
    public Map<String, String> getMapRepresentation() {
        return null;
    }

    @Override
    public void onEventReceived(Event event) {

    }
}
