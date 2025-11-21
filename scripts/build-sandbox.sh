#!/bin/bash
set -e

echo "Building MY Manus sandbox image..."
cd sandbox
docker build -t mymanus-sandbox:latest .
echo "Sandbox image built successfully!"
