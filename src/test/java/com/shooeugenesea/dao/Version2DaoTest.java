package com.shooeugenesea.dao;

import com.shooeugenesea.IntegrationTest;
import com.shooeugenesea.entity.Version2;
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
class Version2DaoTest extends IntegrationTest {

	@Autowired private Version2Dao v2dao;

	//Use a number to present version
	//QUERY PLAN: Limit  (cost=0.29..0.61 rows=3 width=20)
	//QUERY PLAN:   ->  Index Scan using version2_order_idx on version2  (cost=0.29..391.76 rows=3627 width=20)
	//QUERY PLAN:         Index Cond: (build > 6758)
	//QUERY RESULT: {build=9999, id=9671ca44-2fac-4e18-a28d-bbdeb9f21fa0}
	//QUERY RESULT: {build=9998, id=4bc638c6-f15f-4afb-87ea-ca6604db30f3}
	//QUERY RESULT: {build=9997, id=da16214f-d3d8-4e58-8917-4260fa01ba81}
	@Test
	void test_version2_Performance() {
		for (int major = 0; major < 1; major++) { 	
			for (int minor = 0; minor < 10; minor++) {
				System.out.println("Generate build for " + major + "." + minor);
				for (int micro = 0; micro < 10; micro++) {
					for (int build = 0; build < 100; build++) {
						int number = getVersion2Number(major, minor, micro, build);
						v2dao.save(new Version2(UUID.randomUUID(), number));			
					}
				}
			}
		}
		
		String sql = "SELECT * FROM Version2 " +
		 				"WHERE build > " + getVersion2Number(0,6,7,58) + 
		 				" ORDER BY build DESC" +
						" LIMIT 3";
		printQueryPlan(sql);
		printQueryResult(sql);
	}

		private void printQueryResult(String sql) {
		try(Connection conn = DriverManager.getConnection(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword());
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);) {
		   // Extract data from result set
		   while (rs.next()) {
		   		Map<String,Object> map = new HashMap<>();
		   		for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
		   			map.put(rs.getMetaData().getColumnName(i+1), rs.getObject(i+1));
				}
			   System.out.println("QUERY RESULT: " + map);
		   }
		} catch (Exception e) {
		   e.printStackTrace();
		   Assert.fail(e.getMessage());
		}
	}

	private void printQueryPlan(String sql) {
		try(Connection conn = DriverManager.getConnection(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword());
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

	private int getVersion2Number(int major, int minor, int micro, int build) {
		int number = major;
		number = number * Double.valueOf(Math.pow(10, 1)).intValue() + minor;
		number = number * Double.valueOf(Math.pow(10, 1)).intValue() + micro;
		number = number * Double.valueOf(Math.pow(10, 2)).intValue() + build;
		return number;
	}
	
}
