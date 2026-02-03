package com.datacentric.timesense.utils.hibernate;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.datacentric.exceptions.DataCentricException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = Message.Serializer.class)
@JsonDeserialize(using = Message.Deserializer.class)
public class Message implements Serializable {

    public static final String MESSAGE_CODE = "message_code";
    public static final String MESSAGE_ARGS = "message_args";

    private static final long serialVersionUID = 1L;

    private String messageCode;
    private List<String> messageArgs;

    public Message() {
    }

    public Message(String messageCode) {
        this.messageCode = messageCode;
    }

    public Message(String messageCode, List<String> messageArgs) {
        this.messageCode = messageCode;
        this.messageArgs = messageArgs;
    }

    public String getMessageCode() {
        return this.messageCode;
    }

    public void setMessageCode(String messageCode) {
        this.messageCode = messageCode;
    }

    public List<String> getMessageArgs() {
        return messageArgs;
    }

    public void setMessageArgs(List<String> messageArgs) {
        this.messageArgs = messageArgs;
    }

    public Message deepCopy() {
        Message copy = new Message();
        copy.messageCode = this.messageCode;
        copy.messageArgs = this.messageArgs;

        return copy;
    }

    public String getSerializedObject() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Serialize the object to JSON
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static class Serializer extends JsonSerializer<Message> {

        @Override
        public void serialize(
                Message value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            Map<String, Object> map = new HashMap<>();
            map.put(MESSAGE_CODE, value.messageCode);
            map.put(MESSAGE_ARGS, value.messageArgs);
            serializers.defaultSerializeValue(map, gen);
        }

    }

    public static class Deserializer extends JsonDeserializer<Message> {

        @Override
        public Message deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            Map<String, Object> c = p.readValueAs(new TypeReference<Map<String, Object>>() {
            });
            return new Message((String) c.get(MESSAGE_CODE), (List<String>) c.get(MESSAGE_ARGS));
        }

    }

    public static Message mapMessage(String messageStr) throws DataCentricException {
        if (messageStr == null) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(messageStr, Message.class);
        } catch (IOException e) {
            throw new DataCentricException("Failed to parse message JSON: " + messageStr, e);
        }
    }

    public static String toJsonString(Message message) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> map = new HashMap<>();
            map.put(MESSAGE_CODE, message.messageCode);
            map.put(MESSAGE_ARGS, message.messageArgs);
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing Message object to JSON string", e);
        }
    }
}
