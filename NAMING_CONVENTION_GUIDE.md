# UMS IAM Platform - Developer Naming Convention Guide

**Use this guide daily when writing code, creating files, or configuring the platform.**

---

## 🎯 Quick Reference Cheat Sheet

```
┌─────────────────────────────────────────────────────────────┐
│              NAMING CONVENTION QUICK LOOKUP                  │
├─────────────────────────────────────────────────────────────┤
│ WHAT                          │ CONVENTION    │ EXAMPLE      │
├───────────────────────────────┼───────────────┼──────────────┤
│ Microservices                 │ kebab-case    │ auth-service │
│ Docker containers             │ ums-kebab     │ ums-auth     │
│ Docker volumes                │ snake_case    │ mysql_data   │
│ Docker networks               │ kebab-case    │ ums-network  │
│ Databases                     │ snake_case    │ auth_db      │
│ Tables                        │ snake_case    │ user_roles   │
│ Columns                       │ snake_case    │ created_at   │
│ Environment variables         │ UPPER_SNAKE   │ MYSQL_HOST   │
│ Java packages                 │ lowercase     │ com.ums.auth │
│ Java classes                  │ PascalCase    │ AuthService  │
│ Java methods                  │ camelCase     │ getUserId()  │
│ Java variables                │ camelCase     │ userId       │
│ JSON fields                   │ camelCase     │ userId       │
│ REST endpoints                │ lowercase     │ /api/v1/users│
│ Event names                   │ dot.notation  │ user.created │
│ Config files                  │ kebab-dash    │ auth-*.yml   │
│ Documentation                 │ kebab-dash    │ QUICK*.md    │
│ Frontend folders              │ lowercase     │ components   │
│ Frontend components           │ PascalCase    │ UserTable    │
│ Frontend services             │ camelCase     │ authService  │
└─────────────────────────────────────────────────────────────┘
```

---

## 📚 Detailed Guidelines by Layer

### BACKEND LAYER

#### Java Package Structure
```
com.ums.{service-name}
├── com.ums.auth          ✓ (not com.ums.authentication)
├── com.ums.user          ✓
├── com.ums.authorization ✓
├── com.ums.organization  ✓
├── com.ums.notification  ✓
├── com.ums.admin         ✓
├── com.ums.audit         ✓
├── com.ums.gateway       ✓
└── com.ums.common        ✓

Sub-packages (lowercase):
├── controller            ✓ (not Controllers)
├── service               ✓ (not Services)
├── service.impl          ✓
├── repository            ✓ (not Repositories)
├── entity                ✓ (not Entities)
├── dto                   ✓ (not DTOs)
├── mapper                ✓ (not Mappers)
├── config                ✓ (not Config)
├── security              ✓
├── exception             ✓
├── event                 ✓ (or events)
├── filter                ✓
├── interceptor           ✓
├── constant              ✓ (or constants)
└── util                  ✓ (or utils)
```

#### Java Class Naming (PascalCase)
```java
// ✓ CORRECT
public class AuthenticationService { }
public interface UserRepository { }
public record UserDTO(...) { }
public enum UserRole { }
public class GlobalExceptionHandler { }
public class JwtAuthenticationFilter { }
public class SecurityConfig { }
public class UserController { }

// ❌ WRONG
public class authenticationService { }
public class Authentication_Service { }
public class authenticationservice { }
public class auth_service { }
```

#### Java Method/Variable Naming (camelCase)
```java
// ✓ CORRECT
public String generateAccessToken(UserPrincipal principal) {
    String jwtSecret = this.jwtSecret;
    long expirationTime = System.currentTimeMillis() + 900000;
    return Jwts.builder()
        .setSubject(principal.getUserId())
        .setIssuedAt(new Date())
        .setExpiration(new Date(expirationTime))
        .signWith(signingKey)
        .compact();
}

public User getUserById(String userId) {
    return userRepository.findById(userId);
}

private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(secretKey.getBytes());
}

// ❌ WRONG
public String GenerateAccessToken(...) { }        // Method in PascalCase
public String generate_access_token(...) { }      // Method in snake_case
String UserID = "123";                             // Variable in PascalCase
String user_id = "123";                            // Variable in snake_case
```

