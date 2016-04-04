/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package data.simulation;

import java.io.Serializable;

/**
 *
 * @author Alvaro
 */
public class WaypointData implements Serializable {

    private double angularSpeed; // angular speed º/s
    private double speed; // speed in m/s
    private long delay; // delay in milliseconds

    public WaypointData(double angularSpeed, double speed, long delay) {
        this.angularSpeed = angularSpeed;
        this.speed = speed;
        this.delay = delay;
    }
    
    public WaypointData() {
        this.angularSpeed = 0;
        this.speed = 0;
        this.delay = 0;
    }

    public double getAngularSpeed() {
        return angularSpeed;
    }

    public void setAngularSpeed(double accel) {
        this.angularSpeed = accel;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }
}