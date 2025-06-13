# 🚀 TechShop - Backend

Chào mừng đến với dự án backend của **TechShop**\! Đây là một hệ thống thương mại điện tử hoàn chỉnh được xây dựng trên nền tảng Spring Boot, cung cấp các API mạnh mẽ để quản lý sản phẩm, đơn hàng, người dùng và tích hợp thanh toán.

## 🏁 Bắt đầu

Để chạy dự án trên máy cục bộ của bạn, hãy làm theo các bước dưới đây.

### ✅ Yêu cầu cài đặt

  * **Java Development Kit (JDK)** - `v21` hoặc cao hơn
  * **Apache Maven** - `v3.9` hoặc cao hơn
  * **MySQL Server** - `v8.0` hoặc cao hơn
  * Một IDE Java như **IntelliJ IDEA** hoặc **VS Code**

### 🛠️ Hướng dẫn cài đặt

1.  **Clone a repository về máy:**

    ```bash
    git clone <your-repository-url>
    cd <your-repository-directory>
    ```

2.  **Thiết lập Cơ sở dữ liệu:**
    Tệp `Script_Database.sql` đã bao gồm mọi thứ bạn cần, từ việc tạo database `ecommerce` cho đến các bảng và dữ liệu mẫu.

    \<details\>
    \<summary\>➡️ Click vào đây để xem hướng dẫn import database\</summary\>

    #### Cách 1: Sử dụng Command Line (Khuyên dùng)

    Mở terminal và chạy lệnh sau (thay `your_username` bằng tên người dùng MySQL của bạn):

    ```bash
    mysql -u your_username -p ecommerce < ./Script_Database.sql
    ```

    Sau đó, nhập mật khẩu của bạn khi được yêu cầu.

    #### Cách 2: Sử dụng Công cụ GUI (MySQL Workbench, DBeaver)

    1.  Kết nối tới MySQL Server của bạn.
    2.  Mở tệp `Script_Database.sql`.
    3.  Sao chép toàn bộ nội dung và dán vào một cửa sổ truy vấn mới.
    4.  Thực thi (Run) toàn bộ script.

    \</details\>

3.  **Cấu hình ứng dụng (`application.properties`):**
    Đây là bước quan trọng nhất. Mở tệp `src/main/resources/application.properties` và cập nhật các giá trị được đánh dấu `[...]` cho phù hợp với môi trường của bạn.

    | Thuộc tính | Giá trị mẫu | Mô tả |
    | :--- | :--- | :--- |
    | `spring.datasource.username` | `root` | Tên người dùng MySQL của bạn. |
    | `spring.datasource.password` | `your_mysql_password` | Mật khẩu MySQL của bạn. |
    | `cloudinary.cloudName` | `your_cloud_name` | Lấy từ dashboard của Cloudinary. |
    | `cloudinary.apiKey` | `your_api_key` | Lấy từ dashboard của Cloudinary. |
    | `cloudinary.apiSecret` | `your_api_secret` | Lấy từ dashboard của Cloudinary. |
    | `spring.mail.username` | `your_email@gmail.com` | Địa chỉ email Gmail của bạn. |
    | `spring.mail.password` | `your_16_char_app_password` | **Mật khẩu ứng dụng** 16 ký tự từ tài khoản Google. |
    | `spring.security.oauth2...client-id`| `[...]` | Client ID từ Google/GitHub OAuth2 App. |
    | `spring.security.oauth2...client-secret`| `[...]` | Client Secret từ Google/GitHub OAuth2 App. |

    > ⚠️ **Lưu ý quan trọng về Mật khẩu ứng dụng Gmail:**
    > Để gửi email (OTP, thông báo), bạn cần bật "Xác minh 2 bước" cho tài khoản Google và tạo một "Mật khẩu ứng dụng" riêng. **Không sử dụng mật khẩu đăng nhập thông thường của bạn.**

