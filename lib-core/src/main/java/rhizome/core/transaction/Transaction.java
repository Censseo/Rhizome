package rhizome.core.transaction;

import org.json.JSONObject;

import rhizome.core.crypto.PrivateKey;
import rhizome.core.crypto.PublicKey;
import rhizome.core.crypto.SHA256Hash;
import rhizome.core.ledger.PublicAddress;
import rhizome.core.serialization.Serializable;
import rhizome.core.transaction.dto.TransactionDto;

public sealed interface Transaction permits TransactionImpl {

    public static Transaction empty() {
        return TransactionImpl.builder().build();
    }

    public static Transaction of(JSONObject json){
        return serializer().fromJson(json);
    }

    public static Transaction of(Transaction transaction) {
        var transactionImpl = (TransactionImpl) transaction;
        return TransactionImpl.builder()
                .from(transactionImpl.from())
                .to(transactionImpl.to())
                .amount(transactionImpl.amount())
                .isTransactionFee(transactionImpl.isTransactionFee())
                .timestamp(transactionImpl.timestamp())
                .fee(transactionImpl.fee())
                .signingKey(transactionImpl.signingKey())
                .signature(transactionImpl.signature())
                .build();
    }

    public static Transaction of(TransactionDto transactionDto) {
        return serializer().deserialize(transactionDto);
    }

    public static Transaction of(PublicAddress from, PublicAddress to, TransactionAmount amount, PublicKey signingKey, TransactionAmount fee) {
        return TransactionImpl.builder()
                .from(from)
                .to(to)
                .amount(amount)
                .isTransactionFee(false)
                .timestamp(System.currentTimeMillis())
                .fee(fee)
                .signingKey(signingKey)
                .build();
    }

    public static Transaction of(PublicAddress from, PublicAddress to, TransactionAmount amount, PublicKey signingKey, TransactionAmount fee, long timestamp) {
        return TransactionImpl.builder()
                .from(from)
                .to(to)
                .amount(amount)
                .isTransactionFee(false)
                .timestamp(timestamp)
                .fee(fee)
                .signingKey(signingKey)
                .build();
    }

    public static Transaction of(PublicAddress from, PublicAddress to, TransactionAmount amount, PublicKey signingKey) {
        return TransactionImpl.builder()
                .from(from)
                .to(to)
                .amount(amount)
                .isTransactionFee(false)
                .timestamp(System.currentTimeMillis())
                .signingKey(signingKey)
                .build();
    }

    public static Transaction of(PublicAddress to, TransactionAmount amount) {
        return TransactionImpl.builder()
                .to(to)
                .amount(amount)
                .isTransactionFee(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public TransactionDto serialize();
    default TransactionDto serialize(Transaction transaction) {
        return serializer().serialize(transaction);
    }

    public JSONObject toJson();
    default JSONObject toJson(Transaction transaction) {
        return serializer().toJson(transaction);
    }

    public Transaction sign(PrivateKey privateKey);
    public boolean signatureValid();
    public SHA256Hash hashContents();
    public SHA256Hash hash();
    public PublicAddress from();
    public PublicAddress to();

    /**
     * Get instance of the serializer
     * @return
     */
    static TransactionSerializer serializer(){
        return TransactionSerializer.instance;
    }

    /**
     * Serializes the Transaction
     */
    static class TransactionSerializer implements Serializable<TransactionDto, Transaction> {

        static final String TO = "to";
        static final String AMOUNT = "amount";
        static final String TIMESTAMP = "timestamp";
        static final String FEE = "fee";
        static final String TXID = "txid";
        static final String FROM = "from";
        static final String SIGNING_KEY = "signingKey";
        static final String SIGNATURE = "signature";

        static TransactionSerializer instance = new TransactionSerializer();

        @Override
        public TransactionDto serialize(Transaction transaction) {
            var transactionImpl = (TransactionImpl) transaction;
            return new TransactionDto(
                transactionImpl.signature(),
                transactionImpl.signingKey(),
                transactionImpl.timestamp(),
                transactionImpl.to(),
                transactionImpl.amount().amount(),
                transactionImpl.fee().amount(),
                transactionImpl.isTransactionFee()
            );
        }
    
        @Override
        public Transaction deserialize(TransactionDto transactionDto) {
            return TransactionImpl.builder()
                .from(transactionDto.isTransactionFee ? PublicAddress.empty() : PublicAddress.of(transactionDto.signingKey))
                .to(transactionDto.to)
                .amount(new TransactionAmount(transactionDto.amount))
                .isTransactionFee(transactionDto.isTransactionFee)
                .timestamp(transactionDto.timestamp)
                .fee(new TransactionAmount(transactionDto.fee))
                .signingKey(transactionDto.signingKey)
                .signature(transactionDto.signature)
                .build();
        }
    
        @Override
        public JSONObject toJson(Transaction transaction) {
            var transactionImpl = (TransactionImpl) transaction;    
            JSONObject result = new JSONObject();
            result.put(TO, transactionImpl.to().toHexString());
            result.put(AMOUNT, transactionImpl.amount().amount());
            result.put(TIMESTAMP, Long.toString(transactionImpl.timestamp()));
            result.put(FEE, transactionImpl.fee().amount());
            
            if (!transactionImpl.isTransactionFee()) {
                result.put(TXID, transactionImpl.hashContents().toHexString());
                result.put(FROM, transactionImpl.from().toHexString());
                result.put(SIGNING_KEY, transactionImpl.signingKey().toHexString());
                result.put(SIGNATURE, transactionImpl.signature().toHexString());
            } else {
                result.put(TXID, transactionImpl.hashContents().toHexString());
                result.put(FROM, "");
            }
            
            return result;
        }
    
        public Transaction fromJson(JSONObject json) {     
            var builder = TransactionImpl.builder()
                .timestamp(json.getLong(TIMESTAMP))
                .fee(new TransactionAmount(json.getInt(FEE)))
                .to(PublicAddress.of(json.getString(TO)));

        
            if (json.getString("from").isEmpty()) {
                builder.amount(new TransactionAmount(json.getInt(AMOUNT)))
                    .isTransactionFee(true);
            } else {
                builder.from(PublicAddress.of(json.getString(FROM)))
                    .signature(TransactionSignature.of(json.getString(SIGNATURE)))
                    .amount(new TransactionAmount(json.getInt(AMOUNT)))
                    .isTransactionFee(false)
                    .signingKey(PublicKey.of(json.getString(SIGNING_KEY)));
            }   
            
            return builder.build();
        }
    }
}
