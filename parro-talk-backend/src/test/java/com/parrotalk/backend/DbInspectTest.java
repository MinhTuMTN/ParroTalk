package com.parrotalk.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.parrotalk.backend.client.ResendClient;

import java.util.List;
import java.util.Map;

@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=none"
})
class DbInspectTest {

    @MockitoBean
    private ResendClient resendClient;

    @MockitoBean
    private ConnectionFactory connectionFactory;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void inspectDb() {
        System.out.println("--- DB FIX END ---");
    }

    private void checkTableStructure(String tableName) {
        try {
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name = ? AND table_schema = 'public'",
                tableName
            );
            if (columns.isEmpty()) {
                System.out.println("Table " + tableName + " does not exist (or no columns found).");
            } else {
                System.out.println("Table " + tableName + " columns:");
                for (Map<String, Object> col : columns) {
                    System.out.println("  - " + col.get("column_name") + " (" + col.get("data_type") + ", nullable=" + col.get("is_nullable") + ")");
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to check structure of " + tableName + ": " + e.getMessage());
        }
    }

    private void printTableRowCount(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM " + tableName, Integer.class);
            System.out.println("Table " + tableName + " row count: " + count);
        } catch (Exception e) {
            System.out.println("Failed to count rows in " + tableName + ": " + e.getMessage());
        }
    }
}
