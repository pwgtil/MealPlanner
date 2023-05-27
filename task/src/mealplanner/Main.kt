package mealplanner

const val TXT_CHOOSE_CATEGORY = "Which meal do you want to add (breakfast, lunch, dinner)?"
const val TXT_INPUT_NAME = "Input the meal's name:"
const val TXT_INPUT_INGREDIENTS = "Input the ingredients:"
const val TXT_MEAL_ADDED = "The meal has been added!"
const val TXT_CATEGORY = "Category: "
const val TXT_NAME = "Name: "
const val TXT_INGREDIENTS = "Ingredients:"
fun main() {
    TXT_CHOOSE_CATEGORY.let(::println)
    val category = readln()
    TXT_INPUT_NAME.let(::println)
    val name = readln()
    TXT_INPUT_INGREDIENTS.let(::println)
    val ingredients = readln().split(",").map { it.trim() }

    println()
    (TXT_CATEGORY + category).let(::println)
    (TXT_NAME + name).let(::println)
    TXT_INGREDIENTS.let(::println)
    for (ing in ingredients) {
        ing.let(::println)
    }
    TXT_MEAL_ADDED.let(::println)
}
