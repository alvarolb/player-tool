/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package data.simulation.track;

import app.WebDataPlayer;
import data.ObjectRepository;
import data.ObservableObject;
import data.items.Rule;
import data.simulation.AISMessage;
import data.simulation.Sensor;
import data.simulation.WaypointData;
import data.simulation.events.Event;
import geom.LatLng;
import geom.Spherical;
import geom.TurnPoints;
import gui.PromptDistanceAzimuth;
import gui.map.GoogleMapCallback;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import util.XmlUtils;

/**
 *
 * @author alvarolb
 */
public class SimulatedTrack extends AbstractTrack {
    // static data

    private static final long serialVersionUID = 1L;

    public static final String CATEGORY = "SimulatedTrack";
    private static DecimalFormat twoDecimals = new DecimalFormat("#.##");
    // track configuration
    private LatLng trackPosition; // starting point
    private double trackWidth = 300; // track width
    private double trackLength = 600; // track lenght
    private double trackHeight = 0; // track height
    // track AIS Sensor
    private boolean AISTransponder;
    private boolean AISDifferential;
    private long AISMMSI;
    // track waypoints
    private List<LatLng> wayPoints;
    private Map<LatLng, WaypointData> waypointData;
    // track route simulation
    private boolean greatCirclePath;
    private boolean showTurns;
    private boolean showTrackSensorRules;
    private boolean showTrackToTrackRuler;
    private double trackDelay;
    // temporal store for track to sensor distance ruler
    private transient Map<Sensor, Rule> trackSensorRuler;
    private transient Map<SimulatedTrack, Rule> trackToTrackRuler;
    // used in track movement simulation
    private transient SimulatorThread simulatorThread;
    private transient LatLng currentPosition;
    
    //private transient double simulationTime;
    private transient double lastAISMessage;
    //private transient double trackSpeedMs;
    //private transient double trackSpeedAccelMs2;
    private transient double trackHeading = 0;
   
    // GUI
    private transient GoogleMapCallback pathCallback;
    // waypoints data
    private transient PositionItem positionNode;
    private transient ToggleButton addWaypointsNode;
    private transient Button removeWaypointNode;
    private transient EditPositionItem editWaypointNode;
    private transient Button manualAddWaypointNode;
    private transient ComboBox comboBoxWaypointNode;
    private transient DoubleItemValue trackSpeedNode;
    private transient DoubleItemValue trackHeadingNode;
    private transient DoubleItemValue trackAccelNode;
    private transient DoubleItemValue waypointAltitude;
    private transient DoubleItemValue waypointSpeed;
    private transient DoubleItemValue waypointAngularSpeed;
    private transient DoubleItemValue waypointDelay;

    public SimulatedTrack(String trackId) {
        super(trackId, CATEGORY, TrackType.SimulatedTrack);
        this.wayPoints = new ArrayList<>();
        this.waypointData = new HashMap<>();
        this.trackPosition = new LatLng();

        // initialize non-gui transient objects
        this.trackSensorRuler = new HashMap<>();
        this.trackToTrackRuler = new HashMap<>();
        this.currentPosition = new LatLng(trackPosition);
    }

