package com.arenagamer.api.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${mq.exchange.arenagamer}")
    private String exchange;

    @Value("${mq.queue.payment-processing}")
    private String paymentQueue;

    @Value("${mq.queue.bracket-generation}")
    private String bracketQueue;

    @Value("${mq.queue.notification}")
    private String notificationQueue;

    @Value("${mq.routing-key.payment}")
    private String paymentRoutingKey;

    @Value("${mq.routing-key.bracket}")
    private String bracketRoutingKey;

    @Value("${mq.routing-key.notification}")
    private String notificationRoutingKey;

    @Bean
    public TopicExchange arenaGamerExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue paymentQueue() {
        return QueueBuilder.durable(paymentQueue).build();
    }

    @Bean
    public Queue bracketQueue() {
        return QueueBuilder.durable(bracketQueue).build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(notificationQueue).build();
    }

    @Bean
    public Binding paymentBinding() {
        return BindingBuilder.bind(paymentQueue()).to(arenaGamerExchange()).with(paymentRoutingKey);
    }

    @Bean
    public Binding bracketBinding() {
        return BindingBuilder.bind(bracketQueue()).to(arenaGamerExchange()).with(bracketRoutingKey);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(arenaGamerExchange()).with(notificationRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
