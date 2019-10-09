#docker run -p 8088:8080 --rm -it zerobatch:latest 
#docker run -d --hostname rabbithost -p 15672:15672 --name some-rabbit zerobatch:latest  
#rabbitmq:3-management
#bash

docker run -d --name some-rabbit -p 5672:5672 -p 5673:5673 -p 15672:15672 zerobatch:latest