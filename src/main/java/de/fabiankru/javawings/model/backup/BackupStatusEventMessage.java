package de.fabiankru.javawings.model.backup;


import lombok.Getter;
import lombok.Setter;
import org.bson.Document;

@Getter
@Setter
public class BackupStatusEventMessage {

    private String uuid;
    private boolean is_success;
    private String checksum;
    private String checksum_type;
    private int file_size;

    public Document convertToDocument() {
        Document document = new Document();
        document.append("uuid", getUuid());
        document.append("is_success", is_success());
        document.append("checksum", getChecksum());
        document.append("checksum_type", getChecksum_type());
        document.append("file_size", getFile_size());
        return document;
    }

}
