package com.webanhang.team_project.service.order;




import com.webanhang.team_project.dto.order.OrderDTO;
import com.webanhang.team_project.enums.OrderStatus;
import com.webanhang.team_project.enums.PaymentStatus;
import com.webanhang.team_project.exceptions.GlobalExceptionHandler;
import com.webanhang.team_project.model.*;
import com.webanhang.team_project.repository.AddressRepository;
import com.webanhang.team_project.repository.CartRepository;
import com.webanhang.team_project.repository.OrderRepository;
import com.webanhang.team_project.repository.ProductRepository;
import com.webanhang.team_project.service.cart.ICartService;
import com.webanhang.team_project.service.product.IProductService;
import com.webanhang.team_project.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService implements IOrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ICartService cartService;
    private final ModelMapper modelMapper;
    private final CartRepository cartRepository;
    private final ProductService productService;

    @Override
    public Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
    }

    @Override
    public List<Order> userOrderHistory(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        if (orders.isEmpty()) {
            throw new RuntimeException("Không tìm thấy lịch sử đơn hàng cho người dùng: " + userId);
        }
        return orders;
    }

    @Override
    @Transactional
    public Order placeOrder(Long addressId, User user) {
        try {
            Cart cart = cartRepository.findByUserId(user.getId());
            if (cart == null || cart.getCartItems().isEmpty()) {
                throw new RuntimeException("Giỏ hàng trống");
            }

            // Tìm địa chỉ gốc
            Address address = user.getAddress().stream()
                    .filter(a -> Objects.equals(a.getId(), addressId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

            // Tính toán lại tổng giá trị giỏ hàng
            cart = cartService.findUserCart(user.getId());

            Order order = new Order();
            order.setUser(user);
            order.setOrderDate(LocalDateTime.now());

            // Sử dụng trực tiếp địa chỉ đã tồn tại
            order.setShippingAddress(address);
            order.setOrderStatus(OrderStatus.PENDING);
            order.setTotalAmount(cart.getTotalAmount());
            order.setTotalItems(cart.getTotalItems());
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setDiscount(cart.getDiscount());
            order.setTotalDiscountedPrice(cart.getTotalDiscountedPrice());

            // Lưu order trước để có ID
            order = orderRepository.save(order);

            // Tạo danh sách OrderItem từ CartItem
            List<OrderItem> orderItems = new ArrayList<>();

            for (CartItem cartItem : cart.getCartItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(cartItem.getProduct());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setPrice(cartItem.getPrice());
                orderItem.setSize(cartItem.getSize());
                orderItem.setDiscountPercent(cartItem.getDiscountPercent());
                orderItem.setDiscountedPrice(cartItem.getDiscountedPrice());
                // Dự kiến thời gian giao hàng là 7 ngày sau
                orderItem.setDeliveryDate(LocalDateTime.now().plusDays(7));

                orderItems.add(orderItem);

                // Cập nhật số lượng sản phẩm trong kho
                Product product = cartItem.getProduct();
                if (product.getQuantity() < cartItem.getQuantity()) {
                    throw new RuntimeException("Sản phẩm " + product.getTitle() + " không đủ số lượng trong kho");
                }
                product.setQuantity(product.getQuantity() - cartItem.getQuantity());
                productService.updateProduct(product.getId(), product);
            }

            // Thêm danh sách OrderItem vào Order
            order.setOrderItems(orderItems);

            // Xóa giỏ hàng sau khi đặt hàng thành công
            cart.getCartItems().clear();
            cartRepository.save(cart);

            // Lưu lại order với đầy đủ thông tin orderItems
            return orderRepository.save(order);
        } catch (RuntimeException e) {
            if (e instanceof RuntimeException) {
                throw e;
            }
            throw new RuntimeException("Lỗi khi tạo đơn hàng: " + e.getMessage());
        }
    }

    @Override
    public Order confirmedOrder(Long orderId) {
        Order order = findOrderById(orderId);
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Đơn hàng không thể xác nhận ở trạng thái hiện tại");
        }
        order.setOrderStatus(OrderStatus.CONFIRMED);
        return orderRepository.save(order);
    }

    @Override
    public Order shippedOrder(Long orderId) {
        Order order = findOrderById(orderId);
        if (order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new RuntimeException("Đơn hàng phải được xác nhận trước khi gửi");
        }
        order.setOrderStatus(OrderStatus.SHIPPED);
        return orderRepository.save(order);
    }

    @Override
    public Order deliveredOrder(Long orderId) {
        Order order = findOrderById(orderId);
        if (order.getOrderStatus() != OrderStatus.SHIPPED) {
            throw new RuntimeException("Đơn hàng phải được gửi trước khi giao");
        }
        order.setOrderStatus(OrderStatus.DELIVERED);
        order.setPaymentStatus(PaymentStatus.COMPLETED);
        return orderRepository.save(order);
    }

    @Override
    public Order cancelOrder(Long orderId) {
        Order order = findOrderById(orderId);
        if (order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Không thể hủy đơn hàng đã giao");
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        return orderRepository.save(order);
    }

    @Override
    public List<Order> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        if (orders.isEmpty()) {
            throw new RuntimeException("Không có đơn hàng nào");
        }
        return orders;
    }

    @Override
    public void deleteOrder(Long orderId) {
        Order order = findOrderById(orderId);
        if (order.getOrderStatus() != OrderStatus.CANCELLED) {
            throw new RuntimeException("Chỉ có thể xóa đơn hàng đã hủy");
        }
        orderRepository.delete(order);
    }

    @Override
    public Map<String, Object> getOrderStatistics(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        // Lấy đơn hàng trong khoảng thời gian
        List<Order> orders = orderRepository.findByOrderDateBetween(start, end);

        // Thống kê theo trạng thái
        Map<OrderStatus, Long> ordersByStatus = orders.stream()
                .collect(Collectors.groupingBy(Order::getOrderStatus, Collectors.counting()));

        // Thống kê theo thời gian
        Map<String, Long> ordersByTime = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Nhóm theo ngày
        Map<String, List<Order>> groupedByDate = orders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getOrderDate().format(formatter)
                ));

        for (Map.Entry<String, List<Order>> entry : groupedByDate.entrySet()) {
            ordersByTime.put(entry.getKey(), (long) entry.getValue().size());
        }

        result.put("total", orders.size());
        result.put("byStatus", ordersByStatus);
        result.put("byTime", ordersByTime);

        return result;
    }
}
