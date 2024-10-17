package src;
import com.rabbitmq.client.BuiltinExchangeType;
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
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

            while (true) {
                int cpuUsage = new Random().nextInt(100);
                String status = cpuUsage > 90 ? "vermelho" : cpuUsage > 70 ? "amarelo" : "azul";
                String message = createMessage(status, cpuUsage);
                String routingKey = serverName + "." + serviceName;
                channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes(StandardCharsets.UTF_8));
                System.out.println("Sent: " + message);

                Thread.sleep(5000);
            }
        }
    }

    private String createMessage(String status, int cpuUsage) throws Exception {
        int memoryUsage = new Random().nextInt(100);
        int responseTime = new Random().nextInt(100);
        return "{ \"timestamp\": \"" + Instant.now() + "\", \"service\": \"" + serviceName +
                "\", \"status\": \"" + status + "\", \"server\": \"" + serverName + "\", " +
                "\"metrics\": { \"cpu_usage\": " + cpuUsage + ", \"memory_usage\": " + memoryUsage +
                ", \"response_time\": " + responseTime + " } }";
    }

    public static void main(String[] args) throws Exception {
        Emissor agent = new Emissor("Servidorzinho", "Serviçozinho");
        agent.start();
    }
}