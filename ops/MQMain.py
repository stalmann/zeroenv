import pika


from zeroops.LocalOperatorChainSpacy import processDocument

busUrl = "amqp://guest:guest@localhost:5672/%2F"

connection = pika.BlockingConnection(pika.URLParameters(busUrl))

ingestChannel = connection.channel()
ingestChannel.exchange_declare(exchange='zeroexchange', exchange_type='direct')
ingestChannel.queue_declare(queue='zq-in', durable=True)
ingestChannel.queue_bind(exchange='zeroexchange', queue='zq-in', routing_key='ze-ingest')

############################################################################################
def callback(ch, method, properties, body_json):
    print(" [x] Received %r" % body_json)

    body_json = processDocument(body_json)

    digestChannel = connection.channel()

    digestChannel.exchange_declare(exchange='zeroexchange', exchange_type='direct')
    digestChannel.queue_declare(queue='zq-out', durable=True)
    digestChannel.queue_bind(exchange='zeroexchange', queue='zq-out', routing_key='zq-out')

    digestChannel.basic_publish(exchange='zeroexchange',
                                routing_key='zq-out',
                                body=body_json
                                )

############################################################################################

ingestChannel.basic_consume(queue='zq-in', on_message_callback=callback, auto_ack=True)

print(' [*] Waiting for messages. To exit press CTRL+C')
ingestChannel.start_consuming()