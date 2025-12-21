package org.rque.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailDTO {
    // These names must match the Producer's JSON keys exactly
    private String from;
    private String to;
    private String subject;
    private String body;
    private long createdAt;
}