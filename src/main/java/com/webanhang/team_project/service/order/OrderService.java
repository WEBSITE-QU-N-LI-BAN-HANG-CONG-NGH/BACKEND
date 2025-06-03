package com.webanhang.team_project.service.order;




import com.webanhang.team_project.dto.order.OrderDTO;
import com.webanhang.team_project.dto.order.OrderDetailDTO;
import com.webanhang.team_project.enums.OrderStatus;
import com.webanhang.team_project.enums.PaymentMethod;
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
import com.webanhang.team_project.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final UserService userService;

    @Override
    public OrderDTO convertToDto(Order order) {
        return modelMapper.map(order, OrderDTO.class);
    }

    @Override
    public Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));
    }

    @Override
    public List<Order> userOrderHistory(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders;
    }

    @Override
    @Transactional // Rất quan trọng để đảm bảo tất cả các thay đổi được commit hoặc rollback cùng nhau
    public List<Order> placeOrder(Long addressId, User user) {
//    public Order placeOrder(Long addressId, User user) {
        Cart cart = cartRepository.findByUserId(user.getId());
        if (cart == null || cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống");
        }

        // Tìm địa chỉ gốc của người dùng
        Address address = user.getAddress().stream()
                .filter(a -> Objects.equals(a.getId(), addressId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ với ID: " + addressId));

        // Tính toán lại tổng giá trị giỏ hàng (đảm bảo thông tin mới nhất)
        // Lưu ý: findUserCart cũng có thể tạo cart mới nếu chưa có, nhưng ở đây ta đã kiểm tra cart != null
        cart = cartService.findUserCart(user.getId()); // Lấy lại thông tin cart với các tổng đã được tính toán

        // Group cart items by sellerId
        Map<Long, List<CartItem>> itemsBySeller = cart.getCartItems().stream()
                .collect(Collectors.groupingBy(item -> item.getProduct().getSellerId()));

        List<Order> createdOrders = new ArrayList<>();
        // Create separate order for each seller
        for (Map.Entry<Long, List<CartItem>> entry : itemsBySeller.entrySet()) {
            Long sellerId = entry.getKey();
            List<CartItem> sellerItems = entry.getValue();

            // Calculate totals for this seller's items
            int sellerOriginalPrice = sellerItems.stream()
                    .mapToInt(item -> item.getPrice() * item.getQuantity())
                    .sum();

            int sellerDiscountedPrice = sellerItems.stream()
                    .mapToInt(item -> item.getDiscountedPrice() * item.getQuantity())
                    .sum();

            int sellerTotalItems = sellerItems.stream()
                    .mapToInt(CartItem::getQuantity)
                    .sum();

            int sellerDiscount = sellerOriginalPrice - sellerDiscountedPrice;

            Order order = new Order();
            order.setUser(user);
            if( sellerId != null) {
                order.setSellerId(sellerId); // Set sellerId for the order
            }
            order.setOrderDate(LocalDateTime.now());
            order.setShippingAddress(address);
            order.setOrderStatus(OrderStatus.PENDING);
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setPaymentMethod(PaymentMethod.COD); // Default payment method

            // Set calculated totals for this seller
            order.setOriginalPrice(sellerOriginalPrice);
            order.setTotalItems(sellerTotalItems);
            order.setDiscount(sellerDiscount);
            order.setTotalDiscountedPrice(sellerDiscountedPrice);

            // Save order first to get ID for OrderItems
            order = orderRepository.save(order);

            List<OrderItem> orderItems = new ArrayList<>();
            for (CartItem cartItem : sellerItems) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(cartItem.getProduct());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setPrice(cartItem.getPrice());
                orderItem.setSize(cartItem.getSize());
                orderItem.setDiscountPercent(cartItem.getDiscountPercent());
                orderItem.setDiscountedPrice(cartItem.getDiscountedPrice());
                orderItem.setDeliveryDate(LocalDateTime.now().plusDays(7)); // Expected delivery date

                orderItems.add(orderItem);

                // Update product size quantity
                Product product = cartItem.getProduct();
                String sizeName = cartItem.getSize();
                int orderedQuantity = cartItem.getQuantity();

                // Find corresponding ProductSize in the Product's sizes list
                ProductSize targetSize = product.getSizes().stream()
                        .filter(ps -> ps.getName().equals(sizeName))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Order error: Size '" + sizeName +
                                "' not found for product '" + product.getTitle() + "' (ID: " + product.getId() + "). Please check your cart."));

                if (targetSize.getQuantity() < orderedQuantity) {
                    throw new RuntimeException("Order error: Requested quantity exceeds available stock for size '" + sizeName +
                            "' of product '" + product.getTitle() + "' (ID: " + product.getId() + "). Please check your cart.");
                }

                targetSize.setQuantity(targetSize.getQuantity() - orderedQuantity);

                // Update quantity sold
                Long quantitySold = product.getQuantitySold() != null ? product.getQuantitySold() : 0L;
                product.setQuantitySold(quantitySold + orderedQuantity);
            }

            order.setOrderItems(orderItems);
            Order savedOrder = orderRepository.save(order);
            createdOrders.add(savedOrder);
        }
