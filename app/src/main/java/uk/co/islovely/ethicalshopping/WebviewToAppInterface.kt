package uk.co.islovely.ethicalshopping

import android.content.Context
import android.webkit.JavascriptInterface
import androidx.navigation.fragment.findNavController

class WebViewToAppInterface (val context: Context) {

    @JavascriptInterface
    fun navigateToEthicalConsumer(url:String){
        println("executed from js")
        println(url)

        //TODO navigate using actions?  https://developer.android.com/guide/navigation/navigation-navigate
       // val currentFragment = supportFragmentManager.fragments.last()
       // val navController = context.findNavController()
       // navController.navigate(R.id.mySettingsFragment)
        

    }

}