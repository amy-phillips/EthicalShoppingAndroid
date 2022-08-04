package uk.co.islovely.ethicalshopping

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import uk.co.islovely.ethicalshopping.databinding.FragmentSecondBinding
import java.io.BufferedReader
import java.io.InputStream


/**
 * This fragment displays the website for eg Tesco/Sainsburys, and then pokes in the javascript to highlight it
 */
class ShopWebsiteFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var foodSections : List<FoodSection> = emptyList()
    private var amSubscribed : Boolean = false
    private var pageLoaded : Boolean = false
    private val LOGTAG = "ShopWebsiteFragment"
    private var website_url = "https://www.tesco.com/groceries/en-GB/products/256174499"
    private var js_resource = R.raw.tesco
    private val args: ShopWebsiteFragmentArgs by navArgs()

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)

        val website = args.website
        if (website == "tesco") {
            website_url = "https://www.tesco.com/groceries"
            js_resource = R.raw.tesco
        } else if(website == "sainsburys") {
            website_url = "https://www.sainsburys.co.uk/shop/gb/groceries"
            js_resource = R.raw.sainsburys
        } else if(website == "asda") {
            website_url = "https://groceries.asda.com"
            js_resource = R.raw.asda
        } else if(website == "boots") {
            website_url = "https://www.boots.com"
            js_resource = R.raw.boots
        } else if(website == "milkandmore") {
            website_url = "https://www.milkandmore.co.uk"
            js_resource = R.raw.milkandmore
        } else {
            assert(false)
        }

        return binding.root

    }

    private fun dumpFoodsToJson(foods : List<Food>, url: String): String {
        var responseJson = ""
        for ((index, food) in foods.withIndex()) {
            if(index>0) {
                responseJson += ","
            }
            responseJson += "{"
            responseJson += "\"title\":\"${food.title}\","
            responseJson += "\"link\":\"https://www.ethicalconsumer.org${url}#score-table\""
            responseJson += "}\n"
        }
        return responseJson
    }

    private fun generateJS() : String
    {
        // generate the get_score_tables function
        // response={"scores":gScoreTables,"subscription":subscription_to_send};
        /* "scores": {
        "/food-drink/shopping-guide/baked-beans": {
            "table": {
                "good": [
                    {
                        "title": "Mr Organic baked beans [O]",
                        "preprocessed_title": {
                            "name": "Mr Organic baked beans [O]",
                            "one_of": [],
                            "all_of": [
                                "mr",
                                "organic",
                                "baked",
                                "bean"
                            ]
                        },
                        "link": "https://www.ethicalconsumer.org/food-drink/shopping-guide/baked-beans#score-table"
                    },
                    */
        var responseJson = "{\"scores\":{"

        for ((index, section) in foodSections.withIndex()) {
            if(index>0) {
                responseJson += ","
            }
            responseJson += "\"${section.location}\":{"
            responseJson += "\"title\":\"${section.title}\","
            responseJson += "\"table\":{"
            responseJson += "\"good\":[\n"
            responseJson += dumpFoodsToJson(section.good_foods,section.location)
            responseJson += "],"
            responseJson += "\"average\":[\n"
            responseJson += dumpFoodsToJson(section.average_foods,section.location)
            responseJson += "],"
            responseJson += "\"bad\":[\n"
            responseJson += dumpFoodsToJson(section.bad_foods,section.location)
            responseJson += "]\n"
            responseJson += "}\n"
            responseJson += "}\n"
        }
        responseJson += "  }\n,\"subscription\":$amSubscribed }"

        // TODO pre_process_food only once on page load rather than every 30s in get_score_tables

        val getScores =
            """
function get_score_tables() {
    console.log("get_score_tables");
    const response_json = `
    $responseJson
    `
    console.log("made json string");
    const response = JSON.parse(response_json);
    console.log("parsed response json");
    // we didn't do the pre_process/pre_process_food in kotlin, so do that now we have access to the js
    for (const [url, foods] of Object.entries(response.scores)) {
        processed_food = pre_process_food(foods.title);
        for (let food of foods.table.good) {
            food['preprocessed_title']=pre_process(food.title,processed_food);
        }
        for (let food of foods.table.average) {
            food['preprocessed_title']=pre_process(food.title,processed_food);
        }
        for (let food of foods.table.bad) {
            food['preprocessed_title']=pre_process(food.title,processed_food);
        }
    }
    colour_page(response);
    return;
}
"""

        // read in the common js and the tesco/sainsburys/ocado/... js, blat them together, add the ratings info
        val inputStream: InputStream = resources.openRawResource(R.raw.common)
        val common = inputStream.bufferedReader().use(BufferedReader::readText)
        val websiteStream: InputStream = resources.openRawResource(js_resource)
        val website = websiteStream.bufferedReader().use(BufferedReader::readText)
        val matchyStream: InputStream = resources.openRawResource(R.raw.matchymcmatchypants)
        val matchy = matchyStream.bufferedReader().use(BufferedReader::readText)

        //println(matchy + "\n" + website + "\n" + common + "\n" + getScores)

        return matchy + "\n" + website + "\n" + common + "\n" + getScores
    }

    // call this when the page loads, and also when we get progress on the foodsections,
    // when both are ready it will inject js into the webview
    private fun injectJsIfReady() {
        if(foodSections.isEmpty()) {
            Log.d(LOGTAG, "No food sections so can't inject JS")
            return
        }

        if(!pageLoaded) {
            Log.d(LOGTAG, "Page not loaded so can't inject JS")
            return
        }

        val js = generateJS()
        activity?.runOnUiThread(java.lang.Runnable {
            binding.webview.evaluateJavascript(
                js,
                null)
        })
    }

    private fun scoresProgressCallback(progress: Int, subscribed: Boolean, foodsections: List<FoodSection>) {
        amSubscribed = subscribed
        if(progress == 100) {
            // use the data!
            foodSections = foodsections
            injectJsIfReady()

            // TODO progress bar update code should not be duplicated
            activity?.runOnUiThread(java.lang.Runnable {
                try {
                    val progressLayout: LinearLayout = requireActivity().findViewById(R.id.downloadprogress_layout)
                    progressLayout.visibility = View.INVISIBLE
                } catch (e: IllegalStateException) {
                    // we probaly got cleaned up
                }
            })
        } else {
            activity?.runOnUiThread(java.lang.Runnable {
                try {
                    val progressLayout: LinearLayout =
                        requireActivity().findViewById(R.id.downloadprogress_layout)
                    progressLayout.visibility = View.VISIBLE
                    val progressBar: ProgressBar =
                        requireActivity().findViewById(R.id.downloadprogress_bar)
                    progressBar.progress = progress
                } catch (e: IllegalStateException) {
                    // we probaly got cleaned up
                }
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(
            true // default to enabled
        ) {
            override fun handleOnBackPressed() {
                if (binding.webview.canGoBack()) {
                    binding.webview.goBack()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,  // LifecycleOwner
            callback
        )

        // kick off getting score tables
        ScoresRepository.startGettingScores(::scoresProgressCallback)

        (activity as AppCompatActivity?)!!.supportActionBar!!.setTitle("Home")

        //binding.buttonSecond.setOnClickListener {
        //    findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        //}
        binding.webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null) {
                    pageLoaded = false
                    binding.webview.loadUrl(url)
                }
                return false
            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest
            ): Boolean {
                pageLoaded = false
                binding.webview.loadUrl(request.url.toString())
                return false
            }

        }
        binding.webview.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                (activity as AppCompatActivity?)!!.supportActionBar!!.setTitle(title)
            }

            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                Log.d(LOGTAG, "${message.message()} -- From line " +
                        "${message.lineNumber()} of ${message.sourceId()}")
                return true
            }

            override fun onProgressChanged (view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                Log.d(LOGTAG, "Page loading "+newProgress)
                if(newProgress != 100 || pageLoaded) {
                    return
                }
                pageLoaded = true

                // Page loading finished
                // Display the loaded page title in a toast message
                Log.d(LOGTAG,"Page loaded: ${view.title}")

                // Enable disable back forward button
                //button_back.isEnabled = web_view.canGoBack()
                //button_forward.isEnabled = web_view.canGoForward()

                injectJsIfReady()
            }
        }

        binding.webview.settings.setJavaScriptEnabled(true)
        binding.webview.settings.setSupportMultipleWindows(true) // This forces ChromeClient enabled.
        binding.webview.settings.setDomStorageEnabled(true)
        pageLoaded = false
        binding.webview.loadUrl(website_url)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        pageLoaded = false
        foodSections = emptyList()
    }
}