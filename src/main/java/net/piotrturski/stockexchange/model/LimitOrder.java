package net.piotrturski.stockexchange.model;

public class LimitOrder extends Order {

    public LimitOrder(int quantity, int price, int id, Direction direction) {
        super(quantity, price, id, direction);
    }

    @Override
    public void enteringExecution(OrderBook orderBook, int quantity) {
        this.qty -= quantity;
        if (this.qty == 0) {
            orderBook.remove(this);
        }
    }

    @Override
    public void passiveExecution(OrderBook orderBook, int quantity) {
        enteringExecution(orderBook, quantity);
    }
}
