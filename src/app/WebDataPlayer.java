package app;

import data.ConfigurableObservableObject;
import data.ObjectRepository;
import data.ObservableObject;
import data.input.DataListener;
import data.items.InfoData;
import data.items.ObjectData;
import gui.*;
import gui.map.GoogleMap;
import gui.map.GoogleMapObjectCallback;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * @author Jasper Potts
 */
public class WebDataPlayer extends Application implements DataListener, ObjectRepository.ObjectsCallback {

    private MenuBar menuBar;
    private final VBox toolbars = new VBox();
    private static GoogleMap map;
    private static ObjectRepository objectRepository;
    private TabPaneContainer<TextArea> infosTabPane;
    private TitledPaneContainer rightNode;

    public static GoogleMap getGoogleMap() {
        return map;
    }
    
    public static ObjectRepository getObjectRepository(){
        return objectRepository;
    }
    
    private void configureToolbars() {
        // add the item to the menu
        Menu menuToolbars = new Menu("Operation Mode");
        menuBar.getMenus().add(menuToolbars);

        ToggleGroup toggleGroup = new ToggleGroup();

        final RadioMenuItem fileMode = new RadioMenuItem("File Reader");
        final RadioMenuItem socketMode = new RadioMenuItem("Socket Reader");
        final RadioMenuItem simulationMode = new RadioMenuItem("Simulation");

        fileMode.setToggleGroup(toggleGroup);
        socketMode.setToggleGroup(toggleGroup);
        simulationMode.setToggleGroup(toggleGroup);

        menuToolbars.getItems().addAll(fileMode, socketMode, simulationMode);

        fileMode.selectedProperty().addListener(new ChangeListener<Boolean>() {
            private FilePlayerToolbar fileToolbar;

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                if (t1) {
                    if(fileToolbar==null){
                        fileToolbar = new FilePlayerToolbar(WebDataPlayer.this);
                    }
                    toolbars.getChildren().add(fileToolbar);
                } else {
                    toolbars.getChildren().remove(fileToolbar);
                }
            }
        });

