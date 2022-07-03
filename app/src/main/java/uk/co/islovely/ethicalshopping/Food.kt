package uk.co.islovely.ethicalshopping

enum class Rating {
    GOOD, AVERAGE, BAD
}

class Food(val title: String, val rating: Rating, val link : String) {
}

class FoodSection(val location: String) {
    var title : String = "Title";
    var good_foods = mutableListOf<Food>();
    var average_foods = mutableListOf<Food>();
    var bad_foods = mutableListOf<Food>();
    var last_success: Long = 0L;
}
