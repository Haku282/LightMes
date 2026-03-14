# LightMes

Ứng dụng chat LAN viết bằng Java Swing, gồm:
- 1 tiến trình server trung tâm
- Nhiều client kết nối qua IP + port

Project hiện tại tập trung vào chat nhóm real-time, gửi emoji, gửi file/ảnh Base64 và tải về từ phía nhận.

## 1) Cấu trúc project

```text
LightMes/
  README.md
  client/
	 pom.xml
	 src/main/java/com/mycompany/client/
		ChatServer.java
		ChatClientGUI.java
```

## 2) Công nghệ sử dụng

- Java 25
- Java Swing (UI)
- Socket TCP
- Maven (quản lý build)

## 3) Tính năng hiện có

- Chat text real-time nhiều client.
- Hỗ trợ emoji trong tin nhắn.
- Gửi file và ảnh (Base64), phía nhận có thể bấm để tải.
- UI chat dạng bubble, phân biệt tin nhắn của mình / người khác / hệ thống.
- Client nhập động Server IP + Port khi kết nối.
- Server xử lý broadcast theo hàng đợi riêng từng client (giảm nghẽn khi 1 client chậm mạng).

## 4) Chạy ứng dụng

### Cách A: chạy trong VS Code / IDE

1. Chạy class `ChatServer` trước (máy server).
2. Chạy class `ChatClientGUI` trên từng máy user.
3. Khi mở client:
	- Nhập tên hiển thị
	- Nhập IP server (LAN IP của máy chạy server)
	- Nhập port (mặc định `6666`)

### Cách B: chạy bằng javac/java

Từ thư mục `client`:

```powershell
# Compile
& "C:\Program Files\Java\jdk-25.0.2\bin\javac.exe" -encoding UTF-8 -d target/classes src/main/java/com/mycompany/client/ChatServer.java src/main/java/com/mycompany/client/ChatClientGUI.java

# Run server (máy server)
& "C:\Program Files\Java\jdk-25.0.2\bin\java.exe" -cp target/classes com.mycompany.client.ChatServer

# Run client (mỗi máy user)
& "C:\Program Files\Java\jdk-25.0.2\bin\java.exe" -cp target/classes com.mycompany.client.ChatClientGUI
```

## 5) Chạy trên 3 máy (1 server + 2 client)

1. Đảm bảo 3 máy cùng mạng LAN.
2. Trên máy server:
	- Chạy `ChatServer`
	- Ghi lại địa chỉ LAN IP server (ví dụ `192.168.1.10`)
3. Mở firewall cho port server (mặc định `6666`) nếu cần.
4. Trên máy User 1 và User 2:
	- Chạy `ChatClientGUI`
	- Nhập đúng IP server + port
5. Test:
	- Gửi text, emoji
	- Gửi file thường (`.pdf`, `.txt`, ...)
	- Gửi ảnh (`.png`, `.jpg`, ...)

## 6) Lưu ý kỹ thuật quan trọng

- Protocol text:
  - `TEXT:sender: message`
  - `FILE:sender:filename:base64`
- File hiện giới hạn tối đa 10 MB phía client gửi.
- Emoji trong nội dung HTML được encode dạng entity (`&#x...;`) để hiển thị ổn định với Swing HTML renderer.
- Server không giữ lịch sử tin nhắn; chỉ relay message theo thời gian thực.

## 7) Hạn chế hiện tại

- Chưa có login/register và lưu tài khoản.
- Chưa có danh sách online riêng theo sidebar.
- Chưa có lưu lịch sử chat vào database.
- Chưa có mã hóa end-to-end.

## 8) Hướng phát triển tiếp theo

- Bổ sung xác thực người dùng (login/register).
- Thêm danh sách user online và chat private 1-1.
- Lưu lịch sử chat/file metadata vào DB.
- Bổ sung retry/chunking cho file lớn.
- Tách protocol rõ ràng hơn (JSON framing) để mở rộng tính năng.