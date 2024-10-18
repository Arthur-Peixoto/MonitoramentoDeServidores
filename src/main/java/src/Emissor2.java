package src;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Emissor2 {

    private final String serverName;
    private final String serviceName;
    private static final String EXCHANGE_NAME = "service_status";
    private ScheduledExecutorService scheduler;

    public Emissor2(String serverName, String serviceName) {
        this.serverName = serverName;
        this.serviceName = serviceName;
    }

    public void start() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        scheduler = Executors.newScheduledThreadPool(1);

        Supplier<String> statusSupplier = () -> {
            int cpuUsage = new Random().nextInt(100);
            String status = cpuUsage > 90 ? "vermelho" : cpuUsage > 70 ? "amarelo" : "azul";
            String message = createMessage(status, cpuUsage);
            return message;
        };

        Runnable publishMessageTask = () -> {
            String message = statusSupplier.get();
            String routingKey = serverName + "." + serviceName;
            try {
                channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes(StandardCharsets.UTF_8));
                System.out.println("Sent: " + message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        scheduler.scheduleAtFixedRate(publishMessageTask, 0, 5, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("Shutting down...");
                scheduler.shutdown();
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
                channel.close();
                connection.close();
                System.out.println("Channel and Connection closed.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    private String createMessage(String status, int cpuUsage) {
        int memoryUsage = new Random().nextInt(100);
        int responseTime = new Random().nextInt(100);
        return "{ \"timestamp\": \"" + Instant.now() + "\", \"service\": \"" + serviceName +
                "\", \"status\": \"" + status + "\", \"server\": \"" + serverName + "\", " +
                "\"metrics\": { \"cpu_usage\": " + cpuUsage + ", \"memory_usage\": " + memoryUsage +
                ", \"response_time\": " + responseTime + " } }";
    }

    public static void main(String[] args) {
        Stream.of(
                new Emissor2("Servidor2", "bancodedados"),
                new Emissor2("Servidor2", "webserver")
        ).forEach(emissor -> {
            try {
                emissor.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
