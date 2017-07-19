package net.piotrturski.stockexchange.model;

public class IcebergOrder extends Order {

    private int totalQty, peak;

    // builder would be more readable
    public IcebergOrder(int quantity, int price, int id, Direction direction, int peak) {
        super(Math.min(quantity, peak), price, id, direction);
        this.totalQty = quantity;
        this.peak = peak;
    }

    @Override
    public void enteringExecution(OrderBook orderBook, int quantity) {
        totalQty -= quantity;
        if (totalQty == 0) {
            orderBook.remove(this);
        } else {
            // part of the iceberg still exists. show peak
            this.qty = Math.min(totalQty, peak);
        }
    }

    @Override
    public void passiveExecution(OrderBook orderBook, int quantity) {
        this.qty -= quantity; // decrease visible peak
        totalQty -= quantity;
        if (this.qty == 0) {
            // visible peak fully consumed
            orderBook.remove(this);
            if (totalQty != 0) {
                // we need to add another peak
                this.qty = Math.min(totalQty, peak);
                orderBook.add(this);
            }
        }
    }
}
