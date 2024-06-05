package de.fabiankru.javawings.model.api;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InstallationScript {

    private String containerImage;
    private String entrypoint;
    private String script;

}
