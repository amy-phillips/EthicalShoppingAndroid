package uk.co.islovely.ethicalshopping

import android.os.Build
import android.text.Html
import android.util.Log
import android.webkit.CookieManager
import java.net.URL
import javax.net.ssl.HttpsURLConnection

// ScoresRepository is consumed from other layers of the hierarchy.
object ScoresRepository {
    private const val LOGTAG = "ScoresRepository"

    private var foodSections = mutableListOf<FoodSection>()
    private var wasSubscribed = false
    // call this when we make progress grabbing score data
    private var progressCallback: ((Int, Boolean, List<FoodSection>) -> Unit) ?= null
    private var busy : Thread = Thread()

    init {
        println("ScoresRepository class invoked.")
    }

    // call this when entering a fragment that wants to know score info
    fun startGettingScores(_progressCallback: (percentage: Int, subscribed: Boolean, foodsections: List<FoodSection>) -> Unit)
    {
        progressCallback = _progressCallback
        // if not already getting scores kick off getting/refreshing scores
        if(!busy.isAlive) {
            busy = Thread {
                try {
                    getScoreTables()
                } catch (e: Exception) {
                    Log.e(LOGTAG, e.message!!)
                }
            }
            busy.start()
        }
    }
    // call this when leaving a fragment that wanted to know score info
    fun stopGettingScoreUpdates()
    {
        progressCallback=null
    }

    private fun unescape(string: String) : String
    {
        // return new DOMParser().parseFromString(string,'text/html').querySelector('html').textContent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // FROM_HTML_MODE_LEGACY is the behaviour that was used for versions below android N
            // we are using this flag to give a consistent behaviour
            val stringData = Html.fromHtml(string, Html.FROM_HTML_MODE_LEGACY).toString();
            return stringData
        } else {
            val stringData = Html.fromHtml(string).toString();
            return stringData
        }

