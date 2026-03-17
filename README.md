# LightMes

Ứng dụng chat mạng LAN viết bằng Java Swing, gồm 1 server trung tâm và nhiều client kết nối qua TCP socket.

Hỗ trợ **chat phòng chung (All)**, **chat riêng 1-1 (Private)**, gửi emoji, gửi file/ảnh (Base64), đăng nhập/đăng ký tài khoản SQLite, và đăng xuất.

## 1) Cấu trúc project

```text
LightMes/
  README.md
  client/
    pom.xml
    db/users.db                ← Database SQLite (tự tạo khi chạy lần đầu)
    src/main/java/com/mycompany/client/
      ChatServer.java          ← Server trung tâm (đa luồng, hàng đợi riêng mỗi client)
      ChatClientGUI.java       ← Giao diện client (Java Swing)
      UserManager.java         ← Quản lý tài khoản (SQLite CRUD)
      User.java                ← Model User (username + password)
```

## 2) Công nghệ sử dụng

| Công nghệ | Mục đích |
|------------|----------|
| Java 21+ | Ngôn ngữ chính (đã test với Java 25) |
| Java Swing | Giao diện desktop |
| TCP Sockets | Giao tiếp Client ↔ Server |
| SQLite (`sqlite-jdbc` 3.45.3.0) | Lưu trữ tài khoản người dùng |
| Maven | Quản lý build & dependency |

## 3) Tính năng

### Chat
- **Chat phòng chung (All)** — Tất cả user đã đăng nhập đều thấy tin nhắn (broadcast).
- **Chat riêng 1-1 (Private)** — Click vào user ở sidebar → tin nhắn chỉ 2 người thấy; mỗi cuộc trò chuyện lưu lịch sử riêng.
- **Emoji** — Bộ chọn emoji tích hợp (18 emoji), chèn trực tiếp vào ô nhập tin nhắn.
- **Gửi file & ảnh** — Gửi file tối đa **10 MB** qua mã hóa Base64. Ảnh (png/jpg/gif/bmp/webp) hiển thị preview inline; file khác hiện link tải về.
- **Giao diện bubble** — Tin nhắn của mình (xanh lá, bên phải) / người khác (trắng, bên trái) / hệ thống (giữa, xám).

### Tài khoản & Kết nối
- **Đăng ký / Đăng nhập** — Tài khoản lưu trong SQLite phía server.
- **Đăng xuất** — Ngắt kết nối, xóa trạng thái cục bộ, quay về form đăng nhập để đăng nhập lại (cùng hoặc khác tài khoản).
- **Nhập IP + Port linh hoạt** — Client hỏi địa chỉ server khi kết nối (mặc định `127.0.0.1:6666`).

### Sidebar
- **Danh sách user online** — Tự cập nhật khi có người join/rời.
- **Nút Refresh** — Yêu cầu server gửi lại danh sách user thủ công.
- **Chấm đỏ chưa đọc** (●) — Hiển thị bên cạnh cuộc trò chuyện có tin nhắn mới chưa đọc.
- Chuyển nhanh giữa chat All và chat Private bằng 1 click.

### Kiến trúc Server
- **Hàng đợi ghi riêng mỗi client** (`LinkedBlockingQueue`) — Broadcast không chặn; mỗi client có 1 writer thread riêng, gom tin nhắn và flush theo batch.
- **Xác thực đồng bộ** — Server gửi `AUTH:OK` / `AUTH:FAIL` trước khi khởi tạo writer thread, tránh race condition.
- **Tự broadcast danh sách user** (`USERLIST:`) khi có join, disconnect, hoặc yêu cầu refresh.
- **Định tuyến tin nhắn riêng** — Server chỉ chuyển `PRIVATE:` tới sender + target.

## 4) Chạy ứng dụng

### Cách A: Chạy trong IDE (VS Code / IntelliJ)

1. Chạy `ChatServer` trước (máy server).
2. Chạy `ChatClientGUI` trên từng máy user.
3. Khi mở client:
   - Chọn **Login** hoặc **Register**
   - Nhập username + password
   - Nhập IP server (LAN IP hiển thị trong console server)
   - Nhập port (mặc định `6666`)

### Cách B: Chạy bằng Maven

```powershell
cd client

# Compile
mvn compile

# Chạy server
mvn exec:java -Dexec.mainClass="com.mycompany.client.ChatServer"

# Chạy client (terminal riêng)
mvn exec:java -Dexec.mainClass="com.mycompany.client.ChatClientGUI"
```

### Cách C: Chạy bằng javac / java

