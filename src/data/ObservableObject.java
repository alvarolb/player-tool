package data;

import app.WebDataPlayer;
import data.simulation.events.EventListener;
import data.simulation.events.EventProducer;
import gui.map.GoogleMap;
import javafx.application.Platform;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author alvarolb
 */
public abstract class ObservableObject extends EventProducer implements EventListener {

    private static final long serialVersionUID = 1L;

    protected final static GoogleMap gMap = WebDataPlayer.getGoogleMap();
    private final String objectId;
    private final String objectCategory;
    protected boolean showOnMap;
    protected String objectColor;
    protected String objectTag;
    protected long objectPath;
    protected long objectHistory;
    private transient long lastUpdate;
    protected long objectTimestamp;
    
    public ObservableObject(String objectId, String category) {
        this.objectId = objectId;
        this.objectCategory = category;
        this.showOnMap = true;
        this.objectTimestamp = System.currentTimeMillis();
        this.lastUpdate = System.currentTimeMillis();
    }

    public ObservableObject(String objectId, String category, boolean visible) {
        this.objectId = objectId;
        this.objectCategory = category;
        this.showOnMap = visible;
        this.objectTimestamp = System.currentTimeMillis();
        this.lastUpdate = System.currentTimeMillis();
    }
        
    public void update(ObservableObject object)
    {
        this.lastUpdate = System.currentTimeMillis();
    }
    
    public long getCreationTimestamp()
    {
        return this.objectTimestamp;
    }
    
    public long getLastUpdate(){
        return this.lastUpdate;
    }

    public String getObjectId() {
        return this.objectId;
    }

    public String getObjectCategory() {
        return this.objectCategory;
    }

    public abstract Map<String, String> getMapRepresentation();
    
    public void setShowOnMap(boolean showOnMap){
        this.showOnMap = showOnMap;
    }
    
    public void setShowOnMapAndUpdate(boolean showOnMap)
    {
        boolean currentState = this.showOnMap;
        this.showOnMap = showOnMap;
        if(showOnMap && !currentState)
        {
            setOnMap();
        }else if(!showOnMap && currentState)
        {
            removeFromMap();
        }
    }

    public boolean setOnMap() {
        if (showOnMap) {
            final Map<String, String> sourceMap = getMapRepresentation();
            if(sourceMap==null) return false;
            final Map<String, String> mapRepresentation = new HashMap<> (sourceMap);
            
            if (mapRepresentation != null) {
                
                if (objectColor != null) {
                    mapRepresentation.put("object_color", objectColor);
                }
                
                if (objectTag != null) {
                    mapRepresentation.put("object_tag", objectTag);
                }
                
                if (objectPath > 0 )
                {
                    mapRepresentation.put("object_path", String.valueOf(objectPath));
                }
                
                if (objectHistory > 0 )
                {
                    mapRepresentation.put("object_history", String.valueOf(objectHistory));
                }

                if (!Platform.isFxApplicationThread()) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            gMap.setObject(mapRepresentation);
                        }
                    });
                } else {
                    gMap.setObject(mapRepresentation);
                }
                
                return true;
            }
        }
        return false;
    }

    public void removeFromMap() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    gMap.removeObject(getObjectId());
                }
            });
        } else {
            gMap.removeObject(getObjectId());
        }
    }

    public void setObjectColor(String objectColor) {
        this.objectColor = objectColor;
    }

    public void setObjectTag(String objectTag) {
        this.objectTag = objectTag;
    }

    public boolean isShowOnMap() {
        return showOnMap;
    }

    public String getObjectColor() {
        return objectColor;
    }

    public String getObjectTag() {
        return objectTag;
    }

    @Override
    public String toString() {
        if( objectTag!=null ){
            return objectTag;
        }
        else
        {
            return objectId;
        }
    }
}
