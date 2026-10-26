# Campus Voice | Complaint Management System

Campus Voice is a web-based complaint management system designed for academic institutions. Students and faculty can submit grievances and monitor their complete progress history in real time. Department administrators can review, filter, and transition complaints strictly within their assigned department, maintaining an append-only audit trail with required remarks for closure.

---

## 1. Prerequisites

- **Java**: Java 17 or higher (Java 27 runtime supported)
- **Build Tool**: Apache Maven 3.9+ (pre-configured at `C:\Users\devika\maven\apache-maven-3.9.9\bin` and accessible via `mvn` or `.\mvnw.cmd`)
- **Database**: MySQL Server 8.0 (running locally on port 3306)
- **Browser**: Modern web browser (Chrome, Firefox, Edge)

---

## 2. Database Setup & Secure Credentials Configuration

### 2.1 Database Creation
The normalized schema uses the `campus_voice` database. You can create the database manually using the MySQL CLI or MySQL Workbench:

```sql
CREATE DATABASE IF NOT EXISTS campus_voice
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

A complete reference schema script is provided in [`schema.sql`](file:///d:/download/campusvoice/schema.sql).

### 2.2 Secure Local Credentials Configuration
To prevent committing passwords or sensitive credentials into source control, the application is configured with environment variable overrides in [`src/main/resources/application.yml`](file:///d:/download/campusvoice/src/main/resources/application.yml).

| Environment Variable | Description | Default Fallback |
| :--- | :--- | :--- |
| `DB_URL` | JDBC URL for MySQL connection | `jdbc:mysql://localhost:3306/campus_voice?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `DB_USERNAME` | MySQL database username | `root` |
| `DB_PASSWORD` | MySQL database password | `root` |
| `JWT_SECRET` | 256-bit secret key for signing JWTs | Pre-configured secure default |
| `JWT_EXPIRATION_MS` | JWT token validity in milliseconds | `86400000` (24 hours) |
| `PORT` | Backend HTTP server port | `8080` |
| `CORS_ALLOWED_ORIGINS` | Narrow allowed frontend origins | `http://localhost:5500,http://127.0.0.1:5500,http://localhost:3000,http://127.0.0.1:3000,http://localhost:8080,http://127.0.0.1:8080` |

