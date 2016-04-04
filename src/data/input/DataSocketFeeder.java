/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package data.input;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alvaro
 */
public class DataSocketFeeder extends DataFeeder{
    private final int serverPort;
    private boolean start;
    private boolean stop;
    private SocketReader socketReader;
    private Thread threadReader;
    private final boolean logFile;
    
    public DataSocketFeeder(DataListener listener, int serverPort, boolean logFile){
        super(listener);
        this.serverPort = serverPort;
        this.start = false;
        this.stop = false;
        this.logFile = logFile;
    }
   
    private class SocketReader implements Runnable {
        private boolean stop;
        private ServerSocket serverSocket;
        private Socket clientSocket;
        
        public SocketReader(int serverPort){
            this.stop = false;
            try{
                this.serverSocket = new ServerSocket(serverPort);
            }catch(IOException e){
                this.serverSocket = null;
                e.printStackTrace();
            }
        }
        
        public void stop(){
            if(!stop && serverSocket!=null){
                try {
                    stop = true;
                    serverSocket.close();
                    serverSocket = null;
                    if(clientSocket!=null){
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        @Override
        public void run() {
            while(!stop && serverSocket!=null){
                try {
                    // client socket
                    clientSocket = null;
                    
                    // block call to accept incoming connection
                    clientSocket = serverSocket.accept();
                                    
                    // client connected
                    if(clientSocket!=null){
                        
                        // dispatch GUI start
                        dispatchOnStart(true);
                        
                        // create buffered reader for socket
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        
                        // create file writter if needed
                        PrintWriter out = null;
                        if(logFile){
                            File logFolder = new File("log"); 
                            logFolder.mkdirs();
                            File logFile = new File(logFolder, "rec_" + getDateTime() + ".log");
                            FileWriter outFile = new FileWriter(logFile);
                            out = new PrintWriter(outFile);
                        }
                       
                        //Read from socket line by line
                        String strLine;
                        while (!stop && (strLine = in.readLine()) != null) {
                            if(out!=null){
                                out.println(strLine);
                                out.flush();
                            }
                            long ts = processLine(strLine);
                        }
                        
                        // <- here the socket was closed
                        
                        // dispatch GUI stop
                        dispatchOnStop();
                        
                        // release files, sockets
                        if(out!=null){
                            out.close();
                        }
                        in.close();
                        clientSocket.close();
                    }
                    
                } catch (IOException e) {//Catch exception if any
                    //e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void start() {
        if(!start){
            start = true;
            stop = false;
            socketReader = new SocketReader(serverPort);
            threadReader = new Thread(socketReader, "SOCKET READER");
            threadReader.setDaemon(true);
            threadReader.start();
        }
    }

    @Override
    public void stop() {
          if (start && !stop) {
            stop = true;
            start = false;
            if (threadReader != null) {
                try {
                    socketReader.stop();
                    threadReader.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(DataFileFeeder.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }       
    }
    
    @Override
    public void pause() {
        throw new UnsupportedOperationException("Not supported by Socket Feeder.");
    }

    @Override
    public void setSpeed(int speed) {
        throw new UnsupportedOperationException("Not supported by Socket Feeder.");
    }
    
    private static String getDateTime()  
    {  
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");  
        df.setTimeZone(TimeZone.getDefault());  
        return df.format(new Date());  
    }
}
