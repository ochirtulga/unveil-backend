# Unveil Backend - MVP

A simple Spring Boot API for searching bad actor information.

## 🚀 **Quick Start**

### Prerequisites
- Java 21 (or Java 17)
- Maven 3.6+

### Running the Application

1. **Clone and navigate to project directory**
```bash
git clone <your-repo>
cd Unveil-backend
```

2. **Run the application**
```bash
./mvnw spring-boot:run
```

3. **Application will start on http://localhost:8080**

4. **Test the API**
```bash
# Health check
curl http://localhost:8080/api/health

# Search for bad actors
curl "http://localhost:8080/api/search?q=microsoft"

# Get database stats
curl http://localhost:8080/api/stats
```

### H2 Database Console (Development)
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/Unveil`
- Username: `sa`
- Password: (leave empty)

## 📋 **API Endpoints**

### Search Endpoints
- `GET /api/search?q={query}` - Search across all fields
- `GET /api/search/paginated?q={query}&page=0&size=20` - Search with pagination
- `GET /api/search/field?q={query}&field={fieldType}` - Search specific field

### Quick Check Endpoints
- `GET /api/check/email?email={email}` - Check if email is known bad actor
- `GET /api/check/phone?phone={phone}` - Check if phone is known bad actor

### Info Endpoints
- `GET /api/badActor/{id}` - Get specific bad actor details
- `GET /api/categories` - Get all scam types
- `GET /api/stats` - Get database statistics
- `GET /api/health` - Health check

### Example API Responses

**Search Response:**
```json
{
  "query": "microsoft",
  "results": [
    {
      "id": 1,
      "name": "John Microsoft",
      "email": "support@fake-microsoft.com",
      "phone": "+1-800-123-4567",
      "company": "Fake Microsoft Support",
      "description": "Claims to be from Microsoft...",
      "actions": "Tech Support",
      "createdAt": "2024-01-15T10:30:00"
    }
  ],
  "count": 1,
  "found": true
}
```

**Email Check Response:**
```json
{
  "email": "support@fake-microsoft.com",
  "isKnownbad actor": true,
  "message": "WARNING: This email is associated with reported scam activity"
}
```

## 🗄️ **Database**

### Current Setup (MVP)
- **Database**: H2 (file-based)
- **Location**: `./data/Unveil.mv.db`
- **Sample Data**: ~30 bad actor records across different categories

### Database Schema
```sql
CREATE TABLE bad actors (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    company VARCHAR(255),
    description TEXT,
    actions VARCHAR(200),
    reported_by VARCHAR(255),
    created_at TIMESTAMP
);
```

## 🧪 **Testing**

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report
```

## 🏗️ **Building for Production**

```bash
# Create JAR file
./mvnw clean package

# Run the JAR
java -jar target/Unveil-backend-0.0.1-SNAPSHOT.jar
```

## 🌐 **Deployment Options**

### Option 1: Railway.app (Recommended for MVP)
1. Push code to GitHub
2. Connect Railway to your repository
3. Railway auto-detects Java and deploys
4. Get URL like `https://Unveil-production.railway.app`

### Option 2: Render.com
1. Connect GitHub repository
2. Choose "Web Service"
3. Build Command: `./mvnw clean package`
4. Start Command: `java -jar target/Unveil-backend-0.0.1-SNAPSHOT.jar`

### Option 3: Traditional Server
```bash
# Package application
./mvnw clean package

# Upload JAR to server
scp target/Unveil-backend-0.0.1-SNAPSHOT.jar user@server:/app/

# Run on server
java -jar /app/Unveil-backend-0.0.1-SNAPSHOT.jar
```

## ⚙️ **Configuration**

### Environment Variables (Production)
```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/Unveil_prod
export DB_USERNAME=Unveil_user
export DB_PASSWORD=secure_password
export SPRING_PROFILES_ACTIVE=prod
```

### Application Profiles
- **Default**: Development with H2 database
- **dev**: Development with debug logging
- **prod**: Production configuration

## 🔧 **Development**

### Project Structure
```
src/main/java/com/Unveil/
├── UnveilApplication.java     # Main application class
├── controller/
│   └── SearchController.java     # REST API endpoints
├── service/
│   └── SearchService.java        # Business logic
├── repository/
│   └── bad actorRepository.java    # Database access
└── model/
    └── bad actor.java              # Data model

src/main/resources/
├── application.yml               # Configuration
└── data.sql                      # Sample data
```

### Adding New Features

1. **Add new endpoint:**
    - Add method to `SearchController.java`
    - Add business logic to `SearchService.java`
    - Add database method to `bad actorRepository.java` if needed

2. **Add new fields to bad actor:**
    - Update `bad actor.java` entity
    - Update `data.sql` sample data
    - Add new search methods if needed

3. **Change database:**
    - Update `application.yml` datasource configuration
    - Update dependencies in `pom.xml`

## 🚨 **Common Issues & Solutions**

### Port Already in Use
```bash
# Kill process on port 8080
lsof -ti:8080 | xargs kill -9

# Or run on different port
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### Database File Locked
```bash
# Stop application completely
# Delete database files
rm -rf ./data/

# Restart application (will recreate database)
./mvnw spring-boot:run
```

### Out of Memory
```bash
# Increase Java heap size
java -Xmx512m -jar target/Unveil-backend-0.0.1-SNAPSHOT.jar
```

## 📈 **Next Steps for Production**

1. **Switch to PostgreSQL** for better performance
2. **Add authentication** with Spring Security + JWT
3. **Add rate limiting** to prevent abuse
4. **Add monitoring** with Micrometer + Prometheus
5. **Add user reporting** functionality
6. **Add email notifications** for new bad actor reports
7. **Add Elasticsearch** for better search performance

## 🔒 **Security Notes**

### Current Security (MVP)
- Input validation on all endpoints
- SQL injection prevention with JPA
- CORS enabled for all origins (development only)
- Basic error handling

### Production Security TODO
- Add authentication/authorization
- Configure CORS properly
- Add HTTPS/SSL
- Add rate limiting
- Add input sanitization
- Add audit logging

## 📞 **Support**

If you encounter issues:
1. Check the application logs: `logs/Unveil.log`
2. Verify Java version: `java -version`
3. Verify Maven version: `./mvnw -version`
4. Check database console: http://localhost:8080/h2-console

## 📄 **License**

MIT License - feel free to use and modify as needed.