package com.labourse.chat.dto;

import lombok.Data;

@Data
public class IncomingMessage {
    private Long jobId;
    private Long receiverId;
    private String content;
}
