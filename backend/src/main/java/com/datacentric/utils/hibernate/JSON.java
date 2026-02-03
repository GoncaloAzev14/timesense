package com.datacentric.utils.hibernate;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = JSON.Serializer.class)
@JsonDeserialize(using = JSON.Deserializer.class)
public class JSON implements Map<String, Object>, Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, Object> map;

    public JSON() {
        this.map = new HashMap<>();
    }

    public JSON(Map<String, Object> map) {
        this.map = map;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return map.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        assert (value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof JSON
                || value instanceof List);
        return map.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<String> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<Object> values() {
        return map.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return map.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return map.equals(o);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public String toString() {
        return map.toString();
    }

    @SuppressWarnings("unchecked")
    private Object cloneValue(Object value) {
        if (value instanceof Map) {
            Map<String, Object> clonedValue = new HashMap<>();
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                clonedValue.put(entry.getKey(), cloneValue(entry.getValue()));
            }
            return clonedValue;
        } else if (value instanceof List) {
            List<Object> clonedValue = new ArrayList<>();
            for (Object element : (List<Object>) value) {
                clonedValue.add(cloneValue(element));
            }
            return clonedValue;
        } else {
            return value;
        }
    }

    @SuppressWarnings("unchecked")
    public JSON deepCopy() {
        if (map == null) {
            return new JSON();
        }

        return new JSON((Map<String, Object>) cloneValue(map));
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public void setMap(Map<String, Object> map) {
        this.map = map;
    }

    public static class Serializer extends JsonSerializer<JSON> {

        @Override
        public void serialize(JSON value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            serializers.defaultSerializeValue(value.map, gen);
        }

    }

    public static class Deserializer extends JsonDeserializer<JSON> {

        @Override
        public JSON deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            Map<String, Object> c = p.readValueAs(new TypeReference<Map<String, Object>>() {
            });
            return new JSON(c);
        }

    }
}
