package de.fabiankru.javawings.model.api;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.util.List;

@Data
@NoArgsConstructor
public class EggConfiguration {

    private String id;

    private List<String> fileDenylist;

    public Document toDocument() {
        Document document = new Document();
        document.put("id", id);
        document.put("file_denylist", fileDenylist);
        return document;
    }
}
