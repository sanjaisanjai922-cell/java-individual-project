# Subscription Billing Management System

A Java desktop application for managing customers, subscription plans, recurring subscriptions, invoices, payments and billing reports. It uses Java 17, Maven, Swing and MySQL (XAMPP).

## Requirements
- JDK 17 or later
- Apache Maven
- XAMPP with MySQL enabled
- MySQL Connector/J (downloaded automatically by Maven)

## Run
1. Extract the ZIP file.
2. Open XAMPP Control Panel and start **MySQL**.
3. Ensure the MySQL username/password in `src/main/java/subscription/Db.java` match your setup. Default XAMPP settings are `root` with an empty password.
4. Open this project folder in IntelliJ IDEA, Eclipse or VS Code as a Maven project.
5. Run `subscription.Main`, or execute `mvn clean package` followed by `java -jar target/subscription-billing-management-system-1.0-jar-with-dependencies.jar`.
6. The application creates the `subscription_db` database and required tables automatically on first launch.

## Demo accounts
| Role | Email | Password |
|---|---|---|
| Administrator | admin@subscription.com | admin123 |
| Billing Staff | billing@subscription.com | bill123 |
| Customer | customer@example.com | cust123 |

## Main modules
- **Customer Accounts:** view customer profiles; administrator can add customers.
- **Subscription Plans:** create, activate and deactivate plans; customers can browse active plans.
- **Subscriptions:** subscribe to plans, view renewal dates and cancel subscriptions.
- **Invoices:** create first invoices at subscription time, generate invoices for subscriptions due for renewal, and track unpaid/paid invoices.
- **Payments:** record cash, card, UPI or bank-transfer payments and view transaction history.
- **Reports:** customer count, active subscriptions, unpaid invoices, total collections and outstanding balance.

## Demo workflow
1. Log in as administrator and inspect plans, customers and reports.
2. Log in as the demo customer and subscribe to Basic or Professional.
3. Open My Invoices and record a payment using UPI or CARD.
4. Log in as Billing Staff to view all subscriptions, invoices and payment history.
5. Use **Generate Due Invoices** to create invoices for subscriptions whose next billing date is due.

## Database
The database is named `subscription_db`. The application creates it and its tables automatically. A matching manual schema is available at `database/subscription_db.sql`.

## Notes
- This is an academic/demo billing application. Payment recording is simulated; it does not connect to a bank, UPI gateway or card processor.
- Passwords in this sample use SHA-256 for a simple classroom demonstration. A production deployment should use a password-hashing algorithm such as BCrypt, secure session handling and gateway-backed payment verification.
- The billing cycle options are MONTHLY, QUARTERLY and YEARLY.
