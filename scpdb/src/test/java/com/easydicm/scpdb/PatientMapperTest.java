package com.easydicm.scpdb;

import com.easydicm.scpdb.entities.Patient;
import com.easydicm.scpdb.mapper.PatientMapper;
import org.apache.ibatis.io.DefaultVFS;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class PatientMapperTest {


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
        try (SqlSession session = sqlSessionFactory.openSession()) {

            PatientMapper pm = session.getMapper(PatientMapper.class);
            Patient px = new Patient();
            px.setPatientId("1111s@@Rs1");
            px.setPatientName("2394xx20230923");
            pm.insert(px);
            session.commit();
        }
    }
}