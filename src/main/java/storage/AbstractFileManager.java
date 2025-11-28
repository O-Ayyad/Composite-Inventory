package storage;

import com.google.gson.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class AbstractFileManager {

    public final String dataDirName;
    public final String dataDir;

    protected boolean loading;
    Gson gson;

    public AbstractFileManager(String dataDirName){
        this.dataDirName = dataDirName;
        this.dataDir = new File("data" + File.separator + dataDirName).getAbsolutePath();
        try{

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
                .create();}
        catch (Exception e) {
            System.out.println("Couldnt make GSON for File Managers");
        }

        try {
            Path newDir = Path.of(dataDir);
            Files.createDirectories(newDir);
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
    }

    public abstract void load();
    public abstract void save();

}
