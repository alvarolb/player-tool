/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package data.input;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alvaro
 */
public class DataFileFeeder extends DataFeeder {

    private long ts_1;
    private String path;
    private boolean started;
    private boolean paused;
    private boolean stop;
    private int speed;
    private Thread fileThread;
    
    public DataFileFeeder(DataListener listener, String path) {
        super(listener);
        this.path = path;
        ts_1 = 0;
        started = false;
        stop = false;
        fileThread = null;
        speed = 1;
    }
    
    @Override
    protected long processLine(String line) {
        long timestamp =  super.processLine(line);
        if (ts_1 == 0) {
            ts_1 = timestamp;
        } else {
            long tsDiff = (timestamp - ts_1) / speed;
            ts_1 = timestamp;
            if (tsDiff > 0) {
                try {
                    Thread.sleep(tsDiff);
                } catch (Exception e) {
                }
            }                
        }
        return 0;
    }
    
    private class FileLoader implements Runnable {

        private final String filePath;
        
        public FileLoader(String filePath) {
            this.filePath = filePath;
        }
        
        @Override
        public void run() {
            dispatchOnStart(true);
            try {
                BufferedReader in = new BufferedReader(new FileReader(this.filePath));
                
                String strLine;
                //Read File Line By Line
                while (!stop && (strLine = in.readLine()) != null) {
                    while (paused && !stop) {
                        Thread.sleep(500);
                    }
                    processLine(strLine);
                }
                //Close the input stream
                in.close();
            } catch (Exception e) {//Catch exception if any
                System.err.println("Cannot open file '" + this.filePath + "'");
                e.printStackTrace();
            }
            dispatchOnStop();
        }
    }
    
    @Override
    public void start() {
        if (!started) {
            started = true;
            paused = false;
            stop = false;
            fileThread = new Thread(new FileLoader(path), "FILE READER");
            fileThread.setDaemon(true);
            fileThread.start();
        } else if (paused) {
            paused = false;
            dispatchOnResume();
        }
    }
    
    @Override
    public void pause() {
        if (started && !paused) {
            paused = true;
            dispatchOnPause();
        }
    }
    
    @Override
    public void stop() {
        if (started && !stop) {
            stop = true;
            started = false;
            paused = false;
            if (fileThread != null) {
                try {
                    fileThread.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(DataFileFeeder.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }        
    }
    
    @Override
    public void setSpeed(int speed) {
        this.speed = speed;
    }
}
