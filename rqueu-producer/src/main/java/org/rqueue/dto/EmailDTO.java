package org.rqueue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailDTO {
    private String from;
    private String to;
    private String subject;
    private String body;
    private Date createdAt;
}