#### Java Constants (UPPER_SNAKE_CASE)
```java
// ✓ CORRECT
public static final String JWT_PREFIX = "Bearer ";
public static final long TOKEN_EXPIRY_MS = 900000L;
public static final String AUTHORIZATION_HEADER = "Authorization";
public static final String BEARER_PREFIX = "Bearer ";

// ❌ WRONG
public static final String jwtPrefix = "Bearer ";
public static final String JWT_prefix = "Bearer ";
public static final String jwt_prefix = "Bearer ";
```

#### Spring Configuration
```yaml
# ✓ CORRECT - application.yml
spring:
  application:
    name: authentication-service
  datasource:
    url: jdbc:mysql://...
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

# ❌ WRONG
spring:
  application:
    name: authentication_service
    name: AuthenticationService
    name: AuthService
```

#### Entity/DTO Field Naming (camelCase)
```java
// ✓ CORRECT
@Entity
@Table(name = "users")
public class User {
    @Id
    private String userId;
    
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    @Column(name = "email_verified")
    private Boolean emailVerified;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

// DTO
public record UserDTO(
    String userId,
    String firstName,
    String lastName,
    String email,
    Boolean emailVerified,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) { }

// ❌ WRONG
@Column(name = "FirstName")         // Wrong case in column name
private String FirstName;            // Wrong case in field
private String first_name;           // Wrong case in field
```

---

### DATABASE LAYER

#### Database Naming (snake_case)
```sql
-- ✓ CORRECT

-- Databases
CREATE DATABASE auth_db;
CREATE DATABASE user_db;
CREATE DATABASE organization_db;

-- Tables
CREATE TABLE users (
    user_id VARCHAR(36) PRIMARY KEY,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255) UNIQUE,
    email_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id VARCHAR(36),
    role_id VARCHAR(36),
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE organization_members (
    organization_id VARCHAR(36),
    user_id VARCHAR(36),
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (organization_id, user_id)
);

-- ❌ WRONG
CREATE DATABASE Auth_DB;            -- Mixed case
CREATE DATABASE authDB;             -- camelCase

CREATE TABLE Users (...)            -- PascalCase
CREATE TABLE user_Role (...)        -- Mixed case
CREATE TABLE USERS (...)            -- UPPERCASE
```

#### Column Naming (snake_case)
```sql
-- ✓ CORRECT
SELECT 
    user_id,
    first_name,
    last_name,
    email_verified,
    created_at,
    updated_at
FROM users;

-- ❌ WRONG
SELECT userId, firstName FROM users;        -- camelCase
SELECT FirstName, LastName FROM users;      -- PascalCase
SELECT FIRSTNAME, LASTNAME FROM users;      -- UPPERCASE
```

---

### DOCKER & INFRASTRUCTURE LAYER

#### Docker Compose Service Names (kebab-case)
```yaml
# ✓ CORRECT
services:
  mysql:
    container_name: ums-mysql
  
  authentication-service:
    container_name: ums-authentication-service
  
  user-service:
    container_name: ums-user-service
  
  api-gateway:
    container_name: ums-api-gateway

# ❌ WRONG
services:
  MySQL:
    container_name: MySQL
  
  authentication_service:
    container_name: authentication_service
  
  AuthenticationService:
    container_name: AuthenticationService
```

#### Docker Volume Names (snake_case)
```yaml
# ✓ CORRECT
volumes:
  mysql_data:
    driver: local
  
  redis_data:
    driver: local
  
  prometheus_data:
    driver: local

# ❌ WRONG
volumes:
  mysqlData:
  mysql-data:
  MySQL_DATA:
```

#### Docker Network Names (kebab-case)
```yaml
# ✓ CORRECT
networks:
  ums-network:
    name: ums-network
    driver: bridge

# ❌ WRONG
networks:
  ums_network:
  UMSNetwork:
  ums--network:
```

