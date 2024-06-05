package de.fabiankru.javawings.model.api;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class Allocations {

    private boolean forceOutgoingIp = false;
    private DefaultMapping defaultMapping;
    private Map<String, List<Integer>> mappings;

    public Document toDocument() {
        Document document = new Document();
        document.put("force_outgoing_ip", forceOutgoingIp);
        document.put("default_mapping", defaultMapping.toDocument());
        document.put("mappings", mappings);
        return document;
    }

}
