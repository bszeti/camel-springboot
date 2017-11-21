package my.company.utils;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.processor.aggregate.CompletionAwareAggregationStrategy;
import org.apache.camel.processor.aggregate.TimeoutAwareAggregationStrategy;

/**
 * This aggregation strategy "cleans" the exchange after completion of any
 * exception related properties and headers. It can optionally wrap an
 * aggregation strategy. It can be used in cases when the split/multicast is
 * used with stopOnException=false (default) so the route should continue in
 * case of failed sub-exchanges. Aggregation strategies often use the first (or
 * last) exchange as the final merged exchange (e.g.
 * FlexibleAggregationStrategy) and an exception on that exchange can stop any
 * further processing. This strategy makes sure that the exchange has no
 * exceptions so the route will continue processing after the split/multicast.
 */
public class ContinueOnExceptionStrategy
		implements AggregationStrategy, CompletionAwareAggregationStrategy, TimeoutAwareAggregationStrategy {

	private AggregationStrategy wrappedAggregationStrategy;

	public ContinueOnExceptionStrategy() {
	}

	public ContinueOnExceptionStrategy(AggregationStrategy wrappedAggregationStrategy) {
		this.wrappedAggregationStrategy = wrappedAggregationStrategy;
	}

	@Override
	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		if (wrappedAggregationStrategy != null) {
			return wrappedAggregationStrategy.aggregate(oldExchange, newExchange);
		} else {
			return newExchange;
		}
	}

	@Override
	public void timeout(Exchange exchange, int index, int total, long timeout) {
		if (wrappedAggregationStrategy != null
				&& wrappedAggregationStrategy instanceof TimeoutAwareAggregationStrategy) {
			((TimeoutAwareAggregationStrategy) wrappedAggregationStrategy).timeout(exchange, index, index, timeout);
		}
	}

	@Override
	public void onCompletion(Exchange exchange) {
		if (wrappedAggregationStrategy != null
				&& wrappedAggregationStrategy instanceof CompletionAwareAggregationStrategy) {
			((CompletionAwareAggregationStrategy) wrappedAggregationStrategy).onCompletion(exchange);
		}

		// Remove exception, fault and redelivery info from exchange
		exchange.setException(null);
		exchange.removeProperty(Exchange.FAILURE_HANDLED);
		exchange.removeProperty(Exchange.FAILURE_ENDPOINT);
		exchange.removeProperty(Exchange.FAILURE_ROUTE_ID);
		exchange.removeProperty(Exchange.ERRORHANDLER_CIRCUIT_DETECTED);
		exchange.removeProperty(Exchange.ERRORHANDLER_HANDLED);
		exchange.removeProperty(Exchange.EXCEPTION_HANDLED);
		exchange.removeProperty(Exchange.EXCEPTION_CAUGHT);

		Message message = exchange.hasOut() ? exchange.getOut() : exchange.getIn();
		message.setFault(false);
		message.removeHeader(Exchange.REDELIVERED);
		message.removeHeader(Exchange.REDELIVERY_COUNTER);
		message.removeHeader(Exchange.REDELIVERY_DELAY);
		message.removeHeader(Exchange.REDELIVERY_EXHAUSTED);
		message.removeHeader(Exchange.REDELIVERY_MAX_COUNTER);
	}

}
