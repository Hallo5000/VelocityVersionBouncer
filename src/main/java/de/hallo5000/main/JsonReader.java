package de.hallo5000.main;

import jakarta.json.Json;
import jakarta.json.stream.JsonParser;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * This class provides methods to read values from JSON strings that may be truncated at the end.
 * Values can be read as long as the relevant part is valid JSON and the requested key and its value are present in the input.
 * It uses the Jakarta Streaming API.
 */
public class JsonReader {

    private final VelocityVersionBouncer plugin;

    public JsonReader(VelocityVersionBouncer plugin){
        this.plugin = plugin;
    }

    /**
     * Moves the parser according to the path array.
     * @param parser a <code>JsonParser</code> with the string to scan
     * @param path the path to find
     * @return whether the specified path (and the associated value) were found in the JSON
     */
    public boolean findKeyInJson(JsonParser parser, String[] path){
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

    /**
     * When the parser is at the start of a JSON object/array this method moves the parser to the end of it.
     * @param parser the <code>JsonParser</code> to operate
     */
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
     * Reads an object from a JSON formatted string even if a part is missing at the end.
     * Supported value types are <code>null</code>, <code>boolean</code>, <code>String</code> or <code>BigDecimal</code>.
     * @param json valid JSON as a string (can be not-finished)
     * @param path a string array containing every key in the path to the one trying to be found
     * @return an <code>Optional</code> containing the value at the specified path or empty if not found or the value is null
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
            plugin.getLogger().error(plugin.getMessage("error-occurred"),ex);
        }
        return Optional.empty();
    }

    /**
     * Goes to the key specified by <code>path</code> and returns an <code>Optional</code> possibly containing a JSON object.
     * @param json the JSON string to search the path in
     * @param path the path in the JSON string to find the JSON object at
     * @return an <code>Optional</code> containing the JSON object or <code>Optional.empty()</code> if no JSON object was found
     */
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
            plugin.getLogger().error(plugin.getMessage("error-json", ex.toString()));
        }
        return Optional.empty();
    }

    /**
     * Builds a string containing the JSON object the <code>JsonParser</code> is at.
     * @param builder the <code>StringBuilder</code> to build the JSON string with
     * @param parser the <code>JsonParser</code> to get the JSON object from
     */
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

    /**
     * Uses {@link #getObjectFromJson(String, String[])} and returns the value as {@link Boolean}.
     * @param json the JSON from which the {@link Boolean} should be parsed
     * @param path the path at which the parser should search
     * @return an {@link Optional} containing the parsed {@link Boolean} or <code>Optional.empty()</code> if the value is not found, is null or is not a <code>boolean</code>
     */
    public Optional<Boolean> getBooleanFromJson(String json, String[] path){
        return getObjectFromJson(json, path).map(o -> o instanceof Boolean ? (Boolean) o : null);
    }

    /**
     * Uses {@link #getObjectFromJson(String, String[])} and returns the value as {@link String}.
     * @param json the JSON from which the {@link String} should be parsed
     * @param path the path at which the parser should search
     * @return an {@link Optional} containing the parsed {@link String} or <code>Optional.empty()</code> if the value is not found, is null or is not a <code>String</code>
     */
    public Optional<String> getStringFromJson(String json, String[] path){
        return getObjectFromJson(json, path).map(o -> o instanceof String ? (String) o : null);
    }

    /**
     * Uses {@link #getObjectFromJson(String, String[])} and returns the value as {@link BigDecimal}.
     * @param json the JSON from which the {@link BigDecimal} should be parsed
     * @param path the path at which the parser should search
     * @return an {@link Optional} containing the parsed {@link BigDecimal} or <code>Optional.empty()</code> if the value is not found, is null or is not a <code>BigDecimal</code>
     */
    public Optional<BigDecimal> getBigDecimalFromJson(String json, String[] path){
        return getObjectFromJson(json, path).map(o -> o instanceof BigDecimal ? (BigDecimal) o : null);
    }

    /**
     * Calls {@link #getBigDecimalFromJson(String, String[])} and returns it as an {@code Optional<Integer>}.
     * @param json the JSON from which the {@link Integer} should be parsed
     * @param path the path at which the parser should search
     * @return an {@link Optional} containing the parsed {@link Integer} or <code>Optional.empty()</code> if the value is not found, is null or is not an <code>Integer</code>
     * @throws ArithmeticException if the JSON number cannot be represented exactly as an {@link Integer}
     */
    public Optional<Integer> getIntegerFromJson(String json, String[] path){
        return getBigDecimalFromJson(json, path).map(BigDecimal::intValueExact);
    }

    /**
     * Calls {@link #getIntegerFromJson(String, String[])} and returns it as an {@link OptionalInt}.
     * @param json the JSON from which the <code>int</code> should be parsed
     * @param path the path at which the parser should search
     * @return an {@link Optional} containing the parsed <code>int</code> or <code>Optional.empty()</code> if the value is not found, is null or is not an <code>int</code>
     * @throws ArithmeticException if the JSON number cannot be represented exactly as an {@code int}
     */
    public OptionalInt getIntFromJson(String json, String[] path){
        return getIntegerFromJson(json, path).map(OptionalInt::of).orElse(OptionalInt.empty());
    }

}
