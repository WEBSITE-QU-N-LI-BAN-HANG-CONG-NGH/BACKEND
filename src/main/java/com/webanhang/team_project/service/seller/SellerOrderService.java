package com.webanhang.team_project.service.seller;

import com.webanhang.team_project.enums.OrderStatus;
import com.webanhang.team_project.enums.PaymentStatus;
import com.webanhang.team_project.model.Order;
import com.webanhang.team_project.model.Product;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.OrderRepository;
import com.webanhang.team_project.repository.ProductRepository;
import com.webanhang.team_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerOrderService implements ISellerOrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    public Page<Order> getSellerOrders(Long sellerId, int page, int size, String search,
                                       OrderStatus status, LocalDate startDate, LocalDate endDate) {
        Pageable pageable = PageRequest.of(page, size);

        // Đảm bảo người bán tồn tại
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán"));

        List<Order> sellerOrders = orderRepository.findByUserId(sellerId);

        // Lọc theo trạng thái
        if (status != null) {
            sellerOrders = sellerOrders.stream()
                    .filter(order -> order.getOrderStatus() == status)
                    .toList();
        }

        // Lọc theo ngày bắt đầu
        if (startDate != null) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            sellerOrders = sellerOrders.stream()
                    .filter(order -> order.getOrderDate().isAfter(startDateTime) || order.getOrderDate().isEqual(startDateTime))
                    .toList();
        }

        // Lọc theo ngày kết thúc
        if (endDate != null) {
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            sellerOrders = sellerOrders.stream()
                    .filter(order -> order.getOrderDate().isBefore(endDateTime) || order.getOrderDate().isEqual(endDateTime))
                    .toList();
        }

        // Tìm kiếm nếu có từ khóa
        if (search != null && !search.isEmpty()) {
            sellerOrders = sellerOrders.stream()
                    .filter(order ->
                            (order.getUser() != null &&
                                    (order.getUser().getFirstName() != null &&
                                            order.getUser().getFirstName().toLowerCase().contains(search.toLowerCase())) ||
                                    (order.getUser().getLastName() != null &&
                                            order.getUser().getLastName().toLowerCase().contains(search.toLowerCase())) ||
                                    (order.getUser().getEmail() != null &&
                                            order.getUser().getEmail().toLowerCase().contains(search.toLowerCase())))
                    )
                    .toList();
        }

        // Phân trang kết quả
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sellerOrders.size());

        if (start >= sellerOrders.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, sellerOrders.size());
        }

        return new PageImpl<>(
                sellerOrders.subList(start, end),
                pageable,
                sellerOrders.size()
        );
    }

    @Override
    public Order getOrderDetail(Long sellerId, Long orderId) {
        // Đảm bảo người bán tồn tại
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán"));

        // Lấy đơn hàng
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Kiểm tra đơn hàng thuộc người bán
        if (!order.getUser().getId().equals(sellerId)) {
            throw new RuntimeException("Đơn hàng không thuộc người bán này");
        }

        return order;
    }

    @Override
    @Transactional
    public Order updateOrderStatus(Long sellerId, Long orderId, OrderStatus status) {
        // Kiểm tra đơn hàng
        Order order = getOrderDetail(sellerId, orderId);

        // Cập nhật trạng thái
        order.setOrderStatus(status);

        // Cập nhật trạng thái thanh toán nếu cần
        if (status == OrderStatus.DELIVERED) {
            order.setPaymentStatus(PaymentStatus.COMPLETED);
        } else if (status == OrderStatus.CANCELLED) {
            order.setPaymentStatus(PaymentStatus.CANCELLED);
        }

        return orderRepository.save(order);
    }

    @Override
    public Map<String, Object> getOrderStatistics(Long sellerId, String period) {
        Map<String, Object> statistics = new HashMap<>();
        List<Product> sellerProducts = getSellerProducts(sellerId);

        // Đảm bảo người bán tồn tại
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán"));

        // Xác định khoảng thời gian
        LocalDateTime startDate;
        LocalDateTime endDate = LocalDateTime.now();

        if ("day".equals(period)) {
            startDate = LocalDate.now().atStartOfDay();
        } else if ("week".equals(period)) {
            startDate = LocalDate.now().minusDays(7).atStartOfDay();
        } else if ("month".equals(period)) {
            startDate = LocalDate.now().minusMonths(1).atStartOfDay();
        } else if ("year".equals(period)) {
            startDate = LocalDate.now().minusYears(1).atStartOfDay();
        } else {
            // Mặc định: tất cả thời gian
            startDate = LocalDateTime.of(2000, 1, 1, 0, 0);
        }

        // Lấy đơn hàng trong khoảng thời gian
        List<Order> orders = orderRepository.findByOrderDateBetweenAndOrderStatus(startDate, endDate, null);

        // Lọc đơn hàng của người bán
        List<Order> sellerOrders = orders.stream()
                .filter(order -> order.getUser().getId().equals(sellerId))
                .toList();

        // Thống kê theo trạng thái
        long pendingCount = sellerOrders.stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.PENDING)
                .count();

        long confirmedCount = sellerOrders.stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.CONFIRMED)
                .count();

        long shippedCount = sellerOrders.stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.SHIPPED)
                .count();

        long deliveredCount = sellerOrders.stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.DELIVERED)
                .count();

        long cancelledCount = sellerOrders.stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.CANCELLED)
                .count();

        // Tính tổng doanh thu dựa trên số lượng đã bán của sản phẩm
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Product product : sellerProducts) {
            long quantitySold = (product.getQuantitySold() != null) ? product.getQuantitySold() : 0L;
            BigDecimal productRevenue = BigDecimal.valueOf(product.getDiscountedPrice() * quantitySold);
            totalRevenue = totalRevenue.add(productRevenue);
        }

        // Đưa vào kết quả
        statistics.put("totalOrders", sellerOrders.size());
        statistics.put("pendingOrders", pendingCount);
        statistics.put("confirmedOrders", confirmedCount);
        statistics.put("shippedOrders", shippedCount);
        statistics.put("deliveredOrders", deliveredCount);
        statistics.put("cancelledOrders", cancelledCount);
        statistics.put("totalRevenue", totalRevenue);
        statistics.put("period", period);

        return statistics;
    }

    private List<Product> getSellerProducts(Long sellerId) {
        List<Product> allProducts = productRepository.findAll();

        return allProducts.stream()
                .filter(product -> product.getCategory() != null &&
                        product.getCategory().getProducts() != null &&
                        product.getCategory().getProducts().stream()
                                .anyMatch(p -> p.getId().equals(sellerId)))
                .collect(Collectors.toList());
    }
}