        return ""
    }

    private fun fixupOverlyShortTitles(title: String) : String
    {
        if(title=="Crunchie") {
            return "Cadbury's Crunchie"
        } else if(title=="Double Decker") {
            return "Cadbury's Double Decker"
        } else if(title=="Eclairs") {
            return "Cadbury's Eclairs"
        } else if(title=="Flake") {
            return "Cadbury's Flake"
        } else if(title=="Fudge") {
            return "Cadbury's Fudge"
        } else if(title=="Heroes") {
            return "Cadbury's Heroes"
        } else if(title=="Picnic") {
            return "Cadbury's Picnic"
        } else if(title=="Time Out") {
            return "Cadbury's Timeout"
        } else if(title=="Twirl") {
            return "Cadbury's Twirl"
        } else if(title=="Wispa") {
            return "Cadbury's Wispa"
        } else if(title=="Fab") {
            return "Nestle Fab"
        } else if(title=="Zingers") {
            return "James White Zingers"
        }

        if(title.indexOf(' ')<0) {
            Log.d(LOGTAG, "Short title: "+title)
        }
        return title
    }

    private fun addAnyNewFoodSection(new_food : FoodSection) {
        for (fs in foodSections) {
            if (new_food.location == fs.location) {
                // already in, no need to add
                return
            }
        }
        // no match, add it
        foodSections.add(new_food)
    }

    private fun addAnyNewFoodSections(new_foods : MutableList<FoodSection>) {
        // we match by name to check if a food already exists in our list
        for(new_food in new_foods) {
            addAnyNewFoodSection(new_food)
        }
    }

    private fun getScoreTables() {
        progressCallback?.invoke(0, false, emptyList())

        val ecUrl = URL("https://www.ethicalconsumer.org/")
        val ecUrlConnection = ecUrl.openConnection() as HttpsURLConnection
        // if the player has signed in in the EC webview, this will have set a cookie,
        // grab the cookies, so we will get the signed in version of the website
        val ecCookies = CookieManager.getInstance().getCookie("https://www.ethicalconsumer.org/")
        if (ecCookies != null) {
            ecUrlConnection.setRequestProperty("Cookie", ecCookies)
        }

        try {
            val ecData = ecUrlConnection.inputStream.bufferedReader().readText()
            ecUrlConnection.inputStream.close()
            ecUrlConnection.disconnect()
            // is there a call to action to subscribe?
            val sub = Regex("<button[^>]*?value=\"Sign in \">")
            val am_subscribed = !sub.containsMatchIn(ecData)
            // let UI know if we're subscribed/logged in
            progressCallback?.invoke(1, am_subscribed, emptyList())

            // parse out product guides - food and drink
            addAnyNewFoodSections(parseProductGuides("<a class=\"more\" href=\"/food-drink\">Food &amp; Drink guides, news and features</a>",ecData))
            //health and beauty
            addAnyNewFoodSections(parseProductGuides("<a class=\"more\" href=\"/health-beauty\">Health &amp; Beauty guides, news and features</a>",ecData))

            // some more products that are stocked by supermarkets - don;t want all of home and garden tho
            addAnyNewFoodSection(FoodSection("/home-garden/shopping-guide/dishwasher-detergent"))
            addAnyNewFoodSection(FoodSection("/home-garden/shopping-guide/household-cleaners"))
            addAnyNewFoodSection(FoodSection("/home-garden/shopping-guide/laundry-detergents"))
            addAnyNewFoodSection(FoodSection("/home-garden/shopping-guide/toilet-cleaners"))
            addAnyNewFoodSection(FoodSection("/home-garden/shopping-guide/toilet-paper"))
            addAnyNewFoodSection(FoodSection("/home-garden/shopping-guide/washing-liquid"))

            // strip out perfume shops because it has short names and doesn't help
            foodSections.remove(FoodSection("/health-beauty/shopping-guide/perfume-shops"))

            // are there any cached entries that we want to throw away?
            if(am_subscribed!=wasSubscribed) {
                for (fs in foodSections) {
                    Log.d(LOGTAG,"Will rerequest $fs because subscription check change")
                    fs.last_success=0
                    continue
                }
            }
            wasSubscribed=am_subscribed

            // request all the pages - eventually parallelise this
            for (fs in foodSections) {
                val index = foodSections.indexOf(fs)
                val progress: Float = 100.0f*index.toFloat()/foodSections.size.toFloat()
                Log.d(LOGTAG, "Progress $index / ${foodSections.size}")
                progressCallback?.invoke(progress.toInt(), am_subscribed, foodSections)
                val REFRESH_MILLIS : Long = 5*60*1000
                if(fs.last_success != 0L && System.currentTimeMillis() - fs.last_success < REFRESH_MILLIS)
                {
                    Log.d(LOGTAG,"Skipping food $fs because ${fs.last_success}")
                    continue
                }

                // no prev results, or prev results are old, so nuke any possible prev results
                fs.good_foods.clear()
                fs.average_foods.clear()
                fs.bad_foods.clear()

                val url = URL("https://www.ethicalconsumer.org" + fs.location)
                val urlConnection = url.openConnection() as HttpsURLConnection
                val cookies =
                    CookieManager.getInstance().getCookie("https://www.ethicalconsumer.org/")
                if (cookies != null) {
                    urlConnection.setRequestProperty("Cookie", cookies)
                }

                val data = urlConnection.inputStream.bufferedReader().readText()
                urlConnection.inputStream.close()
                urlConnection.disconnect()
                val titleregex = Regex("<h1 class=\"title\">\\s*([\\w\\s&;,-]+?)[\\s]*<")
                val titlematch = titleregex.find(data)
                fs.title = unescape(titlematch?.groupValues?.get(1) ?: "error")

                // parse the score table
                val tableregex = Regex("<table class=\"table\"(.*?)<\\/table>", RegexOption.DOT_MATCHES_ALL) //gms.exec(data);
                val tablematch = tableregex.find(data)
                val tabledata = tablematch?.groupValues?.get(1)
                val tableentryregex = Regex(
                    "<h4>([^<]*)<\\/h4>(?:.*?)?<div class=\"score (\\w+)\">",
                    RegexOption.DOT_MATCHES_ALL
                ) //gms;
                if(tabledata != null) {
                    var tableentry = tableentryregex.find(tabledata)
                    while (tableentry != null)
                    {
                        try {
                            val rating=Rating.valueOf(tableentry.groupValues.get(2).uppercase())
                            val title=fixupOverlyShortTitles(unescape(tableentry.groupValues.get(1))) // flake matches too many things - hack it!
                            var food_list: MutableList<Food>? = null
                            if(rating==Rating.GOOD) {
                                food_list = fs.good_foods
                            } else if(rating==Rating.AVERAGE) {
                                food_list = fs.average_foods
                            } else if(rating==Rating.BAD) {
                                food_list = fs.bad_foods
                            } else {
                                Log.e(LOGTAG, "Unexpected rating $rating")
                                continue
                            }

                            food_list.add(Food(title, rating, "https://www.ethicalconsumer.org${fs.location}#score-table"))
                        } catch (e: Exception) {
                            Log.d(LOGTAG, "Failed to parse rating $e")
                        }

                        tableentry = tableentry.next()
                    }
                }
                if(fs.good_foods.size > 0 || fs.average_foods.size > 0 || fs.bad_foods.size > 0) {
                    fs.last_success = System.currentTimeMillis()
                }
            }
            Log.d(LOGTAG, "Progress DONE")
            progressCallback?.invoke(100, am_subscribed, foodSections)
        } catch (e: Exception) {
            Log.d(LOGTAG, "Failed to parse foods $e")
        }
    }

    private fun parseProductGuides(product_selector: String, page_html: String) : MutableList<FoodSection> {
        var foods=mutableListOf<FoodSection>()

        val list_selection_regex = Regex(product_selector+".*?<h4>Product Guides</h4>.*?<ul>(.*?)</ul>",setOf(
            RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL
        ))
        val product_list_result = list_selection_regex.find(page_html)

        // then split into each entry (can I do that in regex above? - can't figure it out so KISS)
        val li_regex = Regex("<a href=\"([^\"]+)",RegexOption.MULTILINE)
        val product_matches = li_regex.findAll(product_list_result?.value.orEmpty())
        for(product in product_matches) {
            foods.add(FoodSection(product.groupValues.get(1)))
        }

        return foods
    }
}