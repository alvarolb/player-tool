/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package data.simulation.track;

import data.simulation.WaypointData;
import geom.LatLng;
import geom.Spherical;
import geom.TurnPoints;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author alvarolb
 */
public class TrackSimulation {

    // simulation
    private double simulationTime;
    // track 
    private LatLng currentPosition;
    private double currentSpeed;
    private double currentAcceleration;
    private double currentHeading = 0;
    // track travel 
    private LatLng startWaypoint;
    private LatLng destinationWaypoint;
    private LatLng nextWaypoint;
    private int waypointIndex;
    //private double trackSpeedMs;
    //private double trackSpeedAccelMs2;
    //private boolean doingTurn;
    //private boolean waiting;
    //private double turnTarget;
    //private double currentTurn;
    //private double turnDuration;
    private Turn currentTurn;
    //private double waitStart;
    //private double waitDuration;
    private double startDelay;
    private List<LatLng> wayPoints; // list of waypoints
    private Map<LatLng, WaypointData> waypointData; // data for each waypoint
    private Map<LatLng, TurnPoints> turnPoints; // waypoint turn information
    private boolean greatCirclePath;

    private class Turn {

        private double turnDuration; // elapsed time in turn
        private double startHeading; // original heading
        private double currentTurn; // the current accumulated turn
        private double targetTurn; // the target turn
        private double angularSpeed; // w in º/s
        private double turnDelay;
        private double turnDelayAccum;

        public Turn(double startHeading, double targetTurn, double angularSpeed, double delay) {
            this.startHeading = startHeading;
            this.targetTurn = targetTurn;
            this.angularSpeed = angularSpeed;
            this.currentTurn = 0;
            this.turnDuration = 0;

            this.turnDelay = delay * 1000;
            this.turnDelayAccum = 0;
        }

        public boolean isWaiting() {
            return turnDelayAccum < turnDelay;
        }

        public void updateTurn(double elapsedTime) {
            turnDuration += elapsedTime;

            // check that turn is not waiting for delay
            if (isWaiting()) {
                turnDelayAccum += elapsedTime;

                // avoid process turn when the targetTurn or angular speed are 0
            } else if (targetTurn != 0 && angularSpeed != 0) {
                double turnedAngle = ((elapsedTime / 1000.0) * angularSpeed);
                currentTurn += turnedAngle;
                startHeading += targetTurn > 0 ? turnedAngle : -turnedAngle;
            }
        }

        public boolean isTurnFinished() {
            return !isWaiting() && Math.abs(currentTurn) >= Math.abs(targetTurn);
        }

        public double getTurn() {
            return this.currentTurn;
        }

        public double getTurnDuration() {
            return this.turnDuration / 1000.0;
        }

        public double getCurrentHeading() {
            return this.startHeading;
        }
    }

    public LatLng getCurrentPosition() {
        return currentPosition;
    }

    public double getCurrentSpeed() {
        return currentSpeed;
    }

    public double getCurrentAcceleration() {
        return currentAcceleration;
    }

    public double getCurrentHeading() {
        return currentHeading;
    }

    public double getSimulationDuration() {
        return this.simulationTime;
    }

    public boolean simulationStarted() {
        return simulationTime > startDelay * 1000;
    }

    public boolean simulationEnded() {
        return destinationWaypoint == null;
    }

    public TrackSimulation(double startDelay, List<LatLng> wayPoints, Map<LatLng, WaypointData> waypointData) {
        this.startDelay = startDelay;
        this.wayPoints = wayPoints;
        this.waypointData = waypointData;

        this.greatCirclePath = false;

        // start from beginning
        this.waypointIndex = 0;
        updateWaypoints();

        // initialize current poisition
        this.currentPosition = startWaypoint;

        // initialize initial heading
        if (destinationWaypoint != null) {
            this.currentHeading = greatCirclePath ? Spherical.getInitialBearing(currentPosition, destinationWaypoint) : Spherical.getRhumbBearing(currentPosition, destinationWaypoint);
        }

        // initialize initial speed
        WaypointData startData = waypointData.get(startWaypoint);
        this.currentSpeed = startData != null ? startData.getSpeed() : 0;

        // add a synthetic start turn with delay if there is a start waypoint with speed 0
        if (startData.getSpeed() == 0 && startData.getDelay() > 0) {
            this.currentTurn = new Turn(this.currentHeading, 0, 0, startData.getDelay());
            this.waypointIndex = -1; // set to -1 so the next waypoint will be the start
        }

        // fill the turn points information
        this.turnPoints = new HashMap<>();
        for (int i = 1; i < wayPoints.size() - 1; i++) {
            LatLng startPoint = wayPoints.get(i - 1);
            LatLng turnPoint = wayPoints.get(i);
            LatLng endpoint = wayPoints.get(i + 1);

            WaypointData turnData = waypointData.get(turnPoint);
            double normalAccel = turnData.getSpeed() * Math.toRadians(turnData.getAngularSpeed());
            double turnRadius = normalAccel == 0 ? 0 : Math.pow(turnData.getSpeed(), 2) / normalAccel;

            // alternative calculation without using normal accel (results in the same)
            // double turnRadius = turnData.getSpeed() / Math.toRadians(turnData.getAngularSpeed());

            TurnPoints turnPointData = Spherical.calculateTurnPoints(startPoint, turnPoint, endpoint, turnRadius);
            //System.out.println("Computed turn: " + turnPointData);
            turnPoints.put(turnPoint, turnPointData);
        }

        // initialize current acceleration
        this.currentAcceleration = 0;
    }

