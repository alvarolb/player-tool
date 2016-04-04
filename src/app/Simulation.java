/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package app;

import data.ObjectRepository;
import data.ObservableObject;
import data.simulation.Agent;
import data.simulation.Sensor;
import data.simulation.track.SimulatedTrack;

import java.util.ArrayList;
import java.util.List;
import javafx.concurrent.Task;

/**
 *
 * @author alvarolb
 */
public class Simulation {

    private final ObjectRepository repo;

    public Simulation(ObjectRepository repo) {
        this.repo = repo;
    }
    private SimulatorTask simulatorTask;
    private Thread simulatorThread;

    public void start() {
        List<Sensor> sensors = new ArrayList<>();
        List<SimulatedTrack> tracks = new ArrayList<>();
        List<Agent> agents = new ArrayList<>();

        List<ObservableObject> sensorObjects = repo.getObjectsByCategory(Sensor.CATEGORY);
        for (ObservableObject sensorObject : sensorObjects) {
            sensors.add((Sensor) sensorObject);
        }

        List<ObservableObject> trackObjects = repo.getObjectsByCategory(SimulatedTrack.CATEGORY);
        for (ObservableObject trackObject : trackObjects) {
            tracks.add((SimulatedTrack) trackObject);
        }

        List<ObservableObject> agentObjects = repo.getObjectsByCategory(Agent.CATEGORY);
        for (ObservableObject agentObject : agentObjects) {
            agents.add((Agent) agentObject);
        }

        simulatorTask = new SimulatorTask(sensors, tracks, agents, 10);
        simulatorTask.setAccelerationFactor(accelerationFactor);
        simulatorThread = new Thread(simulatorTask);
        simulatorThread.setDaemon(true);
        simulatorThread.start();
    }

    public void stop() {
        if (simulatorThread != null && simulatorTask != null) {
            simulatorTask.cancel();
            try {
                simulatorThread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
            simulatorThread = null;
            simulatorTask = null;
        }
    }
    private int accelerationFactor = 1;
    
    public void setAccelerationFactor(int value){
        this.accelerationFactor = value;
        if(simulatorTask!=null){
            simulatorTask.setAccelerationFactor(value);
        }
    }

    private class SimulatorTask extends Task<Void> {

        private final double simulationStep;
        private final List<Sensor> sensors;
        private final List<SimulatedTrack> tracks;
        private final List<Agent> agents;
        private long lastRefresh; // ms
        private int acceleration = 1;
        
        public void setAccelerationFactor(int acceleration){
            this.acceleration = acceleration;
        }
        
        public SimulatorTask(List<Sensor> sensors, List<SimulatedTrack> tracks, List<Agent> agents, double simulationStep) {
            this.sensors = sensors;
            this.tracks = tracks;
            this.agents = agents;
            this.simulationStep = simulationStep;
            this.lastRefresh = 0;
        }

        @Override
        protected Void call() {
            // reset all track simulations
            try{
                for (SimulatedTrack track : tracks) {
                    track.resetSimulation();
                    track.setOnMap();
                }

                for (Sensor sensor : sensors) {
                    sensor.resetSimulation();
                    sensor.setOnMap();
                }

                for (Agent agent : agents) {
                    agent.resetSimulation();
                    agent.setOnMap();
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            
            int simSteps = 1;
            
            do {
                final long currentTime = System.currentTimeMillis();
                boolean showInGUI = (currentTime - lastRefresh) > 200;
                if (showInGUI) {
                    lastRefresh = currentTime;
                }

                try{
                    for (SimulatedTrack track : tracks) {
                        track.simulationStep(simulationStep);
                        if (showInGUI) {
                            if(track.isVisibleInSimulation()) {
                                track.setOnMap();
                            }else{
                                track.removeFromMap();
                            }
                        }
                    }

                    for (Sensor sensor : sensors) {
                        sensor.simulationStep(tracks, simulationStep);
                        if (showInGUI) {
                            sensor.setOnMap();
                        }
                    }

                    for (Agent agent : agents) {
                        agent.simulationStep(tracks, simulationStep);
                        if (showInGUI) {
                            agent.setOnMap();
                        }
                    }


                    if(acceleration==simSteps){
                        simSteps=0;
                        try {
                            Thread.sleep((long) (simulationStep));
                        } catch (Exception e) {
                            //e.printStackTrace();
                        }
                    }
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
                
                simSteps = simSteps+1  ;
                
            } while (!isCancelled());
            return null;
        }
    }
}