//        Order order = new Order();
//        order.setUser(user);
//        order.setOrderDate(LocalDateTime.now());
//        order.setShippingAddress(address); // Gán địa chỉ tìm được
//        order.setOrderStatus(OrderStatus.PENDING);
//        order.setPaymentStatus(PaymentStatus.PENDING);
//
//        // Lấy tổng tiền từ Cart đã được tính toán (bao gồm cả discount)
//        order.setOriginalPrice(cart.getOriginalPrice()); // Tổng giá gốc
//        order.setTotalItems(cart.getTotalItems());
//        order.setDiscount(cart.getDiscount());
//        order.setTotalDiscountedPrice(cart.getTotalDiscountedPrice()); // Tổng giá sau khi giảm
//
//        // Lưu order trước để có ID cho OrderItems (cần thiết cho mối quan hệ)
//        order = orderRepository.save(order);
//
//        List<OrderItem> orderItems = new ArrayList<>();
//        for (CartItem cartItem : cart.getCartItems()) {
//            OrderItem orderItem = new OrderItem();
//            orderItem.setOrder(order); // Liên kết với Order vừa lưu
//            orderItem.setProduct(cartItem.getProduct());
//            orderItem.setQuantity(cartItem.getQuantity());
//            orderItem.setPrice(cartItem.getPrice());
//            orderItem.setSize(cartItem.getSize()); // Lấy size từ cart item
//            orderItem.setDiscountPercent(cartItem.getDiscountPercent());
//            orderItem.setDiscountedPrice(cartItem.getDiscountedPrice());
//            orderItem.setDeliveryDate(LocalDateTime.now().plusDays(7)); // Dự kiến ngày giao
//
//            orderItems.add(orderItem);
//
//            // --- CẬP NHẬT SỐ LƯỢNG TRONG PRODUCT SIZE ---
//            Product product = cartItem.getProduct(); // Lấy Product từ CartItem
//            String sizeName = cartItem.getSize();
//            int orderedQuantity = cartItem.getQuantity();
//
//            // Tìm ProductSize tương ứng trong danh sách sizes của Product
//            ProductSize targetSize = product.getSizes().stream()
//                    .filter(ps -> ps.getName().equals(sizeName))
//                    .findFirst()
//                    .orElseThrow(() -> new RuntimeException("Lỗi đặt hàng: Không tìm thấy size '" + sizeName +
//                            "' cho sản phẩm '" + product.getTitle() + "' (ID: " + product.getId() + "). Vui lòng kiểm tra lại giỏ hàng."));
//
//            if (targetSize.getQuantity() < orderedQuantity) {
//                throw new RuntimeException("Lỗi đặt hàng: Số lượng yêu cầu lớn hơn số lượng có sẵn cho size '" + sizeName +
//                        "' của sản phẩm '" + product.getTitle() + "' (ID: " + product.getId() + "). Vui lòng kiểm tra lại giỏ hàng.");
//            }
//
//            targetSize.setQuantity(targetSize.getQuantity() - orderedQuantity);
//
//            Long quantitySold = product.getQuantitySold() != null ? product.getQuantitySold() : 0L;
//            product.setQuantitySold(quantitySold + orderedQuantity);
//
//        }
//
//        order.setOrderItems(orderItems); // JPA sẽ quản lý việc lưu các OrderItem này do cascade


        // Clear the cart after creating all orders
        cartService.clearCart(user.getId());
        return createdOrders;
    }

    @Override
    @Transactional
    public Order confirmedOrder(Long orderId) {
        Order order = findOrderById(orderId);
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Đơn hàng không thể xác nhận ở trạng thái hiện tại");
        }
        order.setOrderStatus(OrderStatus.CONFIRMED);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order shippedOrder(Long orderId) {
        Order order = findOrderById(orderId);
        if (order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new RuntimeException("Đơn hàng phải được xác nhận trước khi gửi");
        }
        order.setOrderStatus(OrderStatus.SHIPPED);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order deliveredOrder(Long orderId) {
        Order order = findOrderById(orderId);
        if (order.getOrderStatus() != OrderStatus.SHIPPED) {
            throw new RuntimeException("Đơn hàng phải được gửi trước khi giao");
        }
        // Cập nhật trạng thái đơn hàng
        order.setOrderStatus(OrderStatus.DELIVERED);
        order.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setDeliveryDate(LocalDateTime.now());

        // Cập nhật số lượng đã bán cho các sản phẩm
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setQuantitySold(product.getQuantitySold() + item.getQuantity());
            productRepository.save(product);
        }
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = findOrderById(orderId);
        if (order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new RuntimeException("Không thể hủy đơn hàng đã giao");
        }

        // Trả lại số lượng sản phẩm vào kho
        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = orderItem.getProduct();
            String sizeName = orderItem.getSize();
            int cancelledQuantity = orderItem.getQuantity();

            // Tìm ProductSize tương ứng
            ProductSize targetSize = product.getSizes().stream()
                    .filter(ps -> ps.getName().equals(sizeName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy size '" + sizeName +
                            "' cho sản phẩm '" + product.getTitle() + "' (ID: " + product.getId() + ")"));

            // Tăng số lượng trong kho
            targetSize.setQuantity(targetSize.getQuantity() + cancelledQuantity);

            // Giảm số lượng đã bán nếu đã cập nhật
            Long quantitySold = product.getQuantitySold();
            if (quantitySold != null && quantitySold >= cancelledQuantity) {
                product.setQuantitySold(quantitySold - cancelledQuantity);
            }

            // Lưu lại sản phẩm đã cập nhật
            productRepository.save(product);
        }

        // Cập nhật trạng thái đơn hàng
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.REFUNDED);

        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public List<Order> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        if (orders.isEmpty()) {
            throw new RuntimeException("Không có đơn hàng nào");
        }
        return orders;
    }

    @Override
    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = findOrderById(orderId);
        orderRepository.delete(order);
    }

    @Override
    public Map<String, Object> getOrderStatistics(LocalDate start, LocalDate end) {
        // Lấy tất cả đơn hàng từ cơ sở dữ liệu
        List<Order> allOrders = orderRepository.findAll();
        
        // Lọc ra các đơn hàng trong khoảng thời gian
        List<Order> filteredOrders = allOrders.stream()
                .filter(order -> {
                    LocalDate orderDate = order.getOrderDate().toLocalDate();
                    return !orderDate.isBefore(start) && !orderDate.isAfter(end);
                })
                .collect(Collectors.toList());
        
        // Tính toán số liệu thống kê
        long totalOrders = filteredOrders.size();
        double totalRevenue = filteredOrders.stream()
                .mapToDouble(order -> order.getTotalDiscountedPrice().doubleValue())
                .sum();
        
        // Thống kê theo trạng thái
        Map<OrderStatus, Long> ordersByStatus = filteredOrders.stream()
                .collect(Collectors.groupingBy(Order::getOrderStatus, Collectors.counting()));
        
        // Thống kê đơn hàng theo ngày
        Map<LocalDate, Long> ordersByDate = filteredOrders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getOrderDate().toLocalDate(),
                        Collectors.counting()));
        
        // Thống kê doanh thu theo ngày
        Map<LocalDate, Double> revenueByDate = filteredOrders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getOrderDate().toLocalDate(),
                        Collectors.summingDouble(order -> order.getTotalDiscountedPrice().doubleValue())));
        
        // Kết quả trả về
        Map<String, Object> result = new HashMap<>();
        result.put("totalOrders", totalOrders);
        result.put("totalRevenue", totalRevenue);
        result.put("ordersByStatus", ordersByStatus);
        result.put("ordersByDate", ordersByDate);
        result.put("revenueByDate", revenueByDate);
        
        return result;
    }

    @Override
    @Transactional
    public List<OrderDetailDTO> getAllOrdersByJF() {
        // Sử dụng JPQL với JOIN FETCH để lấy thông tin User cùng với Order
        List<Order> orders= orderRepository.findAllWithUserOrderByOrderDateDesc();
        List<OrderDetailDTO> orderDTOs = orders.stream()
                .map(order -> new OrderDetailDTO(order))
                .toList();
        return orderDTOs;
    }

    @Override
    public List<Order> getPendingOrders() {
        return orderRepository.findByOrderStatus(OrderStatus.PENDING);
    }

    @Override
    public List<Order> getConfirmedOrders() {
        return orderRepository.findOrderByOrderStatus(OrderStatus.CONFIRMED);
    }

    @Override
    public List<Order> getShippedOrders() {
        return orderRepository.findOrderByOrderStatus(OrderStatus.SHIPPED);
    }

    @Override
    public List<Order> getDeliveredOrders() {
        return orderRepository.findOrderByOrderStatus(OrderStatus.DELIVERED);
    }

    @Override
    public List<Order> getCancelledOrders() {
        return orderRepository.findOrderByOrderStatus(OrderStatus.CANCELLED);
    }
}