An example environment file template is provided in [`.env.example`](file:///d:/download/campusvoice/.env.example).

#### Setting Credentials in PowerShell (Current Session):
```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_actual_mysql_password"
$env:JWT_SECRET  = "your_custom_secure_256_bit_secret_key"
```

#### Setting Credentials in Windows (Persistent User Environment):
```powershell
[System.Environment]::SetEnvironmentVariable("DB_PASSWORD", "your_actual_mysql_password", "User")
```

---

## 3. Database Architecture (5 Main Tables)

The schema adheres strictly to normalized relational design:

1. **`departments`**:
   - `id` (BIGINT, PK, Auto Increment)
   - `name` (VARCHAR 100, UNIQUE, NOT NULL)
   - `description` (VARCHAR 255)
   - `created_at` (DATETIME, NOT NULL)

2. **`categories`**:
   - `id` (BIGINT, PK, Auto Increment)
   - `name` (VARCHAR 100, UNIQUE, NOT NULL)
   - `created_at` (DATETIME, NOT NULL)

3. **`users`**:
   - `id` (BIGINT, PK, Auto Increment)
   - `name` (VARCHAR 100, NOT NULL)
   - `email` (VARCHAR 150, UNIQUE, NOT NULL)
   - `password_hash` (VARCHAR 255, NOT NULL) - *Hashed with BCrypt; never returned in API*
   - `role` (VARCHAR 50, NOT NULL) - `STUDENT`, `FACULTY`, `DEPARTMENT_ADMIN`
   - `department_id` (BIGINT, FK -> `departments.id`, NULL for students/faculty)
   - `created_at` (DATETIME, NOT NULL)

4. **`complaints`**:
   - `id` (VARCHAR 20, PK) - Server-generated format: `CV-0001`, `CV-0002`...
   - `user_id` (BIGINT, FK -> `users.id`, NOT NULL)
   - `category_id` (BIGINT, FK -> `categories.id`, NOT NULL)
   - `department_id` (BIGINT, FK -> `departments.id`, NOT NULL)
   - `title` (VARCHAR 120, NOT NULL)
   - `description` (TEXT, NOT NULL)
   - `priority` (VARCHAR 20, NOT NULL) - `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
   - `status` (VARCHAR 20, NOT NULL) - `PENDING`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `REJECTED`
   - `created_at` (DATETIME, NOT NULL)
   - `updated_at` (DATETIME, NOT NULL)

5. **`complaint_history`** (Append-only Audit Trail):
   - `id` (BIGINT, PK, Auto Increment)
   - `complaint_id` (VARCHAR 20, FK -> `complaints.id`, NOT NULL)
   - `acting_user_id` (BIGINT, FK -> `users.id`, NULLABLE for system actions)
   - `acting_user_name` (VARCHAR 100, NOT NULL)
   - `old_status` (VARCHAR 20, NULLABLE on submission)
   - `new_status` (VARCHAR 20, NOT NULL)
   - `remarks` (VARCHAR 1500, NULLABLE except required on RESOLVED/REJECTED)
   - `created_at` (DATETIME, NOT NULL)

*Why no additional session table was added*: The backend implements standard stateless HMAC-SHA256 JWT bearer authentication (`Authorization: Bearer <token>`). Tokens are validated cryptographically on each request, maintaining security without requiring redundant session tables in the database.

---

## 4. Business Rules & Enforcement

1. **Role Authorization**:
   - Students and faculty can create complaints and retrieve **only** their own complaints (`/api/complaints/my`).
   - Department Administrators can query and update **only** complaints assigned to their department (`/api/admin/complaints`). Attempts to view or update complaints belonging to other departments return `403 Forbidden`.
   - Public registration (`/api/auth/signup`) permits only `Student` and `Faculty` roles. Department Administrator accounts cannot be created publicly and must be provisioned through seed scripts or administrative setup.
2. **Server-Generated IDs**:
   - Complaint IDs are strictly generated by the backend sequentially (`CV-0001`, `CV-0002`...) rather than relying on browser counters.
3. **Automated Department Routing & Initial Audit**:
   - On submission, the complaint is routed to the selected department in `ASSIGNED` status.
   - Two audit entries are atomically committed:
     1. `null -> PENDING` ("Complaint submitted.")
     2. `PENDING -> ASSIGNED` ("Assigned to [Department] department.")
4. **Status Transitions & Validation**:
   - Allowed lifecycle: `PENDING` → `ASSIGNED` → `IN_PROGRESS` → `RESOLVED` or `REJECTED`.
   - `RESOLVED` and `REJECTED` are terminal states; closed complaints cannot be altered.
   - Transitioning to `RESOLVED` or `REJECTED` strictly requires non-empty remarks.
5. **Exact Priority Calculation**:
   - Preserves the existing `nextPriority` calculation:
     - Base score: `Harassment` (3 = CRITICAL), `Infrastructure` / `Lab Equipment` (2 = HIGH), `Hostel` / `Examination` / `Academics` (1 = MEDIUM), `Library` / `Other` (0 = LOW).
     - Urgent keywords regex (`\b(urgent|danger|fire|unsafe)\b`) adds +1 (clamped at CRITICAL).

---

## 5. Seed Data & Demo Accounts

The application automatically seeds realistic college data on first startup if the database is empty:

### Departments
Computer Science, Electrical, Mechanical, Civil, Hostel, Maintenance, Library, Administration.

### Complaint Categories
Infrastructure, Academics, Hostel, Harassment, Lab Equipment, Library, Examination, Other.

### Demo Credentials
All seeded demo accounts share the demonstration password: `Password123!`

| Role | Name | Email | Password | Department |
| :--- | :--- | :--- | :--- | :--- |
| **Student** | Aarav Sharma | `student@campus.edu` | `Password123!` | *N/A* |
| **Faculty** | Dr. Priya Raman | `faculty@campus.edu` | `Password123!` | *N/A* |
| **Department Admin** | Prof. Vikram Mehta | `admin.cs@campus.edu` | `Password123!` | Computer Science |
| **Department Admin** | Rajesh Kumar | `admin.maint@campus.edu` | `Password123!` | Maintenance |
| **Department Admin** | Sunita Rao | `admin.hostel@campus.edu` | `Password123!` | Hostel |
| **Department Admin** | Anand Verma | `admin.library@campus.edu` | `Password123!` | Library |

---

## 6. REST API Documentation

### Authentication Method
Standard **JWT Bearer Token**. Include the token in the HTTP header for protected endpoints:
```http
Authorization: Bearer <token>
```

---

### Endpoints

#### 1. Public Catalog & Reference
- **`GET /api/departments`**: Returns list of active departments.
- **`GET /api/categories`**: Returns list of complaint categories.

#### 2. Authentication
- **`POST /api/auth/signup`**
  - **Request Body**:
    ```json
    {
      "name": "Devika Nair",
      "email": "devika@campus.edu",
      "password": "Password123!",
      "confirmPassword": "Password123!",
      "role": "Student"
    }
    ```
  - **Response (201 Created)**:
    ```json
    {
      "token": "eyJhbGciOi...",
      "tokenType": "Bearer",
      "user": {
        "id": 7,
        "name": "Devika Nair",
        "email": "devika@campus.edu",
        "role": "Student",
        "department": ""
      }
    }
    ```

- **`POST /api/auth/login`**
  - **Request Body**:
    ```json
    {
      "email": "student@campus.edu",
      "password": "Password123!",
      "role": "Student"
    }
    ```
  - **Response (200 OK)**:
    ```json
    {
      "token": "eyJhbGciOi...",
      "tokenType": "Bearer",
      "user": {
        "id": 1,
        "name": "Aarav Sharma",
        "email": "student@campus.edu",
        "role": "Student",
        "department": ""
      }
    }
    ```

- **`GET /api/auth/me`** (Requires Bearer token): Returns the current authenticated user's profile.
- **`POST /api/auth/logout`**: Invalidates client session.

---

#### 3. Student & Faculty Complaints
- **`POST /api/complaints`** (Requires `STUDENT` or `FACULTY` role)
  - **Request Body**:
    ```json
    {
      "title": "Broken projector in CS Lab 3",
      "category": "Lab Equipment",
      "department": "Computer Science",
      "description": "HDMI ceiling projector flickers red and shuts off after 5 minutes."
    }
    ```
  - **Response (201 Created)**: Returns the newly created `ComplaintResponse` object with server-assigned ID (`CV-0001`), computed `priority`, and initial `audit` entries.

- **`GET /api/complaints/my`** (Requires `STUDENT` or `FACULTY` role)
  - **Response (200 OK)**: Array of complaint records submitted by the authenticated user.

- **`GET /api/complaints/{id}`**: Returns complaint details if the caller is the owner or the assigned department administrator.
- **`GET /api/complaints/{id}/history`**: Returns the append-only status timeline.

---

#### 4. Department Administrator Queue
- **`GET /api/admin/complaints`** (Requires `DEPARTMENT_ADMIN` role)
  - **Query Parameters**:
    - `status` (Optional): Filter by status (`PENDING`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `REJECTED`)
    - `priority` (Optional): Filter by priority (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`)
    - `search` (Optional): Search term matching ID, title, description, category, or owner name
  - **Response (200 OK)**: List of department complaints ordered by Priority (CRITICAL first) and Submission Date.

