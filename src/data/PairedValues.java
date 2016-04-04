package data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Alvaro
 */
public class PairedValues {
    private final Map<String, String> values;
    
    public PairedValues(){
        this.values = new LinkedHashMap<>();
    }
    
    // add pair separated by :
    public void addPair(String values){
        String[] pair = values.split("\\:");
        if(pair.length==2){
            addPair(pair[0].trim(), pair[1].trim());
        }else if(pair.length>2){
            StringBuilder builder = new StringBuilder();
            for(int i=1; i<pair.length; i++){
                builder.append(pair[i]);
                builder.append(':');
            }
            builder.deleteCharAt(builder.length()-1);
            addPair(pair[0].trim(), builder.toString().trim());
        }
    }
    
    public Set<Entry<String, String>> getCollection(){
        return values.entrySet();
    }
    
    public Map<String, String> getValues(){
        return this.values;
    }
    
    // add pair with key-value
    public void addPair(String key, String value){
        //System.out.println("Adding-> " + key + ":'" + value + "'");
        values.put(key, value);
    }
    
    public String getString(String key){
        return values.containsKey(key) ? values.get(key) : "";
    }
    
    public Boolean getBoolean(String key){
        return values.containsKey(key) ? Boolean.parseBoolean(values.get(key)) : false;
    }
   
    public Double getDouble(String key){
        return values.containsKey(key) ? Double.parseDouble(values.get(key)) : 0;
    }
    
    public Long getLong(String key){
        return values.containsKey(key) ? Long.parseLong(values.get(key)) : 0;
    }
    
    public Integer getInteger(String key){
        return values.containsKey(key) ? Integer.parseInt(values.get(key)) : 0;
    }
    
    public Float getFloat(String key){
        return values.containsKey(key) ? Float.parseFloat(values.get(key)) : 0;
    }
    
    public boolean containsKey(String key){
        return values.containsKey(key);
    }
    
}
