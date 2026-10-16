package com.labourse.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = @Index(name = "idx_job_id", columnList = "jobId"))
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long jobId;
    private Long senderId;
    private Long receiverId;

    @Column(length = 1000)
    private String content;

    private boolean delivered = false;
    private boolean read = false;

    private LocalDateTime sentAt = LocalDateTime.now();
}
