package com.easydicm.storescp;

import com.google.common.io.Resources;
import lombok.SneakyThrows;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * @author dhz
 */
@Configuration
@MapperScan(basePackages = { "com.easydicm.scpdb.mapper"} )
public class AppConfig {

    @SneakyThrows
    @Bean
    public DataSource getDataSource() {


        Properties prop = new Properties();
        InputStream in = Resources.getResource("jdbc.properties").openStream();
        prop.load(in);
        return DataSourceBuilder.create()
                .driverClassName( prop.getProperty("driver") )
                .url( prop.getProperty("url")  )
                .username( prop.getProperty("username") )
                .password( prop.getProperty("password")).build();
    }

    @Bean
    public DataSourceTransactionManager transactionManager() {
        return new DataSourceTransactionManager(getDataSource());
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(getDataSource());
        return sessionFactory.getObject();
    }
}
