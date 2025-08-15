package com.example.hackathon.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYMENT_REQUIRED) // 402
public class InsufficientBalanceException extends RuntimeException {}
