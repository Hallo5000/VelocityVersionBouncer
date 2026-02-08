package de.hallo5000.main;

import jakarta.json.Json;
import jakarta.json.stream.JsonParser;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * This class provides a few methods to read values from json strings which can be broken (e.g. some part is missing at the end).
 * They will work as long as the existing part is proper json and the key and value of the entry that is searched exists.
 * It uses the Jakarta Streaming API
 */
public class JsonReader {

    VelocityVersionBouncer plugin;
    public JsonReader(VelocityVersionBouncer plugin){
        this.plugin = plugin;
    }

    private boolean findKeyInJson(JsonParser parser, String[] path){
        if(path == null || path.length == 0) return false;
        int next_i = 0;
        JsonParser.Event event;
        while(parser.hasNext()){
            event = parser.next();
            if(event == JsonParser.Event.KEY_NAME){
                String key = parser.getString();
                if(key.equals(path[next_i])){
                    next_i++;
                    if(path.length == next_i) return parser.hasNext();
                    else{
                        event = parser.next();
                        if(event != JsonParser.Event.START_OBJECT) break;
                    }
                }
                else{
                    if(parser.hasNext()) event = parser.next(); //skip wrong key
                    if(event == JsonParser.Event.START_OBJECT || event == JsonParser.Event.START_ARRAY){//wrong key -> forward to end of object
                        skipElement(parser);
                    }
                }
            }
        }
        if(parser.hasNext()) parser.next();
        return false;
    }

    private void skipElement(JsonParser parser){
        JsonParser.Event event = parser.currentEvent();
        if(event != JsonParser.Event.START_OBJECT
                && event != JsonParser.Event.START_ARRAY) return;

        while(parser.hasNext()){
            event = parser.next();
            if(event == JsonParser.Event.END_OBJECT || event == JsonParser.Event.END_ARRAY) break;
            if(event == JsonParser.Event.START_OBJECT || event == JsonParser.Event.START_ARRAY) skipElement(parser);
        }
    }

    /**
     * Reads an int from a json formatted string even if a part is missing at the end
     * @param json valid json as a string (can be not-finished)
     * @param path a string array containing every key in the path to the one trying to be found
     * @return an <code>Optional</code> containing the value to the path or empty if not found or null
     */
    public Optional<Object> getObjectFromJson(String json, String[] path){
        if(json == null) return Optional.empty();
        try(JsonParser parser = Json.createParser(new StringReader(json))){
            if(findKeyInJson(parser, path)) {
                JsonParser.Event event = parser.next();
                if(event == JsonParser.Event.VALUE_NULL) return Optional.empty(); //probably not needed
                if(event == JsonParser.Event.VALUE_FALSE) return Optional.of(Boolean.FALSE);
                if(event == JsonParser.Event.VALUE_TRUE) return Optional.of(Boolean.TRUE);
                if(event == JsonParser.Event.VALUE_STRING) return Optional.of(parser.getString());
                if(event == JsonParser.Event.VALUE_NUMBER) return Optional.of(parser.getBigDecimal());
            }
        }catch(Exception ex){
            plugin.getLogger().error("Some error occurred while trying to read some object from a json string: ", ex);
        }
        return Optional.empty();
    }

    public Optional<String> getJsonFromJson(String json, String[] path){
        if(json == null) return Optional.empty();
        try(JsonParser parser = Json.createParser(new StringReader(json))){
            if(findKeyInJson(parser, path)) {
                JsonParser.Event event = parser.next(); //go to object
                if(event != JsonParser.Event.START_OBJECT && event != JsonParser.Event.START_ARRAY) return Optional.empty();
                StringBuilder builder = new StringBuilder();
                buildJson(builder, parser);
                return Optional.of(builder.toString());
            }
        }catch(Exception ex){
            plugin.getLogger().error("Some error occurred while trying to read a json object from another json string: ", ex);
        }
        return Optional.empty();
    }

    private void buildJson(StringBuilder builder, JsonParser parser){
        JsonParser.Event event = parser.currentEvent();
        if(event == JsonParser.Event.START_OBJECT) builder.append("{");
        if(event == JsonParser.Event.START_ARRAY) builder.append("[");
        boolean first = true;
        while(parser.hasNext()){
            event = parser.next();
            if(event == JsonParser.Event.END_OBJECT){
                builder.append("}");
                return;
            }
            if(event == JsonParser.Event.END_ARRAY){
                builder.append("]");
                return;
            }
            if(!first) builder.append(",");
            first = false;
            if(event == JsonParser.Event.KEY_NAME){
                builder.append("\"").append(parser.getString()).append("\":");
                event = parser.next();
            }
            if(event == JsonParser.Event.START_OBJECT || event == JsonParser.Event.START_ARRAY) buildJson(builder, parser);
            if(event == JsonParser.Event.VALUE_NULL) builder.append("null");
            if(event == JsonParser.Event.VALUE_NUMBER) builder.append(parser.getBigDecimal().toPlainString());
            if(event == JsonParser.Event.VALUE_FALSE) builder.append("false");
            if(event == JsonParser.Event.VALUE_TRUE) builder.append("true");
            if(event == JsonParser.Event.VALUE_STRING) builder.append("\"").append(parser.getString()).append("\"");
        }
    }

    public Optional<Boolean> getBooleanFromJson(String json, String[] path){
        return getObjectFromJson(json, path).map(o -> o instanceof Boolean ? (Boolean) o : null);
    }

    public Optional<String> getStringFromJson(String json, String[] path){
        return getObjectFromJson(json, path).map(o -> o instanceof String ? (String) o : null);
    }

    public Optional<BigDecimal> getBigDecimalFromJson(String json, String[] path){
        return getObjectFromJson(json, path).map(o -> o instanceof BigDecimal ? (BigDecimal) o : null);
    }

    public Optional<Integer> getIntegerFromJson(String json, String[] path){
        return getBigDecimalFromJson(json, path).map(BigDecimal::intValueExact);
    }

    public OptionalInt getIntFromJson(String json, String[] path){
        return getIntegerFromJson(json, path).map(OptionalInt::of).orElse(OptionalInt.empty());
    }

}
