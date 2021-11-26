package io.tokend.template.test

import io.tokend.template.features.urlconfig.model.UrlConfig
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.tokend.sdk.factory.JsonApiTools

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UrlConfigTest {
    @Test
    fun aDeserializeWithoutClient() {
        val client = "https://demo.tokend.io/"

        val source = "{\"api\":\"https://api.demo.tokend.io\"," +
                "\"storage\":\"https://s3.eu-west-1.amazonaws.com/demo-identity-storage-festive-colden\"," +
                "\"kyc\":\"${client}verification\",\"terms\":\"${client}terms\"}"

        val config = UrlConfig.fromQrJson(JsonApiTools.objectMapper.readTree(source))

        Assert.assertEquals(
            "Client URL must be obtained from legacy field",
            client, config.client
        )
    }

    @Test
    fun bDeserializeWithClient() {
        val client = "https://demo.tokend.io/"
        val terms = "${client}terms/"

        val source = "{\"api\":\"https://api.demo.tokend.io\"," +
                "\"storage\":\"https://s3.eu-west-1.amazonaws.com/demo-identity-storage-festive-colden\"," +
                "\"client\":\"$client\"}"

        val config = UrlConfig.fromQrJson(JsonApiTools.objectMapper.readTree(source))

        Assert.assertEquals(
            "Client URL must be obtained from JSON",
            client, config.client
        )
        Assert.assertEquals(
            "Terms URL must be created from client URL",
            terms, config.terms
        )
    }

    @Test
    fun cAddProtocol() {
        val source = "api.demo.tokend.io/"
        val protocolPrefix = "https://"

        val config = UrlConfig(source, source, source)

        Assert.assertTrue(
            "Protocol is required for API URL",
            config.api.startsWith(protocolPrefix)
        )
        Assert.assertTrue(
            "Protocol is required for storage URL",
            config.storage.startsWith(protocolPrefix)
        )
        Assert.assertTrue(
            "Protocol is required for client URL",
            config.client.startsWith(protocolPrefix)
        )
    }

    @Test
    fun dKeepProtocol() {
        val protocolPrefix = "http://"
        val source = "${protocolPrefix}api.demo.tokend.io/"

        val config = UrlConfig(source, source, source)

        Assert.assertTrue(
            "Protocol must be saved in API URL",
            config.api.startsWith(protocolPrefix)
        )
        Assert.assertTrue(
            "Protocol must be saved in storage URL",
            config.storage.startsWith(protocolPrefix)
        )
        Assert.assertTrue(
            "Protocol must be saved in client URL",
            config.client.startsWith(protocolPrefix)
        )
    }

    @Test
    fun eAddTrailingSlash() {
        val source = "https://api.demo.tokend.io"

        val config = UrlConfig(source, source, source)

        Assert.assertTrue(
            "Trailing slash is required for API URL",
            config.api.endsWith('/')
        )
        Assert.assertTrue(
            "Trailing slash is required for storage URL",
            config.storage.endsWith('/')
        )
        Assert.assertTrue(
            "Trailing slash is required for client URL",
            config.client.endsWith('/')
        )
    }

    @Test
    fun fKeepEverything() {
        val source = "http://api.demo.tokend.io/"

        val config = UrlConfig(source, source, source)

        Assert.assertEquals(
            "No modifications are required for the source API URL",
            source, config.api
        )
        Assert.assertEquals(
            "No modifications are required for the source storage URL",
            source, config.storage
        )
        Assert.assertEquals(
            "No modifications are required for the source client URL",
            source, config.client
        )
    }
}