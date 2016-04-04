/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package data.input;

import data.items.InfoData;
import data.items.ObjectData;

/**
 *
 * @author Alvaro
 */
public interface DataListener {
    public void onInfoDataReceived(InfoData info);
    public void onObjectDataReceived(ObjectData objectData);
    public void onStart(boolean reset);
    public void onResume();
    public void onPause();
    public void onStop();
}
