package com.webanhang.team_project.service.seller;

import com.webanhang.team_project.dto.seller.SellerProfileDTO;
import com.webanhang.team_project.dto.seller.UpdateSellerProfileRequest;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SellerProfileService implements ISellerProfileService {

    private final UserRepository userRepository;

    @Override
    public SellerProfileDTO getSellerProfile(Long sellerId) {
        // Đảm bảo người bán tồn tại
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán"));

        return convertToProfileDTO(seller);
    }

    @Override
    @Transactional
    public SellerProfileDTO updateSellerProfile(Long sellerId, UpdateSellerProfileRequest request) {
        // Đảm bảo người bán tồn tại
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán"));

        // Cập nhật thông tin
        if (request.getFirstName() != null) {
            seller.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            seller.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            seller.setPhone(request.getPhone());
        }

        // Lưu thông tin cập nhật
        User updatedSeller = userRepository.save(seller);

        return convertToProfileDTO(updatedSeller);
    }

    @Override
    public SellerProfileDTO.ShopInfoDTO getShopInfo(Long sellerId) {
        // Đảm bảo người bán tồn tại
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán"));

        // Trong thực tế, thông tin cửa hàng có thể được lưu trong một bảng riêng
        // Ở đây chúng ta giả định một số thông tin mẫu
        return new SellerProfileDTO.ShopInfoDTO(
                "Shop " + seller.getFirstName(),
                "",
                "Cửa hàng chuyên cung cấp các sản phẩm chất lượng cao",
                "",
                seller.getAddress() != null && !seller.getAddress().isEmpty() ?
                        seller.getAddress().get(0).getProvince() : "",
                seller.getAddress() != null && !seller.getAddress().isEmpty() ?
                        seller.getAddress().get(0).getDistrict() : "",
                seller.getAddress() != null && !seller.getAddress().isEmpty() ?
                        seller.getAddress().get(0).getWard() : "",
                seller.getAddress() != null && !seller.getAddress().isEmpty() ?
                        seller.getAddress().get(0).getStreet() : "",
                seller.getPhone(),
                "Cá nhân"
        );
    }

    @Override
    @Transactional
    public SellerProfileDTO.ShopInfoDTO updateShopInfo(Long sellerId, UpdateSellerProfileRequest.ShopInfo shopInfo) {
        // Đảm bảo người bán tồn tại
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán"));

        // Trong thực tế, thông tin cửa hàng sẽ được cập nhật trong bảng riêng
        // Ở đây chỉ xử lý một số thông tin cơ bản
        if (shopInfo.getPhoneNumber() != null) {
            seller.setPhone(shopInfo.getPhoneNumber());
            userRepository.save(seller);
        }

        // Trả về thông tin cửa hàng đã cập nhật
        return new SellerProfileDTO.ShopInfoDTO(
                shopInfo.getShopName(),
                shopInfo.getLogo(),
                shopInfo.getDescription(),
                shopInfo.getWebsite(),
                shopInfo.getAddress(),
                shopInfo.getCity(),
                shopInfo.getState(),
                shopInfo.getZipCode(),
                shopInfo.getPhoneNumber(),
                shopInfo.getBusinessType()
        );
    }

    @Override
    public Map<String, Object> getVerificationStatus(Long sellerId) {
        // Đảm bảo người bán tồn tại
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán"));

        Map<String, Object> status = new HashMap<>();
        status.put("verified", seller.isActive());
        status.put("verificationDate", seller.getCreatedAt());
        status.put("verificationStatus", seller.isActive() ? "Đã xác minh" : "Chưa xác minh");

        return status;
    }

    // Phương thức hỗ trợ
    private SellerProfileDTO convertToProfileDTO(User seller) {
        // Khởi tạo DTO
        SellerProfileDTO profileDTO = new SellerProfileDTO();
        profileDTO.setId(seller.getId());
        profileDTO.setFirstName(seller.getFirstName());
        profileDTO.setLastName(seller.getLastName());
        profileDTO.setEmail(seller.getEmail());
        profileDTO.setPhone(seller.getPhone());
        profileDTO.setVerified(seller.isActive());
        profileDTO.setCreatedAt(seller.getCreatedAt() != null ?
                seller.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");

        // Thông tin cửa hàng
        profileDTO.setShopInfo(getShopInfo(seller.getId()));

        return profileDTO;
    }
}
