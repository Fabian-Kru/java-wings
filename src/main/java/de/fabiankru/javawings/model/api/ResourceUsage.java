package de.fabiankru.javawings.model.api;


import lombok.Data;
import org.bson.Document;

@Data
public class ResourceUsage {

    private String state;
    private long diskBytes;
    private EnviormentStats enviormentStats;

    public Document toDocument() {
        Document document = new Document();
        document.put("state", state);
        document.put("disk_bytes", diskBytes);
        document.putAll(enviormentStats.toDocument());
        return document;
    }
}
