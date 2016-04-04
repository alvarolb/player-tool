package data.simulation;

import data.ObservableObject;
import data.items.HeatMap;
import data.items.Polyline;
import data.simulation.events.Event;
import data.simulation.track.AbstractTrack;
import data.simulation.track.SimulatedTrack;
import data.simulation.track.TrackDetection;
import data.simulation.track.TrackManager;
import data.statistics.Reports;
import geom.LatLng;
import geom.Spherical;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.text.Text;
import javafx.util.Pair;
import org.ejml.simple.SimpleEVD;
import org.ejml.simple.SimpleMatrix;

import java.io.IOException;
import java.util.*;

/**
 * Created by Alvaro on 3/03/14.
 */
public class CameraAgent extends Agent {

    private static final long serialVersionUID = 1L;
    private static final long ASSIGN_TRACK_TIMEOUT = 2000;
    private static final long AUTO_PANNING_TIMEOUT = 10000;
    private static final long LOST_BID_TIMEOUT = 10000;

    private Camera controllingCamera;
    private Polyline monitoringPath;
    private double panningTransitionDelay = 3000;
    private double maxAdmisibleDistance = 3000;

    private transient TrackManager<TrackDetection> trackManager;
    private transient TrackManager.TrackListener<TrackDetection> trackListener;

    private transient Map<AbstractTrack, CameraAgent> tracksRelations;
    private transient Map<String, Pair<Long, AbstractTrack>> lostBids;

    private transient ComboBox comboBoxControllingCamera;
    private transient ComboBox comboBoxMonitoringPath;
    private transient DoubleItemValue panningTransitionDelayNode;
    private transient DoubleItemValue maxAdmisibleDistanceNode;
    private transient ToggleButton showHeatMap;
    private static transient HeatMap heatMap;
    private transient long lastHeatHashMapUpdate;

    // parameters used in simulation
    private transient AbstractTrack currentTrack = null;
    private transient AbstractTrack currentTentativetrack = null;
    private transient long tentativeTrackTimestamp = 0;

    private boolean autoPanningMode = false;
    private transient long inactivityTime = 0;
    private transient int currentPanningIndex = 0;
    private transient boolean currentPanningIndexIncreasing = true;
    private transient double currentPanningTransitionDelay = 0;
    private transient LatLng lastPanningTarget;
    private transient LatLng currentPanningTarget;
    private transient double currentPanningTargetBearing;
    private transient double currentPanningTargetDistance;

    // DISTANCE, SPEED, ZONE PRIORITY, SAME SENSOR
    private static final SimpleMatrix mat = new SimpleMatrix(new double[][]
    {
            {1./1, 2./1, 1./2, 2./1},
            {1./2, 1./1, 1./3, 1./2},
            {2./1, 3./1, 1./1, 3./1},
            {1./2, 2./1, 1./3, 1./1}
    });

    @Override
    public void resetSimulation() {
        super.resetSimulation();
        trackManager.reset();
        lostBids.clear();
        tracksRelations.clear();
        currentTrack = null;
        currentTentativetrack=null;
        autoPanningMode = false;
        inactivityTime = 0;
        currentPanningIndex = 0;
        currentPanningTransitionDelay = 0;
    }

    private static final SimpleEVD eigen = mat.eig();
    private static final SimpleMatrix tempEigen = eigen.getEigenVector(eigen.getIndexMax());
    private static final SimpleMatrix eigenVector = tempEigen.divide(tempEigen.elementSum());

    //private static final Matrix eigenAHPMatrix = ahpPriorities.eig().getV();

    public CameraAgent(String objectId, String objectCategory) {
        super(objectId, objectCategory);

        this.trackManager = new TrackManager();
        this.trackListener = new TrackListenerImpl();
        this.trackManager.addListener(this.trackListener);

        /*
        this.fusedTrackTrackManager = new TrackManager();
        this.fusedTrackTrackListener = new FusedTrackListenerImpl();
        this.fusedTrackTrackManager.addListener(this.fusedTrackTrackListener);
        */
        this.tracksRelations = new HashMap<>();
        this.lostBids = new HashMap<>();
    }

    // used in serialization for initialize transient objects
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // initialize non-gui transient objects
        this.trackManager = new TrackManager();
        this.trackListener = new TrackListenerImpl();
        this.trackManager.addListener(this.trackListener);

