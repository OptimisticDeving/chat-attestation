package dev.optimistic.chatattestation.util;

import dev.optimistic.chatattestation.crypto.SigningManager;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

public record Payload(byte[] msg, byte[] signature, byte[] key, byte[] nonce, boolean compressedMsg) {
  private static final int ORIGINAL_MSG_BUDGET = (256 - 4) - 64 - 16 - 2 - 1; // 64 for signature size, 16 for key hash, 2 for nonce, 1 for payload header
  private static final Payload BAD_PAYLOAD = new Payload(new byte[0], new byte[0], new byte[0], new byte[0], false);

  public static Payload read(byte[] msg, DataInputStream input) throws IOException {
    final int replacementMsgBytes = input.readUnsignedByte();
    final boolean isCompressed = replacementMsgBytes != 0;
    if (isCompressed) {
      if (msg.length != 0) return BAD_PAYLOAD;
      msg = new byte[replacementMsgBytes];
      input.readFully(msg);
    }
    final byte[] signature = new byte[64];
    input.readFully(signature);
    final byte[] keyHash = new byte[16];
    input.readFully(keyHash);
    final byte[] nonce = new byte[2];
    input.readFully(nonce);
    return new Payload(msg, signature, keyHash, nonce, isCompressed);
  }

  public static byte[] withNonce(byte[] nonce, byte[] msg) {
    final byte[] msgWithNonce = new byte[msg.length + 2];
    System.arraycopy(nonce, 0, msgWithNonce, 0, 2);
    System.arraycopy(msg, 0, msgWithNonce, 2, msg.length);
    return msgWithNonce;
  }

  public static boolean write(byte[] msg, DataOutput output) throws IOException {
    final boolean compressed = msg.length > ORIGINAL_MSG_BUDGET;

    if (compressed) {
      final var out = new ByteArrayOutputStream();
      final var cmp = new ZstdCompressorOutputStream(out);
      cmp.write(msg);
      cmp.flush();
      cmp.close();
      msg = out.toByteArray();
    }

    output.writeByte(compressed ? msg.length : 0);
    if (compressed) output.write(msg);
    final byte[] nonce = new byte[2];
    ThreadLocalRandom.current().nextBytes(nonce);
    msg = withNonce(nonce, msg);
    final var signature = SigningManager.INSTANCE.createSignature(msg);
    output.write(signature);
    output.write(SigningManager.INSTANCE.selfHash);
    output.write(nonce);
    return compressed;
  }

  public String getReplacementMsg() throws IOException {
    return new String(
      this.compressedMsg ? new ZstdCompressorInputStream(new ByteArrayInputStream(this.msg)).readAllBytes() : this.msg,
      StandardCharsets.UTF_8
    );
  }
}
