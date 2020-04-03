# microservicesmagnus

`
Ver topics: docker-compose exec kafka /opt/kafka/bin/kafka-topics.sh --zookeeper zookeeper --list
Ver particoes: docker-compose exec kafka /opt/kafka/bin/kafka-topics.sh --describe --zookeeper zookeeper --topic products
Ver mensagem:  docker-compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic products --from-beginning --timeout-ms 1000

`

`subindo uma imagem docker com váriavel do spring: docker run -d -p 8080:8080 -e "SPRING_PROFILES_ACTIVE=docker" --name my-prd-srv product-service`
