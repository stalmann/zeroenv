#!/usr/bin/env python
import pika
busUrl = "amqp://guest:guest@localhost:5672/%2F"
f=open("/Users/ks/Library/Mobile Documents/com~apple~CloudDocs/Projekte/zero/data/trump.json", "r")
bodyJson =f.read()
print(bodyJson)
connection = pika.BlockingConnection(pika.URLParameters(busUrl))
#connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
channel = connection.channel()

channel.exchange_declare(exchange='zeroexchange', exchange_type='direct')
channel.queue_declare(queue='zq-out', durable=True)
channel.queue_bind(exchange='zeroexchange', queue='zq-out', routing_key='api-out')


channel.basic_publish(exchange='zeroexchange',
                      routing_key='api-out',
                      body=bodyJson
                      )
#print(" [x] Sent 'Hello World!'")

connection.close()
