package uk.co.islovely.ethicalshopping

import android.os.Build
import android.text.Html
import android.util.Log
import android.webkit.CookieManager
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
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

    private val enableEthicalConsumer = true
    private val enableGoodShoppingGuide = true

    private val REFRESH_MILLIS : Long = 5*60*1000

    // call this when entering a fragment that wants to know score info
    fun startGettingScores(_progressCallback: (percentage: Int, subscribed: Boolean, foodsections: List<FoodSection>) -> Unit)
    {
        progressCallback = _progressCallback
        // if not already getting scores kick off getting/refreshing scores
        if(!busy.isAlive) {
            val STACK_SIZE :Long=3000000
            val group = ThreadGroup("getScoresGroup")
            busy = Thread(group, Runnable {
                try {
                    getScoreTables()
                } catch (e: Exception) {
                    Log.e(LOGTAG, e.message!!)
                }
            }, "getScoresThread",STACK_SIZE)

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

    private fun getEthicalConsumerFoodSections() {
        try {
            val ecUrl = URL("https://www.ethicalconsumer.org/")
            val ecUrlConnection = ecUrl.openConnection() as HttpsURLConnection
            // if the player has signed in in the EC webview, this will have set a cookie,
            // grab the cookies, so we will get the signed in version of the website
            val ecCookies = CookieManager.getInstance().getCookie(ecUrl.toString())
            if (ecCookies != null) {
                ecUrlConnection.setRequestProperty("Cookie", ecCookies)
            }

            val ecData: String = ecUrlConnection.inputStream.bufferedReader().readText()
            ecUrlConnection.inputStream.close()
            ecUrlConnection.disconnect()

            val doc: Document = Jsoup.parse(ecData)
            // is there a call to action to subscribe?
            val sign_in_button: Elements = doc.select("[value=\"Sign in \"]")
            val am_subscribed = sign_in_button.size == 0
            // let UI know if we're subscribed/logged in
            progressCallback?.invoke(1, am_subscribed, emptyList())

            // parse out product guides - food and drink
            val food_matches: Elements = doc.select("[href^=\"/food-drink/\"]")
            for(product in food_matches) {
                addAnyNewFoodSection(FoodSection(ecUrl.toString() + product.attr("href")))
            }

            //health and beauty
            val health_matches: Elements = doc.select("[href^=\"/health-beauty/\"]")
            for(product in health_matches) {
                addAnyNewFoodSection(FoodSection(ecUrl.toString() + product.attr("href")))
            }

            // some more products that are stocked by supermarkets - don;t want all of home and garden tho
            addAnyNewFoodSection(FoodSection(ecUrl.toString() + "/home-garden/shopping-guide/dishwasher-detergent"))
            addAnyNewFoodSection(FoodSection(ecUrl.toString() + "/home-garden/shopping-guide/household-cleaners"))
            addAnyNewFoodSection(FoodSection(ecUrl.toString() + "/home-garden/shopping-guide/laundry-detergents"))
            addAnyNewFoodSection(FoodSection(ecUrl.toString() + "/home-garden/shopping-guide/toilet-cleaners"))
            addAnyNewFoodSection(FoodSection(ecUrl.toString() + "/home-garden/shopping-guide/toilet-paper"))
            addAnyNewFoodSection(FoodSection(ecUrl.toString() + "/home-garden/shopping-guide/washing-liquid"))

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
        } catch (e: Exception) {
            Log.d(LOGTAG, "Failed to parse foods $e")
        }
    }

    private fun getGoodShoppingFoodSections() {
        try {
            val ecUrl = URL("https://thegoodshoppingguide.com/")
            val ecUrlConnection = ecUrl.openConnection() as HttpsURLConnection
            val ecData: String = ecUrlConnection.inputStream.bufferedReader().readText()
            ecUrlConnection.inputStream.close()
            ecUrlConnection.disconnect()

            val doc = Jsoup.parse(ecData)

            // parse out product guides - for now ALL OF THEM
            // TODO fewer?
            val all_matches: Elements = doc.select("[href^=\"https://thegoodshoppingguide.com/subject/\"]")
            for(product in all_matches) {
                addAnyNewFoodSection(FoodSection(product.attr("href")))
            }
        } catch (e: Exception) {
            Log.d(LOGTAG, "Failed to parse foods $e")
        }
    }

    private fun parseGoodShoppingEntries(doc:org.jsoup.nodes.Element, rating:Rating, fs: FoodSection) {
        var cssSelect:String=".high"
        var food_list: MutableList<Food>? = null
        if(rating==Rating.GOOD) {
            cssSelect=".bg-brand_row-good"
            food_list = fs.good_foods
        } else if(rating==Rating.BAD) {
            cssSelect=".bg-brand_row-poor"
            food_list = fs.bad_foods
        }
        val entries: Elements = doc.select(cssSelect)
        for (entry in entries) {
            val title_entry = entry.select("h3")
            val title =
                fixupOverlyShortTitles(unescape(title_entry.text())) // flake matches too many things - hack it
            food_list?.add(
                Food(
                    title,
                    rating,
                    fs.location
                )
            )
        }
    }

    private fun downloadScoreTables() {
        try {
            // request all the pages - eventually parallelise this
            for (fs in foodSections) {
                val index = foodSections.indexOf(fs)
                val progress: Float = 100.0f*index.toFloat()/foodSections.size.toFloat()
                Log.d(LOGTAG, "Progress $index / ${foodSections.size}")
                progressCallback?.invoke(progress.toInt(), wasSubscribed, foodSections)
                if(fs.last_success != 0L && System.currentTimeMillis() - fs.last_success < REFRESH_MILLIS)
                {
                    Log.d(LOGTAG,"Skipping food $fs because ${fs.last_success}")
                    continue
                }

                // no prev results, or prev results are old, so nuke any possible prev results
                fs.good_foods.clear()
                fs.average_foods.clear()
                fs.bad_foods.clear()

                val url = URL(fs.location)
                val cookie =
                    CookieManager.getInstance().getCookie(url.host)
                var doc : Document
                if(cookie == null) {
                    doc = Jsoup.connect(fs.location).get()
                } else {
                    doc = Jsoup.connect(fs.location)
                        .cookie(cookie,cookie).get()
                }

                if(url.host == "www.ethicalconsumer.org") {
                    fs.title = doc.title().replace("| Ethical Consumer","")

                    // parse the score table

                    // skip to the table
                    val table = doc.select("div .table-responsive")
                    val table_entries: Elements = table.select("tr[data-category-scores]")
                    for(entry in table_entries) {
                        val title_entry = entry.select("h4")
                        val title=fixupOverlyShortTitles(unescape(title_entry.text())) // flake matches too many things - hack it
                        if(entry.select(".score.good").size > 0) {
                            fs.good_foods.add(Food(title, Rating.GOOD, "https://www.ethicalconsumer.org${fs.location}#score-table"))
                        } else if(entry.select(".score.average").size > 0) {
                            fs.average_foods.add(Food(title, Rating.AVERAGE, "https://www.ethicalconsumer.org${fs.location}#score-table"))
                        } else if(entry.select(".score.bad").size > 0) {
                            fs.bad_foods.add(Food(title, Rating.BAD, "https://www.ethicalconsumer.org${fs.location}#score-table"))
                        }
                    }
                } else if(url.host == "thegoodshoppingguide.com") {
                    fs.title = doc.title().replace("- The Good Shopping Guide","")

                    // parse the score table
                    val tables = doc.select("#rating__rows")
                    for(table in tables) {
                        parseGoodShoppingEntries(table,Rating.GOOD, fs)
                        parseGoodShoppingEntries(table,Rating.BAD, fs)
                    }
                }

                if(fs.good_foods.size > 0 || fs.average_foods.size > 0 || fs.bad_foods.size > 0) {
                    fs.last_success = System.currentTimeMillis()
                }
            }
            Log.d(LOGTAG, "Progress DONE")
            progressCallback?.invoke(100, wasSubscribed, foodSections)
        } catch (e: Exception) {
            Log.d(LOGTAG, "Failed to parse foods $e")
        }
    }

    private fun getScoreTables() {
        progressCallback?.invoke(0, false, emptyList())

        // first get a list of all sections we want to download
        if(enableGoodShoppingGuide) {
            getGoodShoppingFoodSections()
        }
        if(enableEthicalConsumer) {
            getEthicalConsumerFoodSections()
        }

        // then start downloading the sections
        downloadScoreTables()
    }

}