package mealplanner

const val TXT_CHOSE_OPERATION = "What would you like to do (add, show, exit)?"
const val TXT_CHOOSE_CATEGORY = "Which meal do you want to add (breakfast, lunch, dinner)?"
const val TXT_INPUT_NAME = "Input the meal's name:"
const val TXT_INPUT_INGREDIENTS = "Input the ingredients:"
const val TXT_MEAL_ADDED = "The meal has been added!"
const val TXT_CATEGORY = "Category: "
const val TXT_NAME = "Name: "
const val TXT_INGREDIENTS = "Ingredients:"
const val TXT_BYE = "Bye!"
const val TXT_MEAL_WRONG_FORMAT = "Wrong format. Use letters only!"
const val TXT_MEAL_WRONG_CATEGORY = "Wrong meal category! Choose from: breakfast, lunch, dinner."
const val TXT_NO_MEALS = "No meals saved. Add a meal first."
val VALID_OPS = listOf("add", "show", "exit")
val VALID_CATEGORIES = listOf("breakfast", "lunch", "dinner")

fun main() {
    val mealsDB = mutableListOf<Meal>()
    while (true) {
        TXT_CHOSE_OPERATION.let(::println)
        val operation = readln()
        if (operation !in VALID_OPS) continue
        when (operation) {
            VALID_OPS[0] -> {
                mealsDB.add(addMeal())
            }

            VALID_OPS[1] -> {
                showMeals(mealsDB)
            }

            VALID_OPS[2] -> {
                TXT_BYE.let(::println)
                break
            }
        }
    }
}

fun showMeals(meals: MutableList<Meal>) {
    if (meals.size == 0) {
        TXT_NO_MEALS.let(::println)
    } else {
        println()
        for (meal in meals) {
            (TXT_CATEGORY + meal.category).let(::println)
            (TXT_NAME + meal.name).let(::println)
            TXT_INGREDIENTS.let(::println)
            for (i in meal.ingredients) {
                i.let(::println)
            }
            println()
        }
    }
}

fun addMeal(): Meal {
    val category = categoryFormatCheck(TXT_CHOOSE_CATEGORY)
    val name = nameFormatCheck(TXT_INPUT_NAME)
    val ingredients = ingredientsFormatCheck(TXT_INPUT_INGREDIENTS)
    TXT_MEAL_ADDED.let(::println)
    return Meal(category, name, ingredients)
}

fun categoryFormatCheck(message: String): String {
    while (true) {
        message.let(::println)
        val input = readln()
        if (input in VALID_CATEGORIES) return input
        else TXT_MEAL_WRONG_CATEGORY.let(::println)
    }
}

fun nameFormatCheck(message: String): String {
    val regex = "[a-zA-Z]*".toRegex()
    while (true) {
        message.let(::println)
        val input = readln()
        if (input.matches(regex) && input != "") return input
        else TXT_MEAL_WRONG_FORMAT.let(::println)
    }
}

fun ingredientsFormatCheck(message: String): List<String> {
    val regex = "[a-zA-Z ]*".toRegex()
    loop@ while (true) {
        message.let(::println)
        val ingredients = readln().split(",").map { it.trim() }
        for (i in ingredients) {
            if (!i.matches(regex) || i == "") {
                TXT_MEAL_WRONG_FORMAT.let(::println)
                continue@loop
            }
        }
        return ingredients
    }
}

data class Meal(val category: String, val name: String, val ingredients: List<String>)
