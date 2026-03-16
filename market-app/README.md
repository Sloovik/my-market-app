# My Market App

## Запуск локально
mvn clean install
mvn spring-boot:run
Браузер: http://localhost:8080 (H2 console: http://localhost:8080/h2-console)

## Docker
docker build -t my-market-app .
docker run -p 8080:8080 my-market-app

## Тесты
mvn test