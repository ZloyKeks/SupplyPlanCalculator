package by.legan.gt_tss.supplyplancalculator.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Класс конфигурации для WebSocket брокера сообщений и STOMP конечных точек.
 * Включает обмен сообщениями в реальном времени между сервером и клиентами.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Настраивает брокер сообщений для WebSocket связи.
     * Включает простой брокер в памяти для топиков и устанавливает префиксы назначения приложения.
     *
     * @param config реестр брокера сообщений для настройки
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Регистрирует STOMP конечные точки для WebSocket соединений.
     * Настраивает конечную точку для разрешения всех источников и включает резервный вариант SockJS.
     *
     * @param registry реестр STOMP конечных точек
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/websocket")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

}