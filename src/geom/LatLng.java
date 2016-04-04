/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package geom;

import java.io.Serializable;

/**
 *
 * @author alvarolb
 */
public class LatLng implements Serializable{
    private double lat = 91;
    private double lng = 181;
    private double altitude = 0;
    
    public LatLng(){
    }

    public LatLng(LatLng latlon){
        this.lat = latlon.lat;
        this.lng = latlon.lng;
        this.altitude = latlon.altitude;
    }
    public LatLng(double lat, double lng){
        this.lat = lat;
        this.lng = lng;
        this.altitude = 0;
    }
    public LatLng(double lat, double lng, double altitude){
        this.lat = lat;
        this.lng = lng;
        this.altitude = altitude;
    }
    
    public double lat(){
        return this.lat;
    }

    public double lon(){
        return this.lng;
    }

    public double alt() { return this.altitude; }
    
    public void setLat(double lat){
        this.lat = lat;
    }
    
    public void setLon(double lon){
        this.lng = lon;
    }

    public void setAltitude(double altitude) {this.altitude = altitude; }
    
    public boolean isValid(){
        return lat>=-90 && lat<=90 && lng>=-180 && lng<=180;
    }
    
    @Override
    public String toString(){
        return  String.format("%.4f %.4f", lat,  lng);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        return hash;
    }

}
