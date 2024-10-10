package src;
import com.rabbitmq.client.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Assinante {

    private static final String EXCHANGE_NAME = "service_status";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "topic");
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "#");

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + message + "'");
            handleServiceStatus(message);
        };

        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
    }

    private static void handleServiceStatus(String message) {
        if (message.contains("vermelho") || message.contains("amarelo")) {
            generateWorkOrder(message);
        }
    }

    private static void generateWorkOrder(String message) {
        System.out.println("Gerando ordem de serviço: " + message);
        // tem que fazer enviar mensagem para a fila ainda
    }
}