package ru.ifmo.pga.software.design.news.module.client

import com.xebialabs.restito.builder.stub.StubHttp
import com.xebialabs.restito.semantics.Action
import com.xebialabs.restito.semantics.Call
import com.xebialabs.restito.semantics.Condition
import com.xebialabs.restito.server.StubServer
import org.glassfish.grizzly.http.Method
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.ifmo.pga.software.design.news.module.client.exception.VkClientException
import java.util.*

class VkClientTest {
    private var stubServer: StubServer? = null

    @BeforeEach
    fun createStubServer() {
        stubServer = StubServer(PORT).run()
    }

    @AfterEach
    fun shutdownStubServer() {
        stubServer!!.stop()
        stubServer = null
    }

    @Test
    fun simplePingPong() {
        StubHttp.whenHttp(stubServer)
            .match(Condition.method(Method.GET), Condition.startsWithUri("/method/"))
            .then(Action.stringContent("OK"))
        val client: VkClient = TestVkClient("user.info", "abacaba")
        val result = client.fetch(mapOf())
        assertEquals("OK", result)
    }

    @Test
    fun params() {
        StubHttp.whenHttp(stubServer)
            .match(
                Condition.method(Method.GET),
                Condition.startsWithUri("/method/"),
                Condition.parameter("q", "query"),
                Condition.parameter("start_time", "1666547983"),
                Condition.parameter("end_time", "1666548000"),
                Condition.parameter("access_token", "abacaba"),
                Condition.parameter("v", "5.131"),
                Condition.custom { call: Call -> call.parameters.size == 5 }
            ).then(Action.stringContent("OK"))
        val client: VkClient = TestVkClient("newsfeed.search", "abacaba")
        val result = client.fetch(
            mapOf(
                "q" to "query",
                "start_time" to "1666547983",
                "end_time" to "1666548000"
            )
        )
        assertEquals("OK", result)
    }

    @Test
    fun httpNot200() {
        StubHttp.whenHttp(stubServer).match(Condition.alwaysFalse()).then()
        val client: VkClient = TestVkClient("user.info", "abacaba")

        assertThrows<VkClientException> {
            client.fetch(mapOf())
        }
    }

    @Test
    fun illegalUri() {
        val client: VkClient = TestVkClient("\\\\", "\\\\")
        assertThrows<RuntimeException> {
            client.fetch(mapOf())
        }
    }

    private class TestVkClient(method: String?, accessToken: String?) :
        VkClient(false, "localhost", PORT, method!!, accessToken!!)

    companion object {
        private val PORT = Random().nextInt(20000, 65535)
    }
}