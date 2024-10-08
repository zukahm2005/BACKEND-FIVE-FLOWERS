package com.example.backendfiveflowers.controller;

import com.example.backendfiveflowers.entity.Route;
import com.example.backendfiveflowers.entity.UserInfo;
import com.example.backendfiveflowers.service.RouteService;
import com.example.backendfiveflowers.service.UserInfoDetails;
import com.example.backendfiveflowers.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    @Autowired
    private RouteService routeService;

    @Autowired
    private UserInfoService userInfoService;

    // Lấy danh sách hành trình cho người dùng hiện tại
    @GetMapping("/get-route")
    public ResponseEntity<List<Route>> getAllRoutesForUser() {
        UserInfo currentUser = getCurrentUser();
        List<Route> routes = routeService.getAllRoutesForUser(currentUser);
        return ResponseEntity.ok(routes);
    }


    @PostMapping("post-route")
    public ResponseEntity<Route> saveRoute(@RequestBody Route route) {
        UserInfo currentUser = getCurrentUser();
        route.setJourneyDate(LocalDateTime.now());
        Route savedRoute = routeService.saveRoute(route, currentUser);
        return ResponseEntity.ok(savedRoute);
    }


    private UserInfo getCurrentUser() {
        UserInfoDetails userInfoDetails = (UserInfoDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = userInfoDetails.getUsername();
        return userInfoService.findByUserName(username);
    }
}
