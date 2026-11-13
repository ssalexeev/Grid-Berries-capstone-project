package org.capstone.controller.admin.seller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/sellers")
public class AdminSellersController {

    //todo add controller for changing product status

    @PatchMapping("/{userId}")
    public ResponseEntity<List<?>> getUsersForModeration(@PathVariable Long userId){
        return null;
    }
}
