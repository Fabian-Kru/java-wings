package de.fabiankru.javawings.model.api;


import lombok.Getter;
import lombok.Setter;
import org.bson.Document;

@Getter
@Setter
public class NetworkStats {

    public long rxBytes;
    public long txBytes;

    public Document toDocument() {
        Document document = new Document();
        document.put("rx_bytes", rxBytes);
        document.put("tx_bytes", txBytes);
        return document;
    }
}
