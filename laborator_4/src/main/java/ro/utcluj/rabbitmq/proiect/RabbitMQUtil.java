package ro.utcluj.rabbitmq.proiect;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMQUtil {

    private static final String RABBITMQ_HOST = "localhost"; // Change this to your RabbitMQ server host
    private static final String USERNAME = "guest"; // Change this to your RabbitMQ username
    private static final String PASSWORD = "guest"; // Change this to your RabbitMQ password

    private ConnectionFactory factory;

    public RabbitMQUtil() {
        factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);
    }

    public Channel createChannel() throws IOException, TimeoutException {
        Connection connection = factory.newConnection();
        return connection.createChannel();
    }

    public void publishMessage(String exchange, String routingKey, byte[] message) throws IOException, TimeoutException {
        try (Channel channel = createChannel()) {
            channel.basicPublish(exchange, routingKey, null, message);
        }
    }

    public void consumeMessage(String queueName, DeliverCallback deliverCallback) throws IOException, TimeoutException {
        try (Channel channel = createChannel()) {
            channel.queueDeclare(queueName, false, false, false, null);
            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
        }
    }
}
