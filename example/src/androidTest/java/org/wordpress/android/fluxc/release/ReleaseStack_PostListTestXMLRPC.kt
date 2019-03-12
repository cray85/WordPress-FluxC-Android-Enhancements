package org.wordpress.android.fluxc.release

import android.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.list.ListOrder
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForXmlRpcSite
import org.wordpress.android.fluxc.model.list.PostListOrderBy
import org.wordpress.android.fluxc.model.list.datastore.PostListDataStore
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.SCHEDULED
import org.wordpress.android.fluxc.model.post.PostStatus.TRASHED
import org.wordpress.android.fluxc.release.utils.ListStoreConnectedTestHelper
import org.wordpress.android.fluxc.release.utils.ListStoreConnectedTestMode
import org.wordpress.android.fluxc.release.utils.ListStoreConnectedTestMode.MultiplePages
import org.wordpress.android.fluxc.release.utils.ListStoreConnectedTestMode.SinglePage
import org.wordpress.android.fluxc.release.utils.TEST_LIST_CONFIG
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.DEFAULT_POST_STATUS_LIST
import javax.inject.Inject

internal class XmlRpcPostListTestCase(
    val statusList: List<PostStatus> = DEFAULT_POST_STATUS_LIST,
    val order: ListOrder = ListOrder.DESC,
    val orderBy: PostListOrderBy = PostListOrderBy.DATE,
    val testMode: ListStoreConnectedTestMode = SinglePage(false)
)

@RunWith(Parameterized::class)
internal class ReleaseStack_PostListTestXMLRPC(
    private val testCase: XmlRpcPostListTestCase
) : ReleaseStack_XMLRPCBase() {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Inject internal lateinit var listStore: ListStore
    @Inject internal lateinit var postStore: PostStore

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): List<XmlRpcPostListTestCase> = listOf(
                /*
                 These test cases are specifically picked to be non-demanding so that it can run without much setup.
                 They are very easy to extend on, so if we start running them on an account with pre-setup, they can
                 made to be a lot more demanding by ensuring that each post list type returns some data.
                 */
                XmlRpcPostListTestCase(testMode = MultiplePages),
                XmlRpcPostListTestCase(statusList = listOf(DRAFT)),
                XmlRpcPostListTestCase(statusList = listOf(SCHEDULED)),
                XmlRpcPostListTestCase(statusList = listOf(TRASHED)),
                XmlRpcPostListTestCase(order = ListOrder.ASC, testMode = MultiplePages),
                XmlRpcPostListTestCase(orderBy = PostListOrderBy.ID, testMode = MultiplePages)
        )
    }

    private val listStoreConnectedTestHelper by lazy {
        ListStoreConnectedTestHelper(listStore)
    }

    private val postListDataStore by lazy {
        PostListDataStore(mDispatcher, postStore, sSite)
    }

    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
    }

    @Test
    fun test() {
        listStoreConnectedTestHelper.runTest(testCase.testMode, this::createPagedListWrapper)
    }

    private fun createPagedListWrapper(): PagedListWrapper<PostModel> {
        val descriptor = PostListDescriptorForXmlRpcSite(
                site = sSite,
                statusList = testCase.statusList,
                order = testCase.order,
                orderBy = testCase.orderBy,
                config = TEST_LIST_CONFIG
        )
        return listStoreConnectedTestHelper.getList(descriptor, postListDataStore)
    }
}