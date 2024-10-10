package src;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;

public class Consumidor {

    private static final String QUEUE_NAME = "maintenance_queue";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for maintenance orders.");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received maintenance order: '" + message + "'");

            resolveIssue(message);
        };

        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
    }

    private static void resolveIssue(String workOrder) {
        System.out.println("Resolvendo problema: " + workOrder);
    }
}