    // used in serialization for initialize transient objects
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // initialize non-gui transient objects
        this.trackSensorRuler = new HashMap<>();
        this.trackToTrackRuler = new HashMap<>();
        this.currentPosition = new LatLng(trackPosition);
    }
    
    private transient List<Node> configurableNodes = null;

    @Override
    public List<Node> getConfigurableNodes() {
        
        if(configurableNodes!=null){
            return configurableNodes;
        }

        configurableNodes = new ArrayList<>();

        /**
         * TRACK LOCATION
         */
        configurableNodes.add(new Text("SimulatedTrack Location"));
        configurableNodes.add(new Separator());

        positionNode = new PositionItem(trackPosition) {
            
            @Override
            public void onLocationChanged(LatLng latlon) {
                trackPosition = latlon;
                currentPosition = new LatLng(trackPosition);
                if (wayPoints.size() > 0) {
                    wayPoints.set(0, trackPosition);
                }
                setOnMap();
            }
            
        };
        configurableNodes.add(positionNode);


        /**
         * TRACK SHAPE
         */
        configurableNodes.add(new Text("SimulatedTrack Shape"));
        configurableNodes.add(new Separator());

        final DoubleItemValue widthNode = new DoubleItemValue("Width (m)", trackWidth) {
            @Override
            public void onValueChanged(double value) {
                trackWidth = value;
                setOnMap();
            }
        };
        configurableNodes.add(widthNode);

        final DoubleItemValue lengthNode = new DoubleItemValue("Length (m)", trackLength) {
            @Override
            public void onValueChanged(double value) {
                trackLength = value;
                setOnMap();
            }
        };
        configurableNodes.add(lengthNode);
        
        final DoubleItemValue heightNode = new DoubleItemValue("Height (m)", trackHeight) {
            @Override
            public void onValueChanged(double value) {
                trackLength = value;
            }
        };
        configurableNodes.add(heightNode);


        /**
         * TRACK AIS SENSORS
         */
        configurableNodes.add(new Text("SimulatedTrack Sensors"));
        configurableNodes.add(new Separator());

        final LongItemValue aisMMSINode = new LongItemValue("AIS MSSI", AISMMSI) {
            @Override
            public void onValueChanged(long value) {
                AISMMSI = value;
            }
        };
        
        final BooleanItem aisPrecision = new BooleanItem("AIS Differential", AISDifferential)
        {
            @Override
            public void onValueChanged(boolean value){
                AISDifferential = value;
            }
        };
        
        aisMMSINode.setDisable(!AISTransponder);
        aisPrecision.setDisable(!AISTransponder);

        final BooleanItem aisTransponderNode = new BooleanItem("AIS Transponder", AISTransponder) {
            @Override
            public void onValueChanged(boolean value) {
                AISTransponder = value;
                aisMMSINode.setDisable(!value);
                aisPrecision.setDisable(!value);
            }
        };

        configurableNodes.add(aisTransponderNode);
        configurableNodes.add(aisMMSINode);
        configurableNodes.add(aisPrecision);

        /**
         * TRACK ROUTE WAYPOINTS
         */
        configurableNodes.add(new Text("SimulatedTrack Route Waypoints"));
        configurableNodes.add(new Separator());

        // initialize simulation nodes
        trackSpeedNode = new DoubleItemValue("Curr Speed (m/s)", 0) {
            @Override
            public void onValueChanged(double value) {
            }
        };
        
        trackAccelNode = new DoubleItemValue("Curr Accel (m/s2)", 0) {
            @Override
            public void onValueChanged(double value) {
            }
        };
        
        trackHeadingNode = new DoubleItemValue("Curr Heading (º)", 0) {
            @Override
            public void onValueChanged(double value) {
            }
        };

        final Node addIcon = new ImageView(new Image(getClass().getResourceAsStream("/img/add.png")));
        final Node removeIcon = new ImageView(new Image(getClass().getResourceAsStream("/img/remove.png")));
        final Node editIcon = new ImageView(new Image(getClass().getResourceAsStream("/img/edit.png")));
        final Node manualAddIcon = new ImageView(new Image(getClass().getResourceAsStream("/img/manual.png")));

        addWaypointsNode = new ToggleButton();
        addWaypointsNode.setGraphic(addIcon);

        removeWaypointNode = new Button();
        removeWaypointNode.setGraphic(removeIcon);
        
        editWaypointNode = new EditPositionItem() {
            @Override
            public void onLocationSelected(double latitude, double longitude) {
                updateCurrentWayPoint(new LatLng(latitude, longitude));
                editWaypointNode.setSelected(false);
            }
        };
        editWaypointNode.setGraphic(editIcon);

        manualAddWaypointNode = new Button();
        manualAddWaypointNode.setGraphic(manualAddIcon);

        comboBoxWaypointNode = new ComboBox(FXCollections.observableList(wayPoints));
        comboBoxWaypointNode.setPrefWidth(150);

        pathCallback = new GoogleMapCallback() {
            @Override
            public void onLocationSelected(double latitude, double longitude) {
                addWaypoint(new LatLng(latitude, longitude));
            }
        };

        addWaypointsNode.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                if (t1 && !trackPosition.isValid()) {
                    addWaypointsNode.setSelected(false);
                    return;
                }
                if (t1) {
                    gMap.addCallback(pathCallback);
                } else {
                    gMap.removeCallback(pathCallback);
                }
            }
        });

        removeWaypointNode.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                removeCurrentWaypoint();
            }
        });
        
        manualAddWaypointNode.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                if (trackPosition.isValid()) {
                    new PromptDistanceAzimuth() {
                        @Override
                        public boolean onAccept(double distance, double azimuth) {
                            if (distance > 0 && azimuth >= 0 && azimuth <= 360) {

                                LatLng lastItem = wayPoints.size() > 0 ? wayPoints.get(wayPoints.size() - 1) : trackPosition;
                                LatLng offset;

                                if (greatCirclePath) {
                                    offset = Spherical.getDestinationPoint(lastItem, distance, azimuth);
                                } else {
                                    offset = Spherical.getRhumbDestinationPoint(lastItem, distance, azimuth);
                                }

                                addWaypoint(offset);
                                return true;
                            }
                            return false;
                        }
                    }.show();
                }
            }
        });

        GridPane waypointsNode = new GridPane();
        waypointsNode.add(new Label("Waypoints"), 0, 0);
        waypointsNode.add(addWaypointsNode,     1, 0);
        waypointsNode.add(removeWaypointNode,   2, 0);
        waypointsNode.add(editWaypointNode,     3, 0);
        waypointsNode.add(manualAddWaypointNode,4, 0);
        waypointsNode.setHgap(5);
        configurableNodes.add(waypointsNode);

        configurableNodes.add(comboBoxWaypointNode);

        waypointAltitude = new DoubleItemValue("Altitude (m)", 0) {
            @Override
            public void onValueChanged(double value) {
                LatLng waypoint = (LatLng) comboBoxWaypointNode.getValue();
                waypoint.setAltitude(value);
            }
        };
        configurableNodes.add(waypointAltitude);

        waypointSpeed = new DoubleItemValue("Speed (m/s)", 0) {
            @Override
            public void onValueChanged(double value) {
                LatLng waypoint = (LatLng) comboBoxWaypointNode.getValue();
                WaypointData wpData = waypointData.get(waypoint);
                if (wpData != null) {
                    wpData.setSpeed(value);
                    // update turn
                    setTurnRepresentationOnMap();
                }
            }
        };
        configurableNodes.add(waypointSpeed);

        waypointAngularSpeed = new DoubleItemValue("Angular Speed (º/s)", 0) {
            @Override
            public void onValueChanged(double value) {
                LatLng waypoint = (LatLng) comboBoxWaypointNode.getValue();
                WaypointData wpData = waypointData.get(waypoint);
                if (wpData != null) {
                    wpData.setAngularSpeed(value);
                    // update turn
                    setTurnRepresentationOnMap();
                }
            }
        };
        configurableNodes.add(waypointAngularSpeed);

        waypointDelay = new DoubleItemValue("Delay (s)", 0) {
            @Override
            public void onValueChanged(double value) {
                LatLng waypoint = (LatLng) comboBoxWaypointNode.getValue();
                WaypointData wpData = waypointData.get(waypoint);
                if (wpData != null) {
                    wpData.setDelay((long) value);
                }
            }
        };
        configurableNodes.add(waypointDelay);

        comboBoxWaypointNode.valueProperty().addListener(new ChangeListener<LatLng>() {
            @Override
            public void changed(ObservableValue ov, LatLng t, LatLng t1) {
                showCurrentWaypoint(t1);
            }
        });

        // set current waypoint (needed after serialization)
        if (!wayPoints.isEmpty()) {
            comboBoxWaypointNode.setValue(wayPoints.get(0));
        } else {
            showCurrentWaypoint(null);
        }

        configurableNodes.add(new Text("SimulatedTrack Route Simulation"));
        configurableNodes.add(new Separator());

        final DoubleItemValue trackDelayNode = new DoubleItemValue("SimulatedTrack delay (s)", trackDelay) {
            @Override
            public void onValueChanged(double value) {
                trackDelay = value;
            }
        };
        configurableNodes.add(trackDelayNode);

        configurableNodes.add(trackSpeedNode);
        trackSpeedNode.setEditable(false);
        configurableNodes.add(trackAccelNode);
        trackAccelNode.setEditable(false);
        configurableNodes.add(trackHeadingNode);
        trackHeadingNode.setEditable(false);

        // show in turns
        final BooleanItem showTurnsNode = new BooleanItem("Show track turns", showTurns) {
            @Override
            public void onValueChanged(boolean value) {
                showTurns = value;
                if (!value) {
                    removeTurnRepresentationFromMap();
                } else {
                    setOnMap();
                }
            }
        };
        configurableNodes.add(showTurnsNode);
        
        // show in map
        final BooleanItem showSensorDistances = new BooleanItem("Show sensor distances", showTrackSensorRules) {
            @Override
            public void onValueChanged(boolean value) {
                showTrackSensorRules = value;
                if (!value) {
                    removeTrackSensorDistancesFromMap();
                } else {
                    setOnMap();
                }
            }
        };
        configurableNodes.add(showSensorDistances);

        // show in map
        final BooleanItem showTrackDistances = new BooleanItem("Show track distances", showTrackToTrackRuler) {
            @Override
            public void onValueChanged(boolean value) {
                showTrackToTrackRuler = value;
                if (!value) {
                    removeTrackToTrackDistancesFromMap();
                } else {
                    setOnMap();
                }
            }
        };
        configurableNodes.add(showTrackDistances);


        final BooleanItem greatCircleDistance = new BooleanItem("Great-circle path", greatCirclePath) {
            @Override
            public void onValueChanged(boolean value) {
                greatCirclePath = value;
                trackHeading = getWaypointHeading((LatLng) comboBoxWaypointNode.getValue());
                trackHeadingNode.setValueDontNotify(twoDecimals.format(trackHeading));
                setOnMap();
            }
        };

        configurableNodes.add(greatCircleDistance);

        /*
         final Button editPointNode = new Button("Edit waypoints");
         editPointNode.setOnAction(new EventHandler<ActionEvent>() {
         @Override
         public void handle(ActionEvent t) {
         }
         });
         nodes.add(editPointNode);*/

        final ToggleButton simulateNode = new ToggleButton("Simulate Movement");
        simulateNode.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                if (t1) {
                    startSimulation();
                } else {
                    stopSimulation();
                }
            }
        });

        configurableNodes.add(simulateNode);

        return configurableNodes;
    }

    public void addWaypoint(LatLng wayPoint) {
        
        // add start waypoint if list is empty
        if (comboBoxWaypointNode.getItems().isEmpty()) {
            waypointData.put(trackPosition, new WaypointData());
            comboBoxWaypointNode.getItems().add(trackPosition);
        }

        // add current waypoint
        waypointData.put(wayPoint, new WaypointData());
        comboBoxWaypointNode.getItems().add(wayPoint);

        // set current waypoint
        comboBoxWaypointNode.setValue(wayPoint);
    }

    public void removeCurrentWaypoint() {
        if (comboBoxWaypointNode.getValue() != null) {
            // get current waypoint
            LatLng wayPoint = (LatLng) comboBoxWaypointNode.getValue();

            // do not delete starting waypoint
            if (comboBoxWaypointNode.getItems().indexOf(wayPoint) == 0) {
                return;
            }

            // remove waypoint
            comboBoxWaypointNode.getItems().remove(wayPoint);

            // remove waypoint associated data
            waypointData.remove(wayPoint);

            // if only there is one waypoint
            if (wayPoints.size() == 1) {
                comboBoxWaypointNode.getItems().clear();
                waypointData.clear();
            }
        }
    }
    
    public void updateCurrentWayPoint(LatLng newWaypoint) {
        if (comboBoxWaypointNode.getValue() != null) {
            // get current waypoint
            LatLng wayPoint = (LatLng) comboBoxWaypointNode.getValue();

            // get current waypoint index
            int index = comboBoxWaypointNode.getItems().indexOf(wayPoint);

            // update waypoint data for the new point
            waypointData.put(newWaypoint, waypointData.get(wayPoint));

            // update position with new waypoint
            comboBoxWaypointNode.getItems().set(index, newWaypoint);
            comboBoxWaypointNode.setValue(newWaypoint);
            waypointData.remove(wayPoint);
        }
    }

    private void setTrackSensorDistancesOnMap() {
        if(!showTrackSensorRules)
            return;
        
        ObjectRepository repository = WebDataPlayer.getObjectRepository();
        List<ObservableObject> sensors = repository.getObjectsByCategory(Sensor.CATEGORY);
        for (ObservableObject sensor : sensors) {
            Sensor sensorElement = (Sensor) sensor;
            Rule rule = trackSensorRuler.get(sensorElement);
            if (rule == null) {
                rule = new Rule("distance_ " + sensorElement.getObjectId() + "_" + getObjectId());
                rule.setGreatCircle(true);
                trackSensorRuler.put(sensorElement, rule);
            }
            rule.setShowOnMap(false);
            rule.setStartPoint(currentPosition);
            rule.setEndPoint(sensorElement.getSensorPosition());
            rule.setShowOnMap(showOnMap);
            rule.setObjectColor(objectColor);
            rule.setOnMap();
        }
    }

    private void setTrackToTrackDistancesOnMap() {
        if(!showTrackToTrackRuler)
            return;
        
        ObjectRepository repository = WebDataPlayer.getObjectRepository();
        List<ObservableObject> tracks = repository.getObjectsByCategory(SimulatedTrack.CATEGORY);
        for (ObservableObject track : tracks) {
            SimulatedTrack trackElement = (SimulatedTrack) track;
            if (!trackElement.equals(this)) {
                Rule rule = trackToTrackRuler.get(trackElement);
                if (rule == null) {
                    String ruleId = trackElement.getObjectId().compareTo(getObjectId()) >= 0 ? "distance_" + trackElement.getObjectId() + "_" + getObjectId() : "distance_" + getObjectId() + "_" + trackElement.getObjectId();
                    rule = new Rule(ruleId);
                    rule.setGreatCircle(true);
                    trackToTrackRuler.put(trackElement, rule);
                }
                rule.setShowOnMap(false);

                if (trackElement.getObjectId().compareTo(getObjectId()) >= 0) {
                    rule.setStartPoint(trackElement.getCurrentPosition());
                    rule.setEndPoint(currentPosition);
                } else {
                    rule.setStartPoint(currentPosition);
                    rule.setEndPoint(trackElement.getCurrentPosition());
                }

                rule.setShowOnMap(showOnMap);
                rule.setObjectColor(objectColor);
                rule.setOnMap();
            }
        }
    }

    private double getWaypointHeading(LatLng waypoint) {
        int waypointIndex = wayPoints.indexOf(waypoint);
        if (wayPoints.size() > waypointIndex) {
            if (waypointIndex < wayPoints.size() - 1) {
                LatLng startPoint = wayPoints.get(waypointIndex);
                LatLng endPoint = wayPoints.get(waypointIndex + 1);
                if (greatCirclePath) {
                    return Spherical.getInitialBearing(startPoint, endPoint);
                } else {
                    return Spherical.getRhumbBearing(startPoint, endPoint);
                }
            } else if (waypointIndex >= 1) {
                LatLng startPoint = wayPoints.get(waypointIndex - 1);
                LatLng endPoint = wayPoints.get(waypointIndex);
                if (greatCirclePath) {
                    return Spherical.getFinalBearing(startPoint, endPoint);
                } else {
                    return Spherical.getRhumbBearing(startPoint, endPoint);
                }
            }
        }
        return 0;
    }

    private void showCurrentWaypoint(LatLng waypoint) {
        if (waypoint != null) {
            //update the current track position/heading
            currentPosition = waypoint;

            // update the heading
            trackHeading = getWaypointHeading(waypoint);
            trackHeadingNode.setValueDontNotify(twoDecimals.format(trackHeading));

            WaypointData waypointData = this.waypointData.get(waypoint);
            if (waypointData != null) {
                waypointAltitude.setValue(waypoint.alt());
                waypointSpeed.setValueDontNotify(waypointData.getSpeed());
                waypointAltitude.setDisable(false);
                waypointSpeed.setDisable(false);
                waypointAngularSpeed.setValueDontNotify(waypointData.getAngularSpeed());
                waypointAngularSpeed.setDisable(false);
                waypointDelay.setValueDontNotify(waypointData.getDelay());
                waypointDelay.setDisable(false);
            }

        } else {
            //update the current position/heading
            currentPosition = new LatLng(trackPosition);
            trackHeading = 0;
            waypointAltitude.setDisable(true);
            waypointAngularSpeed.setValueDontNotify(0);
            waypointAngularSpeed.setDisable(true);
            waypointSpeed.setValueDontNotify(0);
            waypointSpeed.setDisable(true);
            waypointDelay.setValueDontNotify(0);
            waypointDelay.setDisable(true);
        }

        // show changes on map
        setOnMap();
    }

    private void removeTrackSensorDistancesFromMap() {
        for (Rule rule : trackSensorRuler.values()) {
            rule.removeFromMap();
        }
        trackSensorRuler.clear();
    }

    private void removeTrackToTrackDistancesFromMap() {
        for (Rule rule : trackToTrackRuler.values()) {
            rule.removeFromMap();
        }
        trackToTrackRuler.clear();
    }

    @Override
    public boolean isFused() {
        return false;
    }

    @Override
    public LatLng getPosition() {
        return this.currentPosition;
    }

    
    /*
    public void resetSimulation() {
        // reset simulation variables
        wayPointIndex = 1;
        simulationTime = 0;
        lastAISMessage = 0;
        doingTurn = false;
        waiting = false;
        waitStart = 0;
        waitDuration = 0;
        lastGuiUpdate = 0;

        // reset current position to track position
        currentPosition = new LatLng(trackPosition);

        // set speed/heading to the first waypoint
        if (wayPoints.size() > 1) {
            trackSpeedMs = wayPointsAcelSpeed.get(wayPoints.get(0)).getSpeed();
            if (greatCirclePath) {
                trackHeading = Spherical.getInitialBearing(currentPosition, wayPoints.get(1));
            } else {
                trackHeading = Spherical.getRhumbBearing(currentPosition, wayPoints.get(1));
            }
        }
    }
    */

    
    
    private transient TrackSimulation trackSim = null;
    
    public void resetSimulation() {
        this.trackSim = new TrackSimulation(trackDelay, wayPoints, waypointData);
    }
    public boolean isVisibleInSimulation() {
        return this.trackSim.simulationStarted() && !trackSim.simulationEnded();
    }
    
    private transient long lastGuiUpdate;
    
    public void simulationStep(double simulationStep) {
        // update the simulation
        this.trackSim.simulationStep(simulationStep);

        // update some data used for paint
        this.currentPosition = trackSim.getCurrentPosition();
        this.trackHeading = trackSim.getCurrentHeading();

        // update some values on the interface
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastGuiUpdate > 1000) {
            lastGuiUpdate = currentTime;
            if(trackHeadingNode!=null) trackHeadingNode.setValueDontNotify(twoDecimals.format(trackSim.getCurrentHeading()));
            if(trackSpeedNode!=null) trackSpeedNode.setValueDontNotify(twoDecimals.format(trackSim.getCurrentSpeed()));
            if(trackAccelNode!=null) trackAccelNode.setValueDontNotify(twoDecimals.format(trackSim.getCurrentAcceleration()));
        }
    }

    /*
    public void simulationStep(double simulationStep) {
        // increment the simulation time
        simulationTime += simulationStep;

        // avoid simulation if the simulation time is lower than initial delay
        if (simulationTime < trackDelay * 1000) {
            return;
        }
        
        if (doingTurn) {

            if (waiting) {
                if ((simulationTime - waitStart) >= waitDuration) {
                    doingTurn = false;
                    waiting = false;
                    turnAccum = 0;
                    waitStart = 0;
                    waitDuration = 0;
                    //System.out.println("SimulatedTrack delay end!");
                }
            } else {
                LatLng currentDestPoint = wayPoints.get(wayPointIndex - 1);
                WaypointData waypointData = wayPointsAcelSpeed.get(currentDestPoint);

                if (waypointData.getSpeed() == 0 && waypointData.getDelay() > 0) {
                    waiting = true;
                    waitStart = simulationTime;
                    waitDuration = waypointData.getDelay() * 1000;
                    //System.out.println("SimulatedTrack delay start!");
                } else {
                    doingTurn = false;
                    double turnRadious = trackSpeedMs / waypointData.getAngularSpeed();
                    System.out.println("SimulatedTrack turn ends!");
                    
                    
                    //System.out.println("SimulatedTrack turn ends!");
                    
                     LatLng currentDestPoint = wayPoints.get(wayPointIndex-1);
                     AcelSpeed currentDestAcelSpeed = wayPointsAcelSpeed.get(currentDestPoint);

                     double t = simulationStep/1000.0;
                     double giro = Math.pow(trackSpeedKn*0.514444444, 2)/currentDestAcelSpeed.getAccel();
                     double w = Math.sqrt(currentDestAcelSpeed.getAccel()/giro);
                     double posAct = 0;
                     double posCirc = posAct + w * t + 0.5 * Math.pow(-Math.PI*t,2);
                     
                }
            }
        }

        if (!doingTurn) {

            // check if there is waypoints to follow
            if (wayPointIndex < wayPoints.size()) {
                // get current destination
                LatLng currentDestPoint = wayPoints.get(wayPointIndex);

                // get current destination speed
                WaypointData destinationWaypoint = wayPointsAcelSpeed.get(currentDestPoint);

                // calculate the instant acceleration needed to reach the waypoint at the desired speed
                if (greatCirclePath) {
                    trackSpeedAccelMs2 = (Math.pow(destinationWaypoint.getSpeed(), 2) - Math.pow(trackSpeedMs, 2)) / (2.0 * Spherical.getGreatCircleDistance(currentPosition, currentDestPoint));
                } else {
                    trackSpeedAccelMs2 = (Math.pow(destinationWaypoint.getSpeed(), 2) - Math.pow(trackSpeedMs, 2)) / (2.0 * Spherical.getRhumbDistance(currentPosition, currentDestPoint));
                }

                // calculate the updated track speed based on acceleration, current speed and time
                trackSpeedMs = trackSpeedMs + trackSpeedAccelMs2 * (simulationStep / 1000.0);

                // calculate the travel distance based on acceleration and current speed
                double distance = trackSpeedMs * (simulationStep / 1000.0) + ((trackSpeedAccelMs2 * Math.pow((simulationStep / 1000.0), 2)) / 2.0);

                //System.out.println("Current speed: " + trackSpeedKn + " Acceleration: " + acceleration);

                // calculate the track heading based on current position and destination point
                if (greatCirclePath) {
                    trackHeading = Spherical.getInitialBearing(currentPosition, currentDestPoint);
                } else {
                    trackHeading = Spherical.getRhumbBearing(currentPosition, currentDestPoint);
                }

                // calculate the track current speed based on its current location, the travel distance, and the heading
                if (greatCirclePath) {
                    currentPosition = Spherical.getDestinationPoint(currentPosition, distance, trackHeading);
                } else {
                    currentPosition = Spherical.getRhumbDestinationPoint(currentPosition, distance, trackHeading);
                }

                // update some values on the interface
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastGuiUpdate > 1000) {
                    lastGuiUpdate = currentTime;
                    trackHeadingNode.setValueDontNotify(twoDecimals.format(trackHeading));
                    trackSpeedNode.setValueDontNotify(twoDecimals.format(trackSpeedMs));
                    trackAccelNode.setValueDontNotify(twoDecimals.format(trackSpeedAccelMs2));
                }

                // check if the track should turn
                double heading2 = greatCirclePath ? Spherical.getInitialBearing(currentPosition, currentDestPoint) : Spherical.getRhumbBearing(currentPosition, currentDestPoint);
                double headingDiff = Math.abs(heading2 - trackHeading);

                if (headingDiff > 5.0) {
                    doingTurn = true;
                    //System.out.println("SimulatedTrack " + getObjectId() + " girando...");
                    wayPointIndex += 1;
                }
                // move according to the speed and heading
            } else {
                // calculate the travel distance in the simulation step
                double distance = trackSpeedMs * (simulationStep / 1000.0);
                if (greatCirclePath) {
                    currentPosition = Spherical.getRhumbDestinationPoint(currentPosition, distance, trackHeading);
                } else {
                    currentPosition = Spherical.getRhumbDestinationPoint(currentPosition, distance, trackHeading);
                }
            }
        }
    }*/

    private List<LatLng> getTrackShape() {
        List<LatLng> points = new ArrayList<>();

        double mWidth = trackWidth / 2.0;
        double mHeight = trackLength / 2.0;

        LatLng top = Spherical.getRhumbDestinationPoint(currentPosition, mHeight, 0 + trackHeading);

        LatLng bottom = Spherical.getRhumbDestinationPoint(currentPosition, mHeight, 180 + trackHeading);

        LatLng topLeft = Spherical.getRhumbDestinationPoint(top, mWidth, 270 + trackHeading);
        LatLng topRight = Spherical.getRhumbDestinationPoint(top, mWidth, 90 + trackHeading);
        LatLng headingMarker = Spherical.getRhumbDestinationPoint(currentPosition, mHeight * 2, 0 + trackHeading);

        LatLng bottomLeft = Spherical.getRhumbDestinationPoint(bottom, mWidth, 270 + trackHeading);
        LatLng bottomRight = Spherical.getRhumbDestinationPoint(bottom, mWidth, 90 + trackHeading);

        points.add(topLeft);
        points.add(headingMarker);
        points.add(topRight);

        points.add(bottomRight);
        points.add(bottomLeft);

        return points;
    }

    private String getPath(List<LatLng> points) {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append('[');
        if (points.size() > 0) {
            for (LatLng point : points) {
                strBuilder.append('[');
                strBuilder.append(point.lat());
                strBuilder.append(',');
                strBuilder.append(point.lon());
                strBuilder.append("],");
            }
            strBuilder.deleteCharAt(strBuilder.length() - 1);
        }
        strBuilder.append(']');
        return strBuilder.toString();
    }

    @Override
    public boolean setOnMap() {
        if (trackPosition.isValid()) {
            if (super.setOnMap()) {
                setPathOnMap();
                
                if (showTrackSensorRules) {
                    setTrackSensorDistancesOnMap();
                }
                
                if (showTrackToTrackRuler) {
                    setTrackToTrackDistancesOnMap();
                }
                
                if(showTurns){
                    setTurnRepresentationOnMap();
                }
                
                return true;
            }
        }
        return false;
    }

    @Override
    public void removeFromMap() {
        super.removeFromMap();
        
        clearPathFromMap();
        
        if (showTrackSensorRules) {
            removeTrackSensorDistancesFromMap();
        }

        if (showTrackToTrackRuler) {
            removeTrackToTrackDistancesFromMap();
        }
        
        if(showTurns)
        {
            removeTurnRepresentationFromMap();
        }
    }

    @Override
    public Map<String, String> getMapRepresentation() {
        Map<String, String> track = new HashMap<>();
        track.put("object_id", getObjectId());
        track.put("type", "object");
        track.put("object_type", "poly");
        track.put("poly_center", "[" + currentPosition.lat() + "," + currentPosition.lon() + "]");
        if (objectColor != null) {
            track.put("object_color", objectColor);
        }
        track.put("poly_path", getPath(getTrackShape()));
        return track;
    }

    public Map<String, String> getPathRepresentation() {
        Map<String, String> path = new HashMap<>();
        path.put("object_id", '_' + getObjectId());
        path.put("type", "object");
        path.put("object_type", greatCirclePath ? "geodesic_line" : "line");
        if (objectColor != null) {
            path.put("object_color", objectColor);
        }
        path.put("line_path", getPath(wayPoints));
        return path;
    }
    
    public void setTurnRepresentationOnMap() {
        if(!showTurns){return;}
            
        for(int i=1; i<wayPoints.size()-1;i++)
        {
            LatLng startPoint = wayPoints.get(i-1);
            LatLng turnPoint = wayPoints.get(i);
            LatLng endpoint = wayPoints.get(i+1);
            
            WaypointData turnData = waypointData.get(turnPoint);
            double normalAccel = turnData.getSpeed() * Math.toRadians(turnData.getAngularSpeed());
            double turnRadius = normalAccel == 0 ? 0 : Math.pow(turnData.getSpeed(), 2) / normalAccel;
            
            TurnPoints turnPoints = Spherical.calculateTurnPoints(startPoint, turnPoint, endpoint, turnRadius);
            if(turnPoints!=null)
            {
                final Map<String, String> circle1 = getCircle("_" + getObjectId() + "_turnCenter_" + i, turnPoints.getTurnCenter(), turnRadius);
                final Map<String, String> circle2 = getCircle("_" + getObjectId() + "_turnStart_" + i, turnPoints.getTurnStart(), 5);
                final Map<String, String> circle3 = getCircle("_" + getObjectId() + "_turnEnd_" + i, turnPoints.getTurnEnd(), 5);
                
                if (!Platform.isFxApplicationThread()) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        gMap.setObject(circle1);
                        gMap.setObject(circle2);
                        gMap.setObject(circle3);
                    }
                });
                } else {
                        gMap.setObject(circle1);
                        gMap.setObject(circle2);
                        gMap.setObject(circle3);
                }
            }
        }
    }
    
    private Map<String, String> getCircle(String id, LatLng point, double radius){
        Map<String, String> values = new HashMap<>();
        values.put("object_id", id);
        values.put("type", "object");
        if (objectColor != null) {
            values.put("object_color", objectColor);
        }
        values.put("object_type", "circle");
        values.put("circle_lat", String.valueOf(point.lat()));
        values.put("circle_lon", String.valueOf(point.lon()));
        values.put("circle_radius", String.valueOf(radius));
        return values;
    }
    
    public void removeTurnRepresentationFromMap() {
        for(int i=1; i<wayPoints.size()-1;i++)
        {
            if (!Platform.isFxApplicationThread()) {
                final int index = i;
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        gMap.removeObject("_" + getObjectId() + "_turnCenter_" + index);
                        gMap.removeObject("_" + getObjectId() + "_turnStart_" + index);
                        gMap.removeObject("_" + getObjectId() + "_turnEnd_" + index);
                    }
                });
            } else {
                gMap.removeObject("_" + getObjectId() + "_turnCenter_" + i);
                gMap.removeObject("_" + getObjectId() + "_turnStart_" + i);
                gMap.removeObject("_" + getObjectId() + "_turnEnd_" + i);
            }

        }
    }

    protected void setPathOnMap() {
        if (wayPoints.size() > 0) {
            final Map<String, String> pathRepresentation = getPathRepresentation();
            
            //TODO DEBUG REMOVE
            //final Map<String, String> debugRepresentation = getParallelRepresentation();
            
            if (!Platform.isFxApplicationThread()) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        gMap.setObject(pathRepresentation);
                        //gMap.setObject(debugRepresentation);
                    }
                });
            } else {
                gMap.setObject(pathRepresentation);
                //gMap.setObject(debugRepresentation);
            }
        }
    }

    protected void clearPathFromMap() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    gMap.removeObject('_' + getObjectId());
                }
            });
        } else {
            gMap.removeObject('_' + getObjectId());
        }

    }

    /**
     * Calculates the AIS Message frequency depeding on the track speed in knots
     * 0kn -> 1 message every 3 miniutes >0kn to 60 kn, fom 10 to 2 seconds
     */
    private double getAISRefreshPeriod(double trackSpeed) {
        if (trackSpeed == 0) {
            return 3 * 60 * 1000;
        } else {
            return Math.max(2.0, 10 - (trackSpeed / (60 / 8))) * 1000;
        }
    }

    public AISMessage getAISMessage() {
        if (AISTransponder
                && (lastAISMessage == trackSim.getSimulationDuration()
                || lastAISMessage == 0
                || (trackSim.getSimulationDuration() - lastAISMessage) > getAISRefreshPeriod(trackSim.getCurrentSpeed() * 1.94384449))) {
            lastAISMessage = trackSim.getSimulationDuration();
            System.out.println("Sending ais message");
            return new AISMessage(trackSim.getSimulationDuration(), currentPosition, AISMMSI, "", trackSim.getCurrentSpeed(), trackHeading);
        }
        return null;
    }

    @Override
    public double getSpeed()
    {
        return trackSim!=null ? trackSim.getCurrentSpeed() : 0.0;
    }

    @Override
    public String getSource() {
        return "Simulation";
    }

    /* LOCAL MOVEMENT SIMULATION */
    public void startSimulation() {
        simulatorThread = new SimulatorThread(10);
        simulatorThread.start();
    }

    public void stopSimulation() {
        if (simulatorThread != null) {
            simulatorThread.setRunning(false);
            try {
                simulatorThread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
            simulatorThread = null;
        }
    }

    @Override
    public void onEventReceived(Event event) {

    }

    private class SimulatorThread extends Thread {

        private boolean running;
        private final double simulationStep;
        private long lastGUIUpdate;

        public SimulatorThread(double simulationStep) {
            this.running = true;
            this.simulationStep = simulationStep;
        }

        public void setRunning(boolean value) {
            this.running = value;
        }

        @Override
        public void run() {
            resetSimulation();
            while (running) {
                simulationStep(simulationStep);
                final long currentTime = System.currentTimeMillis();
                boolean showInGUI = (currentTime - lastGUIUpdate) > 100;
                if (showInGUI) {
                    lastGUIUpdate = currentTime;
                    setOnMap();
                }
                try {
                    Thread.sleep((long) simulationStep);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void addKMLWaypointsPlaceMark(Document doc, Element document) {
        Element placeMark = doc.createElement("Placemark");
        placeMark.setAttribute("id", getObjectId());
        document.appendChild(placeMark);

        // add the name (the track id)
        Element name = doc.createElement("name");
        name.appendChild(doc.createTextNode(objectTag != null ? objectTag : getObjectId()));
        placeMark.appendChild(name);

        // add color
        Element style = doc.createElement("Style");
        Element lineStyle = doc.createElement("LineStyle");
        style.appendChild(lineStyle);
        Element color = doc.createElement("color");
        StringBuilder kmlColor = new StringBuilder();
        //#rrggbb -> aabbggrr
        kmlColor.append("ff");
        if(objectColor!=null)
        {
            kmlColor.append(objectColor.substring(5));
            kmlColor.append(objectColor.substring(3, 5));
            kmlColor.append(objectColor.substring(1, 3));
        }else{
            kmlColor.append("000000");
        }
        color.appendChild(doc.createTextNode(kmlColor.toString()));
        lineStyle.appendChild(color);
        placeMark.appendChild(style);

        Element visibility = doc.createElement("visibility");
        visibility.setTextContent(showOnMap ? "1" : "0");
        placeMark.appendChild(visibility);

        Element lineString = doc.createElement("LineString");
        placeMark.appendChild(lineString);

        Element coordinates = doc.createElement("coordinates");
        lineString.appendChild(coordinates);

        StringBuilder coordStr = new StringBuilder();
        for (LatLng wayPoint : wayPoints) {
            coordStr.append(wayPoint.lon());
            coordStr.append(',');
            coordStr.append(wayPoint.lat());
            coordStr.append(',');
            coordStr.append(wayPoint.alt());
            coordStr.append(' ');
        }
        coordinates.appendChild(doc.createTextNode(coordStr.toString()));

        Element extendedData = doc.createElement("ExtendedData");
        placeMark.appendChild(extendedData);

        Element speedValues = doc.createElement("WaypointSpeed");
        extendedData.appendChild(speedValues);

        Element angularSpeedValues = doc.createElement("WaypointAngularSpeed");
        extendedData.appendChild(angularSpeedValues);
        
        Element accelValues = doc.createElement("WaypointNormalAcceleration");
        extendedData.appendChild(accelValues);

        Element delayValues = doc.createElement("WaypointDelay");
        extendedData.appendChild(delayValues);

        StringBuilder speedValueStr = new StringBuilder();
        StringBuilder angularSpeedStr = new StringBuilder();
        StringBuilder accelValueStr = new StringBuilder();
        StringBuilder delayValuesStr = new StringBuilder();

        for (LatLng wayPoint : wayPoints) {
            WaypointData value = waypointData.get(wayPoint);
            speedValueStr.append(value.getSpeed());
            speedValueStr.append(", ");

            // save the angular speed
            angularSpeedStr.append(value.getAngularSpeed());
            angularSpeedStr.append(", ");
            
            // save the normal acceleration for simulation processes
            accelValueStr.append(Math.toRadians(value.getAngularSpeed()) * value.getSpeed());
            accelValueStr.append(", ");

            delayValuesStr.append(value.getDelay());
            delayValuesStr.append(", ");
        }

        speedValueStr.delete(speedValueStr.length() - 2, speedValueStr.length());
        angularSpeedStr.delete(angularSpeedStr.length() - 2, angularSpeedStr.length());
        accelValueStr.delete(accelValueStr.length() - 2, accelValueStr.length());
        delayValuesStr.delete(delayValuesStr.length() - 2, delayValuesStr.length());

        speedValues.appendChild(doc.createTextNode(speedValueStr.toString()));
        angularSpeedValues.appendChild(doc.createTextNode(angularSpeedStr.toString()));
        accelValues.appendChild(doc.createTextNode(accelValueStr.toString()));
        delayValues.appendChild(doc.createTextNode(delayValuesStr.toString()));

        Element trackDelay = doc.createElement("TrackDelay");
        extendedData.appendChild(trackDelay);
        trackDelay.appendChild(doc.createTextNode(String.valueOf(this.trackDelay)));

        Element trackWidth = doc.createElement("TrackWidth");
        extendedData.appendChild(trackWidth);
        trackWidth.appendChild(doc.createTextNode(String.valueOf(this.trackWidth)));

        Element trackLength = doc.createElement("TrackLength");
        extendedData.appendChild(trackLength);
        trackLength.appendChild(doc.createTextNode(String.valueOf(this.trackLength)));
        
        Element trackHeight = doc.createElement("TrackHeight");
        extendedData.appendChild(trackHeight);
        trackHeight.appendChild(doc.createTextNode(String.valueOf(this.trackHeight)));

        if (AISTransponder) {
            Element trackMMSI = doc.createElement("TrackMMSI");
            extendedData.appendChild(trackMMSI);
            trackMMSI.appendChild(doc.createTextNode(String.valueOf(AISMMSI)));
            Element trackDifferential = doc.createElement("TrackDifferential");
            extendedData.appendChild(trackDifferential);
            trackDifferential.appendChild(doc.createTextNode(AISDifferential ? "1" : "0"));
        }
    }

    public LatLng getTrackPosition() {
        return this.trackPosition;
    }

    public LatLng getCurrentPosition() {
        return this.currentPosition;
    }

    public static SimulatedTrack buildTrackFromXMLNode(Element node) {
        SimulatedTrack track = null;

        String name = XmlUtils.getText(XmlUtils.findChild(node, "name"));
        String id = node.getAttribute("id");
        String color = XmlUtils.getText(XmlUtils.findChildByChain(node, new String[]{"Style", "LineStyle", "color"}));
        String visible = XmlUtils.getText(XmlUtils.findChild(node, "visibility"));
        String waypoints = XmlUtils.getText(XmlUtils.findChildByChain(node, new String[]{"LineString", "coordinates"}));

        if(waypoints==null || "".equals(waypoints))
        {
            return track;
        }
        
        track = new SimulatedTrack(id);
        track.objectTag = name;

        if (color != null) {
            StringBuilder kmlColor = new StringBuilder();
            //#rrggbb <- aabbggrr
            kmlColor.append('#');
            kmlColor.append(color.substring(6));
            kmlColor.append(color.substring(4, 6));
            kmlColor.append(color.substring(2, 4));
            track.objectColor = kmlColor.toString();
        }

        track.showOnMap = false;

        // read track delay
        track.trackDelay = XmlUtils.getDouble(XmlUtils.findChildByChain(node, new String[]{"ExtendedData", "TrackDelay"}));

        // read track width
        track.trackWidth = XmlUtils.getDouble(XmlUtils.findChildByChain(node, new String[]{"ExtendedData", "TrackWidth"}));

        // read track lenght
        track.trackLength = XmlUtils.getDouble(XmlUtils.findChildByChain(node, new String[]{"ExtendedData", "TrackLength"}));
        
        // read track lenght
        track.trackHeight = XmlUtils.getDouble(XmlUtils.findChildByChain(node, new String[]{"ExtendedData", "TrackHeight"}), 0);

        // read track mmsi
        String mmsi = XmlUtils.getText(XmlUtils.findChildByChain(node, new String[]{"ExtendedData", "TrackMMSI"}));
        if (mmsi != null) {
            track.AISTransponder = true;
            track.AISMMSI = Long.parseLong(mmsi);
            track.AISDifferential = XmlUtils.getDouble(XmlUtils.findChildByChain(node, new String[]{"ExtendedData", "TrackDifferential"}), 0) == 1 ? true : false;
        } else {
            track.AISTransponder = false;
            track.AISDifferential = false;
        }

        // parse waypoints
        if (waypoints != null) {
            String speed = XmlUtils.getText(XmlUtils.findChildByChain(node, new String[]{"ExtendedData", "WaypointSpeed"}));
            String angularSpeed = XmlUtils.getText(XmlUtils.findChildByChain(node, new String[]{"ExtendedData", "WaypointAngularSpeed"}));
            String normalAcceleration = XmlUtils.getText(XmlUtils.findChildByChain(node, new String[]{"ExtendedData", "WaypointNormalAcceleration"}));
            String delay = XmlUtils.getText(XmlUtils.findChildByChain(node, new String[]{"ExtendedData", "WaypointDelay"}));

            boolean firstWaypoint = true;

            Pattern doublePattern = Pattern.compile("([-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)");

            Matcher waypointMatcher = doublePattern.matcher(waypoints);
            Matcher speedMatcherMatcher = doublePattern.matcher(speed != null ? speed : "");
            Matcher angularSpeedMatcher = doublePattern.matcher(angularSpeed != null ? angularSpeed : "");
            Matcher accelMatcher = doublePattern.matcher(normalAcceleration !=null ? normalAcceleration : "");
            Matcher delayMatcher = doublePattern.matcher(delay !=null ? delay : "");

            while (waypointMatcher.find()) {
                // in kml the longitude is before latitude
                double longitude = Double.parseDouble(waypointMatcher.group(1));
                waypointMatcher.find();
                double latitude = Double.parseDouble(waypointMatcher.group(1));
                waypointMatcher.find();
                double altitude = Double.parseDouble(waypointMatcher.group(1));
                
                double speedValue = speedMatcherMatcher.find() ? Double.parseDouble(speedMatcherMatcher.group(1)) : 0;
                double angularSpeedValue = angularSpeedMatcher.find() ? Double.parseDouble(angularSpeedMatcher.group(1)) : 0;
                double normalAccelerationValue = accelMatcher.find() ? Double.parseDouble(accelMatcher.group(1)) : 0;
                double delayValue = delayMatcher.find() ? Double.parseDouble(delayMatcher.group(1)) : 0;

                LatLng waypoint = new LatLng(latitude, longitude, altitude);
                track.wayPoints.add(waypoint);

                if (firstWaypoint) {
                    firstWaypoint = false;
                    track.trackPosition = waypoint;
                }

                WaypointData data = new WaypointData(angularSpeedValue, speedValue, (long) delayValue);
                track.waypointData.put(waypoint, data);
            }
            
        }

        track.showOnMap = "1".equals(visible);

        return track;
    }
}
