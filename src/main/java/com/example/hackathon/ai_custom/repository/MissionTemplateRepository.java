// src/main/java/com/example/hackathon/ai_custom/repository/MissionTemplateRepository.java
package com.example.hackathon.ai_custom.repository;

import com.example.hackathon.ai_custom.entity.MissionTemplate;
import com.example.hackathon.mission.entity.PlaceCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface MissionTemplateRepository extends JpaRepository<MissionTemplate, Long> {

    List<MissionTemplate> findByPlaceCategoryAndActiveIsTrue(PlaceCategory placeCategory);

    @Query(value = """
        SELECT * 
          FROM mission_template 
         WHERE place_category = :place 
           AND active = 1
      ORDER BY RAND()
         LIMIT 1
        """, nativeQuery = true)
    Optional<MissionTemplate> pickRandomOne(@Param("place") String placeCategoryName);

    @Query(value = """
        SELECT * 
          FROM mission_template 
         WHERE place_category = :place 
           AND active = 1
      ORDER BY RAND()
        """, nativeQuery = true)
    List<MissionTemplate> pickRandomMany(@Param("place") String placeCategoryName,
                                         Pageable pageable);
}
