package de.fabiankru.javawings.model.backup;

import lombok.Data;
import org.bson.Document;

import java.util.Arrays;
import java.util.List;

@Data
public class BackupRequest {

    @Data
    static
    class BackupPart {
        private String name;
    }

    private String checksum;
    private String checksumType;
    private long size;
    private boolean successful;
    private List<BackupPart> parts;


    public static Document convertToDocument(BackupRequest backupRequest) {
        Document document = new Document()
                .append("checksum", backupRequest.getChecksum())
                .append("checksum_type", backupRequest.getChecksumType())
                .append("size", backupRequest.getSize())
                .append("successful", backupRequest.isSuccessful());

        // Konvertiere die Liste von BackupPart-Objekten in eine Liste von Dokumenten
        if (backupRequest.getParts() != null && !backupRequest.getParts().isEmpty()) {
            document.append("parts", Arrays.asList(convertPartsToDocuments(backupRequest.getParts())));
        }

        return document;
    }

    private static Document[] convertPartsToDocuments(List<BackupPart> backupParts) {
        return backupParts.stream()
                .map(backupPart -> new Document("name", backupPart.getName()))
                .toArray(Document[]::new);
    }

}


