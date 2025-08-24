package com.example.hackathon;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@SpringBootApplication
@Slf4j
public class HackathonApplication {

	public static void main(String[] args) {
		SpringApplication.run(HackathonApplication.class, args);
	}

	/**
	 * 애플리케이션 기동 시 DataSource로 커넥션만 가볍게 확인합니다.
	 * DriverManager로 직접 접속하지 않습니다.
	 * 문제가 없으면 URL/USER 로그만 남기고 바로 종료합니다.
	 */
	@Bean
	CommandLineRunner check(DataSource dataSource) {
		return args -> {
			try (Connection conn = dataSource.getConnection()) {
				log.info("DB OK: url={}, user={}",
						conn.getMetaData().getURL(),
						conn.getMetaData().getUserName());
			} catch (SQLException e) {
				log.error("DB connection failed", e);
				// 애플리케이션을 실패로 종료하고 싶다면 아래 예외 재던지기 유지
				throw e;
			}
		};
	}
}
