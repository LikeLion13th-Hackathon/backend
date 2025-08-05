package com.example.hackathon.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
    @GetMapping("/")
    public String hello() {
        return "테스트 진짜 성공! 이제 진짜 자동 배포임;맞아? 맞냐고 저 지금 매우 간절합니다 케이윌이 부릅니다 이러지마 제발 그래서 되는걸까 되니?";
    }
}