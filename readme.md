# 🚀 CPSync Backend

A robust, automated Spring Boot backend that aggregates competitive programming contests from multiple platforms and seamlessly syncs them directly to users' Google Calendars.

Never miss a rating update again. CPSync fetches upcoming schedules from **Codeforces**, **LeetCode**, **AtCoder**, and **CodeChef**, respects user platform preferences, and manages background calendar synchronization via Google OAuth2.

---

## ✨ Features

- 📅 **Automated Google Calendar Sync**
    - Creates calendar events for upcoming contests with direct contest links.
    - Prevents duplicate event creation.

- 🌍 **Multi-Platform Contest Aggregation**
    - **Codeforces** (Official API)
    - **LeetCode** (GraphQL)
    - **CodeChef** (API)
    - **AtCoder** (JSoup HTML Scraping)

- 🔐 **Secure Authentication**
    - Google OAuth2 Login
    - JWT-based session management

- 🛡️ **Enterprise-Grade Security**
    - Google Access & Refresh Tokens encrypted using **AES-256-GCM**
    - Secure token refresh mechanism

- ⚡ **High Performance**
    - Caffeine caching minimizes API requests
    - Faster contest retrieval with reduced rate limiting

- 🤖 **Background Synchronization**
    - Scheduled CRON job runs every day at **3:00 AM**
    - Automatically syncs contests for all active users

- 📈 **Smart Monitoring**
    - Fetcher Health Monitor detects API/scraping failures
    - Logs anomalies for easier debugging

---

# 🛠️ Technology Stack

| Category | Technologies |
|------------|-----------------------------------------------|
| **Core** | Java 21, Spring Boot 3.x |
| **Database** | PostgreSQL, Spring Data JPA, Flyway |
| **Security** | Spring Security, OAuth2 Client, JJWT, AES-256-GCM |
| **Integrations** | Google Calendar API v3, JSoup, Spring RestClient |
| **Performance** | Caffeine Cache, Spring Scheduling |

---

# 🏗️ System Architecture

```
                    Google OAuth2
                           │
                           ▼
                    Authentication
                           │
                           ▼
                    JWT Generation
                           │
                           ▼
                      Spring Boot API
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
 Contest Fetchers     User Preferences   Sync Service
        │                  │                  │
        └──────────────┬───┴──────────────────┘
                       ▼
              Google Calendar API
                       │
                       ▼
                User Google Calendar
```

---

# ⚙️ Workflow

### 1. Authentication

- User signs in using Google OAuth2 (`prompt=consent`)
- Backend securely stores encrypted Refresh Token
- JWT is generated and returned to the frontend

### 2. Contest Fetching

Each platform has its own `ContestFetcher` implementation.

Data Sources:

- Codeforces API
- LeetCode GraphQL
- CodeChef API
- AtCoder HTML Scraper

Contest data is cached using **Caffeine Cache** to reduce external API calls.

### 3. Synchronization

When synchronization is triggered:

- Load user platform preferences
- Fetch upcoming contests
- Check `synced_events` table
- Skip duplicate events
- Create new Google Calendar events

### 4. Token Management

If the Google Access Token expires:

- Refresh Token is automatically used
- New Access Token is generated
- Calendar operation continues seamlessly

---

# 🚀 Getting Started

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL
- Google Cloud Project
- Google Calendar API Enabled
- OAuth2 Client Credentials

---

# 🔧 Environment Variables

Create an `application-local.properties` file or export the following environment variables.

```properties
# Database Configuration
DB_HOST=jdbc:postgresql://<your-db-host>:<port>/<database>
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password

# Google OAuth2 Credentials
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# JWT Secret
JWT_SECRET=a_very_long_secure_random_base64_string

# AES-256-GCM Encryption Key
TOKEN_ENCRYPTION_KEY=a_32_byte_base64_encoded_string
```

> **Note:** `TOKEN_ENCRYPTION_KEY` must be a valid Base64-encoded 256-bit (32-byte) key.

---

# 📦 Installation

## 1. Clone the Repository

```bash
git clone https://github.com/yourusername/cpsync-backend.git

cd cpsync-backend
```

---

## 2. Build the Project

```bash
./mvnw clean install
```

---

## 3. Run the Application

```bash
./mvnw spring-boot:run
```

---

Flyway automatically initializes the database schema during startup.

Application URL:

```
http://localhost:8080
```

---

# 📡 API Reference

## Authentication

| Method | Endpoint | Description |
|------------|-----------------------------------------|--------------------------------|
| GET | `/oauth2/authorization/google` | Start Google OAuth2 Login |

---

## User APIs

| Method | Endpoint | Description |
|------------|-------------------------|------------------------------------|
| GET | `/api/user/profile` | Get logged-in user profile |
| PUT | `/api/user/platforms` | Update enabled platforms |
| PUT | `/api/user/pause` | Pause automatic synchronization |
| PUT | `/api/user/resume` | Resume automatic synchronization |

---

## Contest APIs

| Method | Endpoint | Description |
|------------|------------------|--------------------------------|
| GET | `/api/contests` | Fetch upcoming contests (Public) |
| POST | `/api/sync` | Trigger manual Google Calendar sync |

---

> All `/api/**` endpoints (except `/api/contests`) require:

```
Authorization: Bearer <JWT_TOKEN>
```

---

# 📂 Project Structure

```text
src/
└── main/
    └── java/
        └── com/
            └── cpsync/
                └── cpsync_backend/
                    ├── config/
                    │   ├── Cache Configuration
                    │   ├── OAuth2 Configuration
                    │   └── Security Configuration
                    │
                    ├── controller/
                    │   └── REST API Endpoints
                    │
                    ├── dto/
                    │   └── Request & Response DTOs
                    │
                    ├── exception/
                    │   └── Global Exception Handling
                    │
                    ├── model/
                    │   ├── User
                    │   ├── SyncedEvent
                    │   └── Platform Preferences
                    │
                    ├── repository/
                    │   └── Spring Data JPA Repositories
                    │
                    ├── scheduler/
                    │   └── Daily CRON Jobs
                    │
                    ├── security/
                    │   ├── JWT Filters
                    │   └── OAuth2 Success Handlers
                    │
                    └── service/
                        ├── Contest Services
                        ├── Sync Services
                        ├── Google Services
                        └── impl/
                            ├── CodeforcesFetcher
                            ├── LeetCodeFetcher
                            ├── CodeChefFetcher
                            └── AtCoderFetcher
```

---

# 🔒 Security Highlights

- ✅ Google OAuth2 Authentication
- ✅ JWT Authorization
- ✅ AES-256-GCM Token Encryption
- ✅ Automatic Google Access Token Refresh
- ✅ Duplicate Calendar Event Prevention
- ✅ Platform Preference-Based Synchronization

---

# ⚡ Performance Optimizations

- Caffeine In-Memory Cache
- Scheduled Background Jobs
- Cached Contest Fetching
- Minimal External API Calls
- Efficient Database Queries
- Duplicate Sync Detection

---

# 📅 Supported Platforms

| Platform | Source |
|----------------|----------------|
| Codeforces | Official API |
| LeetCode | GraphQL |
| CodeChef | API |
| AtCoder | JSoup HTML Scraping |

---

# 🤝 Contributing

1. Fork the repository

2. Create a feature branch

```bash
git checkout -b feature/my-feature
```

3. Commit your changes

```bash
git commit -m "Add new feature"
```

4. Push the branch

```bash
git push origin feature/my-feature
```

5. Open a Pull Request

---

# 📄 License

This project is released under the **MIT License**.

Feel free to use, modify, and distribute it for personal or commercial purposes.