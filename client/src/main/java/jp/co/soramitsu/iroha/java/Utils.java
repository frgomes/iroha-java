package jp.co.soramitsu.iroha.java;

import static javax.xml.bind.DatatypeConverter.parseHexBinary;
import static jp.co.soramitsu.crypto.ed25519.Ed25519Sha3.privateKeyFromBytes;
import static jp.co.soramitsu.crypto.ed25519.Ed25519Sha3.publicKeyFromBytes;

import iroha.protocol.BlockOuterClass.Block;
import iroha.protocol.BlockOuterClass.Block_v1;
import iroha.protocol.Endpoint.TxList;
import iroha.protocol.Endpoint.TxStatusRequest;
import iroha.protocol.Primitive;
import iroha.protocol.Primitive.Signature;
import iroha.protocol.Queries;
import iroha.protocol.TransactionOuterClass;
import iroha.protocol.TransactionOuterClass.Transaction.Payload.BatchMeta.BatchType;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.xml.bind.DatatypeConverter;
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3;
import jp.co.soramitsu.iroha.java.detail.Hashable;
import lombok.val;
import org.spongycastle.jcajce.provider.digest.SHA3;

public class Utils {

  /**
   * Parse a keypair from hex strings
   *
   * @param hexPublicKey 64-byte (128-symbol) hexstring, public key
   * @param hexPrivateKey 64-byte (128-symbol) hexstring, private key
   * @return Ed25519-Sha3 KeyPair instance
   */
  public static KeyPair parseHexKeypair(String hexPublicKey, String hexPrivateKey) {
    return new KeyPair(
        parseHexPublicKey(hexPublicKey),
        parseHexPrivateKey(hexPrivateKey)
    );
  }

  /**
   * Parse a public key from hexstring
   *
   * @param hexPublicKey 64-byte (128-symbol) hexstring, public key
   * @return Ed25519-Sha3 PublicKey instance
   */
  public static PublicKey parseHexPublicKey(String hexPublicKey) {
    return publicKeyFromBytes(parseHexBinary(hexPublicKey));
  }

  /**
   * Parse a private key from hexstring
   *
   * @param hexPrivateKey 64-byte (128-symbol) hexstring, private key
   * @return Ed25519-Sha3 PrivateKey instance
   */
  public static PrivateKey parseHexPrivateKey(String hexPrivateKey) {
    return privateKeyFromBytes(parseHexBinary(hexPrivateKey));
  }

  /**
   * Calculate SHA3-256 hash of {@link iroha.protocol.TransactionOuterClass.Transaction}
   *
   * @param tx Protobuf transaction
   * @return 32 bytes hash
   */
  public static byte[] reducedHash(TransactionOuterClass.Transaction tx) {
    return reducedHash(tx.getPayload().getReducedPayload());
  }

  /**
   * Calculate SHA3-256 hash of {@link iroha.protocol.TransactionOuterClass.Transaction.Payload.ReducedPayload}
   *
   * @param reducedPayload Protobuf of ReducedPayload
   * @return 32 bytes hash
   */
  public static byte[] reducedHash(
      TransactionOuterClass.Transaction.Payload.ReducedPayload reducedPayload) {
    val sha3 = new SHA3.Digest256();
    val data = reducedPayload.toByteArray();
    return sha3.digest(data);
  }

  /**
   * Calculate SHA3-256 hash of {@link iroha.protocol.TransactionOuterClass.Transaction}
   *
   * @param tx Protobuf Transaction
   * @return 32 bytes hash
   */
  public static byte[] hash(TransactionOuterClass.Transaction tx) {
    val sha3 = new SHA3.Digest256();
    val data = tx.getPayload().toByteArray();
    return sha3.digest(data);
  }

