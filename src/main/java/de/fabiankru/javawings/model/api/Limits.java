package de.fabiankru.javawings.model.api;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.Document;

@Data
@NoArgsConstructor
public class Limits {

    private long memoryLimit;

    private long swap;

    private int ioWeight;

    private long cpuLimit;

    private long diskSpace;

    private String threads;

    private boolean oomDisabled;

    // Constructor
    public Limits(long memoryLimit, long swap, int ioWeight, long cpuLimit, long diskSpace, String threads, boolean oomDisabled) {
        this.memoryLimit = memoryLimit;
        this.swap = swap;
        this.ioWeight = ioWeight;
        this.cpuLimit = cpuLimit;
        this.diskSpace = diskSpace;
        this.threads = threads;
        this.oomDisabled = oomDisabled;
    }

    public Document toDocument() {
        Document document = new Document();
        document.put("memory_limit", memoryLimit);
        document.put("swap", swap);
        document.put("io_weight", ioWeight);
        document.put("cpu_limit", cpuLimit);
        document.put("disk_space", diskSpace);
        document.put("threads", threads);
        document.put("oom_disabled", oomDisabled);
        return document;
    }
}
