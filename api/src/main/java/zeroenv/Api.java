package zeroenv;


import com.google.gson.GsonBuilder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Api extends AbstractVerticle {

    private static final Logger logger = Logger.getLogger(Api.class.getName());

    private static JsonObject verticleConfig = null;

    private HttpServer http;

    private Connection connection;

    public static void main(String[] args) throws Exception {
        JsonObject config = new JsonObject(new String(Files.readAllBytes(Paths.get("zeroapi.json"))));
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(Api.class.getName(), new DeploymentOptions().setConfig(config));
    }

    @Override
    public void start(Future<Void> future) {

        verticleConfig = config();

        final File certfolder = new File(new File("."), verticleConfig.getString("certfoldername"));
        verticleConfig.put("certpath", certfolder.getAbsolutePath());
        certfolder.mkdirs();

        logger.info(verticleConfig.toString());
        connection = MQConnection.getConnection(verticleConfig);

        http = vertx.createHttpServer(createOptions(vertx, verticleConfig)).requestHandler(createRouter(vertx));

        http.listen(verticleConfig.getInteger("port", 8080), result -> {

            startMQConsumerService(connection);

            if (result.succeeded()) {
                future.complete();
            } else {
                future.fail(result.cause());
            }
        });
    }


    private Router createRouter(Vertx vertx) {

        Router router = Router.router(vertx);
        // Enable multipart form data parsing
        router.post("/document").handler(BodyHandler.create()
                .setUploadsDirectory(verticleConfig.getString("uploadlocation")));

        router.post("/document").handler(ctx -> {
            ctx.response().end(sendUploads(ctx).encodePrettily());
        });

        router.route("/result/*").handler(StaticHandler.create(verticleConfig.getString("downloadlocation")));
        return router;
    }


    private static Map<String, String> metadata(FileUpload fu) {

        Map<String, String> message = new HashMap<String, String>();

        message.put("mimetype", fu.contentType());
        message.put("filesize", fu.size() + "");
        message.put("filename", fu.uploadedFileName());
        message.put("charset", fu.charSet());
        message.put("ops", new JsonArray().add(System.currentTimeMillis()).toString());

        return message;
    }


    private static void startMQConsumerService(Connection connection) {

        final File downloads = new File(verticleConfig.getString("downloadlocation"));
        logger.info("download to " + downloads.getAbsolutePath());

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            logger.info("got " + new String(delivery.getBody()));
            final JsonObject message = new JsonObject(new String(delivery.getBody(), "UTF-8"));
            final JsonArray ops = new JsonArray(message.getString("ops"));

            ops.add(System.currentTimeMillis());
            message.put("ops", ops);

            downloads.mkdirs();

            final File target = new File(downloads, message.getString("messageId"));
            final BufferedWriter writer = new BufferedWriter(new FileWriter(target));

            writer.write(message.encodePrettily());
            writer.close();
        };
        logger.info("StartING consumer ...");
        try {
            MQConnection.createDurableDirect(connection, "zeroexchange", "zq-out", "api-out")
                    .basicConsume("zq-out", true, deliverCallback, consumerTag -> {
                        //logger.info("Consume");
                    });
        } catch (Exception e) {
            throw new RuntimeException("OUTBOUND MQ Consumer failed.", e);
        }
    }

    private JsonArray sendUploads(RoutingContext ctx) {
        final Channel channel = MQConnection.createDurableDirect(connection, "zeroexchange", "zq-in", "api-in");

        final JsonArray results = new JsonArray();

        for (FileUpload f : ctx.fileUploads()) {
            logger.info("uploading " + f.uploadedFileName());

            final String messageId = f.uploadedFileName().substring(verticleConfig.getString("uploadlocation").length() + 1);
            final Map<String, String> message = metadata(f);

            message.put("messageId", messageId);
            message.put("document", vertx.fileSystem().readFileBlocking(f.uploadedFileName()).toString(Charset.forName("utf-8")));

            try {
                String json = new GsonBuilder().create().toJson(message);
                logger.info("sending " + json.length() + "bytes");
                channel.basicPublish("zeroexchange", "ze-ingest", null, json.getBytes("UTF-8"));
                results.add(messageId);

                logger.info("done: " + messageId);
                vertx.fileSystem().deleteBlocking(f.uploadedFileName());
            } catch (Exception e) {
                throw new RuntimeException("sendUploads failed: " + new GsonBuilder().create().toJson(message), e);
            }
        }
        return results;
    }

    private HttpServerOptions createOptions(Vertx vertx, JsonObject config) {

        final HttpServerOptions serverOptions = new HttpServerOptions();

        if (config.getBoolean("ssl"))
            serverOptions.setSsl(true)
                    .setKeyCertOptions(new PemKeyCertOptions().setCertPath(verticleConfig.getString("certpath") + "/server-cert.pem").setKeyPath(verticleConfig.getString("certpath") + "/server-key.pem"))
                    .setUseAlpn(false);

        return serverOptions;
    }


}