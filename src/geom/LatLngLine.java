/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package geom;

/**
 *
 * @author Alvaro
 */
public class LatLngLine {
    private LatLng startPoint;
    private LatLng endPoint;

    public LatLngLine(LatLng startPoint, LatLng endPoint) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }
    public LatLng getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(LatLng startPoint) {
        this.startPoint = startPoint;
    }

    public LatLng getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(LatLng endPoint) {
        this.endPoint = endPoint;
    }
    
    
}