        System.out.println("eigen vector");
        System.out.println(tempEigen);
        double eigenValue = eigen.getEigenvalue(eigen.getIndexMax()).getReal();
        double consistencyIndex = (eigenValue - eigenVector.getNumElements())/(eigenVector.getNumElements()-1);
        double consistencyRatio = consistencyIndex / 0.58;
        System.out.println("Eigen value: " + eigenValue);
        System.out.println("Consistency Index: " + consistencyIndex);
        System.out.println("Consistency Ratio: " + consistencyRatio * 100. + "%");
        System.out.println("normalized eigen vector");
        System.out.println(eigenVector);
        /*
        this.fusedTrackTrackManager = new TrackManager();
        this.fusedTrackTrackListener = new FusedTrackListenerImpl();
        this.fusedTrackTrackManager.addListener(this.fusedTrackTrackListener);
        */

        this.tracksRelations = new HashMap<>();
        this.lostBids = new HashMap<>();
    }

    @Override
    protected List<Node> getConfigurableNodes() {
        List<Node> nodes = super.getConfigurableNodes();

        // Add controlling camera menu
        nodes.add(new Text("Controlling Camera"));
        nodes.add(new Separator());

        ObservableList<ObservableObject> cameras = FXCollections.observableList(repository.getObjectsByCategory(Sensor.CATEGORY));
        comboBoxControllingCamera = new ComboBox(cameras);
        comboBoxControllingCamera.setPrefWidth(150);
        comboBoxControllingCamera.valueProperty().addListener(new ChangeListener<ObservableObject>() {
            @Override
            public void changed(ObservableValue<? extends ObservableObject> observableValue, ObservableObject observableObject, ObservableObject observableObject2) {
                if(observableObject2 instanceof Camera){
                    controllingCamera = (Camera) observableObject2;
                }
            }
        });

        if(controllingCamera!=null)
        {
            comboBoxControllingCamera.setValue(controllingCamera);
        }

        nodes.add(comboBoxControllingCamera);


        // Add monitoring path menu
        nodes.add(new Text("Monitoring Path"));
        nodes.add(new Separator());

        ObservableList<ObservableObject> polylines = FXCollections.observableList(repository.getObjectsByCategory(Polyline.CATEGORY));
        comboBoxMonitoringPath = new ComboBox(polylines);
        comboBoxMonitoringPath.setPrefWidth(150);
        comboBoxMonitoringPath.valueProperty().addListener(new ChangeListener<ObservableObject>() {
            @Override
            public void changed(ObservableValue<? extends ObservableObject> observableValue, ObservableObject observableObject, ObservableObject observableObject2) {
                if(observableObject2 instanceof Polyline){
                    monitoringPath = (Polyline) observableObject2;
                }
            }
        });

        if(monitoringPath!=null)
        {
            comboBoxMonitoringPath.setValue(monitoringPath);
        }

        nodes.add(comboBoxMonitoringPath);

        panningTransitionDelayNode = new DoubleItemValue("Transition Delay", panningTransitionDelay) {
            @Override
            public void onValueChanged(double value) {
                panningTransitionDelay = value;
            }
        };

        nodes.add(panningTransitionDelayNode);

        maxAdmisibleDistanceNode = new DoubleItemValue("Max Monitoring Distance", maxAdmisibleDistance) {
            @Override
            public void onValueChanged(double value) {
                maxAdmisibleDistance = value;
            }
        };

        nodes.add(maxAdmisibleDistanceNode);


        showHeatMap = new ToggleButton("Show Monitoring Heatmap");
        showHeatMap.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
                if(newValue){
                    heatMap = new HeatMap(getObjectId() + "_heatmap");
                }else{
                    if(heatMap!=null){
                        heatMap.removeFromMap();
                        heatMap = null;
                    }
                }
            }
        });

        nodes.add(showHeatMap);

        return nodes;
    }

    private class TrackListenerImpl implements TrackManager.TrackListener<TrackDetection>
    {
        @Override
        public void onTrackCreated(final TrackDetection track, Event event) {
            manageTrackUpdate(track);
        }

        @Override
        public void onTrackUpdated(final TrackDetection track, Event event) {
            manageTrackUpdate(track);
        }

        @Override
        public void onTrackDeleted(final TrackDetection track, Event event) {
            manageTrackDelete(track);
        }
    }

    /*
    private class FusedTrackListenerImpl implements TrackManager.TrackListener<FusedTrack>
    {
        @Override
        public void onTrackCreated(final FusedTrack track, Event event) {
            manageTrackUpdate(track);
        }

        @Override
        public void onTrackUpdated(final FusedTrack track, Event event) {
            manageTrackUpdate(track);
        }

        @Override
        public void onTrackDeleted(final FusedTrack track, Event event) {
            manageTrackDelete(track);
        }
    }
*/

    @Override
    protected void onSensorAttached(Sensor sensor) {
        sensor.addListener(this);
    }

    @Override
    protected void onSensorDetached(Sensor sensor) {
        sensor.removeListener(this);
    }

    @Override
    protected void onAgentAttached(Agent agent) {
        agent.addListener(this);
    }

    @Override
    protected void onAgentDetached(Agent agent)
    {
        agent.removeListener(this);
    }

    @Override
    public void simulationStep(final List<SimulatedTrack> tracks, double simulationStep) {
        super.simulationStep(tracks, simulationStep);

        if(currentTentativetrack!=null){
            long currentTimestamp = System.currentTimeMillis();
            if(currentTimestamp-tentativeTrackTimestamp > ASSIGN_TRACK_TIMEOUT){
                if(currentTrack!=null){
                    sendStopTrackingEvent(currentTrack);
                }
                currentTrack = currentTentativetrack;
                currentTentativetrack = null;
                sendStartTrackingEvent(currentTrack);
            }
        }
        if(!autoPanningMode && currentTrack==null)
        {
            inactivityTime+=simulationStep;
            if(inactivityTime > AUTO_PANNING_TIMEOUT)
            {
                autoPanningMode = true;
                currentPanningTransitionDelay = 0;
                switchToNextMonitoringPoint();
            }
        }

        if(autoPanningMode){
            currentPanningTransitionDelay += simulationStep;
            if(currentPanningTransitionDelay>panningTransitionDelay) {
                currentPanningTransitionDelay = 0;
                switchToNextMonitoringPoint();
            }

            //System.out.println("CurrentPanningDistance " + currentPanningTargetDistance);
            double panSpeed = currentPanningTargetDistance /panningTransitionDelay;
            //System.out.println("PanSpeed " + panSpeed);
            double distance = panSpeed*currentPanningTransitionDelay;
            //System.out.println("Step in " + simulationStep + " -> " + distance);
            LatLng interpolated = Spherical.getRhumbDestinationPoint(lastPanningTarget, distance, currentPanningTargetBearing);
            lookAt(interpolated);
        }


        long currentTimestamp = System.currentTimeMillis();
        Iterator<Pair<Long, AbstractTrack>> iterator = lostBids.values().iterator();
        while(iterator.hasNext())
        {
            Pair<Long, AbstractTrack> value = iterator.next();
            if(currentTimestamp-value.getKey()>LOST_BID_TIMEOUT){
                iterator.remove();
            }
        }

        sendReports();
    }

    private void sendReports()
    {
        // save agent mode state
        int modeState = 0;
        if(currentTentativetrack!=null){
            modeState = 3;
        }
        if(currentTrack!=null){
            modeState = 2;
        }
        if(autoPanningMode){
            modeState = 1;
        }


        double myTrackDistance = currentTrack!=null ? Spherical.getRhumbDistance(controllingCamera.getSensorPosition(), currentTrack.getPosition()) : 0;
        String trackId = currentTrack != null ? ((TrackDetection) currentTrack).getSimulatedTrackId() : "0";

        Reports.getInstance().sendAgentMode(simulationDuration, getObjectId(), modeState, trackId, myTrackDistance, controllingCamera.getLookingAtPoint());
    }

    @Override
    public void onEventReceived(Event event) {
        // call super to draw events
        super.onEventReceived(event);

        switch (event.getRawEventType())
        {
            case Track:
                trackManager.manageTrackEvent(event);
                break;
            case TentativeTracking:
                manageTentativeTrackingEvent(event);
                break;
            case Tracking:
                manageTrackingEvent(event);
                break;
        }
    }

    private void manageTrackingEvent(final Event trackingEvent)
    {
        final AbstractTrack track = trackingEvent.getEventData();
        switch (trackingEvent.getRawEventAction())
        {
            case Create:
                final CameraAgent agent = (CameraAgent) repository.getObject(trackingEvent.getEventSource());
                tracksRelations.put(track, agent);
                break;
            case Delete:
                tracksRelations.remove(track);
                lostBids.remove(track.getObjectId());
                break;
        }
    }

    /**
     * Called when an external agent send an intent to track a new track
     * @param tentativeTrackingEvent
     */
    private void manageTentativeTrackingEvent(Event tentativeTrackingEvent)
    {
        final AbstractTrack track = tentativeTrackingEvent.getEventData();
        // two cameras trying to track the same target
        if(track.equals(currentTrack) || track.equals(currentTentativetrack)){
            // get other agent
            final CameraAgent otherAgent = (CameraAgent) repository.getObject(tentativeTrackingEvent.getEventSource());

            // other agent resolve the conflict with me and the track
            CameraAgent winAgent = otherAgent.resolveConflictWithTrack(this, track);

            // other agent won me so manage my lost
            if(winAgent==otherAgent){
                System.out.println(getObjectId() + " lost a bid against " + otherAgent.getObjectId() + " for track " + track.getObjectId());
                manageLostBidWithTrack(track);
            }
        }
    }

    private void manageLostBidWithTrack(AbstractTrack track){

        lostBids.put(track.getObjectId(), new Pair<>(System.currentTimeMillis(), track));

        // some agent is already tracking or tyring to track, and it achieved more score
        if(track.equals(currentTentativetrack)){
            currentTentativetrack = null;
        }

        // some agent requested track the current track and won the bid
        if(track.equals(currentTrack))
        {
            sendStopTrackingEvent(currentTrack);
            currentTrack = null;
        }
    }

    /**
     * Called when other agent requires to solve a conflict about a track with me
     * @param otherAgent
     * @param track
     * @return
     */
    public CameraAgent resolveConflictWithTrack(CameraAgent otherAgent, AbstractTrack track)
    {
        Camera myCamera = controllingCamera;
        Camera otherCamera = otherAgent.getControllingCamera();
        if(track==null) return null;
        if(myCamera==null && otherCamera==null) return null;
        if(myCamera!=null && otherCamera==null) return this;
        if(myCamera==null && otherCamera!=null) return otherAgent;

        double myTrackDistance = Spherical.getRhumbDistance(myCamera.getSensorPosition(), track.getPosition());
        double otherTrackDistance = Spherical.getRhumbDistance(otherCamera.getSensorPosition(), track.getPosition());

        if(myTrackDistance > otherTrackDistance)
        {
            System.out.println(getObjectId() + " lost a bid against " + otherAgent.getObjectId() + " for track " + track.getObjectId());
            manageLostBidWithTrack(track);
            return otherAgent;
        }

        return this;
    }

    private void sendTentativeStartTrackingEvent(AbstractTrack track){
        Event tentativeStartTrackingEvent = new Event(Event.EventType.TentativeTracking, Event.EventAction.Create, getObjectId(), track);
        dispatchEvent(tentativeStartTrackingEvent);
    }

    private void sendStartTrackingEvent(AbstractTrack track)
    {
        Event startTrackingEvent = new Event(Event.EventType.Tracking, Event.EventAction.Create, getObjectId(), track);
        dispatchEvent(startTrackingEvent);
    }

    private void sendStopTrackingEvent(AbstractTrack track)
    {
        Event stopTrackingEvent = new Event(Event.EventType.Tracking, Event.EventAction.Delete, getObjectId(), track);
        dispatchEvent(stopTrackingEvent);
    }


    /** Manage a track update, that may provoke change the target track
     * if it has more priority
     *
     * @param track
     */
    private void manageTrackUpdate(final AbstractTrack track)
    {
        selectBestTrack();
    }

    private void lookAtCurrentTrack()
    {
        if(currentTrack!=null){
            lookAt(currentTrack.getPosition());
        }
    }

    private void lookAt(final LatLng position){
        if(controllingCamera!=null) {
            controllingCamera.lookAt(position);
        }
        if(heatMap!=null)
        {
            long currentTime = System.currentTimeMillis();
            if(currentTime-lastHeatHashMapUpdate > 1000){
                lastHeatHashMapUpdate = currentTime;
                heatMap.addHeatPoint(position);
            }
        }
    }
