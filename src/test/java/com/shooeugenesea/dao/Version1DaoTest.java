package com.shooeugenesea.dao;

import com.shooeugenesea.IntegrationTest;
import com.shooeugenesea.entity.Version1;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SpringBootTest
class Version1DaoTest extends IntegrationTest {

    @Autowired
    private Version1Dao v1dao;

    // split version to major, minor, micro and build columns with DB indexes
    // Query limit 3	
    //QUERY PLAN: Limit  (cost=0.29..1.16 rows=3 width=32)
    //QUERY PLAN:   ->  Index Scan using version1_major_minor_micro_build_idx on version1  (cost=0.29..982.84 rows=3366 width=32)
    //QUERY PLAN:         Filter: ((major > 0) OR ((major = 0) AND (minor > 6)) OR ((major = 0) AND (minor = 6) AND (micro > 7)) OR ((major = 0) AND (minor = 6) AND (micro = 7) AND (build > 58)))
    //QUERY RESULT: {major=0, minor=9, micro=9, build=99, id=e2cda2e0-6b3f-48c1-bae0-3dcbe97686ff}
    //QUERY RESULT: {major=0, minor=9, micro=9, build=98, id=eb2a2a42-e386-47f2-a143-4581fbf2c2f8}
    //QUERY RESULT: {major=0, minor=9, micro=9, build=97, id=a6bb2b66-06e3-4260-a660-b4f1b43614c8}
    @Test
    void test_version1_Performance() {
        for (int major = 0; major < 1; major++) {
            for (int minor = 0; minor < 10; minor++) {
                System.out.println("Generate build for " + major + "." + minor);
                for (int micro = 0; micro < 10; micro++) {
                    for (int build = 0; build < 100; build++) {
                        v1dao.save(new Version1(UUID.randomUUID(), major, minor, micro, build));
                    }
                }
            }
        }

        // Open a connection
        String sql = "SELECT * FROM Version1 " +
                "WHERE (major > 0) " +
                "OR (major = 0 AND minor > 6) " +
                "OR (major = 0 AND minor = 6 AND micro > 7) " +
                "OR (major = 0 AND minor = 6 AND micro = 7 AND build > 58) " +
                "ORDER BY major DESC, minor DESC, micro DESC, build DESC " +
                "LIMIT 3";
        printQueryPlan(sql);
        printQueryResult(sql);
    }

    private void printQueryResult(String sql) {
        try (Connection conn = DriverManager.getConnection(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql);) {
            // Extract data from result set
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
                    map.put(rs.getMetaData().getColumnName(i + 1), rs.getObject(i + 1));
                }
                System.out.println("QUERY RESULT: " + map);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    private void printQueryPlan(String sql) {
        try (Connection conn = DriverManager.getConnection(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("EXPLAIN ANALYZE " + sql);) {
            // Extract data from result set
            while (rs.next()) {
                System.out.println("QUERY PLAN: " + rs.getString("QUERY PLAN"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

}
