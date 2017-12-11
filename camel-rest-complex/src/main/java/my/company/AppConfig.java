package my.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import my.company.utils.BasicDataSourceMixIn;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Additional beans
 * Beans usually can be configured in the Application.java but in some cases it can cause an infinite dependency loop for unit tests.
 * For example ObjectMapper customization with using RestTemplate in unit tests had that problem
 *
 */
@Configuration
public class AppConfig {
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
	/**
	 * Customize the Spring auto-configured Jackson ObjectMapper (not used by Camel rest).
	 * In this example we set marshaling rules for BasicDataSource.class
	 */
	@Bean
	public Jackson2ObjectMapperBuilderCustomizer customObjectMapper() {
		return new Jackson2ObjectMapperBuilderCustomizer() {
			@Override
			public void customize(Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder) {
				jacksonObjectMapperBuilder
						.mixIn(BasicDataSource.class, BasicDataSourceMixIn.class)
						.indentOutput(true);
			}
		};
	}

	/**
	 * Example how skip auto-configuration and use a custom ObjectMapper.
	 * Also can use a Jackson2ObjectMapperBuilder bean for the same purpose.
	 */
	//@Bean
	ObjectMapper customObjectMapperForSpring() {
		ObjectMapper mapper = new ObjectMapper();
		JavaTimeModule javaTimeModule = new JavaTimeModule();
		javaTimeModule.addSerializer(new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("YYYY-MMM")));
		mapper.registerModule(javaTimeModule);
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.setDateFormat(new SimpleDateFormat("YYYY"));
		return mapper;
	}

	/**
	 * Example to create a WSS4JOutInterceptor to add WS security signature headers for CXF client
	 */
    //See https://ws.apache.org/wss4j/config.html
    @Autowired //autowire PrivateKey argument
    @Bean("addSignatureInterceptor")
	WSS4JOutInterceptor buildAddSignature(PrivateKey privateKey){
        Map<String,Object> securityProperties = new HashMap<>();

        if (StringUtils.isNotBlank(privateKey.getKeystore())) {
            log.debug("Adding signature to SOAP call using {}/{}", privateKey.getKeystore(), privateKey.getAlias());

            Properties properties = new Properties();
            properties.put("org.apache.ws.security.crypto.merlin.keystore.file", privateKey.getKeystore());
            properties.put("org.apache.ws.security.crypto.merlin.keystore.password", privateKey.getPassword());

            securityProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
            securityProperties.put(WSHandlerConstants.MUST_UNDERSTAND, "false"); // Set this as the GlobalWeather service doesn't need/understand security headers
            securityProperties.put(WSHandlerConstants.SIGNATURE_USER, privateKey.getAlias());
            securityProperties.put(WSHandlerConstants.PW_CALLBACK_REF, new CallbackHandler() {
                @Override
                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for (Callback callback: callbacks){
                        if (callback instanceof WSPasswordCallback){
                            ((WSPasswordCallback) callback).setPassword(
                                    //Use key password if a different one is set, otherwise assume that it's tha same as keystore password
                                    privateKey.getKeyPassword() != null ? privateKey.getKeyPassword() : privateKey.getPassword()
                            );
                        }
                    }
                }
            });
            securityProperties.put(WSHandlerConstants.SIG_PROP_REF_ID,"signatureProperties");
            securityProperties.put("signatureProperties",properties);

            return new WSS4JOutInterceptor(securityProperties);
        } else {
            //Create a no-op interceptor if key is not configured
            securityProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.NO_SECURITY);
        }

        return new WSS4JOutInterceptor(securityProperties);
	}

	@ConfigurationProperties(prefix = "GlobalWeather.signatureKey")
    @Component //Create a PrivateKey bean from properties
	public class PrivateKey{
        private String keystore;
        private String password;
        private String alias;
        private String keyPassword;

        public String getKeystore() {
            return keystore;
        }

        public void setKeystore(String keystore) {
            this.keystore = keystore;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public String getKeyPassword() {
            return keyPassword;
        }

        public void setKeyPassword(String keyPassword) {
            this.keyPassword = keyPassword;
        }

        @Override
        public String toString() {
            return "PrivateKey{" +
                    "keystore='" + keystore + '\'' +
                    ", alias='" + alias + '\'' +
                    '}';
        }
    }
}
