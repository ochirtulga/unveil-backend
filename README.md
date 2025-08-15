# Unveil Backend API

A Spring Boot REST API for searching and verifying bad actor information. Built to help users identify and protect themselves from scammers and fraudulent activities.

## 🚀 **Quick Start**

### Prerequisites
- Java 21 (or Java 17+)
- Maven 3.6+
- PostgreSQL database (Supabase recommended)

### Environment Setup

Create environment variables for database connection:
```bash
export DB_URL="postgresql://your-db-host:5432/your-db-name"
export DB_USERNAME="your-username"
export DB_PASSWORD="your-password"
```

### Running the Application

1. **Clone and navigate to project directory**
```bash
git clone <your-repo>
cd unveil-backend
```

2. **Set up environment variables (required)**
```bash
# For local development with Supabase
export DB_URL="postgresql://your-supabase-url"
export DB_USERNAME="postgres"
export DB_PASSWORD="your-supabase-password"
```

3. **Run the application**
```bash
./mvnw spring-boot:run
```

4. **Application will start on http://localhost:8080**

### Testing the API

```bash
# Health check
curl http://localhost:8080/api/v1/health

# Search across all fields
curl "http://localhost:8080/api/v1/search?filter=all&value=microsoft"

# Search by specific field
curl "http://localhost:8080/api/v1/search?filter=email&value=support@fake-microsoft.com"

# Search with pagination
curl "http://localhost:8080/api/v1/search?filter=name&value=john&page=0&size=10"

# Get database stats
curl http://localhost:8080/api/v1/stats
```

## 📋 **API Endpoints**

### Search Endpoints

#### Main Search (with filtering and pagination)
- **GET** `/api/v1/search?filter={filter}&value={query}&page={page}&size={size}`

**Supported Filters:**
- `all` - Search across all fields
- `name` - Search by person name
- `email` - Search by email address (exact match)
- `phone` - Search by phone number (exact match)
- `company` - Search by company name
- `actions` - Search by scam type

**Example Requests:**
```bash
# Search all fields for "microsoft"
GET /api/v1/search?filter=all&value=microsoft

# Find specific email
GET /api/v1/search?filter=email&value=support@fake-microsoft.com

# Search by scam type with pagination
GET /api/v1/search?filter=actions&value=Tech Support&page=0&size=20

# Search by phone number
GET /api/v1/search?filter=phone&value=+1-800-123-4567
```

### Information Endpoints
- **GET** `/api/v1/Case/{id}` - Get specific bad actor details by ID
- **GET** `/api/v1/categories` - Get all available scam types
- **GET** `/api/v1/stats` - Get database statistics
- **GET** `/api/v1/health` - Health check
- **GET** `/api/v1/search/filters` - Get supported filter types and examples

### API Response Examples

**Search Response:**
```json
{
  "filter": "all",
  "value": "microsoft",
  "found": true,
  "message": "2 results found for all: microsoft",
  "results": [
    {
      "id": 1,
      "name": "John Microsoft",
      "email": "support@fake-microsoft.com",
      "phone": "+1-800-123-4567",
      "company": "Fake Microsoft Support",
      "description": "Claims to be from Microsoft, asking for remote desktop access to fix computer virus.",
      "actions": "Tech Support",
      "reportedBy": "FBI Scam Alert",
      "createdAt": "2024-01-15T10:30:00"
    }
  ],
  "pagination": {
    "currentPage": 0,
    "pageSize": 20,
    "totalPages": 1,
    "totalElements": 2,
    "hasNext": false,
    "hasPrevious": false,
    "isFirst": true,
    "isLast": true
  }
}
```

**Health Check Response:**
```json
{
  "status": "UP",
  "service": "ScamGuard Search API",
  "version": "v1",
  "timestamp": "2025-08-14T12:30:45"
}
```

