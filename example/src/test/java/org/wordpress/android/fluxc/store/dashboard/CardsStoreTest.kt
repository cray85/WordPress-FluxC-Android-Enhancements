package org.wordpress.android.fluxc.store.dashboard

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel.PostCardModel
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.CardsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsRestClient.PostsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsUtils
import org.wordpress.android.fluxc.persistence.dashboard.CardsDao
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.CardsErrorType
import org.wordpress.android.fluxc.store.dashboard.CardsStore.FetchedCardsPayload
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals
import kotlin.test.assertNull

/* POST */

const val POST_ID = 1
const val POST_TITLE = "title"
const val POST_CONTENT = "content"
const val POST_FEATURED_IMAGE = "featuredImage"
const val POST_DATE = "2021-12-27 11:33:55"

/* RESPONSE */

private val POST_RESPONSE = PostResponse(
        id = POST_ID,
        title = POST_TITLE,
        content = POST_CONTENT,
        featuredImage = POST_FEATURED_IMAGE,
        date = POST_DATE
)

private val POSTS_RESPONSE = PostsResponse(
        hasPublished = false,
        draft = listOf(POST_RESPONSE),
        scheduled = listOf(POST_RESPONSE)
)

private val CARDS_RESPONSE = CardsResponse(
        posts = POSTS_RESPONSE
)

/* MODEL */

private val POST_MODEL = PostCardModel(
        id = POST_ID,
        title = POST_TITLE,
        content = POST_CONTENT,
        featuredImage = POST_FEATURED_IMAGE,
        date = CardsUtils.fromDate(POST_DATE)
)

private val POSTS_MODEL = PostsCardModel(
        hasPublished = false,
        draft = listOf(POST_MODEL),
        scheduled = listOf(POST_MODEL)
)

private val CARDS_MODEL = listOf(
        POSTS_MODEL
)

@RunWith(MockitoJUnitRunner::class)
class CardsStoreTest {
    @Mock private lateinit var siteModel: SiteModel
    @Mock private lateinit var restClient: CardsRestClient
    @Mock private lateinit var dao: CardsDao

    private lateinit var cardsStore: CardsStore

    @Before
    fun setUp() {
        cardsStore = CardsStore(
                restClient,
                dao,
                initCoroutineEngine()
        )
    }

    @Test
    fun `given cards response, when fetch cards gets triggered, then null no error cards model is returned`() = test {
        val payload = FetchedCardsPayload(CARDS_RESPONSE)
        whenever(restClient.fetchCards(siteModel)).thenReturn(payload)

        val result = cardsStore.fetchCards(siteModel)

        assertThat(result.model).isNull()
        assertThat(result.error).isNull()
    }

    @Test
    fun `given card response with exception, when fetch cards gets triggered, then cards error is returned`() = test {
        val payload = FetchedCardsPayload(CARDS_RESPONSE)
        whenever(restClient.fetchCards(siteModel)).thenReturn(payload)
        whenever(dao.insertWithDate(siteModel.id, CARDS_MODEL)).thenThrow(IllegalStateException("Error"))

        val result = cardsStore.fetchCards(siteModel)

        assertThat(result.model).isNull()
        assertEquals(CardsErrorType.GENERIC_ERROR, result.error.type)
        assertNull(result.error.message)
    }

    @Test
    fun `given cards error, when fetch cards gets triggered, then cards error is returned`() = test {
        val errorType = CardsErrorType.API_ERROR
        val payload = FetchedCardsPayload<CardsResponse>(CardsError(errorType))
        whenever(restClient.fetchCards(siteModel)).thenReturn(payload)

        val result = cardsStore.fetchCards(siteModel)

        assertThat(result.model).isNull()
        assertEquals(errorType, result.error.type)
        assertNull(result.error.message)
    }

    @Test
    fun `given empty cards payload, when fetch cards gets triggered, then cards error is returned`() = test {
        val payload = FetchedCardsPayload<CardsResponse>()
        whenever(restClient.fetchCards(siteModel)).thenReturn(payload)

        val result = cardsStore.fetchCards(siteModel)

        assertThat(result.model).isNull()
        assertEquals(CardsErrorType.INVALID_RESPONSE, result.error.type)
        assertNull(result.error.message)
    }
}
