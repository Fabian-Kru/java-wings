package de.fabiankru.javawings.controller;

import de.fabiankru.javawings.model.system.SystemInformation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NodeController {

    @GetMapping(value = "/api/system",produces="application/json")
    public String SystemInformation() {
        SystemInformation si = new SystemInformation();
        si.setArchitecture("Linux");
        si.setOs("Linux");
        si.setCpuCount("42");
        si.setVersion("0.0.1");
        si.setKernelVersion("0.0.1");
        return si.buildJsonString().toJson();
    }

}
