/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package data.items;

import data.ConfigurableObservableObject;
import data.simulation.events.Event;
import geom.LatLng;
import geom.Spherical;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.text.Text;

/**
 *
 * @author alvarolb
 */
public class Rule extends ConfigurableObservableObject implements Serializable {

    public static final String CATEGORY = "Rule";
    private LatLng startPoint;
    private LatLng endPoint;
    private boolean greatCircle;
    private static DecimalFormat twoDecimals = new DecimalFormat("#.##");

    public Rule(String ruleId) {
        super(ruleId, CATEGORY);
        this.startPoint = new LatLng();
        this.endPoint = new LatLng();
    }

    private static String formatDist(double meters) {
        if (meters < 1000) {
            return twoDecimals.format(meters) + "m";
        } else {
            return twoDecimals.format(meters/1000.0) + "km";
        } 
    }

    @Override
    protected List<Node> getConfigurableNodes() {
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Text("Rule positions"));
        nodes.add(new Separator());

        nodes.add(new Text("Start point"));
        final PositionItem startPointNode = new PositionItem(startPoint) {
            @Override
            public void onLocationChanged(LatLng latlon) {
                setStartPoint(latlon);
            }
        };
        nodes.add(startPointNode);

        nodes.add(new Text("End point"));
        final PositionItem endPointNode = new PositionItem(endPoint) {
            @Override
            public void onLocationChanged(LatLng latlon) {
                setEndPoint(latlon);
            }
        };
        nodes.add(endPointNode);
        
        nodes.add(new Text("Rule options"));
        nodes.add(new Separator());
        
        final BooleanItem greatCircleDistance  = new BooleanItem("Great-circle distance", greatCircle) {
            @Override
            public void onValueChanged(boolean value) {
                greatCircle = value;
                setOnMap();
            }
        };
        
        nodes.add(greatCircleDistance);
        
        return nodes;
    }

    @Override
    public boolean setOnMap() {
        if (startPoint.isValid() && endPoint.isValid()) {
            if(greatCircle){
                objectTag = formatDist(Spherical.getGreatCircleDistance(startPoint, endPoint));
            }else{
                objectTag = formatDist(Spherical.getRhumbDistance(startPoint, endPoint));   
            }
            return super.setOnMap();
        }
        return false;
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
    public Map<String, String> getMapRepresentation() {
        ArrayList<LatLng> points = new ArrayList<>();
        points.add(startPoint);
        points.add(endPoint);
        Map<String, String> path = new HashMap<>();
        path.put("object_id", getObjectId());
        path.put("type", "object");
        path.put("object_type", greatCircle ?  "geodesic_line" : "line");
        path.put("line_path", getPath(points));
        return path;
    }

    public void setStartPoint(LatLng startPoint) {
        if (startPoint != null) {
            this.startPoint = startPoint;
            setOnMap();
        }
    }

    public void setEndPoint(LatLng endPoint) {
        if (endPoint != null) {
            this.endPoint = endPoint;
            setOnMap();
        }
    }

    public void setGreatCircle(boolean greatCircle) {
        this.greatCircle = greatCircle;
    }

    @Override
    public void onEventReceived(Event event) {

    }
}
