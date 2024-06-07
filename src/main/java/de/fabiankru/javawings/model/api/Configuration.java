package de.fabiankru.javawings.model.api;

import lombok.Data;
import org.bson.Document;

import java.util.List;
import java.util.Map;

@Data
public class Configuration {

    private String uuid;

    private ConfigurationMeta meta;

    private boolean crashDetectionEnabled;
    private boolean suspended;

    private String invocation;

    private boolean skipEggScripts;

    private Map<String, String> envVars;

    private Map<String, String> labels;

    private Allocations allocations;

    private Limits build;

    private List<Document> mounts;

    private EggConfiguration egg;

    public Document toDocument() {
        Document document = new Document();
        document.put("uuid", uuid);
        document.put("meta", meta.toDocument());
        document.put("crash_detection_enabled", crashDetectionEnabled);
        document.put("suspended", suspended);
        document.put("invocation", invocation);
        document.put("skip_egg_scripts", skipEggScripts);
        document.put("env_vars", envVars);
        document.put("labels", labels);
        document.put("allocations", allocations.toDocument());
        document.put("build", build.toDocument());
        document.put("mounts", mounts);
        document.put("egg", egg.toDocument());
        return document;
    }

}
