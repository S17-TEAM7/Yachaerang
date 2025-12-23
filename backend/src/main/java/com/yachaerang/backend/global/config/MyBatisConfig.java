package com.yachaerang.backend.global.config;

import com.yachaerang.backend.api.common.*;
import com.yachaerang.backend.global.util.*;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
public class MyBatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception{
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);

        org.apache.ibatis.session.Configuration configuration =
                new org.apache.ibatis.session.Configuration();

        TypeHandlerRegistry registry = configuration.getTypeHandlerRegistry();

        // CodeEnumTypeHandler
        registry.register(MemberStatus.class, new MemberStatusTypeHandler());
        registry.register(Role.class, new RoleTypeHandler());
        registry.register(PeriodType.class, new PeriodTypeHandler());
        registry.register(SenderRole.class, new SenderRoleHandler());
        registry.register(ReactionType.class, new ReactionTypeHandler());

        sqlSessionFactoryBean.setConfiguration(configuration);

        // Mapper XML
        sqlSessionFactoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mappers/**/*.xml")
        );

        return sqlSessionFactoryBean.getObject();

    }
}
