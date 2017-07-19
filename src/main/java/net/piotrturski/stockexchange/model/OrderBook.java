package net.piotrturski.stockexchange.model;

import com.google.common.collect.*;
import lombok.Setter;
import lombok.Value;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;


public class OrderBook {

    final private ImmutableMap<Order.Direction, ListMultimap<Integer, Order>> orders = ImmutableMap.of(
        Order.Direction.Buy, Multimaps.newListMultimap(new TreeMap<Integer, Collection<Order>>().descendingMap(), LinkedList::new),
        Order.Direction.Sell, Multimaps.newListMultimap(new TreeMap<Integer, Collection<Order>>(), LinkedList::new)
    );

    @Setter private Consumer<TransactionEvent> transactionListener = __ -> {};

    public Collection<Order> ordersView(Order.Direction direction) {
        return Collections.unmodifiableCollection(orders.get(direction).values());
    }

    public void newOrder(Order enteringOrder) {

        Order.Direction oppositeDirection = enteringOrder.getDirection() == Order.Direction.Buy ? Order.Direction.Sell : Order.Direction.Buy;
        Predicate<Order> isPriceAcceptable = isPriceAcceptable(enteringOrder);

        add(enteringOrder);

        while (true) {
            Optional<Order> passiveTradePartner = getFirstOrder(enteringOrder.getDirection())
                    .filter(first -> first == enteringOrder)
                    .flatMap(first -> getFirstOrder(oppositeDirection)
                            .filter(isPriceAcceptable));

            if (!passiveTradePartner.isPresent()) {
                break;
            }

            runTransaction(enteringOrder, passiveTradePartner.get());
        }
    }

    @Value
    public static class TransactionEvent {

        private int buyOrderId, sellOrderId, quantity, price;

        TransactionEvent(Order entering, Order passive, int quantity) {
            buyOrderId = (entering.getDirection() == Order.Direction.Buy ? entering : passive).getId();
            sellOrderId = (entering.getDirection() == Order.Direction.Sell ? entering : passive).getId();
            this.quantity = quantity;
            this.price = passive.getPrice();
        }

    }

    void remove(Order order) {
        orders.get(order.getDirection()).remove(order.getPrice(), order);
    }

    void add(Order order) {
        orders.get(order.getDirection()).put(order.getPrice(), order);
    }

    private Predicate<Order> isPriceAcceptable(Order enteringOrder) {
        int priceDifferenceSignExpected = enteringOrder.getDirection() == Order.Direction.Buy ? 1 : -1;
        return passive -> {
            int priceDifference = enteringOrder.getPrice() - passive.getPrice();
            return priceDifference * priceDifferenceSignExpected >= 0;
        };
    }

    private Optional<Order> getFirstOrder(Order.Direction direction) {
        return Optional.ofNullable(
                    Iterables.getFirst(orders.get(direction).entries(), null))
                    .map(Map.Entry::getValue);
    }

    private void runTransaction(Order entering, Order passive) {

        int transactionQuantity = Math.min(entering.getQty(), passive.getQty());

        entering.enteringExecution(this, transactionQuantity);
        passive.passiveExecution(this, transactionQuantity);

        transactionListener.accept(new TransactionEvent(entering, passive, transactionQuantity));
    }

}
