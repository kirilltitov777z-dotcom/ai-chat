package io.margy.aichat.util

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Dns
import org.json.JSONObject
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Умный DNS-резолвер:
 * 1) Пробует системный DNS, но берёт только IPv4 (многие телефоны без IPv6
 *    получают только AAAA-записи и падают с "Unable to resolve host").
 * 2) Если системный DNS не дал IPv4 — резолвит через DNS-over-HTTPS (DoH)
 *    напрямую по IP-адресу (1.1.1.1 / 8.8.8.8), минуя сломанный DNS.
 * 3) В крайнем случае — запасные IPv4 из списка.
 */
object Net {

    // Запасные IPv4 на случай, если DoH тоже недоступен
    private val FALLBACK_IPS = mapOf(
        "text.pollinations.ai" to listOf("104.18.7.216", "104.18.6.216"),
        "gen.pollinations.ai" to listOf("104.18.7.216", "104.18.6.216"),
        "pollinations.ai" to listOf("104.18.7.216", "104.18.6.216"),
        "api.groq.com" to listOf("2a06:98c1:310a::ac40:9514".let { "104.18.38.236" }, "172.64.149.20"),
        "openrouter.ai" to listOf("104.18.2.115", "104.18.3.115"),
        "api.deepseek.com" to listOf("3.173.21.63")
    )

    private val dohClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    /** Резолвит только IPv4 адреса через DoH (Cloudflare, затем Google) */
    private fun resolveDoh(host: String): List<InetAddress> {
        val urls = listOf(
            "https://1.1.1.1/dns-query?name=$host&type=A",
            "https://8.8.8.8/resolve?name=$host&type=A"
        )
        for (u in urls) {
            val ips = runCatching {
                val req = Request.Builder()
                    .url(u)
                    .header("accept", "application/dns-json")
                    .build()
                dohClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use emptyList<String>()
                    val body = resp.body?.string().orEmpty()
                    val arr = JSONObject(body).optJSONArray("Answer") ?: return@use emptyList<String>()
                    val out = ArrayList<String>(arr.length())
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val type = o.optInt("type", 0)
                        val data = o.optString("data", "")
                        // type 1 = A (IPv4)
                        if (type == 1 && data.isNotBlank()) out.add(data)
                    }
                    out
                }
            }.getOrDefault(emptyList())

            if (ips.isNotEmpty()) {
                return ips.mapNotNull { runCatching { InetAddress.getByName(it) }.getOrNull() }
            }
        }
        return emptyList()
    }

    /** Основной резолвер OkHttp */
    val dns: Dns = object : Dns {
        @Throws(java.net.UnknownHostException::class)
        override fun lookup(hostname: String): List<InetAddress> {
            // 1) Системный DNS — но только IPv4
            val system = runCatching { InetAddress.getAllByName(hostname) }.getOrDefault(emptyArray())
            val systemV4 = system.filter { it is java.net.Inet4Address }
            if (systemV4.isNotEmpty()) return systemV4

            // 2) DoH по raw IP (обходит сломанный системный DNS)
            val doh = resolveDoh(hostname)
            if (doh.isNotEmpty()) return doh

            // 3) Запасные IP
            val fb = FALLBACK_IPS[hostname]
                ?.mapNotNull { runCatching { InetAddress.getByName(it) }.getOrNull() }
            if (!fb.isNullOrEmpty()) return fb

            // 4) Хоть что-то из системного (даже IPv6)
            if (system.isNotEmpty()) return system.toList()

            throw java.net.UnknownHostException("Не удалось найти адрес: $hostname")
        }
    }

    /** Проверка связи с хостом (для теста в настройках) */
    fun isReachable(url: String): Boolean = runCatching {
        val req = Request.Builder().url(url.toHttpUrl()).build()
        dohClient.newCall(req).execute().use { true }
    }.getOrDefault(false)
}
