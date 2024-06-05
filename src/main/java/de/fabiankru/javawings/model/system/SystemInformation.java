package de.fabiankru.javawings.model.system;


import lombok.*;
import org.bson.Document;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SystemInformation {

    private String architecture;
    private String cpuCount;
    private String kernelVersion;
    private String os;
    private String version;

    public Document buildJsonString() {
        return new Document()
                .append("architecture", getArchitecture())
                .append("cpucount", getCpuCount())
                .append("kernelversion", getKernelVersion())
                .append("os", getOs())
                .append("version", getVersion());
    }

}
