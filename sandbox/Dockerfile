FROM ubuntu:22.04

# Prevent interactive prompts during package installation
ENV DEBIAN_FRONTEND=noninteractive

# Set timezone
RUN ln -snf /usr/share/zoneinfo/UTC /etc/localtime && echo UTC > /etc/timezone

# Install essential system packages
RUN apt-get update && apt-get install -y \
    # Build essentials
    build-essential \
    gcc \
    g++ \
    make \
    git \
    curl \
    wget \
    vim \
    nano \
    # JSON processing
    jq \
    # Archive tools
    tar \
    gzip \
    zip \
    unzip \
    # Libraries
    libssl3 \
    libcurl4 \
    libxml2 \
    libxmlsec1-dev \
    libjpeg-turbo8 \
    libpng16-16 \
    libfreetype6 \
    # Media processing
    ffmpeg \
    poppler-utils \
    graphviz \
    # Python dependencies
    python3.11 \
    python3.11-dev \
    python3.11-venv \
    python3-pip \
    # Java for some tools
    openjdk-11-jre-headless \
    # Additional utilities
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN useradd -m -s /bin/bash ubuntu && \
    mkdir -p /home/ubuntu/workspace && \
    chown -R ubuntu:ubuntu /home/ubuntu

# Switch to ubuntu user
USER ubuntu
WORKDIR /home/ubuntu

# Set up Python 3.11 as default python
RUN python3.11 -m pip install --user --upgrade pip setuptools wheel

# Install Python packages
RUN python3.11 -m pip install --user \
    # Data science core
    pandas==2.2.3 \
    numpy==2.1.3 \
    matplotlib==3.10.0 \
    seaborn==0.13.2 \
    plotly==5.24.1 \
    # Web development
    fastapi==0.119.1 \
    flask==3.1.0 \
    uvicorn==0.32.1 \
    requests==2.32.3 \
    httpx==0.28.1 \
    # Document processing
    fpdf2==2.8.4 \
    reportlab==4.2.5 \
    pypdf==5.1.0 \
    openpyxl==3.1.5 \
    # Web scraping
    beautifulsoup4==4.12.3 \
    lxml==5.3.0 \
    playwright==1.48.0 \
    # Cloud & APIs
    boto3==1.35.82 \
    openai==1.57.4 \
    # Utilities
    pillow==11.0.0 \
    python-dotenv==1.0.1

# Install NVM and Node.js 22.13
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash && \
    export NVM_DIR="$HOME/.nvm" && \
    [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh" && \
    nvm install 22.13.0 && \
    nvm use 22.13.0 && \
    nvm alias default 22.13.0

# Install global Node packages
RUN export NVM_DIR="$HOME/.nvm" && \
    [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh" && \
    npm install -g pnpm@10.8.0 yarn@1.22.22

# Install Playwright browsers
RUN export NVM_DIR="$HOME/.nvm" && \
    [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh" && \
    python3.11 -m playwright install chromium

# Set environment variables
ENV HOME=/home/ubuntu
ENV USER=ubuntu
ENV SHELL=/bin/bash
ENV PATH=/home/ubuntu/.local/bin:/home/ubuntu/.nvm/versions/node/v22.13.0/bin:$PATH
ENV PYTHONPATH=/home/ubuntu/workspace
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1
ENV NVM_DIR=/home/ubuntu/.nvm

# Create workspace directory
WORKDIR /home/ubuntu/workspace

# Health check
CMD ["/bin/bash"]
