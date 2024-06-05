package de.fabiankru.javawings.model;

import lombok.Data;
import org.bson.Document;

@Data
public class FileData {
    private String name;
    private String created;
    private String modified;
    private String mode;
    private String modeBits;
    private long size;
    private boolean directory;
    private boolean file;
    private boolean symlink;
    private String mime;

    public Document convertToDocument() {
        Document document = new Document();
        document.append("name", getName());
        document.append("created", getCreated());
        document.append("modified", getModified());
        document.append("mode", getMode());
        document.append("mode_bits", getModeBits());
        document.append("size", getSize());
        document.append("directory", isDirectory());
        document.append("file", isFile());
        document.append("symlink", isSymlink());
        document.append("mime", getMime());
        return document;
    }
}