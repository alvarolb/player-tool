/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package geom;

/**
 *
 * @author Alvaro
 */
public class TurnPoints {

    LatLng turnCenter; // turn center
    LatLng turnStart; // turn start point
    LatLng turnEnd; // turn end point
    
    double turnAngle; // total turn angle
    
    double turnStartDistance; // start distance from turn
    double turnEndDistance; // end distance from turn

    public TurnPoints(LatLng turnCenter, LatLng turnStart, LatLng turnEnd) {
        this.turnCenter = turnCenter;
        this.turnStart = turnStart;
        this.turnEnd = turnEnd;
    }

    public LatLng getTurnCenter() {
        return turnCenter;
    }

    public void setTurnCenter(LatLng turnCenter) {
        this.turnCenter = turnCenter;
    }

    public LatLng getTurnStart() {
        return turnStart;
    }

    public void setTurnStart(LatLng turnStart) {
        this.turnStart = turnStart;
    }

    public LatLng getTurnEnd() {
        return turnEnd;
    }

    public void setTurnEnd(LatLng turnEnd) {
        this.turnEnd = turnEnd;
    }

    public double getTurnAngle() {
        return turnAngle;
    }

    public void setTurnAngle(double turnAngle) {
        this.turnAngle = turnAngle;
    }

    public double getTurnStartDistance() {
        return turnStartDistance;
    }

    public void setTurnStartDistance(double turnStartDistance) {
        this.turnStartDistance = turnStartDistance;
    }

    public double getTurnEndDistance() {
        return turnEndDistance;
    }

    public void setTurnEndDistance(double turnEndDistance) {
        this.turnEndDistance = turnEndDistance;
    }

    @Override
    public String toString() {
        return "TurnPoints{" + "turnCenter=" + turnCenter + ", turnStart=" + turnStart + ", turnEnd=" + turnEnd + ", turnAngle=" + turnAngle + ", turnStartDistance=" + turnStartDistance + ", turnEndDistance=" + turnEndDistance + '}';
    }
}