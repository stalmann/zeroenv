#!/usr/bin/env python
import pika

busUrl = "amqp://guest:guest@localhost:5672/%2F"

#connection = pika.BlockingConnection(pika.ConnectionParameters(host='localhost'))
connection = pika.BlockingConnection(pika.URLParameters(busUrl))
channel = connection.channel()
channel.exchange_declare(exchange='zeroexchange', exchange_type='direct')
channel.queue_declare(queue='zq-in', durable=True)
channel.queue_bind(exchange='zeroexchange', queue='zq-in', routing_key='ze-ingest')

def callback(ch, method, properties, body):
    print(" [x] Received %r" % body)


channel.basic_consume(queue='zq-in', on_message_callback=callback, auto_ack=True)

print(' [*] Waiting for messages. To exit press CTRL+C')
channel.start_consuming()