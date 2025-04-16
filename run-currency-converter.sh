#!/bin/bash

# Function to check if a port is in use
check_port() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        if lsof -i :$1 > /dev/null 2>&1; then
            return 0
        fi
    else
        # Linux
        if netstat -tuln | grep ":$1" > /dev/null 2>&1; then
            return 0
        fi
    fi
    return 1
}

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "Docker is not installed. Please install Docker first."
    echo "Visit https://docs.docker.com/get-docker/ for installation instructions."
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker compose &> /dev/null; then
    echo "Docker Compose is not installed. Please install Docker Compose first."
    echo "Visit https://docs.docker.com/compose/install/ for installation instructions."
    exit 1
fi

# Check if ports are available
if check_port 8080; then
    echo "Port 8080 is already in use. Please free up the port and try again."
    exit 1
fi

if check_port 5432; then
    echo "Port 5432 is already in use. Please free up the port and try again."
    exit 1
fi

echo "Downloading and starting Currency Converter Application..."
echo "This may take a few minutes on the first run..."

# Pull the latest image
echo "Pulling the latest image..."
if ! docker pull vasusethia/currency-converter:latest; then
    echo "Failed to pull the image. Please check your internet connection and try again."
    exit 1
fi

# Start the containers
echo "Starting the application..."
docker compose up

# If the user presses Ctrl+C, stop the containers
trap "echo 'Stopping containers...'; docker compose down" SIGINT
