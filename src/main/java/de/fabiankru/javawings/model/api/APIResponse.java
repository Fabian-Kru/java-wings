package de.fabiankru.javawings.model.api;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

@Data
@NoArgsConstructor
public class APIResponse {

    private String state;
    private boolean isSuspended;
    private ResourceUsage utilization;
    private Configuration configuration;

    public Document toDocument() {
        Document document = new Document();
        document.put("state", state);
        document.put("is_suspended", isSuspended);
        document.put("utilization", utilization.toDocument());
        document.put("configuration", configuration.toDocument());
        return document;
    }


}
