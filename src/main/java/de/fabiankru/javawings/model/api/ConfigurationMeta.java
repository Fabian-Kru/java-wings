package de.fabiankru.javawings.model.api;

import lombok.Data;
import org.bson.Document;

@Data
public class ConfigurationMeta {

    private String name;
    private String description;

    public Document toDocument() {
        Document document = new Document();
        document.put("name", name);
        document.put("description", description);
        return document;
    }

}