**Categories Response:**
```json
{
  "actions": [
    "Auto Warranty",
    "Charity",
    "Employment",
    "Government",
    "Health",
    "Investment",
    "Nigerian Prince",
    "Phishing",
    "Romance",
    "Shopping",
    "Tax/IRS",
    "Tech Support",
    "Travel"
  ],
  "count": 13
}
```

**Statistics Response:**
```json
{
  "totalCases": 150,
  "totalActions": 13,
  "status": "Database operational"
}
```

## 🗄️ **Database**

### Current Setup
- **Database**: PostgreSQL (Supabase)
- **Schema**: Automatically managed by Hibernate
- **Sample Data**: ~30 bad actor records across different categories

### Database Schema
```sql
CREATE TABLE bad_actors (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    company VARCHAR(255),
    description TEXT,
    actions VARCHAR(300),
    reported_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

-- Indexes for performance
CREATE INDEX idx_Case_name ON bad_actors(name);
CREATE INDEX idx_Case_email ON bad_actors(email);
CREATE INDEX idx_Case_phone ON bad_actors(phone);
```

### Database Configuration

The application uses PostgreSQL with the following environment variables:
- `DB_URL` - Database connection URL
- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password

## 🧪 **Testing**

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report

# Run specific test class
./mvnw test -Dtest=SearchControllerTest
```

## 🏗️ **Building for Production**

```bash
# Create JAR file
./mvnw clean package

# Run the JAR with environment variables
DB_URL="your-db-url" DB_USERNAME="user" DB_PASSWORD="pass" \
java -jar target/unveil-backend-0.0.1-SNAPSHOT.jar
```

## 🌐 **Deployment Options**

### Option 1: Railway.app (Recommended)
1. Push code to GitHub
2. Connect Railway to your repository
3. Set environment variables in Railway dashboard:
   ```
   DB_URL=postgresql://your-supabase-url
   DB_USERNAME=postgres  
   DB_PASSWORD=your-password
   ```
4. Deploy automatically

### Option 2: Render.com
1. Connect GitHub repository
2. Choose "Web Service"
3. Build Command: `./mvnw clean package`
4. Start Command: `java -jar target/unveil-backend-0.0.1-SNAPSHOT.jar`
5. Add environment variables in Render dashboard

### Option 3: Heroku
```bash
# Install Heroku CLI and login
heroku create unveil-backend

# Set environment variables
heroku config:set DB_URL="your-db-url"
heroku config:set DB_USERNAME="your-username"  
heroku config:set DB_PASSWORD="your-password"

# Deploy
git push heroku main
```

### Option 4: Docker Deployment
```dockerfile
FROM openjdk:21-jdk-slim
COPY target/unveil-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

```bash
# Build and run with Docker
docker build -t unveil-backend .
docker run -p 8080:8080 \
  -e DB_URL="your-db-url" \
  -e DB_USERNAME="user" \
  -e DB_PASSWORD="pass" \
  unveil-backend
```

## ⚙️ **Configuration**

### Application Profiles

#### Default Profile (development)
- Uses PostgreSQL database
- Shows SQL queries in console
- Debug logging enabled

#### Production Profile (`--spring.profiles.active=prod`)
- Optimized for production
- Reduced logging
- Security headers enabled
- Validation mode for database schema

### Environment Variables

**Required:**
- `DB_URL` - PostgreSQL connection URL
- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password

**Optional:**
- `SERVER_PORT` - Application port (default: 8080)
- `SPRING_PROFILES_ACTIVE` - Active profile (dev/prod)

### CORS Configuration
Currently configured to allow all origins for development. Update for production:

```java
@CrossOrigin(origins = "https://your-frontend-domain.com")
```

## 🔧 **Development**

### Project Structure
```
src/main/java/com/unveil/
├── UnveilApplication.java          # Main application class
├── controller/
│   └── SearchController.java      # REST API endpoints
├── service/
│   └── SearchService.java         # Business logic
├── repository/
│   └── CaseRepository.java    # Database access layer
└── entity/
    └── Case.java              # JPA entity model

src/main/resources/
├── application.yaml               # Configuration
└── data.sql                      # Sample data
```