    private LatLng getWayoint(int index) {
        if (index < wayPoints.size()) {
            return wayPoints.get(index);
        }
        return null;
    }

    private void setNextDestination() {
        waypointIndex++;
        updateWaypoints();
        //System.out.println("Next destination is : " + destinationWaypoint);
    }

    private void updateWaypoints() {
        startWaypoint = getWayoint(waypointIndex);
        destinationWaypoint = getWayoint(waypointIndex + 1);
        nextWaypoint = getWayoint(waypointIndex + 2);
    }

    public void simulationStep(double simulationStep) {
        // increment the simulation time
        simulationTime += simulationStep;

        // avoid simulation if the simulation time is lower than initial delay
        if (!simulationStarted() || simulationEnded()) {
            return;
        }

        // check if the track is turning and then update
        if (currentTurn != null) {

            // update the turn data
            currentTurn.updateTurn(simulationStep);

            // update the track heading
            currentHeading = currentTurn.getCurrentHeading();

            // update the track position based on new heading and speed
            if (currentSpeed > 0) {
                double distance = currentSpeed * (simulationStep / 1000.0);
                if (greatCirclePath) {
                    currentPosition = Spherical.getDestinationPoint(currentPosition, distance, currentHeading);
                } else {
                    currentPosition = Spherical.getRhumbDestinationPoint(currentPosition, distance, currentHeading);
                }
            }

            // TODO CHECK TO MOVE SET NEXT DESTINATION TO ANOTHER SITE
            if (currentTurn.isTurnFinished()) {
                //System.out.println("SimulatedTrack turn ends. Total turn: " + currentTurn.getTurn() + " Total duration: " + currentTurn.getTurnDuration());
                currentTurn = null;
                setNextDestination();
            }
        }

        if (currentTurn == null && destinationWaypoint != null) {

            // get destination waypoint data
            WaypointData destinationData = waypointData.get(destinationWaypoint);
            TurnPoints destinationTurn = turnPoints.get(destinationWaypoint);
            double turnDistance = destinationTurn != null ? destinationTurn.getTurnStartDistance() : 0;

            // calculate the remaining distance to destination waypoint
            double remainingDistance;
            if (greatCirclePath) {
                remainingDistance = Spherical.getGreatCircleDistance(currentPosition, destinationWaypoint);
            } else {
                remainingDistance = Spherical.getRhumbDistance(currentPosition, destinationWaypoint);
            }
            remainingDistance -= turnDistance;
            remainingDistance = Math.round(remainingDistance * 1000000.0) / 1000000.0;
            
            if(remainingDistance>0){
                // calculate the instant acceleration needed to reach the waypoint at the desired speed
                currentAcceleration = (Math.pow(destinationData.getSpeed(), 2) - Math.pow(currentSpeed, 2)) / (2.0 * remainingDistance);

                // calculate the updated track speed based on acceleration, current speed and time
                currentSpeed = currentSpeed + currentAcceleration * (simulationStep / 1000.0);

                // calculate the travel distance based on acceleration and current speed
                double distance = currentSpeed * (simulationStep / 1000.0) + ((currentAcceleration * Math.pow((simulationStep / 1000.0), 2)) / 2.0);

                // calculate the track heading based on current position and destination point
                if (greatCirclePath) {
                    currentHeading = Spherical.getInitialBearing(currentPosition, destinationWaypoint);
                } else {
                    currentHeading = Spherical.getRhumbBearing(currentPosition, destinationWaypoint);
                }

                // calculate the track current position based on its current location, the travel distance, and bearing
                if (greatCirclePath) {
                    currentPosition = Spherical.getDestinationPoint(currentPosition, distance, currentHeading);
                } else {
                    currentPosition = Spherical.getRhumbDestinationPoint(currentPosition, distance, currentHeading);
                }
            }

            // compute the next bearing
            double nextBearing;
            if (greatCirclePath) {
                nextBearing = Spherical.getInitialBearing(currentPosition, destinationWaypoint);
            } else {
                nextBearing = Spherical.getRhumbBearing(currentPosition, destinationWaypoint);
            }

            // the track has overload the next waypoint
            if (remainingDistance==0 || Math.abs(nextBearing - currentHeading) > 5.0 || turnDistance>=(remainingDistance+turnDistance)) {
                //System.out.println("RemainingDistance is " + remainingDistance + ". Heading change is " + Math.abs(nextBearing - currentHeading));
                
                // check that there is a turn point
                if(destinationTurn!=null){
                    // set 0 acceleration in the turn
                    currentAcceleration = 0.0;

                    // allow turn delay only in destination with no speed
                    double turnDelay = destinationData.getSpeed() == 0.0 ? destinationData.getDelay() : 0.0;

                    // the turn angle for the last waypoint should be 0
                    currentTurn = new Turn(currentHeading, destinationTurn.getTurnAngle(), destinationData.getAngularSpeed(), turnDelay);

                    //System.out.println("Starting a turn: Delay{" + turnDelay + "}, " + destinationTurn);
                    
                // track has reach the last waypoint
                }else{
                    setNextDestination();
                }
            }
        }

    }
}
