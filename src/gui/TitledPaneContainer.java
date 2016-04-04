/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;

/**
 *
 * @author alvarolb
 */
public class TitledPaneContainer extends Accordion{
    
    private final Map<String, TitledPane> titledPaneMap = new HashMap<>();
    
    public TitledPaneContainer(){
        super();
    }
    
    public List<TitledPane> getAll(){
        List<TitledPane> titledPanes = new ArrayList<>();
         for(Map.Entry<String, TitledPane> tabEntry:titledPaneMap.entrySet()){
           titledPanes.add((TitledPane)tabEntry.getValue().getContent());
        }
        return titledPanes;
    }
    
    public TitledPane get(String key){
        return titledPaneMap.get(key);
    }
    
    public TitledPane remove(String key){
        TitledPane existingTab = titledPaneMap.get(key);
        if(existingTab!=null){
            getPanes().remove(existingTab);
            titledPaneMap.remove(key);
            return existingTab;
        }
        return null;
    }
    
    public boolean contains(String key){
        return titledPaneMap.containsKey(key);
    }

    public void set(String key, TitledPane pane){
        if(!titledPaneMap.containsKey(key)){
            // add to the tab map for later retrieving
            titledPaneMap.put(key, pane);
            this.getPanes().add(pane);
        }
    }
    
    public void clear(){
        getPanes().clear();
        titledPaneMap.clear();
    }
    
}
