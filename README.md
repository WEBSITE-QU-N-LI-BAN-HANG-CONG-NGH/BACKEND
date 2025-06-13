Chắc chắn rồi\! Dưới đây là tệp `README.md` chi tiết hướng dẫn cách cài đặt và chạy dự án backend của bạn. Tôi đã tập trung vào các bước chính để giúp bạn khởi động máy chủ một cách nhanh chóng, đồng thời làm mờ các thông tin nhạy cảm trong tệp cấu hình.

-----

# 🚀 TechShop - Backend

Chào mừng bạn đến với dự án backend của **TechShop**\! Đây là một hệ thống thương mại điện tử hoàn chỉnh được xây dựng trên nền tảng Spring Boot, cung cấp các API mạnh mẽ để quản lý sản phẩm, đơn hàng, người dùng và tích hợp thanh toán.

## 🏁 Bắt đầu

Để chạy dự án trên máy cục bộ của bạn, hãy làm theo các bước được hướng dẫn chi tiết dưới đây.

### ✅ Yêu cầu cài đặt

Trước khi bắt đầu, hãy đảm bảo bạn đã cài đặt các công cụ sau:

  * **Java Development Kit (JDK)** - `v21` hoặc cao hơn
  * **Apache Maven** - `v3.9` hoặc cao hơn
  * **MySQL Server** - `v8.0` hoặc cao hơn
  * Một IDE Java như **IntelliJ IDEA** hoặc **VS Code**

### 🛠️ Hướng dẫn cài đặt

Làm theo các bước sau để thiết lập và chạy dự án.

#### Bước 1: Clone Repository

Mở terminal của bạn và clone repository về máy:

```bash
git clone <your-repository-url>
cd <thư-mục-dự-án>
```

#### Bước 2: Thiết lập Cơ sở dữ liệu

Dự án sử dụng MySQL để lưu trữ dữ liệu. Tệp `Script_Database.sql` đã bao gồm mọi thứ bạn cần, từ việc tạo cơ sở dữ liệu `ecommerce` cho đến các bảng và dữ liệu mẫu cần thiết.

\<details\>
\<summary\>➡️  Click vào đây để xem hướng dẫn import database\</summary\>

##### **Cách 1: Sử dụng Command Line (Khuyên dùng)**

1.  Mở terminal hoặc Command Prompt.

2.  Điều hướng đến thư mục gốc của dự án backend.

3.  Chạy lệnh sau (thay `your_username` bằng tên người dùng MySQL của bạn):

    ```bash
    mysql -u your_username -p ecommerce < ./Script_Database.sql
    ```

4.  Nhập mật khẩu MySQL của bạn khi được yêu cầu.

##### **Cách 2: Sử dụng Công cụ GUI (MySQL Workbench, DBeaver)**

1.  Kết nối tới MySQL Server của bạn bằng công cụ GUI.
2.  Mở tệp `Script_Database.sql` có trong thư mục dự án.
3.  Sao chép toàn bộ nội dung của tệp.
4.  Dán vào một cửa sổ truy vấn mới trong công cụ GUI.
5.  Thực thi (Run) toàn bộ script để tạo database và các bảng.

\</details\>

#### Bước 3: Cấu hình `application.properties`

Đây là bước quan trọng nhất. Tạo một tệp mới có tên `application.properties` trong thư mục `src/main/resources/`.

Sao chép toàn bộ nội dung dưới đây và dán vào tệp vừa tạo. Sau đó, **thay thế các giá trị có dạng `your_...`** bằng thông tin cấu hình của bạn.

