// src/main/java/com/example/hackathon/repository/LeaderboardProjection.java
package com.example.hackathon.repository;

public interface LeaderboardProjection {
    Integer getRnk();
    Integer getUserId();
    String  getNickname();
    Integer getCompletedCount();
}
