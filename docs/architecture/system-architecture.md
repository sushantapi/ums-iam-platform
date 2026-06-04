Client
   |
API Gateway
   |
--------------------------------------------------
|          |          |          |               |
Auth      User     Organization Notification  Admin
Service   Service    Service      Service     Service
   |
RabbitMQ Event Bus
   |
--------------------------------------------------
|                      |
Notification      Other Consumers
Service

Eureka Discovery
Config Server

MySQL Databases

Later create a visual diagram using Draw.io.