- **`PUT /api/admin/complaints/{id}/status`** (Requires `DEPARTMENT_ADMIN` role)
  - **Request Body**:
    ```json
    {
      "newStatus": "IN_PROGRESS",
      "remarks": "Technician dispatched to inspect equipment."
    }
    ```
  - **Response (200 OK)**: Returns the updated `ComplaintResponse` with appended audit history.

---

## 7. How to Run the Application

### Option A: Run Backend Server Directly
In your PowerShell terminal:
```powershell
.\mvnw.cmd spring-boot:run
```
*(Or `mvn spring-boot:run`)*

Once started, open your browser and navigate to:
```
http://localhost:8080/index.html
```
The Spring Boot server serves both the REST API and the frontend HTML/CSS/JS pages directly!

### Option B: Run Frontend via Live Server / Separate Port
If you prefer running the frontend using VS Code Live Server on `http://127.0.0.1:5500`:
- Narrow CORS is already enabled in the backend for `http://localhost:5500`, `http://127.0.0.1:5500`, `http://localhost:3000`, and `http://127.0.0.1:3000`.
- The frontend [`js/app.js`](file:///d:/download/campusvoice/js/app.js) automatically detects the port and routes all API requests to `http://localhost:8080/api`.

---

## 8. Frontend Integration Summary

- **Source of Truth**: `localStorage` was completely replaced by server-managed persistence and authentication.
- **Session Handling**: JWT bearer token and user profile are securely stored in `sessionStorage` and attached to all API requests via the `Authorization: Bearer <token>` header.
- **Design & Layout**: All HTML structure, IDs, CSS class names, modals, and responsive layout styling remain 100% intact as originally provided.
