# Microservices com Spring cloud.

```
Ver topics: docker-compose exec kafka /opt/kafka/bin/kafka-topics.sh --zookeeper zookeeper --list
Ver particoes: docker-compose exec kafka /opt/kafka/bin/kafka-topics.sh --describe --zookeeper zookeeper --topic products
Ver mensagem:  docker-compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic products --from-beginning --timeout-ms 1000
Subindo uma imagem docker com váriavel do spring: docker run -d -p 8080:8080 -e "SPRING_PROFILES_ACTIVE=docker" --name my-prd-srv product-service`
Criando imagem docker: docker build -t fabriciojacob211/account-email:latest .`
Escalando um serviço pelo docker-compose: docker-compose up -d --scale nome=2;
Ver logs de um serviço: docker-compose logs -f --tail=0 nome do serviço
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
Para obter uma lista completa de predicados e filtros disponíveis, consulte a documentação de referência.
[GATEWAY](https://cloud.spring.io/spring-cloud-gateway/single/spring-cloud-gateway.html)

###### Rotas com base no host

Pode-se utilizar o nome do host como rota, conforme o exemplo abaixo:
```
  - id: host_route_200
    uri: http://httpstat.us
    predicates:
      - Host=i.feel.lucky:8080
      - Path=/headerrouting/**
    filters:
      - SetPath=/200
```
Realizando uma requisição:
```
curl http://localhost:8080/headerrouting -H "Host: i.feel.lucky:8080"

```
Outra opção seria colocar o host no arquivo hosts do SO, conforme demonstrado abaixo:
```
sudo bash -c "echo '127.0.0.1 i.feel.lucky im.a.teapot' >> /etc/hosts"
curl http://i.feel.lucky:8080/headerrouting
```

## OAuth 2.0
Para utilizar o serviço exposto, utilizamos oauth2 como mecanimos de segurança, onde:

* Resource owner: é o usuário final.
* Client: aplicação terceira
* Resource server: servidor que expõe as APIs que queremos proteger.
* Authorization server: que emite o token de acesso, conforme autenticação e authorização do cliente e usuário final.

## OpenID
É um complemento ao oauth2, onde permite que aplicativos clientes verifiquem a identidade do usuários, atraveś de um token extra.
