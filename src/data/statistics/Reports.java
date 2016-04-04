package data.statistics;

import geom.LatLng;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by alvarolb on 22/03/14.
 */
public class Reports {

    private static Reports reports;
    private FileLog agentState;
    private Map<Integer, Double> lastReport;

    private Reports()
    {
        agentState = new FileLog("agentState.txt");
        this.lastReport = new HashMap<>();
    }

    public static Reports getInstance()
    {
        if(reports==null){
            reports = new Reports();
        }

        return reports;
    }

    public void sendAgentMode(double time, String agentId, int mode, String trackId, double trackDistance, LatLng lookingAt){
        /*
        final int agentIdInt = Integer.parseInt(agentId.replaceAll("\\D+",""));
        final int trackIdInt = Integer.parseInt(trackId.replaceAll("\\D+",""));
        Double lastReportSource = lastReport.get(agentIdInt);
        if(lastReportSource==null || time-lastReportSource>=1000){
            lastReport.put(agentIdInt, time);
            agentState.writeLine(time/1000 + " " + agentIdInt + " " + mode + " " + trackIdInt + " " + trackDistance + " " + (lookingAt!=null ? lookingAt.lat() : 0) + " " + (lookingAt!=null ? lookingAt.lon(): 0));
        }*/
    }

    private class FileLog
    {
        PrintWriter pw = null;

        public FileLog(String fileName)
        {
            try {
                File file = new File(fileName);
                FileWriter fw = new FileWriter(file);
                pw = new PrintWriter(fw);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void writeLine(String line)
        {
            if(pw!=null) {
                pw.println(line);
                pw.flush();
            }
        }
    }
}
