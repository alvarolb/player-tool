/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package data;

import data.simulation.events.Event;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author alvarolb
 */
public class ObjectRepository{

    private final Map<String, ObservableObject> objects;
    private final List<ObjectsCallback> objectCallbacks;
    //private final TabPaneContainer<ObjectList> objectsTabPane;
    private final TreeView treeView;
    private final CheckBoxTreeItem<ObservableObject> rootItem;
    
    private class ObservableObjectCategory extends ObservableObject
    {
        private boolean visible;
        private boolean autoClear;
        private boolean manualVisibility;

        public ObservableObjectCategory(String text, boolean visible)
        {
            super(text, "");
            this.visible  = visible;
            this.autoClear = false;
            this.manualVisibility = false;
        }

        public ObservableObjectCategory(String text)
        {
            super(text, "");
            this.visible  = true;
            this.autoClear = false;
            this.manualVisibility = false;
        }

        public void setVisible(boolean visible)
        {
            this.visible = visible;
            this.manualVisibility = true;
        }

        public boolean isVisible()
        {
            return visible;
        }

        public boolean isManualVisibility(){
            return manualVisibility;
        }

        @Override
        public Map<String, String> getMapRepresentation() {
            System.err.println("Should not be called!");
            return null;
        }

        @Override
        public void onEventReceived(Event event) {

        }

    }

    public interface ObjectsCallback {

        public void onObjectPropertiesRequested(ObservableObject object);

        public void onObjectSelected(ObservableObject object);

        public void onObjectCreated(ObservableObject object);

        public void onObjectUpdated(ObservableObject object);

        public void onObjectRemoved(ObservableObject object);
    }

    public void addObjectCallback(ObjectsCallback callback) {
        if (!objectCallbacks.contains(callback)) {
            objectCallbacks.add(callback);
        }
    }

    public void removeObjectCallback(ObjectsCallback callback) {
        objectCallbacks.remove(callback);
    }

    private static ObjectRepository instance;

    public static ObjectRepository getInstance()
    {
        if(instance == null){
            instance = new ObjectRepository();
        }
        return instance;
    }

