/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package data.simulation;

import geom.LatLng;

/**
 *
 * @author alvarolb
 */
public class AISMessage {
    private double timestamp;
    private LatLng latLong;
    private long MMSI;
    private String name;
    private double SOG;
    private double COG;

    public AISMessage(double timestamp, LatLng latLong, long MMSI, String name, double SOG, double COG) {
        this.timestamp = timestamp;
        this.latLong = latLong;
        this.MMSI = MMSI;
        this.name = name;
        this.SOG = SOG;
        this.COG = COG;
    }
    
    public double getTimestamp(){
        return timestamp;
    }

    public LatLng getPosition() {
        return latLong;
    }

    public long getMMSI() {
        return MMSI;
    }

    public String getName() {
        return name;
    }

    public double getSOG() {
        return SOG;
    }

    public double getCOG() {
        return COG;
    }
    
    @Override
    public String toString(){
        return "Timestamp(s)[" + (float)timestamp/1000 + "] Lat/Lon(º)[" + latLong.lat() + ", " + latLong.lon() + "] MMSI[" + MMSI + "] SOG(kt)[" + SOG + "] COG(º)[" + COG + "]"; 
    }
    
    
}
