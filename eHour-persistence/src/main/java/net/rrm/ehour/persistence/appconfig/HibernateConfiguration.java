package net.rrm.ehour.persistence.appconfig;

import com.google.common.collect.Lists;
import net.rrm.ehour.appconfig.EhourHomeUtil;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

@org.springframework.context.annotation.Configuration
public class HibernateConfiguration {
    @Autowired
    private DataSource dataSource;

    @Value("${ehour.database}")
    private String databaseName;

    @Value("${ehour.db.cache:true}")
    private String caching;

    private static final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private static final Logger LOGGER = Logger.getLogger(HibernateConfiguration.class);

    @Bean(name = "sessionFactory")
    public SessionFactory getSessionFactory() throws Exception {
        validateAndSetCaching();

        Properties configProperties = EhourHomeUtil.loadDatabaseProperties(databaseName);

        LOGGER.info("Using database type: " + databaseName);

        LocalSessionFactoryBean sessionFactoryBean = new LocalSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource);

        List<Resource> mappingResources = getMappingResources(configProperties);
        sessionFactoryBean.setMappingLocations(mappingResources.toArray(new Resource[mappingResources.size()]));

        sessionFactoryBean.setPackagesToScan("net.rrm.ehour.domain");

        Properties properties = new Properties();

        properties.setProperty(AvailableSettings.DIALECT, (String) configProperties.get("hibernate.dialect"));
        properties.setProperty(AvailableSettings.SHOW_SQL, "false");
        properties.setProperty("use_outer_join", "true");
        properties.setProperty(AvailableSettings.CACHE_REGION_FACTORY, "org.hibernate.cache.ehcache.EhCacheRegionFactory");
        properties.setProperty(AvailableSettings.USE_SECOND_LEVEL_CACHE, caching);
        properties.setProperty("net.sf.ehcache.configurationResourceName", "hibernate-ehcache.xml");
        properties.setProperty(AvailableSettings.USE_QUERY_CACHE, caching);
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, (String) configProperties.get("hibernate.hbm2ddl.auto"));
        properties.setProperty(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "thread");

        sessionFactoryBean.setHibernateProperties(properties);
        sessionFactoryBean.afterPropertiesSet();
        return sessionFactoryBean.getObject();
    }

    private void validateAndSetCaching() {
        setDefaultCachingType();

        validateCachingAttribute();
    }

    private void validateCachingAttribute() {
        if (!caching.equalsIgnoreCase("true") && !caching.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("ehour.db.cache property must either be true or false");
        }
    }

    private void setDefaultCachingType() {
        if (caching == null) {
            caching = "true";
        }
    }

    private List<Resource> getMappingResources(Properties configProperties) throws IOException {
        Resource[] queryResources = resolver.getResources("classpath:query/common/*.hbm.xml");
        ClassPathResource dbQueryResource = new ClassPathResource("query/" + configProperties.getProperty("reportquery.filename"));
        return Lists.asList(dbQueryResource, queryResources);
    }

    @Bean(name = "transactionManager")
    public HibernateTransactionManager getTransactionManager() throws Exception {
        return new HibernateTransactionManager(getSessionFactory());
    }

    @Bean
    public JdbcTemplate getJdbcTemplate() throws Exception {
        return new JdbcTemplate(dataSource);
    }

    void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
