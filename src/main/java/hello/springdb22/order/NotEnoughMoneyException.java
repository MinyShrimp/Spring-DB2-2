package hello.springdb22.order;

/**
 * 고객의 잔고가 부족하면 발생
 */
public class NotEnoughMoneyException extends Exception {
    public NotEnoughMoneyException(String message) {
        super(message);
    }
}