### Adding New Features

1. **Add new search filter:**
    - Update `SearchService.searchByFilter()` method
    - Add new repository method in `CaseRepository`
    - Update filter validation in `isValidFilter()`

2. **Add new endpoint:**
    - Add method to `SearchController`
    - Add business logic to `SearchService`
    - Add database method if needed

3. **Modify Case entity:**
    - Update `Case.java` entity class
    - Update sample data in `data.sql`
    - Database schema will auto-update

### Search Performance Optimization

The application includes several optimizations:
- Database indexes on frequently searched fields
- Pagination to limit result sets
- Connection pooling with HikariCP
- Efficient JPA queries with proper projections

## 🚨 **Common Issues & Solutions**

### Database Connection Issues
```bash
# Check if environment variables are set
echo $DB_URL
echo $DB_USERNAME
echo $DB_PASSWORD

# Test database connection
psql "$DB_URL" -c "SELECT 1;"
```

### Port Already in Use
```bash
# Kill process on port 8080
lsof -ti:8080 | xargs kill -9

# Or run on different port
./mvnw spring-boot:run -Dserver.port=8081
```

### Application Won't Start
```bash
# Check Java version (needs 17+)
java -version

# Check Maven version
./mvnw -version

# Check application logs
tail -f logs/unveil.log
```

### Performance Issues
```bash
# Increase Java heap size
export JAVA_OPTS="-Xmx1g -Xms512m"
./mvnw spring-boot:run

# Or when running JAR
java -Xmx1g -Xms512m -jar target/unveil-backend-0.0.1-SNAPSHOT.jar
```

## 📈 **Production Readiness Checklist**

### ✅ **Current Features**
- [x] RESTful API with proper HTTP status codes
- [x] Input validation and error handling
- [x] Database connection pooling
- [x] Pagination for large result sets
- [x] Database indexes for performance
- [x] Structured logging
- [x] Health check endpoint
- [x] CORS configuration
- [x] Environment-based configuration

### 🚧 **Production TODOs**

**Security:**
- [ ] Add authentication (JWT/OAuth2)
- [ ] Add rate limiting
- [ ] Configure CORS for specific domains
- [ ] Add HTTPS/SSL configuration
- [ ] Add input sanitization
- [ ] Add audit logging

**Performance:**
- [ ] Add caching (Redis)
- [ ] Add full-text search (Elasticsearch)
- [ ] Add database read replicas
- [ ] Add CDN for static content

**Monitoring:**
- [ ] Add metrics collection (Micrometer)
- [ ] Add APM monitoring (New Relic/DataDog)
- [ ] Add error tracking (Sentry)
- [ ] Add structured logging (JSON format)

**Features:**
- [ ] Add user reporting functionality
- [ ] Add email notifications
- [ ] Add bulk search operations
- [ ] Add export functionality (CSV/JSON)

## 🔒 **Security**

### Current Security Measures
- Input validation on all endpoints
- SQL injection prevention with JPA/Hibernate
- Basic error handling (no sensitive data leakage)
- Connection string encryption

### Security Best Practices
- Keep dependencies updated
- Use parameterized queries (already implemented)
- Validate all input data
- Implement proper authentication for sensitive operations
- Use HTTPS in production
- Regular security audits

## 📞 **Support & Monitoring**

### Health Monitoring
- Health check endpoint: `/api/v1/health`
- Database statistics: `/api/v1/stats`
- Application logs: `logs/unveil.log`

### Troubleshooting
1. Check application logs
2. Verify database connectivity
3. Confirm environment variables
4. Test API endpoints manually
5. Check system resources (memory/CPU)

### Performance Monitoring
- Response times via application logs
- Database query performance
- Memory usage monitoring
- Connection pool statistics

## 📄 **License**

MIT License - feel free to use and modify as needed.

---

**Built with Spring Boot 3.x, PostgreSQL, and deployed on modern cloud infrastructure.**