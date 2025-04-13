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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderService implements IOrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ICartService cartService;
    private final ModelMapper modelMapper;
    private final CartRepository cartRepository;
    private final ProductService productService;


//    @Transactional
//    @Override
//    public Order placeOrder(int userId) {
//        Cart cart = cartService.getCartByUserId(userId);
//        Order order = createOrder(cart);
//        List<OrderItem> orderItemList = createOrderItems(order, cart);
//        order.setOrderItems(new HashSet<>(orderItemList));
//        order.setTotalAmount(calculateTotalAmount(orderItemList));
//        Order savedOrder = orderRepository.save(order);
//        cartService.clearCart(cart.getId());
//        return savedOrder;
//    }

//    private Order createOrder(Cart cart) {
//        Order order = new Order();
//        order.setUser(cart.getUser());
//        order.setOrderStatus(OrderStatus.PENDING);
//        order.setOrderDate(LocalDate.now());
//        return order;
//    }
//
//    private List<OrderItem> createOrderItems(Order order, Cart cart) {
//        return cart.getItems().stream().map(cartItem -> {
//            Product product = cartItem.getProduct();
//            product.setInventory(product.getInventory() - cartItem.getQuantity());
//            productRepository.save(product);
//            return new OrderItem(
//                    order,
//                    product,
//                    cartItem.getUnitPrice(),
//                    cartItem.getQuantity());
//        }).toList();
//    }
//
//    private BigDecimal calculateTotalAmount(List<OrderItem> orderItemList) {
//        return orderItemList.stream()
//                .map(item -> item.getPrice()
//                        .multiply(new BigDecimal(item.getQuantity())))
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//    }


//    @Override
//    public List<OrderDTO> getUserOrders(int userId) {
//        List<Order> orders = orderRepository.findByUserId(userId);
//        return  orders.stream().map(this :: convertToDto).toList();
//    }

//    @Override
//    public OrderDTO convertToDto(Order order) {
//        return modelMapper.map(order, OrderDTO.class);
//    }
//
//    public OrderService(CartRepository cartRepository, ICartService ICartService,
//                        IProductService productService, OrderRepository orderRepository, AddressRepository addressRepository) {
//        this.cartRepository = cartRepository;
//        this.ICartService = ICartService;
//        this.productService = productService;
//        this.orderRepository = orderRepository;
//        this.addressRepository = addressRepository;
//    }

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
}