```properties
# ===============================================
#          APPLICATION CONFIGURATION
# ===============================================
spring.application.name=TechShop
api.prefix=/api/v1

# ===============================================
#          DATABASE (MySQL) CONFIGURATION
# ===============================================
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce
spring.datasource.username=root
spring.datasource.password=your_mysql_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# ===============================================
#          JWT & SECURITY CONFIGURATION
# ===============================================
jwt.secret=your_super_secret_jwt_key_that_is_long_and_secure
auth.token.jwtSecret=your_super_secret_jwt_key_that_is_long_and_secure
auth.token.accessExpirationInMils=30000000
auth.token.refreshExpirationInMils=90000000
app.useSecureCookie=true

# ===============================================
#          OAUTH2 (GOOGLE & GITHUB)
# ===============================================
# Google OAuth2
spring.security.oauth2.client.registration.google.client-id=your_google_client_id
spring.security.oauth2.client.registration.google.client-secret=your_google_client_secret
spring.security.oauth2.client.registration.google.scope=email,profile

# GitHub OAuth2
spring.security.oauth2.client.registration.github.client-id=your_github_client_id
spring.security.oauth2.client.registration.github.client-secret=your_github_client_secret
spring.security.oauth2.client.registration.github.scope=read:user,user:email

# OAuth2 Redirect URIs
app.oauth2.redirectUri=http://localhost:5173/oauth2/redirect
app.oauth2.failureRedirectUri=http://localhost:5173/login

# ===============================================
#          EMAIL & OTP CONFIGURATION
# ===============================================
# Mail Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_gmail_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# OTP Configuration
app.otp.expiration-minutes=10
app.otp.resend-cooldown-minutes=2
app.company.logo.url=https://res.cloudinary.com/dgygvrrjs/image/upload/v1745387610/ChatGPT_Image_Apr_5_2025_12_08_58_AM_ociguu.png

# ===============================================
#          EXTERNAL SERVICES (CLOUDINARY & VNPAY)
# ===============================================
# Cloudinary Configuration
cloudinary.cloudName=your_cloudinary_cloud_name
cloudinary.apiKey=your_cloudinary_api_key
cloudinary.apiSecret=your_cloudinary_api_secret
cloudinary.apiSecure=true

# VNPay Configuration
vnpay.tmn-code=N7MGBPT4
vnpay.hash-secret=your_vnpay_hash_secret
vnpay.pay-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.return-url=http://localhost:5173/checkout?step=4

# ===============================================
#          SERVER & NETWORKING
# ===============================================
# CORS Configuration
cors.allowed-origins=http://localhost:5173,http://localhost:5174,http://localhost:5175

# Cloudflare Trusted Proxies (Keep as is)
cloudflare.trusted-proxies=173.245.48.0/20,103.21.244.0/22,103.22.200.0/22,103.31.4.0/22,141.101.64.0/18,108.162.192.0/18,190.93.240.0/20,188.114.96.0/20,197.234.240.0/22,198.41.128.0/17,162.158.0.0/15,104.16.0.0/13,104.24.0.0/14,172.64.0.0/13,131.0.72.0/22
```

> ⚠️ **Lưu ý quan trọng về Mật khẩu ứng dụng Gmail:**
> Để gửi email (OTP, thông báo), bạn cần bật "Xác minh 2 bước" cho tài khoản Google và tạo một **"Mật khẩu ứng dụng"** riêng. **Không sử dụng mật khẩu đăng nhập thông thường của bạn** cho `spring.mail.password`.

#### Bước 4: Build và Chạy ứng dụng

Sau khi hoàn tất cấu hình, bạn có thể khởi động máy chủ bằng một trong hai cách sau:

##### **Cách 1: Sử dụng Maven (Khuyên dùng)**

Mở terminal tại thư mục gốc của dự án và chạy lệnh:

```bash
mvn spring-boot:run
```

Maven sẽ tự động tải các dependency cần thiết, build và khởi chạy ứng dụng.

##### **Cách 2: Chạy từ IDE**

1.  Mở dự án trong IDE của bạn (IntelliJ, VS Code, ...).
2.  Tìm và mở tệp `TeamProjectApplication.java`.
3.  Nhấn nút `Run` hoặc `Debug` bên cạnh phương thức `main`.

🎉 **Tuyệt vời\!** Server của bạn giờ đang chạy tại `http://localhost:8080`. Bạn có thể bắt đầu tương tác với các API được liệt kê trong phần dưới đây.

-----

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