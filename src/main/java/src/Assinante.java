package src;
import com.rabbitmq.client.*;
import java.nio.charset.StandardCharsets;

public class Assinante {

    private static final String EXCHANGE_NAME = "service_status";
    private static final String RESPONSE_QUEUE = "response_queue"; // Fila de respostas para o Emissor

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "#");

        // Escutando a fila de manutenção
        channel.queueDeclare("maintenance_queue", false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + message + "'");
            handleServiceStatus(message);
        };

        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });

        // Escutando as respostas dos consumidores
        channel.queueDeclare(RESPONSE_QUEUE, false, false, false, null);
        DeliverCallback responseCallback = (consumerTag, delivery) -> {
            String responseMessage = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received response: '" + responseMessage + "'");
            try {
                forwardToEmissor(responseMessage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        channel.basicConsume(RESPONSE_QUEUE, true, responseCallback, consumerTag -> { });
    }

    private static void handleServiceStatus(String message) {
        if (message.contains("vermelho") || message.contains("amarelo")) {
            try {
                generateWorkOrder(message);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void generateWorkOrder(String message) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare("maintenance_queue", false, false, false, null);
            channel.basicPublish("", "maintenance_queue", null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println("Gerando ordem de serviço: " + message);
        }
    }

    private static void forwardToEmissor(String responseMessage) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            // Publica a resposta para um tópico do Emissor
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
            String routingKey = "response.emissor";
            channel.basicPublish(EXCHANGE_NAME, routingKey, null, responseMessage.getBytes(StandardCharsets.UTF_8));
            System.out.println("Encaminhando resposta para o Emissor: " + responseMessage);
        }
    }
}