#### Environment Variables (UPPER_SNAKE_CASE)
```bash
# ✓ CORRECT
MYSQL_ROOT_PASSWORD=secure_password
MYSQL_USER=ums_user
MYSQL_PASSWORD=ums_password
MYSQL_HOST=ums-mysql
MYSQL_PORT=3306
AUTH_DB_NAME=auth_db
JWT_SECRET=your_secret_key
JWT_EXPIRATION_MS=900000
SPRING_PROFILES_ACTIVE=docker
LOG_LEVEL=INFO
OTEL_EXPORTER_OTLP_ENDPOINT=http://ums-jaeger:4317

# ❌ WRONG
mysql_root_password=...             # lowercase
MysqlRootPassword=...               # camelCase
MySql_Root_Password=...             # Mixed case
MYSQL-ROOT-PASSWORD=...             # Hyphens
```

#### Docker Image Tagging (service-name:version)
```bash
# ✓ CORRECT
docker build -t authentication-service:1.0 .
docker build -t user-service:1.0 .
docker build -t api-gateway:1.0 .
docker build -t config-service:1.0 .

# ❌ WRONG
docker build -t authentication_service:1.0 .
docker build -t AuthenticationService:1.0 .
docker build -t authenticationservice:1.0 .
```

---

### CONFIGURATION & CLOUD

#### Spring Cloud Config Files (kebab-case)
```
backend/config-repo/
├── authentication-service.yml           ✓
├── authentication-service-dev.yml       ✓
├── authentication-service-docker.yml    ✓
├── authentication-service-prod.yml      ✓
├── user-service.yml                     ✓
├── user-service-dev.yml                 ✓
├── user-service-docker.yml              ✓
├── user-service-prod.yml                ✓
├── authorization-service-docker.yml     ✓
├── organization-service-docker.yml      ✓
├── api-gateway-docker.yml               ✓
└── admin-service-docker.yml             ✓

# ❌ WRONG
authentication_service-docker.yml
AuthenticationServiceDocker.yml
authenticationservice-docker.yml
authentication-service_docker.yml
```

#### Spring Application Names
```yaml
# ✓ CORRECT
spring:
  application:
    name: authentication-service

spring:
  application:
    name: user-service

# ❌ WRONG
spring:
  application:
    name: authentication_service

spring:
  application:
    name: AuthenticationService
```

---

### REST API LAYER

#### REST Endpoint Paths (lowercase, plural nouns)
```
# ✓ CORRECT
GET     /api/v1/users                      # Get all users
POST    /api/v1/users                      # Create user
GET     /api/v1/users/{userId}             # Get user by ID
PUT     /api/v1/users/{userId}             # Update user
DELETE  /api/v1/users/{userId}             # Delete user

GET     /api/v1/organizations              # Get organizations
GET     /api/v1/organizations/{orgId}      # Get org
GET     /api/v1/organizations/{orgId}/members  # Get org members

GET     /api/v1/roles                      # Get all roles
POST    /api/v1/roles                      # Create role

POST    /api/v1/auth/login                 # Login
POST    /api/v1/auth/register              # Register
POST    /api/v1/auth/refresh               # Refresh token

GET     /api/v1/admin/users                # Admin users
GET     /api/v1/admin/dashboard            # Admin dashboard

GET     /api/v1/audits                     # Get audit logs

# ❌ WRONG
GET     /api/v1/user                       # Should be plural
GET     /api/v1/Users                      # Should be lowercase
GET     /api/v1/user_details               # Should be camelCase
GET     /api/v1/getUserById                # Should use noun, not verb
```

#### JSON Response Field Names (camelCase)
```json
// ✓ CORRECT
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "firstName": "Sushant",
  "lastName": "Kumar",
  "email": "sushant@example.com",
  "emailVerified": true,
  "isActive": true,
  "organizationId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "roleIds": ["admin", "user"],
  "createdAt": "2026-06-23T10:30:00Z",
  "updatedAt": "2026-06-23T11:45:00Z"
}

// ❌ WRONG
{
  "user_id": "...",                   // snake_case
  "FirstName": "...",                 // PascalCase
  "User_Id": "...",                   // Mixed case
  "userid": "...",                    // Joined
  "USERID": "..."                     // UPPERCASE
}
```

