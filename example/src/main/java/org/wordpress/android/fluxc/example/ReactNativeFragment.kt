package org.wordpress.android.fluxc.example

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_reactnative.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Error
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Success
import org.wordpress.android.fluxc.store.ReactNativeStore
import org.wordpress.android.util.AppLog
import javax.inject.Inject

class ReactNativeFragment : Fragment() {
    @Inject internal lateinit var reactNativeStore: ReactNativeStore

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_reactnative, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wp_api_request_button.setOnClickListener {
            makeCall(reactNativeStore::performWPAPIRequest, wp_api_url_field, "WP api")
        }
        wp_com_request_button.setOnClickListener {
            makeCall(reactNativeStore::performWPComRequest, wp_com_url_field, "WP.com")
        }
    }

    private fun makeCall(
        callFunction: suspend (path: String, params: Map<String, String>) -> ReactNativeFetchResponse,
        urlField: EditText,
        callType: String
    ) {
        try {
            val fullPath = urlField.text.toString()
            val (path, paramMap) = if (fullPath.contains("?")) {
                val (pathWithoutParams, rawParams) = fullPath.split("?")
                val params = rawParams.split("&").map {
                    val (key, value) = it.split("=")
                    key to value
                }.toMap()
                Pair(pathWithoutParams, params)
            } else {
                Pair(fullPath, emptyMap())
            }

            GlobalScope.launch(Dispatchers.Main) {
                val response = withContext(Dispatchers.IO) {
                    callFunction(path, paramMap)
                }

                when (response) {
                    is Success -> {
                        prependToLog("$callType call succeeded")
                        AppLog.i(AppLog.T.API, "$callType call result: ${response.result}")
                    }
                    is Error -> prependToLog("$callType call failed: ${response.error}")
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            prependToLog("Error parsing url")
        }
    }
}
