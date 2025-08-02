# Equities Trading Application

A full-stack application for managing equity positions and transactions, built with Angular 18 frontend and Spring Boot 3 backend.

## Project Structure

```
equities/
├── angular-ui/          # Angular 18 Frontend Application
├── java-backend/         # Spring Boot 3 Backend Application
└── README.md
```

## Prerequisites

### For Frontend (Angular UI)
- **Node.js** (v18 or higher)
- **npm** (v9 or higher)
- **Angular CLI** (v18 or higher)

### For Backend (Spring Boot)
- **Java 17** (JDK 17 or higher)
- **Gradle** (included with the project)

## Quick Start

### 1. Backend Setup (Spring Boot)

Navigate to the backend directory:
```bash
cd java-backend
```

#### Option A: Using Gradle Wrapper (Recommended)
```bash
# On Windows
./gradlew.bat bootRun

# On macOS/Linux
./gradlew bootRun
```

#### Option B: Using Gradle directly
```bash
gradle bootRun
```

The backend will start on `http://localhost:8080`

**Backend Endpoints:**
- API Base: `http://localhost:8080/api`
- H2 Database Console: `http://localhost:8080/h2-console`
- Health Check: `http://localhost:8080/actuator/health`

### 2. Frontend Setup (Angular UI)

Open a new terminal and navigate to the frontend directory:
```bash
cd angular-ui
```

Install dependencies:
```bash
npm install
```

Start the development server:
```bash
npm start
```

The frontend will start on `http://localhost:4200`

## Development

### Backend Development

The Spring Boot application includes:
- **H2 In-Memory Database** for development
- **JPA/Hibernate** for data persistence
- **RESTful API** endpoints
- **CORS** configured for frontend integration
- **Actuator** for monitoring and health checks

**Key Features:**
- Position management
- Transaction tracking
- Real-time position calculations

### Frontend Development

The Angular application includes:
- **Angular Material** for UI components
- **Proxy configuration** for API calls
- **Jest** for testing
- **TypeScript** for type safety

**Available Scripts:**
```bash
npm start              # Start development server with proxy
npm run build          # Build for production
npm run test           # Run Karma tests
npm run test:jest      # Run Jest tests
npm run test:all       # Run all tests
```

## Database

The application uses **H2 in-memory database** for development:
- **URL**: `jdbc:h2:mem:equitiesdb`
- **Username**: `sa`
- **Password**: `password`
- **Console**: `http://localhost:8080/h2-console`

## API Configuration

The frontend is configured to proxy API calls to the backend:
- Frontend runs on: `http://localhost:4200`
- Backend runs on: `http://localhost:8080`
- API calls are proxied from `/api` to `http://localhost:8080/api`

## Testing

### Backend Tests
```bash
cd java-backend
./gradlew test
```

### Frontend Tests
```bash
cd equities-ui
npm run test:all
```

## Building for Production

### Backend
```bash
cd java-backend
./gradlew build
```

### Frontend
```bash
cd equities-ui
npm run build
```

## Technology Stack

### Backend
- **Spring Boot 3.5.3**
- **Spring Data JPA**
- **H2 Database**
- **Java 17**
- **Gradle**

### Frontend
- **Angular 18**
- **Angular Material**
- **TypeScript**
- **Jest**

## Enhancement

1. This solution can be even expanded using reactive web or streaming middleware layer.

