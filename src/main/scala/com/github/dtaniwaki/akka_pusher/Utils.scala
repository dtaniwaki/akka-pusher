package com.github.dtaniwaki.akka_pusher

import java.math.BigInteger
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Utils {
  val HEX = 16
  val LENGTH = 32

  def byteArrayToString(data: Array[Byte]): String = {
    val bigInteger = new BigInteger(1, data)
    var hash = bigInteger.toString(HEX)

    while (hash.length() < LENGTH) {
      hash = "0" + hash
    }

    hash
  }

  def md5(string: String): String = {
    val bytesOfMessage = string.getBytes("UTF-8")
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(bytesOfMessage)
    byteArrayToString(digest)
  }

  def sha256(secret: String, string: String): String = {
    val signingKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256")

    val mac = Mac.getInstance("HmacSHA256")
    mac.init(signingKey)

    val digest = mac.doFinal(string.getBytes("UTF-8"))

    val bigInteger = new BigInteger(1, digest)
    String.format("%0" + (digest.length << 1) + "x", bigInteger)
  }
}
