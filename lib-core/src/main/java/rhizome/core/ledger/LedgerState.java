package rhizome.core.ledger;

import java.util.Map;
import rhizome.core.transaction.TransactionAmount;

public record LedgerState(Map<PublicAddress, TransactionAmount> state) {}
