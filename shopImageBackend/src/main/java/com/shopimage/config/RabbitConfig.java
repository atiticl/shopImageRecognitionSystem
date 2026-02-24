package com.shopimage.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "cls.exchange";
    public static final String QUEUE = "cls.queue";
    public static final String ROUTING_KEY = "cls.route";
    
    // 批量处理队列配置
    public static final String BATCH_QUEUE = "batch.queue";
    public static final String BATCH_ROUTING_KEY = "batch.process";

    @Bean
    public DirectExchange classificationExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue classificationQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding classificationBinding(Queue classificationQueue, DirectExchange classificationExchange) {
        return BindingBuilder.bind(classificationQueue).to(classificationExchange).with(ROUTING_KEY);
    }

    @Bean
    public Queue batchProcessQueue() {
        return new Queue(BATCH_QUEUE, true);
    }

    @Bean
    public Binding batchProcessBinding(Queue batchProcessQueue, DirectExchange classificationExchange) {
        return BindingBuilder.bind(batchProcessQueue).to(classificationExchange).with(BATCH_ROUTING_KEY);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }
}