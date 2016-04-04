/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.map;

/**
 *
 * @author alvarolb
 */
public interface GoogleMapObjectCallback {
      public void onTagSelected(String objectId);
      public void onTagDoubleClick(String objectId);
}
