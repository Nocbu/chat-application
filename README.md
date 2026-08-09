# 💬 Chat Application

A full-stack real-time chat application built with **Spring Boot** and a lightweight **HTML, CSS & JavaScript** frontend. The application supports user authentication, direct messaging, conversations, online presence, file attachments, and OAuth2-based authentication.

## 🚀 Features

* 🔐 User registration and login
* 🔑 Secure password validation
* 🌐 OAuth2 authentication
* 💬 Real-time messaging
* 👤 Direct messaging between users
* 🗂️ Conversation management
* 🟢 Online/offline presence tracking
* 📎 File attachments in conversations
* 👨‍💼 User/admin actions
* 💾 Persistent data storage with MongoDB
* 🖥️ Simple and responsive HTML, CSS & JavaScript frontend

## 🛠️ Tech Stack

### Backend

* **Java**
* **Spring Boot**
* **Spring Security**
* **Spring Data**
* **OAuth2**
* **Maven**

### Database

* **MongoDB**

### Frontend

* **HTML5**
* **CSS3**
* **JavaScript**

## 🏗️ Project Structure

The backend follows a layered architecture to keep the application organized and maintainable.

```text
src/
└── main/
    └── java/
        └── com/example/chat_application/
            ├── DTO/
            ├── Repositories/
            ├── Services/
            ├── Utilities/
            ├── config/
            └── ChatApplication.java
```

### Main Components

**DTOs**
Used for transferring data between the client and backend while keeping API requests and responses structured.

**Repositories**
Responsible for database interactions involving users, conversations, messages, and file attachments.

**Services**
Contains the application's core business logic, including:

* User management
* Direct messaging
* Conversation management
* Chat messages
* File storage
* User presence

**Security & Configuration**
Handles authentication, authorization, OAuth2 integration, and application configuration.

## 🔄 Application Flow

```text
        ┌──────────────────────┐
        │      Frontend        │
        │ HTML / CSS / JS      │
        └──────────┬───────────┘
                   │
                   │ HTTP / WebSocket
                   ▼
        ┌──────────────────────┐
        │    Spring Boot API   │
        │                      │
        │ Controllers /        │
        │ Services / Security  │
        └──────────┬───────────┘
                   │
                   ▼
        ┌──────────────────────┐
        │       MongoDB        │
        │ Users / Messages /   │
        │ Conversations / Data │
        └──────────────────────┘
```

## ⚙️ Getting Started

### Prerequisites

Make sure you have the following installed:

* Java 17+
* Maven
* MongoDB
* Git

### 1. Clone the repository

```bash
git clone https://github.com/Nocbu/chat-application.git
```

### 2. Navigate to the project

```bash
cd chat-application
```

### 3. Configure MongoDB

Make sure MongoDB is running locally or provide your MongoDB connection string in the application's configuration.

Update the relevant configuration in:

```text
src/main/resources/application.properties
```

> Do not commit passwords, API keys, OAuth credentials, or other secrets to GitHub.

### 4. Run the application

Using Maven:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

The backend will start on the configured application port.

## 🔒 Security

The application includes authentication and authorization mechanisms using **Spring Security** along with OAuth2 support.

Sensitive configuration values should be provided through environment variables or local configuration rather than committed to the repository.

## 📌 Future Improvements

Some features that could be added or improved in future versions:

* Message read receipts
* Typing indicators
* Message reactions
* Message editing and deletion
* Push notifications
* Image previews
* Improved mobile responsiveness
* Automated testing
* Docker support
* Cloud deployment

## 👨‍💻 Author

**Kishan Baghel**

* GitHub: [Nocbu](https://github.com/Nocbu)

---

⭐ If you found this project interesting, consider giving the repository a star!
