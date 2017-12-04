package my.company.utils;

import org.apache.camel.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Example class with common helper methods taking configuration from properties
 */
@ConfigurationProperties(prefix="routeHelper")
@Component //We register this as a component to make it a singleton and so it shows up at /configprops
public class RouteHelper {
    private static final Logger log = LoggerFactory.getLogger(RouteHelper.class);

    private String logHeadersPattern;

    public RouteHelper() {
    }

    public void logHeadersByPattern(@Headers Map<String,Object> headers){
        if (logHeadersPattern != null) {
            log.info("Headers: {}", headers.keySet().stream().filter(s -> s.matches(logHeadersPattern)).collect(Collectors.toList()));
        }
    }

    public String getLogHeadersPattern() {
        return logHeadersPattern;
    }

    public void setLogHeadersPattern(String logHeadersPattern) {
        this.logHeadersPattern = logHeadersPattern;
    }
}
