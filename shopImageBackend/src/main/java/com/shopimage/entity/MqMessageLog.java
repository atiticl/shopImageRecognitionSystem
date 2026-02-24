package com.shopimage.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mq_message_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqMessageLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Lob
    @Column(name = "message_body", nullable = false)
    private String messageBody;

    @Column(name = "status", length = 16)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}