package data.simulation.track;

import data.ConfigurableObservableObject;
import geom.LatLng;

/**
 * Created by Alvaro on 4/03/14.
 */
public abstract class AbstractTrack extends ConfigurableObservableObject {
    private static final long serialVersionUID = 1L;

    public enum TrackType {
        SimulatedTrack ("SimulatedTrack"),
        FusedTrack ("Fused Track");

        private final String name;

        private TrackType(String s) {
            name = s;
        }
        public String toString(){
            return name;
        }
    }

    private TrackType trackType;

    public AbstractTrack(String objectId, String objectCategory, TrackType type) {
        super(objectId, objectCategory);
        this.trackType = type;
    }


    public abstract boolean isFused();
    public abstract LatLng getPosition();
    public abstract double getSpeed();
    public abstract String getSource();

    public TrackType getTrackType()
    {
        return this.trackType;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null ? getObjectId().equals(((AbstractTrack)obj).getObjectId()) : false;
    }
}
