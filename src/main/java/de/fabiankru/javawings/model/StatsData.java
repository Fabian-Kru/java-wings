package de.fabiankru.javawings.model;

import lombok.Data;
import org.bson.Document;

@Data
public class StatsData {
    private double memory;
    private double cpu;
    private long disk;
    private long tx;
    private long rx;
    private long uptime;

    public StatsData(double memory, double cpu, long disk, long tx, long rx, long uptime) {
        this.memory = memory;
        this.cpu = cpu;
        this.disk = disk;
        this.tx = tx;
        this.rx = rx;
        this.uptime = uptime;
    }

    public Document toDocument() {
        return new Document()
                .append("memory_bytes", memory)
                .append("cpu_absolute", cpu)
                .append("disk_bytes", disk)
                .append("uptime", (int) uptime)
                .append("network", new Document("tx_bytes", tx).append("rx_bytes",rx));
    }


}
