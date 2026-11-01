package com.gatekeeper.management.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class HealthService {

    private final JdbcTemplate jdbcTemplate;

    public String getHealth(){
        String databaseStatus= "DOWN";

        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

            if(Integer.valueOf(1).equals(result)){
                databaseStatus = "UP" ;
            }
        }catch (Exception e){
            System.out.println("MySQL Health status " + e.getMessage());
        }
        return databaseStatus;
    }
}
