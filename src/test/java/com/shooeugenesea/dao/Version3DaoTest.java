package com.shooeugenesea.dao;

import com.shooeugenesea.IntegrationTest;
import com.shooeugenesea.entity.Version3;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.UUID;

@SpringBootTest
class Version3DaoTest extends IntegrationTest {

    @Autowired
    private Version3Dao v3dao;

    // persist as byte array
    //QUERY PLAN: Limit  (cost=0.28..0.77 rows=3 width=48)
    //QUERY PLAN:   ->  Index Scan using version3_order_idx on version3  (cost=0.28..368.24 rows=2283 width=48)
    //QUERY PLAN:         Index Cond: (build > '\x0005060708'::bytea)
    //[0, 9, 9, 99]
    //[0, 9, 9, 98]
    //[0, 9, 9, 97]
    @Test
    void test_version3_Performance() {
        for (int major = 0; major < 1; major++) {
            for (int minor = 0; minor < 10; minor++) {
                System.out.println("Generate build for " + major + "." + minor);
                for (int micro = 0; micro < 10; micro++) {
                    for (int build = 0; build < 100; build++) {
                        v3dao.save(new Version3(UUID.randomUUID(), new byte[]{(byte) major, (byte) minor, (byte) micro, (byte) build}));
                    }
                }
            }
        }

        String sql = "SELECT * FROM Version3 " +
                "WHERE build > ?" +
                " ORDER BY build DESC" +
                " LIMIT 3";

        printQueryPlan(sql, new byte[]{0, 5, 6, 7, 58});
        printQueryResult(sql, new byte[]{0, 5, 6, 7, 58});
    }

    private void printQueryPlan(String sql, byte[] version) {
        try (Connection conn = DriverManager.getConnection(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword());
             PreparedStatement stmt = conn.prepareCall("EXPLAIN ANALYZE " + sql);
        ) {
            // Extract data from result set
            stmt.setBytes(1, version);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                System.out.println("QUERY PLAN: " + rs.getString("QUERY PLAN"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    private void printQueryResult(String sql, byte[] version) {
        try (Connection conn = DriverManager.getConnection(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword());
             PreparedStatement stmt = conn.prepareCall(sql);
        ) {
            // Extract data from result set
            stmt.setBytes(1, version);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                System.out.println(Arrays.toString(rs.getBytes("build")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

}
