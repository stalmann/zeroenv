# Extend vert.x image
FROM vertx/vertx3

EXPOSE 8080

# Copy your verticle to the container 
COPY ./zeroapi.jar  /zeroapi/
COPY ./zeroapi.json  /zeroapi/
COPY ./tls/  /zeroapi/tls/

# Launch the verticle
WORKDIR /zeroapi
ENTRYPOINT ["sh", "-c"]
CMD ["exec vertx run zeroenv.Api -cp /zeroapi/zeroapi.jar"]