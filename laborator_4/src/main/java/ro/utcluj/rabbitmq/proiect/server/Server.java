package ro.utcluj.rabbitmq.proiect.server;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import ro.utcluj.rabbitmq.proiect.RabbitMQUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public class Server {

    private final RabbitMQUtil rabbitMQUtil;
    private final ConcurrentHashMap<String, String> tickets; // TicketID -> TicketContent
    private static final String TICKET_REQUEST_QUEUE = "ticketRequests";
    private static final String TICKET_UPDATE_EXCHANGE = "ticketUpdates";

    public Server() {
        this.rabbitMQUtil = new RabbitMQUtil();
        this.tickets = new ConcurrentHashMap<>();
    }

    public void start() {
        try {
            setupTicketRequestConsumer();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    private void setupTicketRequestConsumer() throws IOException, TimeoutException {
        Channel channel = rabbitMQUtil.createChannel();
        channel.queueDeclare(TICKET_REQUEST_QUEUE, false, false, false, null);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            // Process the message and update the tickets map
            String[] parts = message.split(":", 2);
            if (parts.length == 2) {
                String ticketId = parts[0];
                String ticketContent = parts[1];
                tickets.put(ticketId, ticketContent);
                publishTicketUpdate(ticketId, ticketContent);
            }
        };

        channel.basicConsume(TICKET_REQUEST_QUEUE, true, deliverCallback, consumerTag -> {});
    }

    private void publishTicketUpdate(String ticketId, String ticketContent) {
        try {
            String message = ticketId + ":" + ticketContent;
            rabbitMQUtil.publishMessage(TICKET_UPDATE_EXCHANGE, "", message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
