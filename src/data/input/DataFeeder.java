/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package data.input;

import data.PairedValues;
import data.items.InfoData;
import data.items.ObjectData;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;

/**
 *
 * @author Alvaro
 */
public abstract class DataFeeder {

    protected DataListener listener;

    public DataFeeder(DataListener listener) {
        this.listener = listener;
    }

    public abstract void start();

    public abstract void pause();

    public abstract void stop();

    public abstract void setSpeed(int speed);

    public void dispatchOnStart(final boolean reset) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                listener.onStart(reset);
            }
        });
    }
    
    public void dispatchOnResume() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                listener.onResume();
            }
        });
    }

    public void dispatchOnPause() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                listener.onPause();
            }
        });
    }

    public void dispatchOnStop() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                listener.onStop();
            }
        });
    }

    public void dispatchInfoDataReceived(final InfoData infoData) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                listener.onInfoDataReceived(infoData);
            }
        });
    }

    public void dispatchObjectDataReceived(final ObjectData objectData) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                listener.onObjectDataReceived(objectData);
            }
        });
    }
   
    private static final String pairValueRegEx = "(.*?):(.*?);";
    private static final Pattern pairValuePattern = Pattern.compile(pairValueRegEx);
    
    protected long processLine(String line) {
        //System.out.println(line);
        try{
            Matcher matcher = pairValuePattern.matcher(line);
            
            PairedValues values = new PairedValues();
            
             while(matcher.find()){
                 values.addPair(matcher.group(1), matcher.group(2));
            }   
            
            switch(values.getString("type"))
            {
                case "object":
                {
                    ObjectData objectData = new ObjectData(values);
                    this.dispatchObjectDataReceived(objectData);
                     
                }
                    break;
                case "info":
                {
                    InfoData infoData = new InfoData(values);
                    this.dispatchInfoDataReceived(infoData);
                }
                    break;
            }
            
            if(values.containsKey("ts")){
                return values.getLong("ts");
            }
            
        }catch(Exception e){
            System.err.println("Cannot process line: '" + line + "'");
            e.printStackTrace();
        }
        return 0;
    }
}