4.  **Build và Chạy ứng dụng:**
    Sử dụng Maven để build và khởi chạy server:

    ```bash
    mvn spring-boot:run
    ```

    Hoặc chạy trực tiếp từ IDE của bạn bằng cách mở tệp `TeamProjectApplication.java` và nhấn `Run`.

    🎉 **Tuyệt vời\!** Server của bạn giờ đang chạy tại `http://localhost:8080`.

## 🗺️ Cấu trúc API

Tất cả các API đều có tiền tố là `/api/v1`.

\<details\>
\<summary\>➡️ Click vào đây để xem chi tiết các API Endpoints\</summary\>

  * `/api/v1/auth/**`: Các API liên quan đến xác thực (đăng nhập, đăng ký, OTP,...).
  * `/api/v1/admin/**`: Các API dành cho quản trị viên.
  * `/api/v1/seller/**`: Các API dành cho người bán.
  * `/api/v1/customer/**`: Các API dành cho khách hàng.
  * `/api/v1/products/**`: Các API chung để truy vấn sản phẩm.
  * `/api/v1/categories/**`: Các API chung để truy vấn danh mục.
  * `/api/v1/cart/**`: Các API quản lý giỏ hàng.
  * `/api/v1/orders/**`: Các API quản lý đơn hàng.
  * `/api/v1/payment/**`: Các API xử lý thanh toán.

\</details\>


## ✨ Tính năng nổi bật

Dự án bao gồm đầy đủ các tính năng cần thiết của một trang web thương mại điện tử hiện đại:

  * 🔐 **Xác thực & Phân quyền:**
      * Đăng ký/Đăng nhập với JWT, xác thực OTP qua Email.
      * Đăng nhập nhanh qua **Google** & **GitHub** (OAuth2).
      * Phân quyền rõ ràng cho `ADMIN`, `SELLER`, và `CUSTOMER`.
  * 🛍️ **Quản lý Sản phẩm:**
      * CRUD sản phẩm, quản lý danh mục đa cấp.
      * Tìm kiếm, lọc sản phẩm theo nhiều tiêu chí (giá, màu, danh mục,...).
  * 🛒 **Giỏ hàng & Đặt hàng:**
      * Quản lý giỏ hàng linh hoạt cho người dùng đã đăng nhập.
      * Quy trình đặt hàng thông minh, tự động tách đơn cho từng người bán.
  * 💳 **Thanh toán:**
      * Tích hợp cổng thanh toán **VNPay**.
      * Hỗ trợ thanh toán khi nhận hàng (COD).
  * 📊 **Dashboard Chuyên dụng:**
      * **Admin Dashboard:** Toàn quyền quản lý hệ thống, xem thống kê tổng quan.
      * **Seller Dashboard:** Quản lý sản phẩm, đơn hàng và theo dõi doanh thu riêng.
  * ☁️ **Lưu trữ đám mây:** Tích hợp **Cloudinary** để tối ưu hóa việc lưu trữ và phân phối hình ảnh.


## 📁 Cấu trúc thư mục dự án

```
src/main/java/com/webanhang/team_project
├── config          // Cấu hình Spring (Security, Mail, CORS,...)
├── controller      // Tầng xử lý request HTTP
│   ├── admin
│   ├── common
│   ├── customer
│   └── seller
├── dto             // Data Transfer Objects
├── enums           // Các hằng số (OrderStatus, UserRole,...)
├── exceptions      // Xử lý ngoại lệ toàn cục
├── model           // Các thực thể (Entities) JPA
├── repository      // Tầng truy cập dữ liệu (Data Access Layer)
├── security        // Cấu hình bảo mật (JWT, OAuth2, OTP,...)
│   ├── jwt
│   ├── oauth2
│   ├── otp
│   └── userdetails
├── service         // Tầng logic nghiệp vụ (Business Logic)
└── utils           // Các lớp tiện ích
```
-----