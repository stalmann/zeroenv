package zeroenv;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class MQConnection {

    private MQConnection() {
    }

    public static Connection connect(String amqpUri) {
        try {

            System.out.println("connect to " + amqpUri);
            final ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(amqpUri);
            final Connection connection = factory.newConnection();

            Runtime.getRuntime().addShutdownHook(new Thread()
            {
                public void run()
                {
                    System.out.println("Shutdown Hook is running !");
                    try {

                        connection.close();

                    } catch (IOException e) {
                        System.out.println("Exception on close: " + e.toString());
                    }
                }
            });

            return connection;

        } catch (Exception e) {
            throw new RuntimeException("MQ Connection failed, uri: " + amqpUri);
        }
    }

    public static Channel createChannel(Connection mqConnection, String exchangeName, String queueName, String routingKey) {
        try {

            final Channel channel = mqConnection.createChannel();
            channel.exchangeDeclare(exchangeName, "direct", true);
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, exchangeName, routingKey);

            return channel;

        }catch(Exception e) {
            throw new RuntimeException("Channel declareAndBindQueue failed." + e.getMessage());
        }
    }

}
