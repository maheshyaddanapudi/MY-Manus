#!/bin/bash
set -e

echo "Waiting for backend to be ready..."
until curl -s http://localhost:8080/api/health > /dev/null; do
    sleep 2
    echo "Waiting..."
done

echo "Backend is ready! Generating frontend API client..."
cd frontend
npm run generate-api
echo "Frontend API client generated successfully!"
