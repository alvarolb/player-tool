package data;

import data.simulation.events.Event;
import geom.LatLng;
import gui.map.GoogleMapCallback;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author alvarolb
 */
public abstract class ConfigurableObservableObject extends ObservableObject{

    public ConfigurableObservableObject(String objectId, String objectCategory) {
        super(objectId, objectCategory);
    }

    public ConfigurableObservableObject(String objectId, String objectCategory, boolean visible) {
        super(objectId, objectCategory, visible);
    }
    
    public abstract class StringItem extends GridPane {

        private final Text text;
        private final TextField textField;

        public abstract void onTextChanged(String text);

        public StringItem(String title, String currentValue) {
            this.text = new Text(title);
            this.textField = new TextField(currentValue);

            this.textField.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> ov, String t, String t1) {
                    onTextChanged(t1);
                }
            });
            
            add(textField, 0, 0);
            add(text, 1, 0);

            setHgap(5);
        }

        public void setText(String text) {
            textField.setText(text);
        }
    }
    
    public abstract class ColorItem extends GridPane {

        private final Text text;
        private final ColorPicker colorPiker;

        public abstract void onColorChanged(String color);

        public ColorItem(String title, String currentValue) {
            this.text = new Text(title);
            this.colorPiker = new ColorPicker();
            if(currentValue!=null && !"".equals(currentValue)){
                colorPiker.valueProperty().setValue(Color.web(currentValue));
            }else{
                colorPiker.valueProperty().setValue(Color.BLACK);
            }

            colorPiker.valueProperty().addListener(new ChangeListener<Color>(){
                @Override
                public void changed(ObservableValue<? extends Color> ov, Color t, Color t1) {
                    String color = "#" + t1.toString().subSequence(2, 8); //0xrrggbbaa
                    //System.out.println(color);
                    onColorChanged(color);
                }
            });
            
            add(colorPiker, 0, 0);
            add(text, 1, 0);

            setHgap(5);
        }

        public void setColor(String color) {
            colorPiker.valueProperty().setValue(Color.web(color));
        }
    }
    
    public abstract class EditPositionItem extends ToggleButton implements GoogleMapCallback
    {
        public EditPositionItem()
        {        
            selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                    if (t1) {
                        gMap.addCallback(EditPositionItem.this);
                    } else {
                        gMap.removeCallback(EditPositionItem.this);
                    }
                }
            });
        }
    }

    public abstract class PositionItem extends GridPane {
        private final LatLng position;
        private final DoubleItemValue latitude;
        private final DoubleItemValue longitude;
        private final ToggleButton togglePosition = new ToggleButton("Set from map");
        private final GoogleMapCallback positionCallback;

        public abstract void onLocationChanged(LatLng latlon);

        public PositionItem(LatLng value) {
            this.position = value;
            latitude = new DoubleItemValue("Latitude (º)", value.lat()) {
                @Override
                public void onValueChanged(double value) {
                    position.setLat(value);
                    onLocationChanged(position);
                }
            };
            
            add(latitude, 0 ,0);

            longitude = new DoubleItemValue("Longitude (º)", value.lon()) {
                @Override
                public void onValueChanged(double value) {
                    position.setLon(value);
                    onLocationChanged(position);
                }
            };
            
            add(longitude, 0, 1);

            positionCallback = new GoogleMapCallback() {
                @Override
                public void onLocationSelected(double lat, double lon) {
                    latitude.setValue(lat);
                    longitude.setValue(lon);
                    togglePosition.selectedProperty().setValue(false);
                }
            };

            togglePosition.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                    if (t1) {
                        gMap.addCallback(positionCallback);
                    } else {
                        gMap.removeCallback(positionCallback);
                    }
                }
            });
            
            add(togglePosition, 0, 2);
            
            setVgap(5);
        }

        public void setLatitude(double lat) {
            latitude.setValue(lat);
        }

        public void setLongitude(double lon) {
            longitude.setValue(lon);
        }
        
        public void setSelected(boolean value)
        {
            togglePosition.setSelected(false);
        }
        
        public boolean isSelected(){
            return togglePosition.isSelected();
        }
    }
    
    public abstract class DoubleItemValue extends GridPane {

        private final Text text;
        private final TextField textField;
        private final ChangeListener<String> listener;

        public abstract void onValueChanged(double value);

        public DoubleItemValue(String title, double currentValue) {
            this.text = new Text(title);
            this.textField = new TextField(String.valueOf(currentValue));
            
            this.listener = new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> ov, String t, String t1) {
                    try {
                        if (!"".equals(t1)) {
                            double value = Double.parseDouble(t1);
                            onValueChanged(value);
                        } else {
                            onValueChanged(0);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        setValueDontNotify(t);
                    }
                }
            };

            this.textField.textProperty().addListener(this.listener);

            add(textField, 0, 0);
            add(text, 1, 0);
            
            setHgap(5);
        }

        public void setValue(double value) {
            textField.setText(String.valueOf(value));
        }
        
        public void setValueDontNotify(double value) {
            this.textField.textProperty().removeListener(this.listener);
            textField.setText(String.valueOf(value));
            this.textField.textProperty().addListener(this.listener);
        }
        
        public void setValueDontNotify(String value) {
            this.textField.textProperty().removeListener(this.listener);
            textField.setText(value);
            this.textField.textProperty().addListener(this.listener);
        }

        public double getValue() {
            return Double.parseDouble(textField.getText());
        }
        
        public void setEditable(boolean value){
            this.textField.setEditable(value);
        }
    }
    
    public abstract class LongItemValue extends GridPane {

        private final Text text;
        private final TextField textField;
        private final ChangeListener<String> listener;

        public abstract void onValueChanged(long value);

        public LongItemValue(String title, long currentValue) {
            this.text = new Text(title);
            this.textField = new TextField(String.valueOf(currentValue));
            
            this.listener = new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> ov, String t, String t1) {
                    try {
                        if (!"".equals(t1)) {
                            long value = Long.parseLong(t1);
                            onValueChanged(value);
                        } else {
                            onValueChanged(0);
                        }
                    } catch (Exception e) {
                        setValueDontNotify(t);
                    }
                }
            };

            this.textField.textProperty().addListener(this.listener);

            add(textField, 0, 0);
            add(text, 1, 0);
            
            setHgap(5);
        }

        public void setValue(long value) {
            textField.setText(String.valueOf(value));
        }
        
        public void setValueDontNotify(long value) {
            this.textField.textProperty().removeListener(this.listener);
            textField.setText(String.valueOf(value));
            this.textField.textProperty().addListener(this.listener);
        }
        
        public void setValueDontNotify(String value) {
            this.textField.textProperty().removeListener(this.listener);
            textField.setText(value);
            this.textField.textProperty().addListener(this.listener);
        }

        public double getValue() {
            return Long.parseLong(textField.getText());
        }
    }


    public abstract class BooleanItem extends GridPane {
        private final CheckBox checkBox;

        public abstract void onValueChanged(boolean value);

        public BooleanItem(String title, boolean currentValue) {
            this.checkBox = new CheckBox();
            this.checkBox.setSelected(currentValue);
            this.checkBox.setText(title);

            this.checkBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                    onValueChanged(t1);
                }
            });

            add(checkBox, 0, 0);
        }

        public void setValue(boolean value) {
            this.checkBox.setSelected(value);
        }
    }

    public class EventTable extends GridPane {
        private final TableView<Event> tableView;
        private ObservableList<Event> observableList;

        public EventTable() {
            this.tableView = new TableView<>();
            this.observableList = FXCollections.observableArrayList();
            this.tableView.getColumns().addAll(Event.getColumns());
            this.tableView.setItems(observableList);
            add(tableView, 0, 0);
        }

        public void addEvent(Event event)
        {
            observableList.add(0, event);
            if(observableList.size()>10){
                observableList.remove(observableList.size()-1);
            }
        }

        public void clear()
        {
            observableList.clear();
        }
    }

    public Node getConfigurationNode() {
        VBox container = new VBox();
        List<Node> childrenConfiguration = getConfigurableNodes();
        if(childrenConfiguration!=null){
            container.getChildren().addAll(childrenConfiguration);
        }
        container.getChildren().addAll(getObservableConfiguration());
        for (Node node : container.getChildren()) {
            VBox.setMargin(node, new Insets(3));
        }
        ScrollPane pane = new ScrollPane();
        pane.setContent(container);

        return pane;
    }
    
    public List<Node> getObservableConfiguration(){
        List<Node> nodes = new ArrayList<>();
     
        nodes.add(new Text("Display options"));
        nodes.add(new Separator());
        
                
        // show in map
        final BooleanItem showInMapNode = new BooleanItem("Show on map", showOnMap){
            @Override
            public void onValueChanged(boolean value) {
                showOnMap = value;
                if(value){
                    setOnMap();
                }else{
                    removeFromMap();
                }
            }
        };
        nodes.add(showInMapNode);
        
        // change color
        final ColorItem objectColorNode = new ColorItem("Color", objectColor){
            @Override
            public void onColorChanged(String color) {
                objectColor = color;
                setOnMap();
            }

        };
        nodes.add(objectColorNode);
        
        final StringItem objectTagNode = new StringItem("Tag", objectTag){

            @Override
            public void onTextChanged(String text) {
                objectTag = text.isEmpty() ? null : text;
                setOnMap();
            }
            
        };
        nodes.add(objectTagNode);
        
        final LongItemValue objectPathNode = new LongItemValue("Object Path", objectPath)
        {

            @Override
            public void onValueChanged(long value) {
                objectPath = value;
                setOnMap();
            }
            
        };
        nodes.add(objectPathNode);
        
        final LongItemValue objectHistoryNode = new LongItemValue("Object History", objectHistory)
        {

            @Override
            public void onValueChanged(long value) {
                objectHistory = value;
                setOnMap();
            }
            
        };
        nodes.add(objectHistoryNode);
        
        
        
        return nodes;
    }   
    
    protected abstract List<Node> getConfigurableNodes();
}
