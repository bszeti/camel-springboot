package com.mycompany;

import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.jms.ConnectionFactory;

/**
 * Configuration parameters filled in from application.properties and overridden using env variables on Openshift.
 */
@Configuration
@ConfigurationProperties(prefix = "amq")
public class JMSConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JMSConfiguration.class);

    private String remoteUrl;
    private String username;
    private String password;
    private Integer maxConnections;

    @Bean(name="jms")
	 public JmsComponent jmsComponent(@Autowired ConnectionFactory pooledConnectionFactory) {
	 	JmsComponent component = JmsComponent.jmsComponent(pooledConnectionFactory);
	 	return component;
    }

    @Bean
    @Primary
    public ConnectionFactory pooledConnectionFactory(){
        log.info("Broker url: {}",remoteUrl);
        JmsConnectionFactory factory = new JmsConnectionFactory(remoteUrl);
        factory.setUsername(this.getUsername());
        factory.setPassword(this.getPassword());

        PooledConnectionFactory jmsPoolConnectionFactory = new PooledConnectionFactory();
        jmsPoolConnectionFactory.setConnectionFactory(factory);
        jmsPoolConnectionFactory.setMaxConnections(maxConnections);

        return jmsPoolConnectionFactory;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }
}