  /**
   * Calculate SHA3-256 hash of {@link Block_v1}
   *
   * @param b BlockV1
   * @return 32 bytes hash
   */
  public static byte[] hash(Block_v1 b) {
    val sha3 = new SHA3.Digest256();
    val data = b.getPayload().toByteArray();
    return sha3.digest(data);
  }

  /**
   * Calculate SHA3-256 hash of {@link Block}
   *
   * @param b Protobuf Block
   * @return 32 bytes hash
   */
  public static byte[] hash(Block b) {
    switch (b.getBlockVersionCase()) {
      case BLOCK_V1:
        return hash(b.getBlockV1());
      default:
        throw new IllegalArgumentException(
            String.format("Block has undefined version: %s", b.getBlockVersionCase()));
    }
  }

  /**
   * Calculate SHA3-256 hash of {@link Queries.Query}
   *
   * @param q Protobuf Query
   * @return 32 bytes hash
   */
  public static byte[] hash(Queries.Query q) {
    val sha3 = new SHA3.Digest256();
    val data = q.getPayload().toByteArray();
    return sha3.digest(data);
  }

  /* default */
  static <T extends Hashable> Primitive.Signature sign(T t, KeyPair kp) {
    byte[] rawSignature = new Ed25519Sha3().rawSign(t.hash(), kp);

    return Signature.newBuilder()
        .setSignature(
            Utils.toHex(rawSignature)
        )
        .setPublicKey(
            Utils.toHex(kp.getPublic().getEncoded())
        )
        .build();
  }

  /**
   * This method is here only because some old versions of Android do not have Objects.nonNull
   *
   * @param obj any object
   * @return true if object is not null
   */
  public static boolean nonNull(Object obj) {
    return obj != null;
  }

  /**
   * Helper method to create {@link TxStatusRequest} from byte array
   *
   * @param hash tx hash
   * @return {@link TxStatusRequest}
   */
  public static TxStatusRequest createTxStatusRequest(byte[] hash) {
    return TxStatusRequest.newBuilder()
        .setTxHash(Utils.toHex(hash))
        .build();
  }

  /**
   * Helper method tto create {@link TxList} from iterable
   *
   * @param list list of protobuf transactions
   * @return {@link TxList}
   */
  public static TxList createTxList(Iterable<TransactionOuterClass.Transaction> list) {
    return TxList.newBuilder()
        .addAllTransactions(list)
        .build();
  }

  /**
   * Create Ordered Batch of transactions from iterable
   */
  public static Iterable<TransactionOuterClass.Transaction> createTxOrderedBatch(
      Iterable<TransactionOuterClass.Transaction> list, KeyPair keyPair) {
    return createBatch(list, BatchType.ORDERED, keyPair);
  }

  /**
   * Create Atomic Batch of transactions from iterable
   */
  public static Iterable<TransactionOuterClass.Transaction> createTxAtomicBatch(
      Iterable<TransactionOuterClass.Transaction> list, KeyPair keyPair) {
    return createBatch(list, BatchType.ATOMIC, keyPair);

  }

  /**
   * Convert bytes to hexstring
   */
  public static String toHex(byte[] b) {
    return DatatypeConverter.printHexBinary(b);
  }

  private static Iterable<String> getBatchHashesHex(
      Iterable<TransactionOuterClass.Transaction> list) {
    return StreamSupport.stream(list.spliterator(), false)
        .map(tx -> toHex(reducedHash(tx)))
        .collect(Collectors.toList());
  }

  private static Iterable<TransactionOuterClass.Transaction> createBatch(
      Iterable<TransactionOuterClass.Transaction> list, BatchType batchType, KeyPair keyPair) {
    final Iterable<String> batchHashes = getBatchHashesHex(list);
    return StreamSupport.stream(list.spliterator(), false)
        .map(tx -> Transaction
            .parseFrom(tx)
            .makeMutable()
            .setBatchMeta(batchType, batchHashes)
            .sign(keyPair)
            .build()
        )
        .collect(Collectors.toList());
  }


}
