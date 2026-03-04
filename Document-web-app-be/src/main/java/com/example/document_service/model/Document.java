package com.example.document_service.model;
import java.util.Date;

import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonRepresentation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    @BsonId
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String id;
    private long userId;
    private String filename;
    private String content;
    private Date uploadDate;
    private Date lastModified;
    private DocumentMetadata metadata;
}
