package net.piotrturski.stockexchange.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public abstract class Order {

    int qty, price, id;
    Direction direction;

    public enum Direction {
        Buy, Sell;
    }

    public abstract void enteringExecution(OrderBook orderBook, int quantity);

    public abstract void passiveExecution(OrderBook orderBook, int quantity);
}
