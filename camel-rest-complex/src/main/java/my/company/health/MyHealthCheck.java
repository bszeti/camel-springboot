package my.company.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Date;

//All HealthIndicator beans are used automatically
@Component
public class MyHealthCheck implements HealthIndicator {
	private static final Logger log = LoggerFactory.getLogger(MyHealthCheck.class);
	
	@Override
	public Health health() {
		String hostname="unknown";
		
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException ex) {
			log.warn("Failed to get hostname.",ex);
		}
		
		return Health.up()
				.withDetail("hostname", hostname)
				.withDetail("localTime", LocalDateTime.now())
				.withDetail("date", new Date()) // uses spring.jackson.date-format
				.build();
	}

}
