package com.easydicm.scpdb;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.io.IOException;
import java.util.List;

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
        ScriptEngineManager manager = new ScriptEngineManager();
        List<ScriptEngineFactory> factories = manager.getEngineFactories();
        for (ScriptEngineFactory factory : factories)
        {
            System.out.println(factory.getNames());
        }

    }
}