// src/main/java/com/example/hackathon/entity/CharacterKind.java
package com.example.hackathon.entity;

import lombok.Getter;

@Getter
public enum CharacterKind {
    CHICK("삐약이"),
    CAT("야옹이");

    private final String defaultName;

    CharacterKind(String defaultName) {
        this.defaultName = defaultName;
    }
}
