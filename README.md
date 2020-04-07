# Microservices com Spring cloud.

```
Ver topics: docker-compose exec kafka /opt/kafka/bin/kafka-topics.sh --zookeeper zookeeper --list
Ver particoes: docker-compose exec kafka /opt/kafka/bin/kafka-topics.sh --describe --zookeeper zookeeper --topic products
Ver mensagem:  docker-compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic products --from-beginning --timeout-ms 1000
Subindo uma imagem docker com váriavel do spring: docker run -d -p 8080:8080 -e "SPRING_PROFILES_ACTIVE=docker" --name my-prd-srv product-service`
Criando imagem docker: docker build -t fabriciojacob211/account-email:latest .`
Escalando um serviço pelo docker-compose: docker-compose up -d --scale nome=2;
```

###### Design patterns microservices

- Service discovery
- Gateway
- Configuração centralizada
- Circuit breaker
- Trace distribuido

## Spring cloud gateway
Neste projeto usa-se o Spring cloud gateway, que será exposto aos clientes externamente. Neste existem configuradas as rotas, que são compostas por:
* Predicates: que selecionam uma rota com base em informações na solicitação HTTP recebida.
* Filters: que pode modificar a solicitação quanto/ou a resposta.
* Destination URI: que descreve para onde enviar um pedido
* ID: nome da rota
Para obter uma lista completa de predicados e filtros disponíveis, consulte a documentação de referência:[gateway] (https://cloud.spring.io/spring-cloud-gateway/single/spring-cloud-gateway.html)
