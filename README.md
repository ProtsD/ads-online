# Ads-Online Application

**Ads-Online** is a platform where you can easily buy or sell goods. You can post an ad for a brand-new or pre-owned item.
To get started, you need to create an account and log in to the application.
Once authorized, you will be able to create and edit ads, as well as update your account information.

---

### ✅ Available Features

1. Create, update, and delete ads
2. Upload, update, and view ad images
3. Add, update, and delete comments on ads
4. View all available ads or ads posted by the current user
5. View detailed information about a specific ad
6. View all comments on a selected ad

---

### 🔐 Security

Implemented using **Spring Security**.

#### Roles:

1. **Anonymous User** – unauthenticated users can view all available ads
2. **User** – can manage only their own ads
3. **Admin** – can manage all ads

---

### ⚙️ Technology Stack

* [Java SE 21](https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html)
* [Spring Boot](https://spring.io/projects/spring-boot)
* [Spring Web Services](https://spring.io/projects/spring-ws)
* [Spring Security](https://spring.io/projects/spring-security)
* [Hibernate](https://hibernate.org/)
* [PostgreSQL](https://www.postgresql.org/)
* [Liquibase](https://www.liquibase.org/)
* [Docker](https://www.docker.com/)

---

### 🚀 Installation Steps

#### 1. Install Docker Desktop
1. [Download Docker Desktop](https://www.docker.com/products/docker-desktop) and install it.
2. For Windows, install [WSL 2](https://learn.microsoft.com/en-us/windows/wsl/install-manual).
3. Follow the [official installation guide](https://docs.docker.com/desktop/install/windows-install) to complete setup.

#### 2. Set up the project
1. Download the `docker-compose.yml` and `Dockerfile`.
2. Place them in the folder: `C:\ads-online`
3. Open **Command Prompt**: Press **Win + R**, type `CMD`, and hit Enter.
4. Navigate to the project directory:
   ```bash
   cd C:\ads-online
5. Start the containers:
   ```bash
   docker-compose up
   
#### 3. Access the application
* Frontend: http://localhost:3000


---

### 👤 Author

- Denis Prots