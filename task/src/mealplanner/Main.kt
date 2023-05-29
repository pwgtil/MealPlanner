package mealplanner
import java.sql.Connection
import java.sql.DriverManager

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
    val connection = createDB()
    while (true) {
        TXT_CHOSE_OPERATION.let(::println)
        val operation = readln()
        if (operation !in VALID_OPS) continue
        when (operation) {
            VALID_OPS[0] -> {
                val meal = addMeal()
                addMealToDB(meal, connection)
            }

            VALID_OPS[1] -> {
                val meals = getMealsFromDV(connection)
                showMeals(meals)
            }

            VALID_OPS[2] -> {
                TXT_BYE.let(::println)
                connection.close()
                break
            }
        }
    }
}

fun createDB(): Connection {
    val connection = DriverManager.getConnection("jdbc:sqlite:meals.db")
    val statement = connection.createStatement()
//    statement.executeUpdate("drop table if exists meals")
//    statement.executeUpdate("drop table if exists ingredients")
//    statement.executeUpdate("create table meals (category string, meal string, meal_id integer)")
//    statement.executeUpdate("create table ingredients (ingredient string, ingredient_id integer, meal_id integer)")
    statement.executeUpdate("create table if not exists meals (category string, meal string, meal_id integer)")
    statement.executeUpdate("create table if not exists ingredients (ingredient string, ingredient_id integer, meal_id integer)")
    return connection
}

fun addMealToDB(meal: Meal, connection: Connection) {
    val statement = connection.createStatement()
    val lastID = statement.executeQuery("select max(meal_id) from meals").getInt("max(meal_id)")
    statement.executeUpdate("insert into meals values('${meal.category}', '${meal.name}', ${lastID + 1})")
    meal.ingredients.forEachIndexed { i, s ->
        statement.executeUpdate("insert into ingredients values('${s}', '$i', ${lastID + 1})")
    }
}

fun getMealsFromDV(connection: Connection): List<Meal> {
    val statement = connection.createStatement()
    val mealsRS = statement.executeQuery("select * from meals")
    val mealsStructured = mutableMapOf<Int, Meal>()
    while (mealsRS.next()){
        val id = mealsRS.getInt("meal_id")
        val category = mealsRS.getString("category")
        val name = mealsRS.getString("meal")
        val ingStatement = connection.createStatement()
        val ingredientsRS = ingStatement.executeQuery("select * from ingredients where meal_id = '$id'")
        val ingredients = mutableListOf<String>()
        while (ingredientsRS.next()) {
            ingredients.add(ingredientsRS.getString("ingredient"))
        }
        mealsStructured[id] = Meal(category, name, ingredients)
    }
    return mealsStructured.values.toList()
}

fun showMeals(meals: List<Meal>) {
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
