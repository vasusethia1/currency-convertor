# Currency Converter Application

A Spring Boot application for currency conversion with historical rates support.

## Prerequisites

- Docker
- Docker Compose

## Quick Start

1. Download the run script:

```bash
curl -O https://raw.githubusercontent.com/amitdobal/currency-converter/main/run-currency-converter.sh
```

2. Make the script executable:

```bash
chmod +x run-currency-converter.sh
```

3. Run the application:

```bash
./run-currency-converter.sh
```

That's it! The application will:

- Download the latest version
- Start the PostgreSQL database
- Run the application
- Automatically fetch initial exchange rates

## Accessing the Application

- API Documentation: http://localhost:8080/swagger-ui.html
- Health Check: http://localhost:8080/actuator/health

## Features

- Real-time currency conversion
- Historical exchange rates
- Automatic daily rate updates
- RESTful API
- Swagger documentation
- Health monitoring

## Stopping the Application

Press `Ctrl+C` in the terminal where the application is running, or:

```bash
docker-compose down
```

To stop and remove all data:

```bash
docker-compose down -v
```

## Troubleshooting

1. If the application fails to start:

   - Check if port 8080 is available
   - Check if port 5432 is available
   - Check Docker logs: `docker-compose logs -f app`

2. If the database connection fails:

   - Check if PostgreSQL is running: `docker-compose ps`
   - Check database logs: `docker-compose logs -f postgres`

3. If exchange rates are not updating:
   - Check application logs: `docker-compose logs -f app`
   - Verify API key configuration

## Support

For any issues or questions, please contact the development team.

## Development

- The application runs on port 8080
- PostgreSQL runs on port 5432
- Database credentials:
  - Username: postgres
  - Password: postgres
  - Database: currency_converter

## API Documentation

The application uses Swagger for API documentation. Access it at:
http://localhost:8080/swagger-ui.html

## Edge cases
- Database down
- API Rate Limit Exceeded
- Third party currency API down (Backoff and Retry)
- Currency precision issue
- Indexing in DB
- 