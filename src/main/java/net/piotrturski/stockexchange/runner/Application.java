package net.piotrturski.stockexchange.runner;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.collect.ImmutableMap;
import io.vavr.control.Try;
import net.piotrturski.stockexchange.model.IcebergOrder;
import net.piotrturski.stockexchange.model.LimitOrder;
import net.piotrturski.stockexchange.model.Order;
import net.piotrturski.stockexchange.model.OrderBook;
import net.piotrturski.stockexchange.model.OrderBook.TransactionEvent;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET;

public class Application {

    ObjectMapper objectMapper = new ObjectMapper()
                                        .disable(JsonParser.Feature.AUTO_CLOSE_SOURCE)
                                        .disable(AUTO_CLOSE_TARGET)
                                        .registerModule(new ParameterNamesModule());;

    ImmutableMap<String, Class<? extends Order>> types = ImmutableMap.of("Limit", LimitOrder.class,
                                                                         "Iceberg", IcebergOrder.class);

    public void run(InputStream input, OutputStream output) throws IOException {

        OrderBook orderBook = new OrderBook();
        LinkedList<TransactionEvent> transactions = new LinkedList<>();
        orderBook.setTransactionListener(transactions::add);

        IOUtils.lineIterator(input, StandardCharsets.UTF_8).forEachRemaining(line -> Try.run(() -> {

                    transactions.clear();
                    GenericOrder genericOrder = objectMapper.readValue(line, GenericOrder.class);
                    JsonParser jsonParser = objectMapper.treeAsTokens(genericOrder.order);
                    Class<? extends Order> aClass = types.get(genericOrder.type);
                    Order order = objectMapper.readValue(jsonParser, aClass);

                    orderBook.newOrder(order);

                    serialize(orderBook, output);
                    IOUtils.write("\n", output, StandardCharsets.UTF_8);
                    output.flush();
                    for (TransactionEvent tr : transactions) {
                        objectMapper.writeValue(output, tr);
                        IOUtils.write("\n", output, StandardCharsets.UTF_8);
                    }
                    output.flush();
        }));
    }

    static class GenericOrder {
        public String type;
        public JsonNode order;
    }

    private void serialize(OrderBook orderBook, OutputStream outputStream) throws IOException {
        Map<String, List<ImmutableMap<String, Integer>>> map =
                EntryStream.of("buyOrders", Order.Direction.Buy,
                                "sellOrders", Order.Direction.Sell)
                .mapValues(orderBook::ordersView)
                .mapValues(orders -> StreamEx.of(orders)
                        .map(order -> ImmutableMap.of("id", order.getId(),
                                "price", order.getPrice(),
                                "quantity", order.getQty()))
                        .toList())
                .toMap();

        objectMapper.writeValue(outputStream, map);
    }

    public static void main(String[] args) throws IOException {
        new Application().run(System.in, System.out);
    }
}
