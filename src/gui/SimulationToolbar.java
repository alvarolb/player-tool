package gui;

import app.Simulation;
import data.ObjectRepository;
import data.ObservableObject;
import data.input.DataListener;
import data.items.*;
import data.simulation.*;
import data.simulation.track.SimulatedTrack;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import util.XmlUtils;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 *
 * @author Alvaro
 */
public class SimulationToolbar extends ToolBar implements DataListener {

    private final Button createSensor;
    private final Button createTrack;
    private final Button createRule;
    private final Button createAngle;
    private final Button createPolygon;
    private final Button createPolyline;
    private final Button createHeatmap;
    private final Button createAgent;
    private final Button saveScenario;
    private final Button loadScenario;
    private final Button loadXMLObjects;
    
    private final Button exportKMLTracks;
    private final Button exportKMLPoly;
    private final Button importKMLTracks;
    private final Button importKMLPoly;
    
    private final ToggleButton runSimulation;
    private final Slider speed;
    private final ObjectRepository repository;

    private abstract class CreateObjectPopup extends Stage {

        public abstract boolean onAccept(String sensorType, String sensorId);

        public CreateObjectPopup(String objectType, String... types) {
            final GridPane gridPane = new GridPane();
            gridPane.setPadding(new Insets(3, 3, 3, 3));
            gridPane.setVgap(5);
            gridPane.setHgap(5);

            final ComboBox<String> type = new ComboBox();
            gridPane.add(new Text(objectType + " Type"), 0, 0);
            gridPane.add(type, 1, 0);


            final TextField sensorId = new TextField();
            sensorId.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    if (onAccept(type.valueProperty().getValue(), sensorId.getText())) {
                        close();
                    }
                }
            });
            gridPane.add(new Text(objectType + " Id"), 0, 1);
            gridPane.add(sensorId, 1, 1);

            final Button accept = new Button("Accept");
            final Button cancel = new Button("Cancel");
            gridPane.add(accept, 0, 2);
            gridPane.add(cancel, 1, 2);

            for (int i = 0; i < types.length; i++) {
                type.getItems().add(types[i]);
            }
            type.valueProperty().setValue(types[0]);

            accept.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    if (onAccept(type.valueProperty().getValue(), sensorId.getText())) {
                        close();
                    }
                }
            });
            
            cancel.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    close();
                }
            });

            VBox rootBox = new VBox();
            Label title = new Label("Create Sensor");
            rootBox.getChildren().addAll(title, new Separator(), gridPane);

            Scene scene = new Scene(rootBox, 222, 100);
            setScene(scene);
            
            // the request focus should be called when the node is part of a Scene
            sensorId.requestFocus();
        }
    }
    
    private abstract class CreateObject extends Stage {

        public abstract boolean onAccept(String ruleId);

        public CreateObject(String object) {
            final GridPane gridPane = new GridPane();
            gridPane.setPadding(new Insets(3, 3, 3, 3));
            gridPane.setVgap(5);
            gridPane.setHgap(5);

            final TextField ruleId = new TextField();
            ruleId.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    if (onAccept(ruleId.getText())) {
                        close();
                    }
                }
            });
            gridPane.add(new Text(object + " Id"), 0, 1);
            gridPane.add(ruleId, 1, 1);

            final Button accept = new Button("Accept");
            final Button cancel = new Button("Cancel");
            gridPane.add(accept, 0, 2);
            gridPane.add(cancel, 1, 2);

            accept.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    if (onAccept(ruleId.getText())) {
                        close();
                    }
                }
            });
            
            cancel.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    close();
                }
            });

            VBox rootBox = new VBox();
            Label title = new Label("Create " + object);
            rootBox.getChildren().addAll(title, new Separator(), gridPane);

            Scene scene = new Scene(rootBox, 222, 100);
            setScene(scene);
            
            ruleId.requestFocus();
        }
    }

    private abstract class CreateTrackPopup extends Stage {

        public abstract boolean onAccept(String trackId);

        public CreateTrackPopup() {
            final GridPane gridPane = new GridPane();
            gridPane.setPadding(new Insets(3, 3, 3, 3));
            gridPane.setVgap(5);
            gridPane.setHgap(5);

            final TextField trackId = new TextField();
            trackId.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    if (onAccept(trackId.getText())) {
                        close();
                    }
                }
            });
            gridPane.add(new Text("SimulatedTrack Id"), 0, 1);
            gridPane.add(trackId, 1, 1);

            final Button accept = new Button("Accept");
            final Button cancel = new Button("Cancel");
            gridPane.add(accept, 0, 2);
            gridPane.add(cancel, 1, 2);

            accept.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    if (onAccept(trackId.getText())) {
                        close();
                    }
                }
            });
            
            cancel.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent actionEvent) {
                    close();
                }
            });

            VBox rootBox = new VBox();
            Label title = new Label("Create SimulatedTrack");
            rootBox.getChildren().addAll(title, new Separator(), gridPane);

            Scene scene = new Scene(rootBox, 222, 100);
            setScene(scene);
            
            trackId.requestFocus();
        }
    }

    public SimulationToolbar(ObjectRepository respository) {
        this.repository = respository;
        createSensor = new Button("Sensor");
        createTrack = new Button("SimulatedTrack");
        createRule = new Button("Rule");
        createAngle = new Button("Angle");
        createPolygon = new Button("Polygon");
        createPolyline = new Button("Polyline");
        createHeatmap = new Button("Heatmap");
        createAgent = new Button("Agent");
        runSimulation = new ToggleButton("Run");
        speed = new Slider(1, 100, 1);
        
        saveScenario = new Button("Save");
        loadScenario = new Button("Load");
        loadXMLObjects = new Button("XML Sensors");
        importKMLTracks = new Button("KML Tracks");
        exportKMLTracks = new Button("KML Tracks");
        importKMLPoly = new Button("KML Poly");
        exportKMLPoly = new Button("KML Poly");

        createSensor.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {

                final CreateObjectPopup sensorIdPopup = new CreateObjectPopup("Sensor", "Radar", "AIS", "Camera") {
                    @Override
                    public boolean onAccept(String sensorType, String sensorId) {
                        Sensor sensor = null;
                        if (!"".equals(sensorId) && !repository.containsObject(sensorId)) {
                            if ("AIS".equals(sensorType)) {
                                sensor = new AISSensor(sensorId);
                            } else if ("Radar".equals(sensorType)) {
                                sensor = new RadarSensor(sensorId);
                            } else if("Camera".equals(sensorType)){
                                sensor = new Camera(sensorId);
                            }
                        }
                        if(sensor!=null){
                            repository.setObjectAndShow(sensor);
                            return true;
                        }else{
                            return false;
                        }
                    }
                };

                sensorIdPopup.show();
            }
        });

        createTrack.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                final CreateTrackPopup trackIdPopup = new CreateTrackPopup() {
                    @Override
                    public boolean onAccept(String fieldText) {
                        if (!"".equals(fieldText) && !repository.containsObject(fieldText)) {
                            SimulatedTrack track = new SimulatedTrack(fieldText);
                            repository.setObjectAndShow(track);
                            return true;
                        }
                        return false;
                    }
                };

                trackIdPopup.show();
            }
        });
        
        createRule.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                final CreateObject ruleIdPopup = new CreateObject("Rule") {
                    @Override
                    public boolean onAccept(String fieldText) {
                        if (!"".equals(fieldText) && !repository.containsObject(fieldText)) {
                            Rule rule = new Rule(fieldText);
                            repository.setObjectAndShow(rule);
                            return true;
                        }
                        return false;
                    }
                };
                
                ruleIdPopup.show();
            }
        });
        
        createAngle.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                final CreateObject ruleIdPopup = new CreateObject("Angle") {
                    @Override
                    public boolean onAccept(String fieldText) {
                        if (!"".equals(fieldText) && !repository.containsObject(fieldText)) {
                            Angle angle = new Angle(fieldText);
                            repository.setObjectAndShow(angle);
                            return true;
                        }
                        return false;
                    }
                };
                
                ruleIdPopup.show();
            }
        });
        
        createPolygon.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                final CreateObject ruleIdPopup = new CreateObject("Polygon") {
                    @Override
                    public boolean onAccept(String fieldText) {
                        if (!"".equals(fieldText) && !repository.containsObject(fieldText)) {
                            Polygon poly = new Polygon(fieldText);
                            repository.setObjectAndShow(poly);
                            return true;
                        }
                        return false;
                    }
                };
                
                ruleIdPopup.show();
            }
        });

        createPolyline.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                final CreateObject polyLineIdPopup = new CreateObject("Polyline") {
                    @Override
                    public boolean onAccept(String fieldText) {
                        if (!"".equals(fieldText) && !repository.containsObject(fieldText)) {
                            Polyline polyline = new Polyline(fieldText);
                            repository.setObjectAndShow(polyline);
                            return true;
                        }
                        return false;
                    }
                };
                polyLineIdPopup.show();
            }
        });

        createHeatmap.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                final CreateObject heatmapIdPopup = new CreateObject("HeatMap") {
                    @Override
                    public boolean onAccept(String fieldText) {
                        if (!"".equals(fieldText) && !repository.containsObject(fieldText)) {
                            HeatMap heatmap = new HeatMap(fieldText);
                            repository.setObjectAndShow(heatmap);
                            return true;
                        }
                        return false;
                    }
                };
                heatmapIdPopup.show();
            }
        });

        createAgent.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                final CreateObjectPopup agentIdPopup = new CreateObjectPopup("Agent", "Camera", "Fusion") {
                    @Override
                    public boolean onAccept(String sensorType, String sensorId) {
                        Agent agent = null;
                        if (!"".equals(sensorId) && !repository.containsObject(sensorId)) {
                            switch (sensorType){
                                case "Camera":
                                    agent = new CameraAgent(sensorId, CameraAgent.CATEGORY);
                                    break;
                                case "Fusion":
                                    agent = new FusionAgent(sensorId, FusionAgent.CATEGORY);
                                    break;
                            }
                        }
                        if(agent!=null) repository.setObjectAndShow(agent);
                        return agent != null;
                    }
                };
                agentIdPopup.show();
            }
        });

        final Simulation simulation = new Simulation(repository);
        runSimulation.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                if (t1) {
                    simulation.setAccelerationFactor((int)speed.getValue());
                    simulation.start();
                } else {
                    simulation.stop();
                }
            }
        });
        
        speed.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
                int speedValue = new_val.intValue();
                //speedLabel.setText(speedValue + "x");
               simulation.setAccelerationFactor(speedValue);
            }
        });
        
        saveScenario.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                FileChooser fileChooser = new FileChooser();

                //Set extension filter
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Scenario files (*.scn)", "*.scn");
                fileChooser.getExtensionFilters().add(extFilter);

                //Show open file dialog
                final File file = fileChooser.showSaveDialog(null);
                if (file != null) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            boolean result = repository.saveObjects(file);
                        }
                    });
                }
            }
        });

        loadScenario.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                FileChooser fileChooser = new FileChooser();

                //Set extension filter
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Scenario files (*.scn)", "*.scn");
                fileChooser.getExtensionFilters().add(extFilter);

                //Show open file dialog
                final File file = fileChooser.showOpenDialog(null);
                if (file != null) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            boolean result = repository.loadObjects(file);
                        }
                    });
                }
            }
        });
        
        loadXMLObjects.setOnAction(new EventHandler<ActionEvent>(){
            @Override
            public void handle(ActionEvent t) {
                FileChooser fileChooser = new FileChooser();

                //Set extension filter
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Object files (*.xml)", "*.xml");
                fileChooser.getExtensionFilters().add(extFilter);

                //Show open file dialog
                final File file = fileChooser.showOpenDialog(null);
                if (file != null) {
                    try {
                        Document doc = XmlUtils.createDocumentFromFile(file);
                        if (doc != null) {
                            List<Element> sensors = XmlUtils.findChildren(doc.getDocumentElement(), "sensor");
                            for (Element sensor : sensors) {
                                Element name = XmlUtils.findChild(sensor, "name");
                                Sensor sensorS = Sensor.buildSensorFromXMLNode(sensor);
                                if(sensor!=null){
                                    repository.setObject(sensorS);
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
        });
        
        importKMLTracks.setOnAction(new EventHandler<ActionEvent>(){

            @Override
            public void handle(ActionEvent t) {
                FileChooser fileChooser = new FileChooser();

                //Set extension filter
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("KML tracks (*.kml)", "*.kml");
                fileChooser.getExtensionFilters().add(extFilter);

                //Show open file dialog
                final File file = fileChooser.showOpenDialog(null);
                if (file != null) {
                    try {
                        Document doc = XmlUtils.createDocumentFromFile(file);
                        if (doc != null) {
                            Element kmlDocument = XmlUtils.findChild(doc.getDocumentElement(), "Document");
                            if(kmlDocument!=null){
                                List<Element> tracks = XmlUtils.findChildren(kmlDocument, "Placemark");
                                for (Element track : tracks) {
                                    SimulatedTrack trackS = SimulatedTrack.buildTrackFromXMLNode(track);
                                    if(trackS!=null){
                                        repository.setObjectAndShow(trackS);
                                    }
                                }    
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            
        });
        
        importKMLPoly.setOnAction(new EventHandler<ActionEvent>(){

            @Override
            public void handle(ActionEvent t) {
                FileChooser fileChooser = new FileChooser();

                //Set extension filter
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("KML polygon files (*.kml)", "*.kml");
                fileChooser.getExtensionFilters().add(extFilter);

                //Show open file dialog
                final File file = fileChooser.showOpenDialog(null);
                if (file != null) {
                    try {
                        Document doc = XmlUtils.createDocumentFromFile(file);
                        if (doc != null) {
                            Element kmlDocument = XmlUtils.findChild(doc.getDocumentElement(), "Document");
                            if(kmlDocument!=null){
                                List<Element> placemarks = XmlUtils.findChildren(kmlDocument, "Placemark");
                                for (Element placemark : placemarks) {
                                    Polygon polygon = Polygon.buildPolygonFromXMLNode(placemark);
                                    if(polygon!=null){
                                        repository.setObjectAndShow(polygon);
                                    }
                                }    
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            
        });
        
        exportKMLTracks.setOnAction(new EventHandler<ActionEvent>(){
            @Override
            public void handle(ActionEvent t) {
                FileChooser fileChooser = new FileChooser();

                //Set extension filter
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("KML files (*.kml)", "*.kml");
                fileChooser.getExtensionFilters().add(extFilter);

                //Show open file dialog
                final File file = fileChooser.showSaveDialog(null);
                if (file != null) {
                    try {
                        Document doc = XmlUtils.createDocument();
            
                        Element root = doc.createElement("kml");
                        root.setAttribute("xmlns", "http://www.opengis.net/kml/2.2");
                        doc.appendChild(root);

                        Element document = doc.createElement("Document");
                        root.appendChild(document);
                        
                        // add placemarks
                        List<ObservableObject> objects = repository.getObjectsByCategory(SimulatedTrack.CATEGORY);
                        for(ObservableObject object:objects){
                            ((SimulatedTrack)object).addKMLWaypointsPlaceMark(doc, document);
                        }
                        
                        TransformerFactory transfac = TransformerFactory.newInstance();
                        Transformer trans = transfac.newTransformer();
                        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
                        trans.setOutputProperty(OutputKeys.INDENT, "yes");
                        trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

                        //create string from xml tree
                        FileWriter sw = new FileWriter(file);
                        StreamResult result = new StreamResult(sw);
                        DOMSource source = new DOMSource(doc);
                        trans.transform(source, result);
                        sw.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        
        exportKMLPoly.setOnAction(new EventHandler<ActionEvent>(){
            @Override
            public void handle(ActionEvent t) {
                FileChooser fileChooser = new FileChooser();

                //Set extension filter
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("KML files (*.kml)", "*.kml");
                fileChooser.getExtensionFilters().add(extFilter);

                //Show open file dialog
                final File file = fileChooser.showSaveDialog(null);
                if (file != null) {
                    try {
                        Document doc = XmlUtils.createDocument();
            
                        Element root = doc.createElement("kml");
                        root.setAttribute("xmlns", "http://www.opengis.net/kml/2.2");
                        doc.appendChild(root);

                        Element document = doc.createElement("Document");
                        root.appendChild(document);
                        
                        // add placemarks
                        List<ObservableObject> objects = repository.getObjectsByCategory(Polygon.CATEGORY);
                        for(ObservableObject object:objects){
                            ((Polygon)object).addKMLPolygonPlaceMark(doc, document);
                        }
                        
                        TransformerFactory transfac = TransformerFactory.newInstance();
                        Transformer trans = transfac.newTransformer();
                        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
                        trans.setOutputProperty(OutputKeys.INDENT, "yes");
                        trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

                        //create string from xml tree
                        FileWriter sw = new FileWriter(file);
                        StreamResult result = new StreamResult(sw);
                        DOMSource source = new DOMSource(doc);
                        trans.transform(source, result);
                        sw.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        getItems().addAll(new Label("Create:"), createSensor, createTrack, createRule, createAngle, createPolygon, createPolyline, createHeatmap, createAgent, new Label("Simulation:"), runSimulation, speed, new Separator(), new Text("Scenario"), saveScenario, loadScenario, new Label("Import:"), loadXMLObjects, importKMLTracks, importKMLPoly, new Label("Export:"), exportKMLTracks, exportKMLPoly);
    }

    @Override
    public void onInfoDataReceived(InfoData infoData) {
    }

    @Override
    public void onObjectDataReceived(ObjectData objectData) {
    }

    @Override
    public void onStart(boolean reset) {
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onStop() {
    }
}
