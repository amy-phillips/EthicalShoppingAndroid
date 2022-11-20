package uk.co.islovely.ethicalshopping

import android.app.Activity
import android.content.Context
import android.webkit.JavascriptInterface
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController

class WebViewToAppInterface (private val context: Context) {

    @JavascriptInterface
    fun navigateToEthicalConsumer(url:String){
        // I assume I'm in the shop website fragment here, if not who knows what happened!
        // run this on main thread, or when you rotate the screen you'll get a stupid exception about the backstack state
        (context as Activity).runOnUiThread(java.lang.Runnable {
            val navController = Navigation.findNavController(context, R.id.nav_host_fragment_content_main)
            val action = ShopWebsiteFragmentDirections.actionShopWebsiteFragmentToEthicalConsumerFragment()
            action.url = url
            navController.navigate(action)
        })
    }
}