# Giai đoạn 1: Build ứng dụng bằng Maven
# Sử dụng một image có sẵn Maven và Java (chọn phiên bản Java phù hợp với dự án của bạn, ví dụ 21)
FROM maven:3.9-eclipse-temurin-21 AS build

# Đặt thư mục làm việc bên trong container
WORKDIR /app

# Sao chép file pom.xml trước để tận dụng cache của Docker
COPY pom.xml .

# Tải các dependency
RUN mvn dependency:go-offline

# Sao chép toàn bộ mã nguồn còn lại
COPY src ./src

# Chạy lệnh build của Maven để tạo file .jar
RUN mvn clean package -DskipTests


# Giai đoạn 2: Chạy ứng dụng
# Sử dụng một image Java nhỏ gọn hơn để chạy, giúp container nhẹ hơn
FROM eclipse-temurin:21-jre-jammy

# Đặt thư mục làm việc
WORKDIR /app

# Sao chép file .jar đã được build từ giai đoạn 'build'
COPY --from=build /app/target/*.jar app.jar

# Mở cổng mà ứng dụng Spring Boot của bạn đang chạy (ví dụ: 8080)
EXPOSE 8080

# Lệnh để khởi động ứng dụng khi container chạy
# Render sẽ tự động cung cấp biến môi trường PORT, ứng dụng của bạn sẽ dùng nó
ENTRYPOINT ["java", "-jar", "app.jar"]