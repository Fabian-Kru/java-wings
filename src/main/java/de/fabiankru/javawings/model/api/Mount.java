package de.fabiankru.javawings.model.api;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

@Data
@NoArgsConstructor
public class Mount {

    private boolean isDefault;

    private String target;

    private String source;

    private boolean readOnly;


    public Document toDocument() {
        Document document = new Document();
        document.put("is_default", isDefault);
        document.put("target", target);
        document.put("source", source);
        document.put("read_only", readOnly);
        return document;
    }
}
