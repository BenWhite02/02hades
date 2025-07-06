# 🔥 Hades Backend

> **The Powerful Decision Engine Below** - Kotlin + Spring Boot backend for the Kairos platform

## 🚀 Quick Start

### Prerequisites
- Java 21 or later
- No additional dependencies required (uses H2 in-memory database)

### Running the Application

```powershell
# Build the project
.\gradlew build

# Run the application
.\gradlew bootRun
```

### Testing the Application

```powershell
# Test health endpoint
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/actuator/health"

# Access H2 console
# Open browser: http://localhost:8080/api/v1/h2-console
# JDBC URL: jdbc:h2:mem:hadesdb
# Username: sa
# Password: password
```

## 📁 Project Structure

```
hades/
├── src/main/kotlin/com/kairos/hades/    # Main source code
├── src/main/resources/                  # Configuration files
├── src/test/                           # Test source code
├── build.gradle.kts                    # Build configuration
└── README.md                           # This file
```

## 🎯 Next Steps

1. Verify application starts successfully
2. Test health endpoints
3. Add your business entities
4. Implement EligibilityAtoms framework
5. Add security and authentication

---

**Built with ❤️ for the Kairos platform**