---

### EVENT-DRIVEN ARCHITECTURE

#### Event/Message Names (dot notation)
```
# ✓ CORRECT
user.created
user.updated
user.deleted
user.email.verified

role.created
role.assigned
role.removed

organization.created
organization.member.added
organization.member.removed

notification.email.sent
notification.sms.sent

audit.log.created

# ❌ WRONG
user_created
UserCreated
userCreated
user-created
user.Created
user..created
```

#### Event Handler Method Names (camelCase with On prefix)
```java
// ✓ CORRECT
@EventListener
public void onUserCreated(UserCreatedEvent event) { }

@RabbitListener(queues = "notification.user.created.queue")
public void onUserCreatedEvent(UserCreatedEvent event) { }

public void handleUserRegistration(UserCreatedEvent event) { }

// ❌ WRONG
public void OnUserCreated(...) { }         // PascalCase
public void on_user_created(...) { }       // snake_case
public void OnUserCreatedEvent(...) { }    // Mixed case
```

---

### FRONTEND LAYER

#### Frontend Directory Structure (lowercase)
```
src/
├── api/                          ✓ (not APIs or Api)
│   ├── apiClient.ts             ✓ (camelCase)
│   └── services/
│       ├── authService.ts       ✓ (camelCase)
│       ├── userService.ts       ✓ (camelCase)
│       └── organizationService.ts
├── components/                   ✓ (lowercase folder)
│   ├── LoginForm.tsx            ✓ (PascalCase file)
│   ├── UserTable.tsx            ✓ (PascalCase file)
│   └── OrganizationDetails.tsx  ✓ (PascalCase file)
├── pages/                        ✓
│   ├── LoginPage.tsx            ✓
│   ├── DashboardPage.tsx        ✓
│   └── AdminPanel.tsx           ✓
├── store/                        ✓
│   ├── authStore.ts             ✓ (camelCase)
│   └── uiStore.ts               ✓ (camelCase)
├── hooks/                        ✓
│   ├── useAuth.ts               ✓ (camelCase with use prefix)
│   └── useUser.ts               ✓
├── utils/                        ✓ (or utils)
│   ├── formatDate.ts            ✓ (camelCase)
│   └── validateEmail.ts         ✓ (camelCase)
└── config/                       ✓
    └── environment.ts           ✓ (camelCase)
```

#### React Component Naming (PascalCase)
```tsx
// ✓ CORRECT
export function LoginPage() { }
export const UserTable: React.FC = () => { }
export const OrganizationDetailsModal = ({ orgId }: Props) => { }
export function AdminDashboard() { }

// ❌ WRONG
export function loginPage() { }            // camelCase
export function login_page() { }           // snake_case
export function LoginPageComponent() { }   // Redundant suffix
```

#### React Hook Naming (use prefix + camelCase)
```tsx
// ✓ CORRECT
const useAuth = () => { }
const useUser = (userId: string) => { }
const useOrganization = (orgId: string) => { }
const useFetch = (url: string) => { }

// ❌ WRONG
const Auth = () => { }                 // Missing 'use' prefix
const authHook = () => { }             // Missing 'use' prefix
const useAuthHook = () => { }          // Redundant 'Hook'
const UseAuth = () => { }              // PascalCase
```

#### TypeScript/JavaScript Variables (camelCase)
```typescript
// ✓ CORRECT
const userId = "123";
const firstName = "Sushant";
let isAuthenticated = false;
var organizationName = "Acme Corp";
const userRoleIds: string[] = ["admin", "user"];

// ❌ WRONG
const user_id = "123";                 // snake_case
const UserId = "123";                  // PascalCase
const USER_ID = "123";                 // UPPERCASE
const userId_ = "123";                 // Trailing underscore
```

