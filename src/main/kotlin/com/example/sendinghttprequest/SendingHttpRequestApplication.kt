package com.example.sendinghttprequest

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.server.ResponseStatusException

fun main(args: Array<String>) {
    runApplication<SendingHttpRequestApplication>(*args)
}

@SpringBootApplication
class SendingHttpRequestApplication

@RestController
class TweetController {

    companion object {
        val log = LoggerFactory.getLogger(TweetController::class.java)
    }

    @GetMapping("/slow-service-tweets")
    fun getAllTweets(): List<Tweet> {
        Thread.sleep(2000L)
        return listOf(
            Tweet("RestTemplate rules", "@user1"),
            Tweet("WebClient is better", "@user2"),
            Tweet("OK, both are useful", "@user1"),
        )
    }

    @GetMapping("/tweets-blocking")
    fun getTweetsBlocking(): List<Tweet> {
        log.info("Starting BLOCKING Controller!")
        val uri = getSlowServiceUri()
        val restTemplate = RestTemplate()
        val response =
            restTemplate.exchange(uri, HttpMethod.GET, null, object : ParameterizedTypeReference<List<Tweet>>() {})
        val result = response.body ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY)
        result.forEach { log.info(it.toString()) }
        log.info("Exiting BLOCKING Controller!");
        return result
    }

    @GetMapping("/tweets-non-blocking")
    suspend fun getTweetsNonBlocking(): Flow<Tweet> {
        log.info("Starting NON-BLOCKING Controller!")
        val tweetFlux = WebClient.create()
            .get()
            .uri(getSlowServiceUri())
            .retrieve()
            .bodyToFlow<Tweet>()
            .onEach { log.info(it.toString()) }

        log.info("Exiting NON-BLOCKING Controller!")
        return tweetFlux
    }

    private fun getSlowServiceUri() = "http://localhost:8080/slow-service-tweets"
}

data class Tweet(
    var text: String,
    var user: String,
)