        socketMode.selectedProperty().addListener(new ChangeListener<Boolean>() {
            private SocketPlayerToolbar socketToolbar;

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                if (t1) {
                    if(socketToolbar==null){
                        socketToolbar = new SocketPlayerToolbar(WebDataPlayer.this);
                    }
                    toolbars.getChildren().add(socketToolbar);
                } else {
                    toolbars.getChildren().remove(socketToolbar);
                }
            }
        });

        simulationMode.selectedProperty().addListener(new ChangeListener<Boolean>() {
            private SimulationToolbar simToolbar;

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                if (t1) {
                    if(simToolbar==null){
                        simToolbar = new SimulationToolbar(objectRepository);
                    }
                    toolbars.getChildren().add(simToolbar);
                } else {
                    toolbars.getChildren().remove(simToolbar);
                }
            }
        });

        // set socked mode as default toolbar
        socketMode.setSelected(true);
    }
    
    private CheckMenuItem showRightPane;

    @Override
    public void start(Stage stage) {

        map = new GoogleMap();
        objectRepository = ObjectRepository.getInstance();
        objectRepository.addObjectCallback(this);
        this.menuBar = new MenuBar();

        final SplitPane leftPane = new SplitPane();
        leftPane.setMinWidth(180);
        SplitPane.setResizableWithParent(leftPane, Boolean.FALSE);
        
        final SplitPane contentPane = new SplitPane();
        rightNode = new TitledPaneContainer();
        rightNode.setMinWidth(270);
        SplitPane.setResizableWithParent(rightNode, Boolean.FALSE);
        
        Menu menuFile = new Menu("File");
        Menu menuView = new Menu("View");

        // player mode elements

        // view elements
        final CheckMenuItem showLeftPane = new CheckMenuItem("Left Pane");
        final CheckMenuItem showRightPane = new CheckMenuItem("Right Pane");
        this.showRightPane = showRightPane;
        //showLeftToolbar.setSelected(true);
        //showRightToolbar.setSelected(false);
        menuView.getItems().addAll(showLeftPane, showRightPane);

        menuBar.getMenus().addAll(menuFile, menuView);

        configureToolbars();


        showLeftPane.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue ov, Boolean old_val, Boolean new_val) {
                if (new_val) {
                    contentPane.getItems().add(0, leftPane);
                    if(contentPane.getDividers().size()==2){
                        contentPane.setDividerPosition(0, 0); 
                        contentPane.setDividerPosition(1, 1); 
                    }else{
                        contentPane.setDividerPosition(0, 0); 
                    }
                } else {
                    contentPane.getItems().remove(leftPane);
                    if(contentPane.getDividers().size()==1){
                        contentPane.setDividerPosition(0, 1); 
                    }
                }
            }
        });
        
        showRightPane.selectedProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                if(t1){
                    contentPane.getItems().add(rightNode);
                    if(contentPane.getDividers().size()==2){
                        contentPane.setDividerPosition(0, 0); 
                        contentPane.setDividerPosition(1, 1); 
                    }else{
                        contentPane.setDividerPosition(0, 1); 
                    }
                }else{
                    contentPane.getItems().remove(rightNode);
                    if(contentPane.getDividers().size()==1){
                        contentPane.setDividerPosition(0, 0); 
                    }
                }
            }
        });
       
        // add left pane
        showLeftPane.setSelected(true);

        // create the tab  pane for info data
        infosTabPane = new TabPaneContainer<>(TextArea.class);        
        
        TabPane repositoryTabPane = new TabPane();
        Tab repositoryTab = new Tab("Repository");
        repositoryTab.setContent(objectRepository.getView());
        repositoryTabPane.getTabs().add(repositoryTab);

        // set the left box with listpane and info pane components
        leftPane.setOrientation(Orientation.VERTICAL);
        leftPane.setDividerPositions(0.7);
        leftPane.getItems().addAll(repositoryTabPane);
        HBox.setHgrow(leftPane, Priority.NEVER);

        
        contentPane.getItems().addAll(map);
        contentPane.setOrientation(Orientation.HORIZONTAL);
        VBox.setVgrow(contentPane, Priority.ALWAYS);

        // full root view of the screen
        VBox rootBox = new VBox();
        rootBox.getChildren().addAll(menuBar, toolbars, contentPane);

        // create scene & show
        stage.setTitle("Object Player");
        Scene scene = new Scene(rootBox, 1000, 800, Color.web("#666970"));

        stage.setScene(scene);
        stage.show();
        
        
        // add callback for listen for clicked tags
        final GoogleMapObjectCallback tagCallback = new GoogleMapObjectCallback() {
            @Override
            public void onTagSelected(String objectId) {
                ObservableObject observableObject = objectRepository.getObject(objectId);
                if(observableObject!=null)
                {
                    onObjectSelected(observableObject);
                }
            }

            @Override
            public void onTagDoubleClick(String objectId) {
                ObservableObject observableObject = objectRepository.getObject(objectId);
                if(observableObject!=null)
                {
                    onObjectSelected(observableObject);
                }
            }
        };
        map.addObjectCallback(tagCallback);
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void onInfoDataReceived(InfoData infoData) {
        setInfoText(infoData.getObjectCategory(), infoData.toString());
    }

    private void setInfoText(String category, String text) {
        TextArea textArea = infosTabPane.getCategory(category);
        if (textArea == null) {
            textArea = infosTabPane.addCategory(category);
        }
        textArea.setText(text);
    }

    @Override
    public void onObjectPropertiesRequested(final ObservableObject object) {
        if (object instanceof ConfigurableObservableObject) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    if(!showRightPane.isSelected()){
                        showRightPane.selectedProperty().setValue(Boolean.TRUE);
                    }
                    if (!rightNode.contains(object.getObjectId())) {
                        rightNode.clear();
                        final ConfigurableObservableObject configurable = (ConfigurableObservableObject) object;
                        TitledPane pane = new TitledPane(object.getObjectCategory() + " - " + object.getObjectId(), configurable.getConfigurationNode());
                        rightNode.set(object.getObjectId(), pane);
                        rightNode.setExpandedPane(pane);
                    }else{
                        TitledPane pane = rightNode.get(object.getObjectId());
                        rightNode.setExpandedPane(pane);
                    }
                }
            });
        }
    }

    @Override
    public void onObjectSelected(ObservableObject object) {
        map.zoomToObject(object.getObjectId());
    }
    
    @Override
    public void onObjectDataReceived(ObjectData objectData) {
        switch (objectData.getOperation()) {
            case SET: objectRepository.setObject(objectData); break;
            case REMOVE: objectRepository.removeObject(objectData); break;
        }
    }

    @Override
    public void onObjectCreated(ObservableObject object) {
        object.setOnMap();
        //if (object instanceof ConfigurableObservableObject) {
        //    onObjectSelected(object);
        //}
    }

    @Override
    public void onObjectRemoved(final ObservableObject object) {
        object.removeFromMap();
        if (object instanceof ConfigurableObservableObject) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    rightNode.remove(object.getObjectId());
                }
            });
        }
    }

    @Override
    public void onObjectUpdated(ObservableObject object) {
        object.setOnMap();
    }
    
    /**
     * DATA FEEDERS LISTENER
     */
    
    @Override
    public void onStart(boolean clearPrevious) {
        // clear tracks from map
        if(clearPrevious){
            objectRepository.clear();
        }
    }

    @Override
    public void onStop() {
    }
    
    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }
}
