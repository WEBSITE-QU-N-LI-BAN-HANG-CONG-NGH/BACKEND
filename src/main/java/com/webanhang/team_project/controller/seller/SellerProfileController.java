package com.webanhang.team_project.controller.seller;

import com.webanhang.team_project.dto.response.ApiResponse;
import com.webanhang.team_project.dto.seller.SellerProfileDTO;
import com.webanhang.team_project.dto.seller.UpdateSellerProfileRequest;
import com.webanhang.team_project.model.User;
import com.webanhang.team_project.service.seller.ISellerProfileService;
import com.webanhang.team_project.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/seller/profile")
public class SellerProfileController {

    private final ISellerProfileService sellerProfileService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse> getSellerProfile(@RequestHeader("Authorization") String jwt) {
        User seller = userService.findUserByJwt(jwt);
        SellerProfileDTO profileDTO = sellerProfileService.getSellerProfile(seller.getId());
        return ResponseEntity.ok(ApiResponse.success(profileDTO, "Lấy thông tin hồ sơ người bán thành công"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse> updateSellerProfile(
            @RequestHeader("Authorization") String jwt,
            @RequestBody UpdateSellerProfileRequest request) {

        User seller = userService.findUserByJwt(jwt);
        SellerProfileDTO updatedProfile = sellerProfileService.updateSellerProfile(seller.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(updatedProfile, "Cập nhật hồ sơ người bán thành công"));
    }

    @GetMapping("/shop")
    public ResponseEntity<ApiResponse> getShopInfo(@RequestHeader("Authorization") String jwt) {
        User seller = userService.findUserByJwt(jwt);
        var shopInfo = sellerProfileService.getShopInfo(seller.getId());
        return ResponseEntity.ok(ApiResponse.success(shopInfo, "Lấy thông tin cửa hàng thành công"));
    }

    @PutMapping("/shop")
    public ResponseEntity<ApiResponse> updateShopInfo(
            @RequestHeader("Authorization") String jwt,
            @RequestBody UpdateSellerProfileRequest.ShopInfo shopInfo) {

        User seller = userService.findUserByJwt(jwt);
        var updatedShopInfo = sellerProfileService.updateShopInfo(seller.getId(), shopInfo);
        return ResponseEntity.ok(ApiResponse.success(updatedShopInfo, "Cập nhật thông tin cửa hàng thành công"));
    }

    @GetMapping("/verification-status")
    public ResponseEntity<ApiResponse> getVerificationStatus(@RequestHeader("Authorization") String jwt) {
        User seller = userService.findUserByJwt(jwt);
        var status = sellerProfileService.getVerificationStatus(seller.getId());
        return ResponseEntity.ok(ApiResponse.success(status, "Lấy trạng thái xác minh thành công"));
    }
}
