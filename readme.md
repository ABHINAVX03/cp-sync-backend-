# 🚀 CPSync Backend

A robust, automated Spring Boot backend that aggregates competitive programming contests from multiple platforms and seamlessly syncs them directly to users' Google Calendars.

Never miss a rating update again. CPSync fetches upcoming schedules from **Codeforces**, **LeetCode**, **AtCoder**, and **CodeChef**, respects user platform preferences, and manages background calendar synchronization via Google OAuth2.

---

## ✨ Key Features

* **📅 Automated Calendar Sync:** Creates Google Calendar events for upcoming contests with direct links.
* **🌍 Multi-Platform Aggregation:** Supports Codeforces (API), LeetCode (GraphQL), CodeChef (API), and AtCoder (HTML Scraping).
* **🔐 Secure Authentication:** Google OAuth2 login flow with seamless JWT session management.
* **🛡️ Enterprise-Grade Security:** Google Access and Refresh tokens are encrypted at rest using AES-256-GCM.
* **⚡ High Performance:** Implements Caffeine caching to prevent API rate-limiting and ensure fast contest retrieval.
* **🤖 Background Scheduling:** Automated daily CRON jobs run at 3:00 AM to sync new contests for all active users.
* **📈 Smart Monitoring:** Built-in Fetcher Health Monitor tracks scraping/API health and logs anomalies.

---

## 🛠️ Technology Stack

| Category | Technologies |
| :--- | :--- |
| **Core** | Java 21, Spring Boot 3.x |
| **Data & Persistence** | PostgreSQL, Spring Data JPA, Flyway DB Migrations |
| **Security** | Spring Security, OAuth2 Client, JJWT, Java Cryptography Extension (AES-256-GCM) |
| **Integrations** | Google Calendar API v3, JSoup (Web Scraping), Spring RestClient |
| **Performance** | Caffeine Cache, Spring Scheduling |

---

## 🏗️ System Architecture & Workflow

1.  **Authentication Flow:** Users authenticate via Google OAuth2 (`prompt=consent` for offline access). The backend stores an encrypted Refresh Token and issues a JWT to the frontend.
2.  **Data Ingestion:** `ContestFetcher` implementations pull upcoming contests from respective platforms. Results are temporarily stored in local Caffeine caches to minimize external API load.
3.  **Synchronization Engine:** When triggered (manually or via the nightly CRON job), the `SyncService` determines user preferences, checks the `synced_events` database table to prevent duplicates, and pushes new events to the Google Calendar API.
4.  **Token Management:** The `GoogleTokenRefreshService` automatically handles expired access tokens before attempting calendar operations.

---

## 🚀 Getting Started

### Prerequisites

* **Java 21** or higher
* **Maven** 3.9+
* **PostgreSQL** database (Local or Cloud, e.g., Neon)
* **Google Cloud Console** project with:
    * OAuth 2.0 Client IDs configured (Web application).
    * Google Calendar API enabled.

### Environment Variables

Create an `application-local.properties` file or export the following variables to your environment:

```properties
# Database Configuration
DB_HOST=jdbc:postgresql://<your-db-host>:<port>/<dbname>
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password

# Google OAuth2 Credentials
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# Security Keys
JWT_SECRET=a_very_long_secure_random_base64_string_for_jwt
TOKEN_ENCRYPTION_KEY=a_32_byte_base64_encoded_string_for_aes_gcm

Note: The TOKEN_ENCRYPTION_KEY must be a valid base64-encoded 256-bit (32-byte) key.

Installation & Execution

1. git clone [https://github.com/yourusername/cpsync-backend.git](https://github.com/yourusername/cpsync-backend.git)
    cd cpsync-backend
    ```

2.  **Build the project:**
```bash
    ./mvnw clean install
    ```

3.  **Run the application:**
```bash
    ./mvnw spring-boot:run
    ```

Flyway will automatically initialize the database schema on startup. The application will be available at `http://localhost:8080`.

---

## 📡 API Reference

All `/api/**` endpoints (except public contest fetches) require a valid `Authorization: Bearer <JWT>` header.

### Authentication & User
* `GET /oauth2/authorization/google` - Initiates Google OAuth2 login flow.
* `GET /api/user/profile` - Fetches the current user's profile and preferences.
* `PUT /api/user/platforms` - Updates user's enabled competitive programming platforms.
* `PUT /api/user/pause` - Temporarily halts background calendar syncing.
* `PUT /api/user/resume` - Resumes background calendar syncing.

### Contests & Sync
* `GET /api/contests` - Fetches all upcoming contests across all platforms (Public).
* `POST /api/sync` - Manually triggers a Google Calendar sync for the authenticated user.

---

## 📂 Project Structure

```text
src/main/java/com/cpsync/cpsync_backend/
├── config/         # Security, Cache, and OAuth2 configurations
├── controller/     # REST API endpoints
├── dto/            # Data Transfer Objects (Requests & Responses)
├── exception/      # Global Exception Handling
├── model/          # JPA Entities (User, SyncedEvent, Platform Preferences)
├── repository/     # Spring Data JPA Repositories
├── scheduler/      # CRON jobs for background tasks
├── security/       # JWT Filters and OAuth2 Success Handlers
└── service/        # Business logic 
    └── impl/       # Platform-specific fetchers (Codeforces, LeetCode, etc.)