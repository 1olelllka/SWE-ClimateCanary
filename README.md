# ClimateCanary – G1T4

## Usage & Configuration

### Webapp

#### Development

* **Frontend**

First of all you need to generate the APIs for frontend (in `/webapp` folder):
```bash
mvn -q -B generate-sources -Dskip.frontend=true
```

The frontend folder is located at `/webapp/src/main/frontend`

Start the frontend in development mode:
```bash
npm i   # Install packages
npm run dev   # Run the application in development mode
```

The application will be available at: `http://localhost:3000`

* **Backend**

Comment out the `app` container in `/docker-compose.yml` file:
```yaml
services:
#   app:
#     build:
#       context: ./webapp
#       args:
#         LOG_DIR: ${LOG_DIR:-/app/logs}
#     image: skel-app
#     container_name: "${TEAM_NAME:-local}-app"
#     ports:
#       - "${HOST_PORT:-8080}:8080"
#     environment:
#       SPRING_PROFILES_ACTIVE: prod
#       APP_JWT_SECRET: "${APP_JWT_SECRET}"
#       TEAM_NAME: "${TEAM_NAME:-local}"
redis:
    ...
postgres:
    ...
```
> Optional: You may also comment out the postgres container and use an in-memory H2 database instead

Then run the `docker-compose` and `mvn spring-boot:run`
```bash
docker compose up -d    # Run the containers
mvn spring-boot:run
```

> *If you commented out the `postgres` container, you must change the configuration file `/webapp/src/main/resources/application.properties` to use `H2 Database` before running the `docker compose` and `mvn`:*

```properties
...
# database config
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.url=jdbc:h2:mem:ClimateCanary;DB_CLOSE_DELAY=-1
spring.datasource.username=sa
spring.datasource.password=passwd
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create
spring.h2.console.enabled=true
...
```

#### Run Production

In order to run Webapp use ```docker compose up``` command with environmental variables
```bash
export TEAM_NAME=<TEAM_NAME> # Optional team name, defaults to local
export APP_JWT_SECRET=<VALID_512_BITS_JWT_SECRET_KEY>
export LOG_DIR=<ABSOLUTE_DOCKER_PATH> # Optional logs path, otherwise defaults to /app/logs
docker compose up --build -d # Build the project and run it in background
docker compose logs -f # Show the following logs from containers
docker compose down # Stop and remove containers
```

Example:
```bash
export TEAM_NAME=G1T4
export APP_JWT_SECRET=HEm3FCUc3APqk3tySyuKfZfHDrqlBfHu55bEiF1EhHzARzEMvwfqIsgmrxoULlGKp67wfHanmssIDkPBIJ5U5o
export LOG_DIR=/tmp/logs
docker compose up --build -d
docker compose logs -f
```

### Raspberry Pi

#### For Raspberry Pi configuration refer to this [readme](raspberrypi/README.md).

### Arduino

#### For Arduino Setup refer to this [readme](arduino/docs/ARDUINO_SETUP.md)

## Authors

Diana Postupaieva | Jacob Solomon | Josua Rebay | Matthias Tiefenthaler | Oleh Sichko 