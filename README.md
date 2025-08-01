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

* [Download Docker](https://www.docker.com/products/docker-desktop)
* [Install WSL 2 (for Windows)](https://learn.microsoft.com/en-us/windows/wsl/install-manual)
* [Install Docker Desktop](https://docs.docker.com/desktop/install/windows-install/)

#### 2. Install IntelliJ IDEA

* [Download IntelliJ IDEA Ultimate or Community Edition](https://www.jetbrains.com/idea/download/?section=windows)

#### 3. Clone the Application

* In IntelliJ IDEA:
  `File -> New -> Project from Version Control`
* Paste the project URL in the pop-up window:
  `https://github.com/ProtsD/Ads-Online.git`
* Click **Clone**

#### 4. Run the Frontend

```bash
docker run -p 3000:3000 --rm ghcr.io/dmitry-bizin/front-react-avito:v1.18
```

* Frontend will be available at: [http://localhost:3000](http://localhost:3000)

---

### 👤 Author

- Denis Prots