#### TypeScript Interfaces/Types (PascalCase)
```typescript
// ✓ CORRECT
interface User {
  userId: string;
  firstName: string;
  lastName: string;
  emailVerified: boolean;
}

type UserRole = "admin" | "user" | "moderator";

interface ApiResponse<T> {
  data: T;
  message: string;
  success: boolean;
}

// ❌ WRONG
interface user { }                     // lowercase
interface IUser { }                    // I prefix (outdated)
interface UserInterface { }            // Redundant suffix
type user_role = ...;                  // snake_case
```

---

## 🔍 Common Mistakes to Avoid

| ❌ WRONG | ✓ CORRECT | Layer |
|----------|-----------|-------|
| `authentication_service` | `authentication-service` | Service name |
| `authentication-service` (container) | `ums-authentication-service` | Docker container |
| `authDB` | `auth_db` | Database name |
| `mysql_host` | `MYSQL_HOST` | Environment variable |
| `AuthenticationService` (package) | `com.ums.auth` | Java package |
| `get_user_id()` | `getUserId()` | Java method |
| `UserId` (variable) | `userId` | Java variable |
| `USER_ID` (variable) | `userId` | Java field |
| `/api/v1/user` | `/api/v1/users` | REST endpoint |
| `{userId: "123"}` | `{"userId": "123"}` | JSON (camelCase) |
| `user_created` | `user.created` | Event name |
| `loginPage` (component) | `LoginPage` | React component |
| `UseAuth` (hook) | `useAuth` | React hook |

---

## 📋 Pre-commit Checklist

Before committing code, verify:

- [ ] Service names use kebab-case
- [ ] Container names use ums-{kebab-case}
- [ ] Database/table/column names use snake_case
- [ ] Environment variables use UPPER_SNAKE_CASE
- [ ] Java packages use lowercase dot-separated
- [ ] Java classes use PascalCase
- [ ] Java methods/fields use camelCase
- [ ] JSON responses use camelCase
- [ ] REST endpoints use lowercase with plural nouns
- [ ] Events use dot notation
- [ ] Config files use {service}-{profile}.yml
- [ ] Frontend components use PascalCase
- [ ] Frontend services use camelCase

---

## 🎓 Examples by Layer

### Example 1: Creating a New Feature

**Feature**: Add user deactivation capability

**Correct naming across layers**:

```
Backend Java:
  Package: com.ums.user
  Class: UserDeactivationService
  Method: deactivateUser()
  Field: isActive

Database:
  Table: users
  Column: is_active
  Event table: user_events

Event:
  Name: user.deactivated
  Handler: onUserDeactivated()

REST API:
  Endpoint: PUT /api/v1/users/{userId}/deactivate
  Response: { "userId": "...", "isActive": false }

Config:
  File: user-service-docker.yml
  Property: spring.datasource.url

Frontend:
  Service: userService.ts
  Method: deactivateUser()
  Component: UserDeactivationModal.tsx
  Hook: useUserDeactivation()
```

### Example 2: Creating a New Service

**Service**: Order Management Service

**Correct naming across layers**:

```
Service Name: order-service
Container Name: ums-order-service
Database: order_db
Tables: orders, order_items, order_status
Package: com.ums.order
Main Class: OrderServiceApplication
Service Class: OrderService
Controller: OrderController
Repository: OrderRepository
Entity: Order, OrderItem
DTO: OrderDTO, OrderItemDTO
Events: 
  - order.created
  - order.shipped
  - order.delivered
REST Endpoints:
  - /api/v1/orders
  - /api/v1/orders/{orderId}
  - /api/v1/orders/{orderId}/items
Config File: order-service-docker.yml
Volume: order_data
```

---

## 🔗 References

- **Platform Documentation**: See STARTUP_GUIDE.md
- **Architecture Details**: See PRODUCTION_READY_SUMMARY.md
- **Docker Configuration**: See DOCKER_QUICK_REFERENCE.md
- **Naming Compliance Report**: See NAMING_CONVENTION_COMPLIANCE.md

---

**Last Updated**: 2026-06-23
**Status**: Active - Use for all new development
**Violations**: Report to Architecture Team
