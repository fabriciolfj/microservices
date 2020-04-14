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

###### Uso https
O endpoint exposto, será protegido por um certificado, no qual  gerado pelo comando:
```
keytool -genkeypair -alias localhost -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore edge.p12 -validity 3650
```

###### Uso jwks
É um conjunto de chaves que contém as chaves públicas usadas (podem ser usadas por servidores de recursos, para verificar tokens jwt emitidos pelo servidor de autorização) para verificar qualquer JSON Web Token (JWT) emitido pelo servidor de autorização e assinado usando o algoritmo de assinatura RS256.

## OAuth 2.0
Para utilizar o serviço exposto, utilizamos oauth2 como mecanimos de segurança, onde:

* Resource owner: é o usuário final.
* Client: aplicação terceira
* Resource server: servidor que expõe as APIs que queremos proteger.
* Authorization server: que emite o token de acesso, conforme autenticação e authorização do cliente e usuário final.

## OpenID
É um complemento ao oauth2, onde permite que aplicativos clientes verifiquem a identidade do usuários, atraveś de um token extra, um token de id. Este token de id e codificado como um Json web Token (JWT) e contém uma série de dados, como id e endereço de email do usuário. O token Id é assinado digitalmente usando assinaturas da Web Json. Isso permite que um aplicativo cliente confie nas informações no token de identificação, validando sua assinatura digital utilizando chaves púbicas do servidor de autorização.

## Exemplos de requisições
Pegar um token para um usuario com permissão de escrita:
```
curl -k https://writer:secret@localhost:8443/oauth/token -d grant_type=password -d username=fabricio -d password=password -s | jq .
```

Pegar um token para um usuario com permissão de leitura:
```
curl -k https://reader:secret@localhost:8443/oauth/token -d grant_type=password -d username=fabricio -d password=password -s | jq .
```

## Config server
Podemos encriptar e decriptar informações sensíveis no repositório do config server (onde as configurações dos microservices estãom localizadas), através dos endpoints encrypt e decrypt. Exemplo
```
curl -k https://dev-usr:dev-pwd@localhost:8443/config/encrypt --data-urlencode "hello word"
curl -k https://dev-usr:dev-pwd@localhost:8443/config/decrypt -d 287489034780982342748903274897789453384795385734uiouoi
```

Para utilizar a informações encriptada, usamos o sufixo {cipher}:
```
'{cipher}89389043872093482039wejhrjoewikjlshfiowuroiwuroiweuroiweuriow'
```

## Kubernetes
Os microservices são executados em containers e estes gerenciados pelo orquestrator kubernetes. Alguns conceitos das apis do kubernetes:
* Node - representa um servidor, virtual ou físico, no cluster.
* Pod - menor componente possível, que pode consistir em um ou mais contâiners.
* Deployment - utilizado para implantar e atualizar pods, este delega a responsabilidade de criar e monitorar os pods para um replicaset.
* Replicaset - é utilizado para garantir que um número especificado de pods, esteja sendo executado o tempo todo. Exemplo: se um pode for excluído, ele será substituido por um novo.
* Service - é um ponto final de rede estável que você pode usar para se conectar a um ou vários pods. As solicitações enviadas a um serviço, serão encaminhadas para um dos pods disponíveis usando um load balance. Um serviço é atribuido a um up ou nome dns.
* Ingress - para gerenciar o acesso externo a serviços em um cluster kubernetes, normalmente usando http.
* Namespace -  é usado para agrupar e, em alguns níveis, isolar recursos em um cluster kubernetes.
* ConfigMap - é utilizado para armazenar a configuração utilizada pelos contêiners.
* Secret - é usado para armazenar dados confidenciais usados por contêiners.
* DaemonSet - garanti que cada pod é executado em cada nó, em um conjunto de nódulos no cluster.