```powershell
cd client

# Compile (cần sqlite-jdbc JAR trong classpath)
javac -encoding UTF-8 -d target/classes -cp "path/to/sqlite-jdbc-3.45.3.0.jar" src/main/java/com/mycompany/client/*.java

# Chạy server
java -cp "target/classes;path/to/sqlite-jdbc-3.45.3.0.jar" com.mycompany.client.ChatServer

# Chạy client
java -cp "target/classes;path/to/sqlite-jdbc-3.45.3.0.jar" com.mycompany.client.ChatClientGUI
```

## 5) Chạy trên nhiều máy (1 Server + N Client)

1. Đảm bảo tất cả máy cùng **mạng LAN**.
2. Trên máy server:
   - Chạy `ChatServer`
   - Ghi lại LAN IP hiển thị trong console (ví dụ `192.168.1.10`)
3. Mở firewall cho port server (mặc định `6666`) nếu cần.
4. Trên mỗi máy user:
   - Chạy `ChatClientGUI`
   - Đăng ký (lần đầu) hoặc Đăng nhập
   - Nhập đúng IP server + port
5. Test:
   - Gửi text & emoji ở phòng chung (All)
   - Click user trong sidebar → gửi tin nhắn riêng (Private)
   - Gửi file (`.pdf`, `.txt`, …) và ảnh (`.png`, `.jpg`, …)
   - Đăng xuất → Đăng nhập lại

## 6) Giao thức (Protocol)

| Prefix | Hướng | Mô tả |
|--------|-------|-------|
| `AUTH:LOGIN:user:pass` | Client → Server | Yêu cầu đăng nhập |
| `AUTH:REGISTER:user:pass` | Client → Server | Yêu cầu đăng ký |
| `AUTH:OK` / `AUTH:FAIL` | Server → Client | Phản hồi xác thực |
| `TEXT:sender: message` | Broadcast | Tin nhắn phòng chung |
| `FILE:sender:filename:base64` | Broadcast | Gửi file phòng chung |
| `PRIVATE:sender:target:message` | Server → sender+target | Tin nhắn riêng 1-1 |
| `PRIVATE:sender:target:FILE:filename:base64` | Server → sender+target | Gửi file riêng 1-1 |
| `USERLIST:user1,user2,...` | Broadcast | Danh sách user online |
| `LOGOUT:username` | Client → Server | Yêu cầu đăng xuất |
| `REFRESH_USERS` | Client → Server | Yêu cầu cập nhật danh sách user |

**Ghi chú:**
- Giới hạn file: **10 MB** (kiểm tra phía client).
- Emoji được encode dạng HTML entity (`&#x...;`) để hiển thị ổn định trên Swing HTML renderer.
- Server **không lưu lịch sử** tin nhắn — chỉ relay message theo thời gian thực.
- Phản hồi xác thực được gửi **đồng bộ** (synchronous) trước khi writer thread bắt đầu, tránh race condition.

## 7) Tổng quan mã nguồn

| File | Dòng | Chức năng |
|------|------|-----------|
| `ChatServer.java` | 287 | TCP server, xử lý đa luồng, xác thực, định tuyến broadcast/private, quản lý danh sách user online |
| `ChatClientGUI.java` | 956 | Giao diện Swing (header, sidebar, chat pane, thanh nhập liệu), lịch sử mỗi cuộc trò chuyện, theo dõi chưa đọc, bộ chọn emoji, gửi/nhận file, luồng đăng nhập/đăng xuất |
| `UserManager.java` | 74 | Kết nối SQLite, `register()`, `login()`, `getAllUsers()`, tự tìm đường dẫn DB linh hoạt |
| `User.java` | 12 | Model đơn giản (username + password) |
| `pom.xml` | 20 | Cấu hình Maven: Java 21, dependency `sqlite-jdbc` |

## 8) Hạn chế hiện tại

- Chưa lưu lịch sử chat (tin nhắn mất khi disconnect).
- Chưa có mã hóa end-to-end.
- Mật khẩu lưu **plaintext** trong SQLite (chưa hash).
- Chưa hỗ trợ group chat tùy chỉnh (chỉ có All + Private 1-1).
- Chưa có retry / chunking cho file lớn.

## 9) Hướng phát triển

- Hash mật khẩu (BCrypt / Argon2).
- Lưu lịch sử chat & metadata file vào database.
- Thêm group chat (tạo/quản lý nhóm).
- Hỗ trợ retry / chunking cho file lớn.
- Chuyển protocol sang JSON framing để dễ mở rộng.
- Thêm TLS/SSL cho kết nối mã hóa.