package zeroenv;


import com.google.gson.Gson;
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

public class Api extends AbstractVerticle {


    private static Connection mqConnection = null;

    //amqp://userName:password@hostName:portNumber/virtualHost
    //amqp://guest:guest@hostName:5672/

    private HttpServer http;


    public static void main(String[] args) throws Exception {
        JsonObject config = new JsonObject(new String(Files.readAllBytes(Paths.get("zeroapi.json"))));
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(Api.class.getName(), new DeploymentOptions().setConfig(config));
    }

    private static Map<String, String> metadata(FileUpload fu) {

        Map<String, String> message = new HashMap<String, String>();

        try {
            message.put("mimetype", fu.contentType());
            message.put("filesize", fu.size() + "");
            message.put("filename", fu.uploadedFileName());
            message.put("charset", fu.charSet());
            message.put("ops", new JsonArray().add(System.currentTimeMillis()).toString());
        } catch (Exception e) {
            message.put("error-mdread", e.toString());
        }

        return message;
    }

    private static void startReceiver(JsonObject config) {

        final Gson gson = new GsonBuilder().create();

        checkConnection(config);

        try {
            Channel channel = mqConnection.createChannel();
            channel.queueDeclare(config.getString("responsequeue", "responsequeue"), false, false, false, null);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                //final String messageString = new String(delivery.getBody(), "UTF-8");
                try {
                    final JsonObject message = new JsonObject(new String(delivery.getBody(), "UTF-8"));
                    final JsonArray ops = new JsonArray(message.getString("ops"));
                    ops.add(System.currentTimeMillis());
                    message.put("ops", ops);
                    final File downloads = new File(config.getString("downloadlocation"));
                    downloads.mkdirs();
                    final File target = new File(downloads, message.getString("messageId") + ".json");
                    final BufferedWriter writer = new BufferedWriter(new FileWriter(target));
                    writer.write(message.encodePrettily());
                    writer.close();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            };
            channel.basicConsume(config.getString("responsequeue", "responsequeue"), true, deliverCallback, consumerTag -> {
            });
        } catch (Exception e) {
            System.out.println(e.getCause());
        }
    }

    private static void checkConnection(JsonObject config) {
        int counter = 0;
        while (mqConnection == null || !mqConnection.isOpen()) {
            try {
                Thread.sleep(100 * counter++);
            } catch (Exception e) {
            }
            System.out.println("creating new mq connection");
            try {
                mqConnection = MQConnection.connect(config.getString("mquri"));
                System.out.println("connected");
            } catch (Exception e) {
                System.out.println(e.getCause());

            }
        }
    }

    @Override
    public void start(Future<Void> future) {

        final File certfolder = new File(new File("."), config().getString("certfoldername"));
        config().put("certpath", certfolder.getAbsolutePath());
        certfolder.mkdirs();
        System.out.println(config().toString());

        http = vertx.createHttpServer(createOptions(vertx, config())).requestHandler(createRouter(vertx));
        http.listen(config().getInteger("port", 8080), result -> {

            startReceiver(config());

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
                .setUploadsDirectory(config().getString("uploadlocation")));

        router.post("/document").handler(ctx -> {
            checkConnection(config());
            Channel channel = null;
            try {
                channel = mqConnection.createChannel();
                channel.queueDeclare(config().getString("requestqueue"), false, false, false, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ctx.response().end(sendUploads(ctx, channel).encodePrettily());
        });

        router.route("/result/*").handler(StaticHandler.create(config().getString("downloadlocation")));
        //router.route().failureHandler(ErrorHandler.create(true));

        return router;
    }

    private JsonArray sendUploads(RoutingContext ctx, Channel channel) {

        final Gson gson = new GsonBuilder().create();
        final JsonArray results = new JsonArray();
        for (FileUpload f : ctx.fileUploads()) {
            final String messageId = f.uploadedFileName().substring(config().getString("uploadlocation").length() + 1);
            Map<String, String> message = metadata(f);
            message.put("messageId", messageId);
            message.put("document", vertx.fileSystem().readFileBlocking(f.uploadedFileName()).toString(Charset.forName("utf-8")));

            try {
                channel.basicPublish("", config().getString("requestqueue"), null, gson.toJson(message).getBytes("UTF-8"));
                results.add(messageId);
                vertx.fileSystem().deleteBlocking(f.uploadedFileName());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(gson.toJson(message));
            }
        }
        return results;
    }

    private HttpServerOptions createOptions(Vertx vertx, JsonObject config) {
        HttpServerOptions serverOptions = new HttpServerOptions();
        if (config.getBoolean("ssl"))
            serverOptions.setSsl(true)
                    .setKeyCertOptions(new PemKeyCertOptions().setCertPath(config().getString("certpath") + "/server-cert.pem").setKeyPath(config().getString("certpath") + "/server-key.pem"))
                    .setUseAlpn(false);

        return serverOptions;
    }


}