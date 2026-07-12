# Enterprise TransitOps System (Hackhathon)

An Enterprise Transport Operations Management System built with Spring Boot 3, Thymeleaf, and Bootstrap 5.

## Tech Stack
- Java 17
- Spring Boot 3.2+
- Spring MVC, Security, Data JPA
- Hibernate
- MySQL
- Maven
- Thymeleaf
- Bootstrap 5 & Font Awesome
- Chart.js
- Apache POI, OpenPDF

## Modules
1. **Vehicle Management** - Manage fleet, tracking status and capacity.
2. **Driver Management** - Manage driver profiles, licenses, and safety scores.
3. **Trip Management** - Dispatch, track, and complete trips.
4. **Maintenance** - Track vehicle repairs and costs.
5. **Fuel Logs** - Track fuel consumption and expenses.
6. **Expense Management** - Additional fleet expense tracking.
7. **Reports** - Generate and export PDF and Excel reports.
8. **RBAC** - Complete role-based access control (Admin, Fleet Manager, Dispatcher, Financial, Safety).

## Setup Instructions
1. Ensure MySQL is running on localhost:3306.
2. Create database `hackhathon`.
   ```sql
   CREATE DATABASE hackhathon;
   ```
3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
4. The database schema will be generated automatically by Hibernate (since `spring.jpa.hibernate.ddl-auto=update` is set in application.properties).
