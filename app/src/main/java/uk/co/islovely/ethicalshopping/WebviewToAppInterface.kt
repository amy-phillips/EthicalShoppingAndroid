package uk.co.islovely.ethicalshopping

import android.app.Activity
import android.content.Context
import android.webkit.JavascriptInterface
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController

class WebViewToAppInterface (private val context: Context) {

    @JavascriptInterface
    fun navigateToEthicalConsumer(url:String){
        println("executed from js")
        println(url)

        // I assume I'm in the shop website fragment here, if not who knows what happened!
        //val fragment = (context as Activity).getSupportFragmentManager().findFragmentById(R.id.pageview)
        val navController = Navigation.findNavController(context as Activity, R.id.nav_host_fragment_content_main)
        val action = ShopWebsiteFragmentDirections.actionShopWebsiteFragmentToEthicalConsumerFragment()
        action.url = url
        navController.navigate(action)
    }

}