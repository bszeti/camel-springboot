package my.company.utils;

import org.apache.camel.Exchange;
import org.apache.camel.impl.MDCUnitOfWork;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * A custom unit of work that expects/generated an business Id and uses it in MDC logging as camel.breadcrumbId
 */
public class CustomMDCBreadCrumbIdUnitOfWork extends MDCUnitOfWork {
	//A unit of work is created for each exchange so each run during a split/multicast 
	public CustomMDCBreadCrumbIdUnitOfWork(Exchange exchange) {
		super(exchange);
		//Expecting "businessId" and generate one if missing.
		if (exchange.getIn().getHeader("businessId") == null) {
			exchange.getIn().setHeader("businessId",UUID.randomUUID().toString());
		}
		MDC.put(MDC_BREADCRUMB_ID, exchange.getIn().getHeader("businessId",String.class));
	}
}
