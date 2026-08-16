# Chat Application

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-24-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-Database-47A248?style=for-the-badge&logo=mongodb&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-010101?style=for-the-badge)
![OAuth2](https://img.shields.io/badge/OAuth2-Google%20Login-4285F4?style=for-the-badge&logo=google&logoColor=white)

A full-stack real-time chat application built with **Spring Boot** and a lightweight **HTML, CSS, and JavaScript** frontend. The application supports user authentication, direct messaging, conversation management, online presence, file attachments, message search, and OAuth2-based authentication.

## Overview

This project is designed like a production-style messaging platform. It combines secure authentication, real-time communication, persistent storage, admin moderation, file sharing, and search features in one application.

## Features

- User registration and login
- Secure password validation
- OAuth2 authentication with Google
- Real-time public messaging
- Direct messaging between users
- Conversation management for DMs
- Online/offline presence tracking
- File attachments and file metadata storage
- Admin actions such as delete, clear, ban, unban, and kick
- Message search for faster chat lookup
- MongoDB-based persistence
- Simple and responsive frontend

## Tech Stack

### Backend

- Java
- Spring Boot
- Spring Security
- Spring Data MongoDB
- Spring WebSocket
- OAuth2
- Maven

### Database

- MongoDB

### Frontend

- HTML5
- CSS3
- JavaScript

## Project Structure

The backend follows a layered architecture to keep the application organized and maintainable.

```text
src/
└── main/
    └── java/
        └── com/example/chat_application/
            ├── controller/
            ├── DTO/
            ├── Repositories/
            ├── Services/
            ├── Utilities/
            ├── config/
            ├── listener/
            ├── model/
            ├── security/
            └── ChatApplication.java
```

### Main Components

**Controllers**
Handle REST endpoints and WebSocket message flow for authentication, chat, files, conversations, admin actions, user search, and message search.

**DTOs**
Used for transferring data between the client and backend while keeping request and response payloads structured.

**Repositories**
Responsible for database access involving users, conversations, messages, and file attachments.

**Services**
Contain the core business logic, including:

- User management
- Direct messaging
- Conversation management
- Chat message persistence
- File storage
- Presence tracking
- OAuth processing

**Security and Configuration**
Handles authentication, authorization, WebSocket configuration, MongoDB setup, password encoding, and application security settings.

## Application Flow

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
        │ Controllers /        │
        │ Services / Security  │
        └──────────┬───────────┘
                   │
                   ▼
        ┌──────────────────────┐
        │       MongoDB        │
        │ Users / Messages /   │
        │ Conversations / Files│
        └──────────────────────┘
```

## Demo

Add your live demo link here when you deploy the project:

- Live demo: `currently not available`
- GitHub repository: [chat-application](https://github.com/Nocbu/chat-application)

## Screenshots

### Chat Interface
![Chat Application Interface](https://raw.githubusercontent.com/Nocbu/chat-application/main/images/chat_application.png)

### Features in Action
![Chat Application Features](https://raw.githubusercontent.com/Nocbu/chat-application/main/images/chat_application1.png)

## Setup

### Prerequisites

Make sure you have the following installed:

- Java 24
- Maven
- MongoDB
- Git

### 1. Clone the repository

```bash
git clone https://github.com/Nocbu/chat-application.git
```

### 2. Navigate to the project

```bash
cd chat-application
```

### 3. Configure the application

Update `src/main/resources/application.properties` with your local values:

- MongoDB URI and database name
- Google OAuth client ID and client secret
- AES encryption key
- Admin username and password
- File upload directory

> Do not commit secrets, passwords, API keys, or OAuth credentials to GitHub.

### 4. Start MongoDB

Make sure MongoDB is running locally or update the connection string in the application configuration.

### 5. Run the application

Using Maven Wrapper:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

The backend will start on the configured application port.

## API Highlights

### Authentication

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/session`
- `GET /api/auth/oauth2/success`

### Messages

- `GET /api/messages/history`
- `GET /api/messages/direct/{conversationId}`
- `POST /api/messages/direct/{conversationId}/read`
- `DELETE /api/messages/direct/{messageId}/me`
- `DELETE /api/messages/direct/{messageId}/everyone`
- `GET /api/messages/search?q=`

### Conversations

- `POST /api/conversations/direct`
- `GET /api/conversations/my`

### Files

- `POST /api/files/upload`
- `GET /api/files/download/{fileId}`
- `GET /api/files/info/{fileId}`

### Admin

- `POST /api/admin/action`
- `GET /api/admin/users`
- `GET /api/admin/stats`

### User Search

- `GET /api/users/search?q=`

## WebSocket Topics

- `/topic/public` - group chat updates
- `/topic/presence` - online/offline status
- `/topic/user/{username}` - private notifications and conversation updates
- `/topic/direct/{conversationId}` - direct message channel

## Data Model

MongoDB collections used by the app:

- `users`
- `messages`
- `conversations`
- `files`

## Security Notes

- Chat message content is encrypted before being stored in MongoDB.
- Passwords are stored with BCrypt hashing.
- Google OAuth users may need to choose a username before using direct messages.
- Admin actions are verified with configured credentials.

## Future Improvements

Some features that could be added in future versions:

- Message read receipts for all chat types
- Typing indicators
- Message reactions
- Message editing and deletion windows
- Image and PDF previews
- Pagination or infinite scroll for long histories
- Automated testing
- Docker support
- Cloud deployment
- AI chat summary
- AI smart reply suggestions
- AI moderation for spam and toxic content
- Semantic search across conversations

## Resume Value

This project demonstrates:

- Real-time backend development
- Secure authentication and authorization
- MongoDB data modeling
- WebSocket-based messaging
- File handling and persistent storage
- Admin moderation workflows
- Encryption and session management
- Search functionality
- Scope for AI-powered product features

## Author

**Nocbu**

- GitHub: [Nocbu](https://github.com/Nocbu)

## License

This project does not currently include a license. Add one if you want to publish it publicly.
