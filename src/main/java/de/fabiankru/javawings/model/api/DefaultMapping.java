package de.fabiankru.javawings.model.api;

import lombok.Data;
import org.bson.Document;

@Data
public class DefaultMapping {

    private String ip;
    private int port;

    public Document toDocument() {
        Document document = new Document();
        document.put("ip", ip);
        document.put("port", port);
        return document;
    }

}
