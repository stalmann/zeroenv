package zeroenv;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MQConnection {

    private static final Logger logger = Logger.getLogger(MQConnection.class.getName());

    private MQConnection() {
    }

    private static Connection connect(String amqpUri) {
        try {

            logger.info("Connect to " + amqpUri);
            final ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(amqpUri);
            final Connection connection = factory.newConnection();

            Runtime.getRuntime().addShutdownHook(new Thread()
            {
                public void run()
                {
                    logger.info("Shutdown Hook is running !");
                    try {
                        connection.close();
                    } catch (IOException e) {
                    }
                }
            });

            return connection;

        } catch (Exception e) {
            throw new RuntimeException("MQ Connection failed, uri: " + amqpUri);
        }
    }

    public static Connection getConnection(JsonObject config) {
        final String uri = config.getString("mquri");
        Connection mqConnection = null;;
        int counter = 0;
        String cause = "";
        do  {
            logger.info("Try connecting to: " + uri);
            try {
                mqConnection = connect(uri);
                if(mqConnection != null && mqConnection.isOpen())
                    return mqConnection;
            } catch (Exception e) {
                cause = e.getCause().getMessage();
                logger.warning("MQ not connected.");
            }
            try {
                Thread.sleep(300 * counter++);
            } catch (Exception e) {
            }
        } while (++counter < 4);
        throw new RuntimeException("RabbitMQ not available: " + cause);
    }


    public static Channel createDurableDirect(Connection connection, String exchange, String queue, String route) {
        try {

            final Channel channel = connection.createChannel();

            AMQP.Exchange.DeclareOk exc_ok = channel.exchangeDeclare(exchange, "direct");
            logger.info( exc_ok.toString());
            com.rabbitmq.client.AMQP.Queue.DeclareOk que_ok = channel.queueDeclare(queue, true, false, false, null);
            logger.info( que_ok.toString());
            com.rabbitmq.client.AMQP.Queue.BindOk bind_ok = channel.queueBind(queue, exchange, route);
            logger.info( bind_ok.toString());

            return channel;

        } catch(Exception e) {
            throw new RuntimeException("Creating channel (exchange="+exchange+", queue="+queue+", route="+route+") with connection "+  connection +" failed.", e);
        }
    }

}
