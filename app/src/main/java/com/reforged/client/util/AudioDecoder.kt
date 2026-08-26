package com.reforged.client.util

import kotlin.math.abs

object AudioDecoder {

    private const val MAP = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMN0PQRSTUVWXYZO123456789+/="

    fun decode(url: String, userId: Long): String {
        if (url.isEmpty() || !url.contains("extra=")) return url

        val parts = url.split("?extra=")[1].split("#")
        val content = parts[0]
        val opsStr = if (parts.size > 1) parts[1] else ""
        val ops = opsStr.split("\u0001")

        var result = decodeBase(content)
        for (op in ops) {
            if (op.isEmpty()) continue
            val cmd = op[0]
            val arg = if (op.length > 1) op.substring(1) else ""
            result = when (cmd) {
                'v' -> result.reversed()
                'r' -> r(result, arg.toInt())
                's' -> s(result, arg.toInt())
                'i' -> i(result, arg.toInt(), userId)
                'x' -> x(result, arg)
                else -> result
            }
        }

        return if (result.startsWith("http")) result else url
    }

    private fun decodeBase(str: String): String {
        // Simple base64-like decode based on MAP
        // VK uses its own base64-like encoding for the content part
        return str // Simplified for now, usually it's just content
    }

    private fun r(str: String, arg: Int): String {
        val list = str.toCharArray()
        val mapLen = MAP.length
        for (i in list.indices) {
            val index = MAP.indexOf(list[i])
            if (index != -1) {
                var nextIndex = index - arg
                if (nextIndex < 0) nextIndex += mapLen
                list[i] = MAP[nextIndex]
            }
        }
        return String(list)
    }

    private fun s(str: String, arg: Int): String {
        val len = str.length
        if (len == 0) return str
        val res = str.toCharArray()
        val indexes = IntArray(len)
        var cur = arg
        for (i in len - 1 downTo 0) {
            cur = abs((cur * (i + 1)) % len)
            indexes[i] = cur
        }
        for (i in 1 until len) {
            val j = len - i - 1
            val tmp = res[i]
            res[i] = res[indexes[j]]
            res[indexes[j]] = tmp
        }
        return String(res)
    }

    private fun i(str: String, arg: Int, userId: Long): String {
        return x(str, (userId xor arg.toLong()).toString())
    }

    private fun x(str: String, arg: String): String {
        if (arg.isEmpty()) return str
        val xorVal = arg[0].code
        val res = str.toCharArray()
        for (i in res.indices) {
            res[i] = (res[i].code xor xorVal).toChar()
        }
        return String(res)
    }
}
