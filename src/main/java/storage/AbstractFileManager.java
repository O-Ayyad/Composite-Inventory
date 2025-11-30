package storage;

import com.google.gson.*;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public abstract class AbstractFileManager {


    public final String dataDirName;
    public final String dataDir;

    protected boolean loading;
    Gson gson;
    Gson zonedGson;

    public AbstractFileManager(String dataDirName){
        this.dataDirName = dataDirName;
        this.dataDir = new File("data" + File.separator + dataDirName).getAbsolutePath();
        try {

            this.gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                        @Override
                        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                            return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                        }
                    })
                    .registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                        @Override
                        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
                            return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        }
                    })
                    .setPrettyPrinting()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();

            this.zonedGson = new GsonBuilder()
                    .registerTypeAdapter(ZonedDateTime.class, new JsonSerializer<ZonedDateTime>() {
                        @Override
                        public JsonElement serialize(ZonedDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                            if (src == null) {
                                return JsonNull.INSTANCE;
                            }
                            return new JsonPrimitive(src.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
                        }
                    })
                    .registerTypeAdapter(ZonedDateTime.class, (JsonDeserializer<ZonedDateTime>) (json, typeOfT, context) -> {
                        if (json == null || json.isJsonNull()) {
                            return null;
                        }

                        String dateTimeString = json.getAsString();

                        try {
                            return ZonedDateTime.parse(dateTimeString, DateTimeFormatter.ISO_ZONED_DATE_TIME);
                        } catch (Exception e1) {
                            try {
                                return ZonedDateTime.parse(dateTimeString);
                            } catch (Exception e2) {
                                throw new JsonParseException("Failed to parse ZonedDateTime: " + dateTimeString, e2);
                            }
                        }
                    })
                    .setPrettyPrinting()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
        }
        catch (Exception e) {
            System.out.println("Couldnt make GSON for File Managers");
        }

        try {
            Path newDir = Path.of(dataDir);
            Files.createDirectories(newDir);
        }catch (IOException e){
            System.out.println("ERROR: "+e.getMessage());
        }
    }

    public abstract LoadResult load(boolean firstOpen);
    public abstract void save();

    public void showError(String message, boolean firstOpen){
        System.out.println(message);
        if(!firstOpen) {
            JOptionPane.showMessageDialog(null, message, "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public record LoadResult(boolean success, Exception error) {
    }
}
