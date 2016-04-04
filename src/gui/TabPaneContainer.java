package gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 *
 * @author Alvaro
 */
public class TabPaneContainer<T extends Node> extends TabPane {
    private final Map<String, Tab> objectTabs = new HashMap<>();
    private final Class<T> classType;
    
    public TabPaneContainer(Class<T> classType){
        super();
        this.classType = classType;
    }
    
    public List<T> getAllCategories(){
        List<T> objectList = new ArrayList<>();
         for(Map.Entry<String, Tab> tabEntry:objectTabs.entrySet()){
           objectList.add((T)tabEntry.getValue().getContent());
        }
        return objectList;
    }
    
    public T getCategory(String categoryName){
        Tab value = objectTabs.get(categoryName);
        if(value!=null){
            return (T) value.getContent();
        }
        return null;
    }
        
    public T removeCategory(String categoryName){
        Tab existingTab = objectTabs.get(categoryName);
        if(existingTab!=null){
            getTabs().remove(existingTab);
            objectTabs.remove(categoryName);
            return (T) existingTab.getContent();
        }
        return null;
    }

    public T addCategory(String categoryName){
        
        // instantiate the object and append to the tabpane
        try{
            // try to create an instance
            T object = classType.newInstance();
            
            // create new tab
            Tab tab = new Tab();
            tab.setClosable(false);
            tab.setText(categoryName);

            // set content to tab
            tab.setContent(object);

            // add tab to the guy
            getTabs().add(tab);
            
            // add to the tab map for later retrieving
            objectTabs.put(categoryName, tab);

            return object;
            
        }catch(InstantiationException | IllegalAccessException e){
            e.printStackTrace();
        }
       
        return null;
    }
}
