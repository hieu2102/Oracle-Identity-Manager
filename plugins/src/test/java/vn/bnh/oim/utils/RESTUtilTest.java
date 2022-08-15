package vn.bnh.oim.utils;


import org.junit.jupiter.api.Test;

public class RESTUtilTest {
    @Test
    public void createHeader() {
        System.out.println(RESTUtil.buildHeader("LOS_API"));
    }
}