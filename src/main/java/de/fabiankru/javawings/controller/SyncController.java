package de.fabiankru.javawings.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SyncController {

    @PostMapping("/api/servers/{server}/sync")
    public HttpStatus sync(@PathVariable String server) {
        System.out.println("sync");
        System.out.println(server);
        return HttpStatus.OK;
    }

}
