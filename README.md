TechShop - Backend
Đây là dự án backend cho website thương mại điện tử TechShop, được xây dựng bằng Spring Boot. Dự án quản lý các chức năng cốt lõi như người dùng, sản phẩm, giỏ hàng, đơn hàng, thanh toán và các trang quản trị cho admin và người bán.

Công nghệ sử dụng
Backend:
Java 21
Spring Boot 3.2.4 (Web, Data JPA, Security)
Maven
Cơ sở dữ liệu: MySQL
Xác thực: Spring Security, JSON Web Token (JWT), OAuth2
Lưu trữ ảnh: Cloudinary
Gửi Email: Spring Boot Mail, Thymeleaf (for email templates)
Thanh toán: Tích hợp VNPay
Yêu cầu cài đặt
JDK 21 hoặc cao hơn.
Maven 3.9 hoặc cao hơn.
MySQL Server đang hoạt động.
Một IDE lập trình Java như IntelliJ IDEA hoặc Visual Studio Code.
Hướng dẫn cài đặt và chạy dự án
1. Clone Repository
Bash

git clone <your-repository-url>
cd <your-repository-directory>
2. Cài đặt Cơ sở dữ liệu (Database Setup)
Dự án sử dụng cơ sở dữ liệu MySQL. Tệp Script_Database.sql đã bao gồm lệnh tạo cơ sở dữ liệu ecommerce và toàn bộ cấu trúc bảng cũng như dữ liệu mẫu. Bạn chỉ cần import tệp này vào MySQL.

Cách 1: Sử dụng Command Line
Mở terminal hoặc command prompt và chạy lệnh sau (nhớ thay your_username bằng tên người dùng MySQL của bạn):

Bash

mysql -u your_username -p ecommerce < ./Script_Database.sql
Sau đó nhập mật khẩu MySQL của bạn khi được hỏi.

Cách 2: Sử dụng công cụ (MySQL Workbench, DBeaver, etc.)

Kết nối tới MySQL server của bạn.
Mở một cửa sổ truy vấn mới.
Mở tệp Script_Database.sql, sao chép toàn bộ nội dung và dán vào cửa sổ truy vấn.
Thực thi (run) toàn bộ script.
3. Cấu hình ứng dụng (application.properties)
Đây là bước quan trọng nhất. Mở tệp src/main/resources/application.properties và chỉnh sửa các thông tin cần thiết.

a. Cấu hình kết nối Database:
Thay đổi thông tin cho phù hợp với môi trường của bạn.

Properties

spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password
b. Cấu hình Cloudinary (Lưu trữ ảnh):
Bạn cần có tài khoản Cloudinary để lấy các thông tin này.

Properties

cloudinary.cloudName=your_cloud_name
cloudinary.apiKey=your_api_key
cloudinary.apiSecret=your_api_secret
c. Cấu hình Email (Gmail):
Để gửi email OTP và xác nhận đơn hàng, bạn cần tạo một "Mật khẩu ứng dụng" cho tài khoản Gmail của mình.

Truy cập vào quản lý tài khoản Google.
Vào mục "Bảo mật".
Bật "Xác minh 2 bước".
Trong phần "Mật khẩu ứng dụng", tạo một mật khẩu mới và sao chép nó.
Properties

spring.mail.username=your_gmail_address@gmail.com
spring.mail.password=your_16_character_app_password
d. (Tùy chọn) Cấu hình OAuth2 (Đăng nhập mạng xã hội):
Nếu bạn muốn sử dụng chức năng đăng nhập với Google và GitHub, bạn cần tạo ứng dụng trên nền tảng của họ để lấy client-id và client-secret.

Properties

# Google OAuth2
spring.security.oauth2.client.registration.google.client-id=your_google_client_id
spring.security.oauth2.client.registration.google.client-secret=your_google_client_secret

# GitHub OAuth2
spring.security.oauth2.client.registration.github.client-id=your_github_client_id
spring.security.oauth2.client.registration.github.client-secret=your_github_client_secret
4. Build và chạy ứng dụng
Sau khi đã cấu hình xong, bạn có thể chạy ứng dụng bằng Maven.

Mở terminal tại thư mục gốc của dự án và chạy lệnh:

Bash

mvn spring-boot:run
Hoặc bạn có thể chạy trực tiếp từ IDE của mình bằng cách mở lớp TeamProjectApplication.java và nhấn "Run".

Ứng dụng sẽ khởi động và chạy mặc định tại http://localhost:8080.

Cấu trúc API
Tất cả các API đều có tiền tố là /api/v1.

/api/v1/auth/**: Các API liên quan đến xác thực (đăng nhập, đăng ký, OTP,...).
/api/v1/admin/**: Các API dành cho quản trị viên.
/api/v1/seller/**: Các API dành cho người bán.
/api/v1/customer/**: Các API dành cho khách hàng.
/api/v1/products/**: Các API chung để truy vấn sản phẩm.
/api/v1/categories/**: Các API chung để truy vấn danh mục.
/api/v1/cart/**: Các API quản lý giỏ hàng.
/api/v1/orders/**: Các API quản lý đơn hàng.
/api/v1/payment/**: Các API xử lý thanh toán.

Tính năng chính
Xác thực & Phân quyền:
Đăng ký, đăng nhập bằng JWT (JSON Web Token).
Xác thực qua OTP (One-Time Password) gửi qua email.
Đăng nhập bằng mạng xã hội (Google, GitHub) qua OAuth2.
Hệ thống phân quyền rõ ràng: ADMIN, SELLER, CUSTOMER.
Quản lý Sản phẩm:
Thêm, sửa, xóa sản phẩm.
Hệ thống danh mục 2 cấp (danh mục cha, danh mục con).
Tìm kiếm và lọc sản phẩm đa tiêu chí (giá, màu sắc, danh mục,...).
Giỏ hàng & Đặt hàng:
Quản lý giỏ hàng cho từng người dùng.
Quy trình đặt hàng từ giỏ hàng.
Tự động tách đơn hàng dựa trên người bán.
Thanh toán:
Tích hợp thanh toán qua cổng VNPay.
Thanh toán khi nhận hàng (COD).
Trang quản trị (Dashboard):
Admin: Quản lý toàn bộ người dùng, sản phẩm, đơn hàng và xem thống kê tổng quan.
Seller: Quản lý sản phẩm, đơn hàng của riêng mình và xem thống kê doanh thu.
Tương tác người dùng:
Đánh giá sản phẩm.
Cập nhật thông tin cá nhân, địa chỉ.
Lưu trữ hình ảnh: Tích hợp Cloudinary để lưu trữ và quản lý hình ảnh sản phẩm.