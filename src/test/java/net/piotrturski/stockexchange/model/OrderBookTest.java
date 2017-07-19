package net.piotrturski.stockexchange.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import net.piotrturski.stockexchange.model.OrderBook.TransactionEvent;
import net.piotrturski.stockexchange.runner.Application;
import one.util.streamex.StreamEx;
//import org.assertj.guava.api.Assertions;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.function.Consumer;

import static net.piotrturski.stockexchange.model.Order.Direction.Buy;
import static net.piotrturski.stockexchange.model.Order.Direction.Sell;
import static org.assertj.core.api.Assertions.assertThat;

public class OrderBookTest {

    LinkedList<TransactionEvent> transactionEvents = new LinkedList<>();
    Consumer<TransactionEvent> transactionEventListener = transactionEvents::add;
    OrderBook orderBook = new OrderBook();

    @Test
    public void should_sell_at_best_price() {
        orderBook.setTransactionListener(transactionEventListener);

        Order[] o = new Order[] {
                new LimitOrder(20, 14, 1, Buy),
                new IcebergOrder(50, 15, 2, Buy, 20),
                new LimitOrder(15, 16, 3, Sell),
                new LimitOrder(60, 13, 4, Sell)
        };

        Arrays.stream(o).forEach(orderBook::newOrder);


        assertThat(orderBook.ordersView(Buy))
                .containsExactly(o[0]);

        assertThat(transactionEvents)
                .containsExactly(
                        new TransactionEvent(o[3], o[1], 20),
                        new TransactionEvent(o[3], o[1], 20),
                        new TransactionEvent(o[3], o[1], 10),
                        new TransactionEvent(o[3], o[0], 10)
                        );
    }

    @Test
    public void entering_iceberg_should_be_able_to_consume_more_than_peak() throws Exception {
        orderBook.setTransactionListener(transactionEventListener);

        LimitOrder buy1 = new LimitOrder(20, 10, 1, Buy);
        LimitOrder buy2 = new LimitOrder(20, 10, 1, Buy);
        IcebergOrder icebergSell = new IcebergOrder(35, 10, 1, Sell, 2);

        orderBook.newOrder(buy1);
        orderBook.newOrder(buy2);
        orderBook.newOrder(icebergSell);

        assertThat(transactionEvents).hasSize(18);

        assertThat(orderBook.ordersView(Sell)).isEmpty();
        assertThat(orderBook.ordersView(Buy)).containsExactly(buy2);

        assertThat(buy2.getQty()).isEqualTo(5);

    }

}