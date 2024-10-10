package src;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Random;

public class Emissor {

    private final String serverName;
    private final String serviceName;
    private static final String EXCHANGE_NAME = "service_status";

    public Emissor(String serverName, String serviceName) {
        this.serverName = serverName;
        this.serviceName = serviceName;
    }

    public void start() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, "topic");

            while (true) {
                String status = checkServiceStatus();
                String message = createMessage(status);
                String routingKey = serverName + "." + serviceName;
                channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes(StandardCharsets.UTF_8));
                System.out.println("Sent: " + message);

                Thread.sleep(5000);
            }
        }
    }

    private String checkServiceStatus() {

        int cpuUsage = new Random().nextInt(100);
        String status = cpuUsage > 90 ? "vermelho" : cpuUsage > 70 ? "amarelo" : "azul";
        return status;
    }

    private String createMessage(String status) {
        return "{ \"timestamp\": \"" + Instant.now() + "\", \"service\": \"" + serviceName +
                "\", \"status\": \"" + status + "\", \"server\": \"" + serverName + "\" }";
        //tem que fazer isso de metrics ainda que eu n entendi
    }

    public static void main(String[] args) throws Exception {
        Emissor agent = new Emissor("Servidorzinho", "Serviçozinho");
        agent.start();
    }
}