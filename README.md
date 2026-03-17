# LightMes

A Java Swing LAN chat application with a central server and multiple clients connecting via TCP sockets.

Supports **public chat (All)**, **private 1-on-1 chat**, emoji, file/image transfer (Base64), SQLite-based login/register, and logout.

## 1) Project Structure

```text
LightMes/
  README.md
  client/
    pom.xml
    db/users.db                ← SQLite database (auto-created on first run)
    src/main/java/com/mycompany/client/
      ChatServer.java          ← Central server (multi-threaded, per-client write queue)
      ChatClientGUI.java       ← Client GUI (Java Swing)
      UserManager.java         ← Account management (SQLite CRUD)
      User.java                ← User model (username + password)
```

## 2) Tech Stack

| Technology        | Purpose                           |
|-------------------|-----------------------------------|
| Java 21+          | Language runtime (tested Java 25) |
| Java Swing        | Desktop GUI                       |
| TCP Sockets       | Client ↔ Server communication     |
| SQLite (`sqlite-jdbc` 3.45.3.0) | User account storage |
| Maven             | Build & dependency management     |

## 3) Features

### Chat
- **Public Chat (All)** — All logged-in users see messages (broadcast).
- **Private 1-on-1 Chat** — Click a user in the sidebar → messages are only visible to the two participants; each conversation keeps its own history.
- **Emoji** — Built-in emoji picker (18 emojis) inserts directly into the message field.
- **File & Image Transfer** — Send files up to **10 MB** via Base64 encoding. Images (png/jpg/gif/bmp/webp) are previewed inline; other files show a download link.
- **Bubble-style UI** — Own messages (green, right-aligned) / others (white, left-aligned) / system (centered, gray).

### Account & Connection
- **Register / Login** — Accounts stored in SQLite on the server side.
- **Logout** — Disconnects, clears local state, returns to the login form for re-authentication with same or different account.
- **Dynamic Server IP + Port** — Client prompts for server address at connect time (default `127.0.0.1:6666`).

### Sidebar
- **Online user list** — Auto-updates on join/leave events.
- **Refresh button** — Manually request an updated user list from the server.
- **Unread indicator** (● red dot) — Appears next to conversations with unread messages.
- One-click switching between All chat and private conversations.

### Server Architecture
- **Per-client write queue** (`LinkedBlockingQueue`) — Non-blocking broadcast; each client has a dedicated writer thread that drains queued messages and flushes in batch.
- **Auth-first protocol** — Server writes `AUTH:OK` / `AUTH:FAIL` synchronously before spawning the writer thread.
- **Auto-broadcast user list** (`USERLIST:`) on every join, disconnect, or refresh request.
- **Private message routing** — Server delivers `PRIVATE:` messages only to sender + target.

## 4) Running the Application

### Option A: Run in IDE (VS Code / IntelliJ)

1. Run `ChatServer` first (on the server machine).
2. Run `ChatClientGUI` on each user machine.
3. On the client:
   - Choose **Login** or **Register**
   - Enter username + password
   - Enter the server's LAN IP (shown in server console output)
   - Enter port (default `6666`)

### Option B: Run with Maven

```powershell
cd client

# Compile
mvn compile

# Run server
mvn exec:java -Dexec.mainClass="com.mycompany.client.ChatServer"

# Run client (separate terminal)
mvn exec:java -Dexec.mainClass="com.mycompany.client.ChatClientGUI"
```

### Option C: Run with javac / java

```powershell
cd client

# Compile (sqlite-jdbc JAR must be in classpath)
javac -encoding UTF-8 -d target/classes -cp "path/to/sqlite-jdbc-3.45.3.0.jar" src/main/java/com/mycompany/client/*.java

# Run server
java -cp "target/classes;path/to/sqlite-jdbc-3.45.3.0.jar" com.mycompany.client.ChatServer

# Run client
java -cp "target/classes;path/to/sqlite-jdbc-3.45.3.0.jar" com.mycompany.client.ChatClientGUI
```

## 5) Multi-machine Setup (1 Server + N Clients)

1. Ensure all machines are on the same **LAN**.
2. On the server machine:
   - Run `ChatServer`
   - Note the LAN IP printed in the console (e.g. `192.168.1.10`)
3. Open the firewall for the server port (default `6666`) if needed.
4. On each client machine:
   - Run `ChatClientGUI`
   - Register (first time) or Login
   - Enter the server's LAN IP + port
5. Test:
   - Send text & emoji in public chat (All)
   - Click a user in the sidebar → send private messages
   - Send files (`.pdf`, `.txt`, …) and images (`.png`, `.jpg`, …)
   - Logout → Login again

## 6) Protocol Reference

| Prefix | Direction | Description |
|--------|-----------|-------------|
| `AUTH:LOGIN:user:pass` | Client → Server | Login request |
| `AUTH:REGISTER:user:pass` | Client → Server | Register request |
| `AUTH:OK` / `AUTH:FAIL` | Server → Client | Authentication response |
| `TEXT:sender: message` | Broadcast | Public chat message |
| `FILE:sender:filename:base64` | Broadcast | Public file transfer |
| `PRIVATE:sender:target:message` | Server → sender+target | Private text message |
| `PRIVATE:sender:target:FILE:filename:base64` | Server → sender+target | Private file transfer |
| `USERLIST:user1,user2,...` | Broadcast | Online user list |
| `LOGOUT:username` | Client → Server | Logout request |
| `REFRESH_USERS` | Client → Server | Request user list refresh |

**Notes:**
- File size limit: **10 MB** (enforced client-side).
- Emoji characters are encoded as HTML entities (`&#x...;`) for stable rendering in Swing's HTML renderer.
- The server is **stateless** — it relays messages in real-time and does not persist chat history.
- Auth response is sent **synchronously** before the writer thread starts, preventing race conditions.

## 7) Source File Overview

| File | Lines | Responsibility |
|------|-------|----------------|
| `ChatServer.java` | 287 | TCP server, client handler threads, auth routing, broadcast/private message dispatch, user list management |
| `ChatClientGUI.java` | 956 | Swing UI (header, sidebar, chat pane, input bar), per-conversation history, unread tracking, emoji picker, file send/receive, login/logout flow |
| `UserManager.java` | 74 | SQLite connection, `register()`, `login()`, `getAllUsers()`, flexible DB path resolution |
| `User.java` | 12 | Simple model class (username + password fields) |
| `pom.xml` | 20 | Maven config: Java 21, `sqlite-jdbc` dependency |

## 8) Current Limitations

- No chat history persistence (messages are lost on disconnect).
- No end-to-end encryption.
- Passwords stored as **plaintext** in SQLite (no hashing).
- No custom group chat (only public All + private 1-on-1).
- No message retry / chunking for large files.

## 9) Future Improvements

- Hash passwords (BCrypt / Argon2).
- Persist chat history & file metadata to the database.
- Add custom group chat (create/manage groups).
- Implement retry / chunking for large file transfers.
- Migrate protocol to JSON framing for better extensibility.
- Add TLS/SSL for encrypted connections.