/*
    private void lookToNextWaypoint()
    {
        if(monitoringPath!=null){
            List<LatLng> path =  monitoringPath.getPolylinePoints();
            if(!path.isEmpty()) {
                currentPanningTarget = path.get(currentPanningIndex);
                lookAt(currentPanningTarget);
                currentPanningIndex = (currentPanningIndex + 1) % path.size();
            }
        }
    }*/

    private void switchToNextMonitoringPoint()
    {
        if(monitoringPath==null) return;
        List<LatLng> path =  monitoringPath.getPolylinePoints();
        if(path.size()>=2){
            lastPanningTarget = currentPanningTarget != null ? currentPanningTarget : path.get(0);
            currentPanningIndex = currentPanningIndexIncreasing ? currentPanningIndex + 1 : currentPanningIndex - 1;
            if(currentPanningIndex<0){
                currentPanningIndexIncreasing = true;
                currentPanningIndex = 1;
            }
            if(currentPanningIndex>=path.size()){
                currentPanningIndexIncreasing = false;
                currentPanningIndex = path.size()-2;
            }
            currentPanningTarget = path.get(currentPanningIndex);
            currentPanningTargetDistance = Spherical.getRhumbDistance(lastPanningTarget, currentPanningTarget);
            currentPanningTargetBearing = Spherical.getRhumbBearing(lastPanningTarget, currentPanningTarget);
            // look at will be automatically interpolated in simulation step
        }else if(!path.isEmpty()){
            currentPanningTarget = path.get(0);
            lookAt(currentPanningTarget);
        }
    }

    private void selectBestTrack()
    {
        AbstractTrack bestTrack = getBestAlternative();

        if(bestTrack==null){
            currentTrack = null;
            currentTentativetrack = null;
        }else{
            boolean bestIsCurrent = bestTrack.equals(currentTrack);
            if(bestIsCurrent)
            {
                currentTrack = bestTrack;
            }
            // new best tentative track. notify only if is not already established
            else if(!bestTrack.equals(currentTentativetrack)){
                currentTentativetrack = bestTrack;
                tentativeTrackTimestamp = System.currentTimeMillis();
                sendTentativeStartTrackingEvent(bestTrack);
            }
        }

        // reset inactivity time to avoid auto panning mechanism when following a track
        if(autoPanningMode && currentTrack!=null)
        {
            autoPanningMode = false;
            inactivityTime = 0;
        }

        lookAtCurrentTrack();
    }
    /**
     * In this case is necessary to determine what is the best track in the
     * tracks repository to follow (or wait an update)
     * @param track
     */
    private void manageTrackDelete(final AbstractTrack track)
    {
        if(currentTrack != null && currentTrack.equals(track)){
            sendStopTrackingEvent(currentTrack);
            currentTrack = null;
            selectBestTrack();
        }

        /*
        Collection<SimulatedTrack> tracks = trackManager.getCurrentTracks();
        for(SimulatedTrack existingTrack : tracks)
        {
            double trackScore = getTrackScore(existingTrack);
            if(trackScore>bestTrackScore)
            {
                bestTrackScore = trackScore;
                bestTrack = existingTrack;
            }
        }

        // cannot appear a simulated track with better score than a fused track
        if(bestTrack==null){
            Collection<SimulatedTrack> simulatedTracks = trackManager.getCurrentTracks();
            for(SimulatedTrack simulatedTrack : simulatedTracks)
            {
                double trackScore = getTrackScore(simulatedTrack);
                if(trackScore>bestTrackScore)
                {
                    bestTrackScore = trackScore;
                    bestTrack = simulatedTrack;
                }
            }
        }

        currentTrack = bestTrack;
        currentTrackScore = bestTrackScore;

        if(bestTrack==null)
        {
            // TODO implement a fail-safe mechanism to start panning for new tracks
        }
        */
    }

    private AbstractTrack getBestAlternative()
    {
        ArrayList<TrackDetection> tracks = trackManager.getCurrentTracks();

        // filter too far
        Iterator<TrackDetection> iterator = tracks.iterator();
        while(iterator.hasNext()){
            TrackDetection detection = iterator.next();
            double trackDistance = Spherical.getRhumbDistance(controllingCamera.getSensorPosition(), detection.getPosition());
            if(trackDistance>maxAdmisibleDistance){
                iterator.remove();
            }
        }

        // no tracks, no alternative
        if(tracks.size()==0) return null;

        SimpleMatrix matrix = new SimpleMatrix(tracks.size(), eigenVector.getNumElements());

        SimpleMatrix temporalMatrix = new SimpleMatrix(tracks.size(), 1);

        // DISTANCE
        for(int i=0; i<tracks.size(); i++)
        {
            double trackDistance = Spherical.getRhumbDistance(controllingCamera.getSensorPosition(), tracks.get(i).getPosition());
            //System.out.println("distance is: " + trackDistance);
            temporalMatrix.set(i, 0, trackDistance);
        }
        // normalize distance
        double sum = temporalMatrix.elementSum();
        for(int i=0;i<tracks.size();i++)
        {
            temporalMatrix.set(i,0, sum - temporalMatrix.get(i,0));
        }
        temporalMatrix = temporalMatrix.divide(sum);
        matrix.insertIntoThis(0, 0, temporalMatrix);

        // SPEED
        for(int i=0; i<tracks.size(); i++)
        {
            double trackSpeed = tracks.get(i).getSpeed();
            temporalMatrix.set(i, 0, trackSpeed);
        }
        temporalMatrix = temporalMatrix.divide(temporalMatrix.elementSum());
        matrix.insertIntoThis(0, 1, temporalMatrix);

        // ZONAL PRIORITY
        for(int i=0; i<tracks.size(); i++)
        {
            temporalMatrix.set(i, 0, 0);
        }
        matrix.insertIntoThis(0, 2, temporalMatrix);

        // SAME SENSOR
        if(currentTrack!=null) {
            for (int i = 0; i < tracks.size(); i++) {
                temporalMatrix.set(i, 0, tracks.get(i).getSource().equals(currentTrack.getSource()) ? 1 : 0);
            }
            matrix.insertIntoThis(0, 3, temporalMatrix);
        }

        //System.out.println("priority matrix");
        //System.out.println(matrix);

        SimpleMatrix result = matrix.mult(eigenVector);

        double max = 0;
        AbstractTrack bestTrack = null;
        for(int i=0; i<tracks.size();i++)
        {
            AbstractTrack currentTrack = tracks.get(i);
            if(!lostBids.containsKey(currentTrack.getObjectId())){
                double current = result.get(i,0);
                if(current>max){
                    max = current;
                    bestTrack = currentTrack;
                }
            }
        }

        return bestTrack;
    }

    public Camera getControllingCamera()
    {
        return controllingCamera;
    }
    public Polyline getMonitoringPath() { return monitoringPath; }

   private List<Pair<String, String>> getAgentDetails(){
       ArrayList<Pair<String, String>> properties = new ArrayList<>();

       properties.add(new Pair<>("Visible Tracks", String.valueOf(trackManager.getTraksCount())));
       properties.add(new Pair<>("Inactivity Time", String.valueOf(inactivityTime)));
       properties.add(new Pair<>("Panning Mode", String.valueOf(autoPanningMode)));
       properties.add(new Pair<>("Following", currentTrack!= null ? currentTrack.toString() : "None"));
       properties.add(new Pair<>("Tentative Track", currentTentativetrack != null ? currentTentativetrack.toString() : "None"));

       return properties;
   }

    @Override
    public Map<String, String> getMapRepresentation() {
        if(controllingCamera==null) return null;

        final Map<String, String> path = new HashMap<>();
        path.put("object_id", getObjectId());
        path.put("type", "object");
        path.put("object_type", "circle");
        if (objectColor != null) {
            path.put("object_color", objectColor);
        }
        path.put("circle_lat", String.valueOf(controllingCamera.getSensorPosition().lat()));
        path.put("circle_lon", String.valueOf(controllingCamera.getSensorPosition().lon()));
        path.put("circle_radius", "100");

        List<Pair<String, String>> properties = getAgentDetails();
        if(!properties.isEmpty()){
            StringBuilder builder = new StringBuilder().append('{');
            for(Pair<String, String> property : properties){
                builder.append("'").append(property.getKey()).append("':'").append(property.getValue()).append("',");
            }
            builder.deleteCharAt(builder.length()-1).append('}');
            path.put("object_details", builder.toString());
        }

        return path;
    }
}
