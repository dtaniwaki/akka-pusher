package com.github.dtaniwaki.akka_pusher

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.math.BigInteger
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac

object Utils {
  def byteArrayToString(data: Array[Byte]) = {
    val bigInteger = new BigInteger(1, data)
    var hash = bigInteger.toString(16)

    while (hash.length() < 32) {
      hash = "0" + hash
    }

    hash
  }

  def md5(string: String) = {
    val bytesOfMessage = string.getBytes("UTF-8")
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(bytesOfMessage)
    byteArrayToString(digest)
  }

  def sha256(secret: String, string: String) = {
    val signingKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256")

    val mac = Mac.getInstance("HmacSHA256")
    mac.init(signingKey)

    val digest = mac.doFinal(string.getBytes())

    val bigInteger = new BigInteger(1, digest)
    String.format("%0" + (digest.length << 1) + "x", bigInteger)
  }
}