    private ObjectRepository() {
        this.objects = new ConcurrentHashMap<>();
        //this.objectsTabPane = new TabPaneContainer<>(ObjectList.class);
        this.objectCallbacks = new ArrayList<>();


        final Node rootIcon = new ImageView(new Image(getClass().getResourceAsStream("/img/objects.png")));
        this.rootItem = new CheckBoxTreeItem<>((ObservableObject)new ObservableObjectCategory("Objects"), rootIcon);
        this.rootItem.setExpanded(true);
        this.treeView = new TreeView(rootItem);
        //this.treeView.setEditable(true);
        this.treeView.setShowRoot(false);
        this.treeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        treeView.setCellFactory(CheckBoxTreeCell.<ObjectRepository>forTreeView());

        this.treeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue ov, Object oldValue, Object newValue) {
                if (newValue != null) {
                    CheckBoxTreeItem<ObservableObject> newItem = (CheckBoxTreeItem<ObservableObject>) newValue;
                    notifyObjectSelected(newItem.getValue());
                }
            }
        });

        treeView.setOnMouseClicked(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent mouseEvent)
            {
                if(mouseEvent.getClickCount() == 2)
                {
                    CheckBoxTreeItem<ObservableObject> selectedItem = (CheckBoxTreeItem<ObservableObject>) treeView.getSelectionModel().getSelectedItem();
                    notifyObjectPropertiesRequested(selectedItem.getValue());
                }
            }
        });

        final ContextMenu contextMenu = new ContextMenu();
        final MenuItem removeObject = new MenuItem();
        final MenuItem autoClear = new MenuItem();
        final MenuItem properties = new MenuItem();

        contextMenu.setOnShowing(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e) {
                CheckBoxTreeItem<ObservableObject> selectedItem = (CheckBoxTreeItem<ObservableObject>) treeView.getSelectionModel().getSelectedItem();
                if(categoryItems.containsValue(selectedItem)) {
                    removeObject.setText("Remove");
                    properties.setVisible(false);
                    autoClear.setText("Enable/Disable auto clear category");
                }else if(objectItems.containsValue(selectedItem)){
                    removeObject.setText("Remove");
                    autoClear.setText("Enable/Disable auto clear object");
                    properties.setText("Properties");
                    properties.setVisible(true);
                }
            }
        });

        properties.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                CheckBoxTreeItem<ObservableObject> selectedItem = (CheckBoxTreeItem<ObservableObject>) treeView.getSelectionModel().getSelectedItem();
                notifyObjectPropertiesRequested(selectedItem.getValue());
            }
        });

        removeObject.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                CheckBoxTreeItem<ObservableObject> selectedItem = (CheckBoxTreeItem<ObservableObject>) treeView.getSelectionModel().getSelectedItem();

                // clear objects under the selected category
                if(categoryItems.containsValue(selectedItem)) {
                    List<ObservableObject> removeObjects = getObjectsByCategory(selectedItem.getValue().getObjectId());
                    for (ObservableObject observableObject : removeObjects) {
                        removeObject(observableObject);
                    }
                    
                // clear de selected object
                }else if(objectItems.containsValue(selectedItem)){
                    removeObject(selectedItem.getValue());
                }
            }
        });

        autoClear.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                CheckBoxTreeItem<ObservableObject> selectedItem = (CheckBoxTreeItem<ObservableObject>) treeView.getSelectionModel().getSelectedItem();

                // autoclear objects under the selected category
                if(categoryItems.containsValue(selectedItem)) {
                    ObservableObjectCategory category = (ObservableObjectCategory) selectedItem.getValue();

                // autoclear the selected object
                }else if(objectItems.containsValue(selectedItem)){
                    
                }
            }
            
        });
        
        contextMenu.getItems().addAll(removeObject, autoClear, properties);

        this.treeView.setContextMenu(contextMenu);

        //VBox.setVgrow(objectsTabPane, Priority.ALWAYS);
        VBox.setVgrow(treeView, Priority.ALWAYS);
    }

    public Node getView() {
        return this.treeView;
    }

    public void setObjectAndShow(ObservableObject object) {
        setObject(object);
        notifyObjectPropertiesRequested(object);
    }

    public void setObject(ObservableObject object) {
        if (!objects.containsKey(object.getObjectId())) 
        {
            objects.put(object.getObjectId(), object);
            addObjectToList(object);
            notifyObjectCreated(object);
        }else
        {
            ObservableObject existing = objects.get(object.getObjectId());
            
            // update the existing object
            existing.update(object);

            // notify object update
            notifyObjectUpdated(existing);
        }
        
        //deleteExpiredObjects(60000);
    }

    private Map<String, CheckBoxTreeItem<ObservableObject>> categoryItems = new HashMap<>();
    private Map<String, CheckBoxTreeItem<ObservableObject>> objectItems = new HashMap<>();

    private void addObjectToList(ObservableObject object) {
        CheckBoxTreeItem<ObservableObject> categoryTree = categoryItems.get(object.getObjectCategory());

        if (categoryTree == null) {
            final ObservableObjectCategory category = new ObservableObjectCategory(object.getObjectCategory(), object.isShowOnMap());
            categoryTree = new CheckBoxTreeItem<>((ObservableObject) category);
            categoryTree.getValue().setObjectTag(object.getObjectCategory() + " [1]");
            categoryTree.setExpanded(false);
            categoryTree.setSelected(object.isShowOnMap());
            // detect manual changes in selected property to automatically adapt the visibility of new objects under category
            categoryTree.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean aBoolean2) {
                    category.setVisible(aBoolean2);
                }
            });
            treeView.getRoot().getChildren().add(categoryTree);
            categoryItems.put(object.getObjectCategory(), categoryTree);
        }
        
        final ObservableObjectCategory category = (ObservableObjectCategory) categoryTree.getValue();

        // create and add new node representing the object
        final CheckBoxTreeItem<ObservableObject> treeObject = new CheckBoxTreeItem<>(object);
        categoryTree.getChildren().add(treeObject);

        // set visibility according to category visibility or its own visibility
        object.setShowOnMap(category.isManualVisibility() ? category.isVisible() : object.isShowOnMap());
        treeObject.setSelected(object.isShowOnMap());

        // listen on selected property to change object visibility
        treeObject.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean aBoolean2) {
                treeObject.getValue().setShowOnMapAndUpdate(observableValue.getValue());
            }
        });

        // update category tree with the number of elements
        categoryTree.getValue().setObjectTag(object.getObjectCategory() + " [" + categoryTree.getChildren().size() + "]");

        // save object associated with its id
        objectItems.put(object.getObjectId(), treeObject);
    }

    public void removeObject(ObservableObject object) {
        if (object!=null && objects.containsKey(object.getObjectId())) {
            objects.remove(object.getObjectId());
            removeObjectFromList(object);
            notifyObjectRemoved(object);
        }
        /*else{
            System.err.println("Cannot delete object with id: " + object.getObjectId() + " (not found)");
        }*/
    }

    private void removeObjectFromList(ObservableObject object) {
        CheckBoxTreeItem<ObservableObject> categoryTree = categoryItems.get(object.getObjectCategory());
        if (categoryTree != null) {
            CheckBoxTreeItem<ObservableObject> objectTree = objectItems.get(object.getObjectId());
            if (objectTree != null) {
                categoryTree.getChildren().remove(objectTree);
                if (categoryTree.getChildren().size() == 0) {
                    categoryItems.remove(object.getObjectCategory());
                    treeView.getRoot().getChildren().remove(categoryTree);
                }
                objectItems.remove(object.getObjectId());
            }
            categoryTree.getValue().setObjectTag(object.getObjectCategory() + " [" + categoryTree.getChildren().size() + "]"); 
        }
    }

    /*
     private void removeObjectFromList(ObservableObject object) {
     ObjectList objectList = objectsTabPane.getCategory(object.getObjectCategory());
     if (objectList != null) {
     objectList.removeObject(object.getObjectId());
     }
     }*/
    public ObservableObject getObject(String objectId) {
        return objects.get(objectId);
    }

    public boolean containsObject(String objectId) {
        return objects.containsKey(objectId);
    }

    public List<ObservableObject> getObjectsByCategory(String category) {
        List<ObservableObject> objectsR = new ArrayList<>();
        if (category != null && !"".equals(category)) {
            for (Entry<String, ObservableObject> entry : objects.entrySet()) {
                if (category.equals(entry.getValue().getObjectCategory())) {
                    objectsR.add(entry.getValue());
                }
            }
        }
        return objectsR;
    }

    public void clear() {
        for (ObservableObject object : objects.values()) {
            notifyObjectRemoved(object);
        }
        objects.clear();
        clearList();
    }

    private void clearList() {
        categoryItems.clear();
        objectItems.clear();
        treeView.getRoot().getChildren().clear();
    }

    public List<ObservableObject> getObjects() {
        List<ObservableObject> objectsR = new ArrayList<>();
        for (Entry<String, ObservableObject> entry : objects.entrySet()) {
            objectsR.add(entry.getValue());
        }
        return objectsR;
    }

    public Map<String, ObservableObject> getoAllObjects(){
        return objects;
    }

    public boolean saveObjects(File file) {
        try {
            FileOutputStream fOut = new FileOutputStream(file);
            ObjectOutputStream objectOutput = new ObjectOutputStream(fOut);
            objectOutput.writeObject(getObjects());
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean loadObjects(File file) {
        try {
            FileInputStream fIn = new FileInputStream(file);
            ObjectInputStream objectOutput = new ObjectInputStream(fIn);
            List<ObservableObject> objects = (List<ObservableObject>) objectOutput.readObject();
            System.out.println("Loaded: " + objects.size() + " items.");
            for (ObservableObject object : objects) {
                setObject(object);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void notifyObjectPropertiesRequested(ObservableObject object) {
        if (object != null) {
            for (ObjectsCallback callback : objectCallbacks) {
                callback.onObjectPropertiesRequested(object);
            }
        }
    }

    private void notifyObjectSelected(ObservableObject object) {
        if (object != null) {
            for (ObjectsCallback callback : objectCallbacks) {
                callback.onObjectSelected(object);
            }
        }
    }

    private void notifyObjectCreated(ObservableObject object) {
        if (object != null) {
            for (ObjectsCallback callback : objectCallbacks) {
                callback.onObjectCreated(object);
            }
        }
    }

    private void notifyObjectUpdated(ObservableObject object) {
        if (object != null) {
            for (ObjectsCallback callback : objectCallbacks) {
                callback.onObjectUpdated(object);
            }
        }
    }

    private void notifyObjectRemoved(ObservableObject object) {
        if (object != null) {
            for (ObjectsCallback callback : objectCallbacks) {
                callback.onObjectRemoved(object);
            }
        }
    }
    
    public void deleteExpiredObjects(long millisTimeout)
    {
        long currenTime = System.currentTimeMillis();
        List<ObservableObject> toDeleteObjects = new ArrayList<>();
        for(ObservableObject object : objects.values())
        {
            if((currenTime-object.getLastUpdate())>millisTimeout)
            {
                toDeleteObjects.add(object);
            }
        }
        for(ObservableObject object : toDeleteObjects)
        {
            removeObject(object);
        }
    }
}
