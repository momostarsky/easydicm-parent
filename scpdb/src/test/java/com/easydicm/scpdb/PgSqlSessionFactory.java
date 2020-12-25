package com.easydicm.scpdb;

import java.sql.SQLException;

import javax.sql.DataSource;

import com.easydicm.scpdb.mapper.PatientMapper;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

/**
 * 基于  resources:datasource-config.xml  文件生成Postgres  数据库链接
 *
 * @author dhz
 */
public class PgSqlSessionFactory {


    private PgSqlSessionFactory() {


    }



    public static SqlSessionFactory getSessionFactory()  {

        String driver = "org.postgresql.Driver";
        String url = "jdbc:postgresql://192.168.1.101:5432/postgres";
        String username = "postgres";
        String password = "dpp#1688";

        DataSource dataSource = new PooledDataSource(driver, url, username, password);


        TransactionFactory transactionFactory = new JdbcTransactionFactory();

        Environment environment = new Environment("development", transactionFactory, dataSource);

        Configuration  configuration = new Configuration(environment);


        configuration.addMappers("com.easydicm.scpdb.mapper");

        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        return sqlSessionFactory;

    }


}
