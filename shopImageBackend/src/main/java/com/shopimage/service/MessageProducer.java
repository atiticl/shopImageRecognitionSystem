package com.shopimage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopimage.config.RabbitConfig;
import com.shopimage.entity.MqMessageLog;
import com.shopimage.repository.MqMessageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessageProducer {
    private final RabbitTemplate rabbitTemplate;
    private final MqMessageLogRepository mqMessageLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void send(Map<String, Object> payload, Long taskId) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, body);
            mqMessageLogRepository.save(MqMessageLog.builder()
                    .taskId(taskId)
                    .messageBody(body)
                    .status("SENT")
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            mqMessageLogRepository.save(MqMessageLog.builder()
                    .taskId(taskId)
                    .messageBody(payload.toString())
                    .status("FAILED")
                    .createdAt(LocalDateTime.now())
                    .build());
            throw new RuntimeException(e);
        }
    }

    public void sendBatchProcessMessage(Map<String, Object> payload) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            // 使用不同的路由键来处理批量任务
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, "batch.process", body);
            
            Long taskId = (Long) payload.get("taskId");
            mqMessageLogRepository.save(MqMessageLog.builder()
                    .taskId(taskId)
                    .messageBody(body)
                    .status("SENT")
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            Long taskId = (Long) payload.get("taskId");
            mqMessageLogRepository.save(MqMessageLog.builder()
                    .taskId(taskId)
                    .messageBody(payload.toString())
                    .status("FAILED")
                    .createdAt(LocalDateTime.now())
                    .build());
            throw new RuntimeException(e);
        }
    }
}