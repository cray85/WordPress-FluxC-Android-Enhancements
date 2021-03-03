package org.wordpress.android.fluxc.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.customer.WCCustomerMapper
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerApiResponse
import org.wordpress.android.fluxc.persistence.CustomerSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCCustomerStoreTest {
    val error = WooError(INVALID_RESPONSE, NETWORK_ERROR, "Invalid site ID")

    private val restClient: CustomerRestClient = mock()
    private val mapper: WCCustomerMapper = mock()

    private lateinit var store: WCCustomerStore

    @Before
    fun setUp() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(WCCustomerModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()

        store = WCCustomerStore(
                restClient,
                initCoroutineEngine(),
                mapper
        )
    }

    @Test
    fun `fetch single customer with success returns success`() = test {
        // given
        val siteModelId = 1
        val remoteCustomerId = 2L
        val siteModel = SiteModel().apply { id = siteModelId }

        val response: CustomerApiResponse = mock()
        whenever(restClient.fetchSingleCustomer(siteModel, remoteCustomerId))
                .thenReturn(WooPayload(response))
        val model: WCCustomerModel = mock()
        whenever(mapper.map(siteModel, response)).thenReturn(model)

        // when
        val result = store.fetchSingleCustomer(siteModel, remoteCustomerId)

        // then
        assertFalse(result.isError)
        assertEquals(model, result.model)
    }

    @Test
    fun `fetch single customer with error returns error`() = test {
        // given
        val siteModelId = 1
        val remoteCustomerId = 2L
        val siteModel = SiteModel().apply { id = siteModelId }

        whenever(restClient.fetchSingleCustomer(siteModel, remoteCustomerId)).thenReturn(WooPayload(error))

        // when
        val result = store.fetchSingleCustomer(siteModel, remoteCustomerId)

        // then
        assertTrue(result.isError)
        assertEquals(error, result.error)
    }

    @Test
    fun `fetch customers with success returns success and cache`() = test {
        // given
        val siteModelId = 1
        val siteModel = SiteModel().apply { id = siteModelId }

        val customerOne: CustomerApiResponse = mock()
        val customerTwo: CustomerApiResponse = mock()
        val response = arrayOf(customerOne, customerTwo)
        whenever(restClient.fetchCustomers(siteModel, 25))
                .thenReturn(WooPayload(response))
        val modelOne = WCCustomerModel().apply {
            remoteCustomerId = 1L
            localSiteId = siteModelId
        }
        val modelTwo = WCCustomerModel().apply {
            remoteCustomerId = 2L
            localSiteId = siteModelId
        }
        whenever(mapper.map(siteModel, customerOne)).thenReturn(modelOne)
        whenever(mapper.map(siteModel, customerTwo)).thenReturn(modelTwo)

        // when
        val result = store.fetchCustomers(siteModel, 25)

        // then
        assertFalse(result.isError)
        assertEquals(listOf(modelOne, modelTwo), result.model)
        assertEquals(modelOne, CustomerSqlUtils.getCustomersForSite(siteModel)[0])
        assertEquals(modelTwo, CustomerSqlUtils.getCustomersForSite(siteModel)[1])
    }

    @Test
    fun `fetch customers with and search success returns success and does not cache`() = test {
        // given
        val siteModelId = 1
        val siteModel = SiteModel().apply { id = siteModelId }
        val searchQuery = "searchQuery"

        val customerOne: CustomerApiResponse = mock()
        val customerTwo: CustomerApiResponse = mock()
        val response = arrayOf(customerOne, customerTwo)
        whenever(restClient.fetchCustomers(siteModel, 25, searchQuery = searchQuery))
                .thenReturn(WooPayload(response))
        val modelOne: WCCustomerModel = mock()
        val modelTwo: WCCustomerModel = mock()
        whenever(mapper.map(siteModel, customerOne)).thenReturn(modelOne)
        whenever(mapper.map(siteModel, customerTwo)).thenReturn(modelTwo)

        // when
        val result = store.fetchCustomers(siteModel, 25, searchQuery = searchQuery)

        // then
        assertFalse(result.isError)
        assertEquals(listOf(modelOne, modelTwo), result.model)
        assertTrue(CustomerSqlUtils.getCustomersForSite(siteModel).isEmpty())
    }

    @Test
    fun `fetch customers with and email success returns success and does not cache`() = test {
        // given
        val siteModelId = 1
        val siteModel = SiteModel().apply { id = siteModelId }
        val email = "email"

        val customerOne: CustomerApiResponse = mock()
        val customerTwo: CustomerApiResponse = mock()
        val response = arrayOf(customerOne, customerTwo)
        whenever(restClient.fetchCustomers(siteModel, 25, email = email))
                .thenReturn(WooPayload(response))
        val modelOne: WCCustomerModel = mock()
        val modelTwo: WCCustomerModel = mock()
        whenever(mapper.map(siteModel, customerOne)).thenReturn(modelOne)
        whenever(mapper.map(siteModel, customerTwo)).thenReturn(modelTwo)

        // when
        val result = store.fetchCustomers(siteModel, 25, email = email)

        // then
        assertFalse(result.isError)
        assertEquals(listOf(modelOne, modelTwo), result.model)
        assertTrue(CustomerSqlUtils.getCustomersForSite(siteModel).isEmpty())
    }

    @Test
    fun `fetch customers with and role success returns success and does not cache`() = test {
        // given
        val siteModelId = 1
        val siteModel = SiteModel().apply { id = siteModelId }
        val role = "role"

        val customerOne: CustomerApiResponse = mock()
        val customerTwo: CustomerApiResponse = mock()
        val response = arrayOf(customerOne, customerTwo)
        whenever(restClient.fetchCustomers(siteModel, 25, role = role))
                .thenReturn(WooPayload(response))
        val modelOne: WCCustomerModel = mock()
        val modelTwo: WCCustomerModel = mock()
        whenever(mapper.map(siteModel, customerOne)).thenReturn(modelOne)
        whenever(mapper.map(siteModel, customerTwo)).thenReturn(modelTwo)

        // when
        val result = store.fetchCustomers(siteModel, 25, role = role)

        // then
        assertFalse(result.isError)
        assertEquals(listOf(modelOne, modelTwo), result.model)
        assertTrue(CustomerSqlUtils.getCustomersForSite(siteModel).isEmpty())
    }

    @Test
    fun `fetch customers with and remote ids success returns success and does not cache`() = test {
        // given
        val siteModelId = 1
        val siteModel = SiteModel().apply { id = siteModelId }
        val remoteCustomerIds = listOf(1L)

        val customerOne: CustomerApiResponse = mock()
        val customerTwo: CustomerApiResponse = mock()
        val response = arrayOf(customerOne, customerTwo)
        whenever(restClient.fetchCustomers(siteModel, 25, remoteCustomerIds = remoteCustomerIds))
                .thenReturn(WooPayload(response))
        val modelOne: WCCustomerModel = mock()
        val modelTwo: WCCustomerModel = mock()
        whenever(mapper.map(siteModel, customerOne)).thenReturn(modelOne)
        whenever(mapper.map(siteModel, customerTwo)).thenReturn(modelTwo)

        // when
        val result = store.fetchCustomers(siteModel, 25, remoteCustomerIds = remoteCustomerIds)

        // then
        assertFalse(result.isError)
        assertEquals(listOf(modelOne, modelTwo), result.model)
        assertTrue(CustomerSqlUtils.getCustomersForSite(siteModel).isEmpty())
    }

    @Test
    fun `fetch customers with and excluded ids success returns success and does not cache`() = test {
        // given
        val siteModelId = 1
        val siteModel = SiteModel().apply { id = siteModelId }
        val excludedCustomerIds = listOf(1L)

        val customerOne: CustomerApiResponse = mock()
        val customerTwo: CustomerApiResponse = mock()
        val response = arrayOf(customerOne, customerTwo)
        whenever(restClient.fetchCustomers(siteModel, 25, excludedCustomerIds = excludedCustomerIds))
                .thenReturn(WooPayload(response))
        val modelOne: WCCustomerModel = mock()
        val modelTwo: WCCustomerModel = mock()
        whenever(mapper.map(siteModel, customerOne)).thenReturn(modelOne)
        whenever(mapper.map(siteModel, customerTwo)).thenReturn(modelTwo)

        // when
        val result = store.fetchCustomers(siteModel, 25, excludedCustomerIds = excludedCustomerIds)

        // then
        assertFalse(result.isError)
        assertEquals(listOf(modelOne, modelTwo), result.model)
        assertTrue(CustomerSqlUtils.getCustomersForSite(siteModel).isEmpty())
    }

    @Test
    fun `fetch customers with error returns error and not cache`() = test {
        // given
        val siteModelId = 1
        val siteModel = SiteModel().apply { id = siteModelId }

        whenever(restClient.fetchCustomers(siteModel, 25)).thenReturn(WooPayload(error))

        // when
        val result = store.fetchCustomers(siteModel, 25)

        // then
        assertTrue(result.isError)
        assertTrue(CustomerSqlUtils.getCustomersForSite(siteModel).isEmpty())
    }
}
