package uk.co.islovely.ethicalshopping

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.fragment.app.Fragment
import uk.co.islovely.ethicalshopping.databinding.FragmentSecondBinding
import java.io.BufferedReader
import java.io.InputStream

//TODO fix bug where when you let it load all the foods, then click tesco it loads them again

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class TescoFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var foodSections : List<FoodSection> = emptyList()
    private var amSubscribed : Boolean = false
    private var pageLoaded : Boolean = false
    private var LOG_TAG = "TescoFragment"
    private val tesco_url = "https://www.tesco.com/groceries/en-GB/products/256174499"

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    private fun dump_foods_to_json(foods : List<Food>, url: String): String {
        var response_json = ""
        for ((index, food) in foods.withIndex()) {
            if(index>0) {
                response_json += ","
            }
            response_json += "{"
            response_json += "\"title\":\"${food.title}\","
            response_json += "\"link\":\"https://www.ethicalconsumer.org${url}#score-table\""
            response_json += "}\n"
        }
        return response_json
    }

    private fun generate_js() : String
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
        var response_json = "{\"scores\":{"

        for ((index, section) in foodSections.withIndex()) {
            if(index>0) {
                response_json += ","
            }
            response_json += "\"${section.location}\":{"
            response_json += "\"title\":\"${section.title}\","
            response_json += "\"table\":{"
            response_json += "\"good\":[\n"
            response_json += dump_foods_to_json(section.good_foods,section.location)
            response_json += "],"
            response_json += "\"average\":[\n"
            response_json += dump_foods_to_json(section.average_foods,section.location)
            response_json += "],"
            response_json += "\"bad\":[\n"
            response_json += dump_foods_to_json(section.bad_foods,section.location)
            response_json += "]\n"
            response_json += "}\n"
            response_json += "}\n"
        }
        response_json += "  }\n,\"subscription\":$amSubscribed }"

        // TODO pre_process_food only once on page load rather than every 30s in get_score_tables
        val get_scores =
            """
function get_score_tables() {
    console.log("AMY getting scores");
    const response_json = `
    $response_json
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
    display_call_to_login_if_necessary(response);
    colour_page(response);
    return;
}
console.log("Amy rules");
"""

        // read in the common js and the tesco js, blat them together, add the ratings info
        val in_s: InputStream = resources.openRawResource(R.raw.common)
        val common = in_s.bufferedReader().use(BufferedReader::readText)
        val tesco_s: InputStream = resources.openRawResource(R.raw.tesco)
        val tesco = tesco_s.bufferedReader().use(BufferedReader::readText)
        val matchy_s: InputStream = resources.openRawResource(R.raw.matchymcmatchypants)
        val matchy = matchy_s.bufferedReader().use(BufferedReader::readText)

        println(matchy + "\n" + tesco + "\n" + common + "\n" + get_scores)

        return matchy + "\n" + tesco + "\n" + common + "\n" + get_scores
    }

    // call this when the page loads, and also when we get progress on the foodsections,
    // when both are ready it will inject js into the webview
    private fun injectJsIfReady() {
        if(foodSections.isEmpty()) {
            Log.d(LOG_TAG, "No food sections so can't inject JS")
            return
        }

        if(!pageLoaded) {
            Log.d(LOG_TAG, "Page not loaded so can't inject JS")
            return
        }

        val js = generate_js()
        activity?.runOnUiThread(java.lang.Runnable {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                binding.webview.evaluateJavascript(
                    js,
                    null)
            } else {
                binding.webview.loadUrl("javascript:"+js)
            }
        })
    }

    private fun scoresProgressCallback(progress: Int, subscribed: Boolean, foodsections: List<FoodSection>) {
        amSubscribed = subscribed;
        if(progress == 100) {
            // use the data!
            foodSections = foodsections

            injectJsIfReady()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // kick off getting score tables
        ScoresRepository.startGettingScores(::scoresProgressCallback)

        //binding.buttonSecond.setOnClickListener {
        //    findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        //}

        binding.webview.getSettings().setJavaScriptEnabled(true);
        binding.webview.loadUrl(tesco_url)

        binding.webview.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                Log.d(LOG_TAG, "${message.message()} -- From line " +
                        "${message.lineNumber()} of ${message.sourceId()}")
                return true
            }

            override fun onProgressChanged (view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                Log.d(LOG_TAG, "Page loading "+newProgress)
                if(newProgress != 100 || pageLoaded) {
                    return
                }
                pageLoaded = true

                // Page loading finished
                // Display the loaded page title in a toast message
                Log.d(LOG_TAG,"Page loaded: ${view.title}")

                // Enable disable back forward button
                //button_back.isEnabled = web_view.canGoBack()
                //button_forward.isEnabled = web_view.canGoForward()

                injectJsIfReady()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        pageLoaded = false
        foodSections = emptyList()
    }
}