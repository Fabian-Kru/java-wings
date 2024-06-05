package de.fabiankru.javawings.model.api;

import lombok.Data;
import org.bson.Document;

@Data
public class EnviormentStats {

    private long memoryBytes;
    private long memoryLimitBytes;
    private float cpuAbsolute;
    private NetworkStats network;
    private long uptime;

    public Document toDocument() {
        Document document = new Document();
        document.put("memory_bytes", memoryBytes);
        document.put("memory_limit_bytes", memoryLimitBytes);
        document.put("cpu_absolute", cpuAbsolute);
        document.put("network", network.toDocument());
        document.put("uptime", uptime);
        return document;
    }
}
