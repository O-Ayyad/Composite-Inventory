package constants;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.LocalDateTime;

public final class Constants{
    private Constants() {}

    public static final String NOT_FOUND_PNG = "src/main/resources/icons/itemIcons/imageNotFound.png";

    public static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return LocalDateTime.parse(json.getAsString());
        }
    }
    public LocalDateTime cutoffTime = null;
}
