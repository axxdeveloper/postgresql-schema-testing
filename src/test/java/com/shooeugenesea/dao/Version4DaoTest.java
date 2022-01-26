package com.shooeugenesea.dao;

import com.shooeugenesea.IntegrationTest;
import com.shooeugenesea.entity.Version4;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.shaded.org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SpringBootTest
class Version4DaoTest extends IntegrationTest {

    @Autowired
    private Version4Dao v4dao;

    // persist as string
    //	QUERY PLAN: Limit  (cost=0.29..0.80 rows=3 width=48)
    //QUERY PLAN:   ->  Index Scan using version4_build_idx on version4  (cost=0.29..512.72 rows=2996 width=48)
    //QUERY PLAN:         Index Cond: ((build)::text > '0000000600070058'::text)
    //QUERY RESULT: {build=0000000900090099, id=d6ce8743-b7d4-413b-9bff-62dc67e82e34}
    //QUERY RESULT: {build=0000000900090098, id=08b88dc5-6d7e-4f96-b72a-6302f281eaed}
    //QUERY RESULT: {build=0000000900090097, id=0f5767b2-f5df-4139-8895-b2c8f0bef069}
    @Test
    void test_version4_Performance() {
        for (int major = 0; major < 1; major++) {
            for (int minor = 0; minor < 10; minor++) {
                System.out.println("Generate build for " + major + "." + minor);
                for (int micro = 0; micro < 10; micro++) {
                    for (int build = 0; build < 100; build++) {
                        String number = getVersion4Text(major, minor, micro, build);
                        v4dao.save(new Version4(UUID.randomUUID(), number));
                    }
                }
            }
        }

        String sql = "SELECT * FROM Version4 " +
                "WHERE build > '" + getVersion4Text(0, 6, 7, 58) + "'" +
                " ORDER BY build DESC" +
                " LIMIT 3";
        printQueryPlan(sql);
        printQueryResult(sql);
    }

    private String getVersion4Text(int major, int minor, int micro, int build) {
        return StringUtils.leftPad(String.valueOf(major), 4, '0')
                + StringUtils.leftPad(String.valueOf(minor), 4, '0')
                + StringUtils.leftPad(String.valueOf(micro), 4, '0')
                + StringUtils.leftPad(String.valueOf(build), 4, '0');
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
