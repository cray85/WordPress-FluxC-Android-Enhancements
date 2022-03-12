package org.wordpress.android.fluxc.wc

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.yarolegovich.wellsql.WellSql
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.TestSiteSqlUtils
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSSRModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.system.WCSystemPluginResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.system.WCSystemPluginResponse.SystemPluginModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.system.WooSystemRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.system.WooSystemRestClient.SSRResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.system.WooSystemRestClient.WPSiteSettingsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.system.toDomainModel
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.site.SiteUtils
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WooCommerceStoreTest {
    private companion object {
        const val TEST_SITE_REMOTE_ID = 1337L
    }

    private val appContext = ApplicationProvider.getApplicationContext<Application>()
    private val restClient = mock<WooSystemRestClient>()
    private val siteStore = mock<SiteStore>()

    private val wooCommerceStore = WooCommerceStore(
            appContext = appContext,
            dispatcher = Dispatcher(),
            coroutineEngine = initCoroutineEngine(),
            siteStore = siteStore,
            systemRestClient = restClient,
            wcCoreRestClient = mock(),
            siteSqlUtils = TestSiteSqlUtils.siteSqlUtils
    )
    private val error = WooError(INVALID_RESPONSE, NETWORK_ERROR, "Invalid site ID")
    private val site = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_REMOTE_ID
    }

    private val response = WCSystemPluginResponse(
        listOf(
            SystemPluginModel(
                plugin = "woocommerce-services/woocommerce-services",
                name = "WooCommerce Shipping &amp; Tax",
                version = "1.0",
                url = "url"
            ),
            SystemPluginModel(
                plugin = "other-plugin/other-plugin",
                name = "Other Plugin",
                version = "2.0",
                url = "url"
            )
        ),
        listOf(
            SystemPluginModel(plugin = "inactive", name = "Inactive", version = "1.0", url = "url")
        )
    )

    private val sampleJsonObj = stringToJsonObject(
            UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/system-status.json")
    )

    private val ssrResponse = SSRResponse(
            environment = sampleJsonObj.get("environment"),
            database = sampleJsonObj.get("database"),
            activePlugins = sampleJsonObj.get("active_plugins"),
            theme = sampleJsonObj.get("theme"),
            settings = sampleJsonObj.get("settings"),
            security = sampleJsonObj.get("security"),
            pages = sampleJsonObj.get("pages")
    )

    private val ssrModel = WCSSRModel(
            TEST_SITE_REMOTE_ID,
            ssrResponse.environment?.toString(),
            ssrResponse.database?.toString(),
            ssrResponse.activePlugins?.toString(),
            ssrResponse.theme?.toString(),
            ssrResponse.settings?.toString(),
            ssrResponse.security?.toString(),
            ssrResponse.pages?.toString()
    )

    private fun stringToJsonObject(jsonText: String): JsonObject {
        return JsonParser().parse(jsonText).asJsonObject
    }

    @Before
    fun setUp() {
        val config = SingleStoreWellSqlConfigForTests(
            appContext,
            listOf(SitePluginModel::class.java, SiteModel::class.java),
            WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun testGetWooCommerceSites() {
        val nonWooSite = SiteModel().apply { siteId = 42 }
        WellSql.insert(nonWooSite).execute()

        assertEquals(0, wooCommerceStore.getWooCommerceSites().size)

        val wooJetpackSite = SiteModel().apply {
            siteId = 43
            hasWooCommerce = true
            setIsWPCom(false)
        }
        WellSql.insert(wooJetpackSite).execute()

        assertEquals(1, wooCommerceStore.getWooCommerceSites().size)

        val wooAtomicSite = SiteModel().apply {
            siteId = 44
            hasWooCommerce = true
            setIsWPCom(true)
        }
        WellSql.insert(wooAtomicSite).execute()

        assertEquals(2, wooCommerceStore.getWooCommerceSites().size)
    }

    @Test
    fun `when fetching plugin fails, then error returned`() = test {
        val result = getPlugin(isError = true)

        Assertions.assertThat(result.error).isEqualTo(error)
    }

    @Test
    fun `when fetching plugin succeeds, then success returned`() = test {
        val result = getPlugin(isError = false)

        Assertions.assertThat(result.isError).isFalse
        Assertions.assertThat(result.model).isNotNull
    }

    @Test
    fun `when fetching plugin succeeds, then plugins inserted into db`() = test {
        getPlugin(isError = false)
        val expectedModel = response.plugins.mapIndexed { index, model ->
            model.toDomainModel(site.id).apply { id = index + 1 }
        }

        val result = wooCommerceStore.getSitePlugins(site)

        Assertions.assertThat(result)
            .hasSameSizeAs(expectedModel)
            .allMatch { model ->
                expectedModel.any { model.id == it.id && model.name == it.name && model.isActive == it.isActive }
            }
    }

    @Test
    fun `when fetching ssr fails, then error returned`() = test {
        val result = fetchSSR(isError = true)

        Assertions.assertThat(result.error).isEqualTo(error)
    }

    @Test
    fun `when fetching ssr succeeds, then success returned`() {
        runBlocking {
            val result = fetchSSR(isError = false)

            Assertions.assertThat(result.isError).isFalse
            Assertions.assertThat(result.model).isNotNull
        }
    }

    @Test
    fun `when fetching a jetpack cp site, then fetch metadata from the remote site manually`() {
        runBlocking {
            val site = SiteUtils.generateJetpackCPSite()
            whenever(siteStore.fetchSites(any())).thenReturn(OnSiteChanged(1, updatedSites = listOf(site)))
            whenever(siteStore.sites).thenReturn(listOf(site))
            whenever(restClient.fetchSiteSettings(site)).thenReturn(
                    WooPayload(
                            WPSiteSettingsResponse(title = "new title")
                    )
            )
            whenever(restClient.checkIfWooCommerceIsAvailable(site)).thenReturn(WooPayload(true))

            val sites = wooCommerceStore.fetchWooCommerceSites().model!!

            verify(restClient).fetchSiteSettings(site)
            verify(restClient).checkIfWooCommerceIsAvailable(site)
            Assertions.assertThat(sites.first().hasWooCommerce).isTrue
            Assertions.assertThat(sites.first().name).isEqualTo("new title")
        }
    }

    private suspend fun getPlugin(isError: Boolean = false): WooResult<List<SitePluginModel>> {
        val payload = WooPayload(response)
        if (isError) {
            whenever(restClient.fetchInstalledPlugins(any())).thenReturn(WooPayload(error))
        } else {
            whenever(restClient.fetchInstalledPlugins(any())).thenReturn(payload)
        }
        return wooCommerceStore.fetchSitePlugins(site)
    }

    private suspend fun fetchSSR(isError: Boolean = false): WooResult<WCSSRModel> {
        val payload = WooPayload(ssrResponse)
        if (isError) {
            whenever(restClient.fetchSSR(any())).thenReturn(WooPayload(error))
        } else {
            whenever(restClient.fetchSSR(any())).thenReturn(payload)
        }
        return wooCommerceStore.fetchSSR(site)
    }
}
