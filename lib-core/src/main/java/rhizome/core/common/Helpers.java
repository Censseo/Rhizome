package rhizome.core.common;


import rhizome.core.transaction.TransactionAmount;

public class Helpers {

    private Helpers() {}

    public static TransactionAmount PDN(double amount) {
        return new TransactionAmount((long) (amount * Constants.DECIMAL_SCALE_FACTOR));
    }
}
