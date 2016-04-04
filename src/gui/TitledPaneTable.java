/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import java.util.Arrays;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 *
 * @author alvarolb
 */
public class TitledPaneTable extends TitledPane{
    private final TableView<PairItem> tableView = new TableView();
    final ObservableList<PairItem> data = FXCollections.observableArrayList();

    public TitledPaneTable(){
        super();
       
        setContent(tableView);
        
        TableColumn columnKey = new TableColumn("Key");
        columnKey.setMinWidth(100);
        columnKey.setCellValueFactory(new PropertyValueFactory<PairItem,String>("key"));
        
        TableColumn columnValue = new TableColumn("Value");
        columnValue.setMinWidth(100);
        columnValue.setCellValueFactory(new PropertyValueFactory<PairItem,String>("value"));
        
        tableView.setItems(data);
        tableView.getColumns().addAll(columnKey, columnValue);
    }
    
    public void addItem(String key, String value){
        data.add(new PairItem(key,value));
    }
    
    public class PairItem{
       private final SimpleStringProperty key;
       private final SimpleStringProperty value;

       public PairItem(String key, String value){
           this.key = new SimpleStringProperty(key);
           this.value = new SimpleStringProperty(value);
       }
       
       public String getKey(){
           return key.get();
       }
       
       public String getValue(){
           return value.get();
       }
       
       public void setKey(String key){
           this.key.set(key);
       }
       
        public void setValue(String value){
           this.value.set(value);
       }
   }
     
}
