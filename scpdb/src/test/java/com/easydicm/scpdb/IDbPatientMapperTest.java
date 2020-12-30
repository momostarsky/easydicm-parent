package com.easydicm.scpdb;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class IDbPatientMapperTest {


    private SqlSessionFactory sqlSessionFactory;



    @BeforeEach
    void setUp() throws IOException {



        sqlSessionFactory = PgSqlSessionFactory.getSessionFactory();

        SqlSession session = sqlSessionFactory.openSession();

        Assertions.assertNotNull(session);

        session.close();


    }

    @AfterEach
    void tearDown() {



    }

    @Test
    void insert() {
//        try (SqlSession session = sqlSessionFactory.openSession()) {
//
//
//        }
    }
}