package com.zioneltechnology.kjva_bible_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;

@SpringBootTest
public class DdlTest {
    @Autowired EntityManager em;
    @Test
    public void test() {
        System.out.println("DDL TEST RAN");
    }
}
