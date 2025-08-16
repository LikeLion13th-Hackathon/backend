package com.example.hackathon.dto.mypage;

import lombok.Data;

@Data
public class MyPageUpdateDTO {

    private String nickname;
    private String job;
    private Boolean marketingConsent;
    private Boolean locationConsent;
}
