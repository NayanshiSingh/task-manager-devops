# Task Manager API

A simple REST API for managing tasks, built with Spring Boot as a demonstration project for DevOps CI/CD pipelines.

## 📋 Project Overview

This project demonstrates a production-grade CI/CD pipeline implementation using GitHub Actions, including:
- Continuous Integration with security scans
- Continuous Deployment to Kubernetes
- Container vulnerability scanning
- Code quality enforcement

## 🛠️ Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Build Tool | Maven |
| Container | Docker |
| CI/CD | GitHub Actions |
| Orchestration | Kubernetes |

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Docker (optional)

### Run Locally

```bash
# Clone the repository
git clone <repository-url>
cd task-manager

# Build the project
mvn clean package

# Run the application
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

### Run with Docker

```bash
# Build Docker image
docker build -t task-manager:latest .

# Run container
docker run -d -p 8080:8080 task-manager:latest
```

## 📡 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET    | `/api/tasks` | Get all tasks |
| GET    | `/api/tasks/{id}` | Get task by ID |
| POST   | `/api/tasks` | Create new task |
| PUT    | `/api/tasks/{id}` | Update task |
| DELETE | `/api/tasks/{id}` | Delete task |
| GET    | `/api/tasks/health` | Health check |
| GET    | `/actuator/health` | Actuator health |

### Example Request

```bash
# Create a task
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"My Task","description":"Task description"}'

# Get all tasks
curl http://localhost:8080/api/tasks
```

## 🔧 CI/CD Pipeline

### CI Pipeline (`.github/workflows/ci.yml`)

| Stage | Tool | Purpose |
|-------|------|---------|
| Checkout | actions/checkout | Retrieve source code |
| Setup Java | actions/setup-java | Install JDK 17 |
| Linting | Checkstyle | Enforce coding standards |
| SAST | CodeQL | Static security analysis |
| SCA | OWASP Dependency Check | Dependency vulnerability scan |
| Unit Tests | JUnit 5 | Validate business logic |
| Build | Maven | Package JAR |
| Docker Build | Docker | Create container image |
| Container Scan | Trivy | Image vulnerability scan |
| Smoke Test | Docker | Runtime validation |
| Push | DockerHub | Publish image |

### CD Pipeline (`.github/workflows/cd.yml`)

| Stage | Purpose |
|-------|---------|
| Deploy | Deploy to Kubernetes |
| DAST | Dynamic security testing |

## 🔐 Secrets Configuration

Configure these secrets in your GitHub repository:

| Secret | Description |
|--------|-------------|
| `DOCKERHUB_USERNAME` | Your DockerHub username |
| `DOCKERHUB_TOKEN` | DockerHub access token |

### How to Configure

1. Go to your GitHub repository
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add each secret with its value

## 📁 Project Structure

```
.
├── .github/workflows/
│   ├── ci.yml           # CI pipeline
│   └── cd.yml           # CD pipeline
├── k8s/
│   ├── deployment.yaml  # K8s Deployment
│   └── service.yaml     # K8s Service
├── src/
│   ├── main/java/com/taskmanager/
│   │   ├── Application.java
│   │   ├── controller/TaskController.java
│   │   ├── model/Task.java
│   │   └── service/TaskService.java
│   ├── main/resources/application.properties
│   └── test/java/com/taskmanager/TaskServiceTest.java
├── Dockerfile
├── pom.xml
├── checkstyle.xml
└── README.md
```

## 🧪 Testing

```bash
# Run unit tests
mvn test

# Run checkstyle
mvn checkstyle:check

# Run dependency check
mvn org.owasp:dependency-check-maven:check
```

## 📄 License

This project is created for educational purposes as part of the DevOps curriculum.
