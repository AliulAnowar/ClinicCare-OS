# Security Policy

## Supported Versions

The following table details which versions of **ClinicCare OS** currently receive security updates:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

---

## Reporting a Vulnerability

We take the security of **ClinicCare OS** seriously. If you discover a security vulnerability—such as SQL injection risks, exposed database credentials, unauthorized access in authentication flows, or sensitive data exposure across departments—please report it responsibly.

### How to Report

1. **Do NOT** create a public GitHub issue for security vulnerabilities.
2. Submit a private security advisory via the repository's **Security** tab on GitHub, or send an email directly to the project maintainer.
3. Include clear details and steps to reproduce the issue, such as:
   * Affected component or class (e.g., `DBConnection.java`, `AdminDashboardController.java`).
   * Proof-of-concept (PoC) code or SQL query snippets.
   * Expected vs. actual security behavior.

### What to Expect

* **Acknowledgment**: You will receive an initial response within 48 hours.
* **Assessment**: The issue will be triaged and assessed within 5 business days.
* **Patch & Disclosure**: Once resolved, a fix will be pushed to the `main` branch, and a release advisory will acknowledge your contribution (if requested).

---

## Security Best Practices for Deployment

* **Database Credentials**: Never commit production passwords or API secrets to public repositories. Ensure `com/clinic/util/DBConnection.java` uses environment variables or a local configuration file ignored by `.gitignore`.
* **Least Privilege Access**: Configure the MySQL user for the clinic application with only the required privileges (`SELECT`, `INSERT`, `UPDATE`) on the `clinic_db` database.
* **Network Isolation**: When running in a multi-terminal clinic setup (Analytics, Lab Center, Pharmacy), ensure DB connections are routed through a secure, encrypted local network (LAN/VPN).