package mealplanner

import java.io.FileWriter
import java.sql.Connection
import java.sql.DriverManager

// ---------------------------------------------------------------------------------------------------------------------
// --MESSAGES-----------------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
const val TXT_CHOSE_OPERATION = "What would you like to do (add, show, plan, save, exit)?"
const val TXT_CHOOSE_CATEGORY_TO_ADD = "Which meal do you want to add (breakfast, lunch, dinner)?"
const val TXT_CHOOSE_CATEGORY_TO_SHOW = "Which category do you want to print (breakfast, lunch, dinner)?"
const val TXT_INPUT_NAME = "Input the meal's name:"
const val TXT_INPUT_INGREDIENTS = "Input the ingredients:"
const val TXT_MEAL_ADDED = "The meal has been added!"
const val TXT_CATEGORY = "Category: "
const val TXT_NAME = "Name: "
const val TXT_INGREDIENTS = "Ingredients:"
const val TXT_BYE = "Bye!"
const val TXT_MEAL_WRONG_FORMAT = "Wrong format. Use letters only!"
const val TXT_MEAL_WRONG_CATEGORY = "Wrong meal category! Choose from: breakfast, lunch, dinner."
const val TXT_NO_MEALS = "No meals found."
const val TXT_CHOOSE_FROM_LIST_ABOVE = "Choose the %s for %s from the list above:"
const val TXT_MEALS_PLANNED_FOR_DAY = "Yeah! We planned the meals for %s."
const val TXT_MEAL_DONT_EXIST = "This meal doesn’t exist. Choose a meal from the list above."
const val TXT_UNABLE_TO_SAVE_PLAN = "Unable to save. Plan your meals first."
const val TXT_INPUT_FILE_NAME = "Input a filename:"
const val TXT_PLAN_FILE_SAVED = "Saved!"
val VALID_OPS = listOf("add", "show", "exit", "plan", "save", "admin")
val VALID_CATEGORIES = listOf("breakfast", "lunch", "dinner")
val DAYS_OF_WEEK = mapOf(
    0 to "Monday",
    1 to "Tuesday",
    2 to "Wednesday",
    3 to "Thursday",
    4 to "Friday",
    5 to "Saturday",
    6 to "Sunday"
)

// ---------------------------------------------------------------------------------------------------------------------
// --MENU LOOP IN MAIN--------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
fun main() {
    var connection = connectDB()
    while (true) {
        TXT_CHOSE_OPERATION.let(::println)
        val operation = readln()
        if (operation !in VALID_OPS) continue
        when (operation) {
            VALID_OPS[0] -> { // ADD
                val meal = addMeal()
                addMealToDB(meal, connection)
            }

            VALID_OPS[1] -> { // SHOW
                val meals = getMealsFromDVbyCategory(connection)
                showMeals(meals)
            }

            VALID_OPS[2] -> { // EXIT
                TXT_BYE.let(::println)
                connection.close()
                break
            }

            VALID_OPS[3] -> { // PLAN
                val meals = getMealsFromDB(connection)
                val plan: List<Plan> = addPlan(meals)
                showCurrentPlan(plan)
                addPlanToDB(connection, plan)
            }

            VALID_OPS[4] -> { // SAVE
                val shoppingList = getShoppingListFromDB(connection)
                savePlanToFile(shoppingList)
            }

            VALID_OPS[5] -> { // ADMIN - drop DB
                connection = dropAndCreateDB()
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// --DB CONNECTION SETUP------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
fun connectDB(): Connection {
    val connection = DriverManager.getConnection("jdbc:sqlite:meals.db")
    val statement = connection.createStatement()
    statement.executeUpdate("create table if not exists meals (meal_id integer primary key, category text, meal text)")
    statement.executeUpdate("create table if not exists ingredients (ingredient text, ingredient_id integer, meal_id integer, foreign key (meal_id) references meals(meal_id))")
    statement.executeUpdate("create table if not exists plan (day_of_week integer, meal_id integer, foreign key (meal_id) references meals(meal_id))")
    return connection
}

fun dropAndCreateDB(): Connection {
    val connection = DriverManager.getConnection("jdbc:sqlite:meals.db")
    val statement = connection.createStatement()
    statement.executeUpdate("drop table if exists meals")
    statement.executeUpdate("drop table if exists ingredients")
    statement.executeUpdate("drop table if exists plan")
    statement.executeUpdate("create table meals (meal_id integer primary key, category text, meal text)")
    statement.executeUpdate("create table ingredients (ingredient text, ingredient_id integer, meal_id integer, foreign key (meal_id) references meals(meal_id))")
    statement.executeUpdate("create table plan (day_of_week integer, meal_id integer, foreign key (meal_id) references meals(meal_id))")
    return connection
}

// ---------------------------------------------------------------------------------------------------------------------
// --OUTPUT HANDLING TO DB OR FILE--------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
fun savePlanToFile(shoppingList: Map<String, Int>?) {
    if (shoppingList == null) {
        TXT_UNABLE_TO_SAVE_PLAN.let(::println)
    } else {
        TXT_INPUT_FILE_NAME.let(::println)
        val filename = readln()
        val fileContent = StringBuilder()
        shoppingList.forEach {
            fileContent.append(it.key + if (it.value > 1) " x${it.value}\n" else "\n")
        }
        FileWriter(filename).use {
            it.write(fileContent.toString())
        }
        TXT_PLAN_FILE_SAVED.let(::println)
    }
}

fun addPlanToDB(connection: Connection, plan: List<Plan>) {
    val statement = connection.createStatement()
    statement.executeUpdate("DELETE FROM plan")
    val values = StringBuilder()
    plan.forEach { day ->
        day.meals.forEach { meal ->
            values.append("(${day.day}, ${meal.id}),")
        }
    }
    statement.executeUpdate("insert into plan (day_of_week, meal_id) values ${values.toString().trim(',')}")
}

fun addMealToDB(meal: Meal, connection: Connection) {
    val statement = connection.createStatement()
    val lastID = statement.executeQuery("select max(meal_id) from meals").getInt("max(meal_id)")
    statement.executeUpdate("insert into meals values(${lastID + 1}, '${meal.category}', '${meal.name}')")
    meal.ingredients.forEachIndexed { i, s ->
        statement.executeUpdate("insert into ingredients values('${s}', '$i', ${lastID + 1})")
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// --INPUT HANDLING FROM DB---------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
fun getShoppingListFromDB(connection: Connection): Map<String, Int>? {
    val statement = connection.createStatement()
    val planCheckSum =
        statement.executeQuery("select sum(distinct day_of_week) from plan").getInt("sum(distinct day_of_week)")
    return if (planCheckSum != 21) {
        null
    } else {
        val ingredientsQuery =
            statement.executeQuery("select ingredient from ingredients inner join plan on ingredients.meal_id = plan.meal_id")
        val allIngredients = mutableListOf<String>()
        while (ingredientsQuery.next()) {
            allIngredients.add(ingredientsQuery.getString("ingredient"))
        }
        allIngredients.groupingBy { it }.eachCount().toSortedMap()
    }
}

fun getMealsFromDB(connection: Connection, mealCategory: String? = null): List<Meal> {
    val statement = connection.createStatement()
    val query = String.format(
        "select * from meals%s", if (mealCategory != null) {
            " where category = '$mealCategory'"
        } else ""
    )
    val mealsRS = statement.executeQuery(query)
    val mealsStructured = mutableMapOf<Int, Meal>()
    while (mealsRS.next()) {
        val id = mealsRS.getInt("meal_id")
        val category = mealsRS.getString("category")
        val name = mealsRS.getString("meal")
        val ingStatement = connection.createStatement()
        val ingredientsRS = ingStatement.executeQuery("select * from ingredients where meal_id = '$id'")
        val ingredients = mutableListOf<String>()
        while (ingredientsRS.next()) {
            ingredients.add(ingredientsRS.getString("ingredient"))
        }
        mealsStructured[id] = Meal(category, name, ingredients, id)
    }
    return mealsStructured.values.toList()
}

fun getMealsFromDVbyCategory(connection: Connection): List<Meal> {
    val category = categoryFormatCheck(TXT_CHOOSE_CATEGORY_TO_SHOW, TXT_MEAL_WRONG_CATEGORY)
    return getMealsFromDB(connection, category)
}

// ---------------------------------------------------------------------------------------------------------------------
// --VIEW HANDLING INPUT/OUTPUT-----------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
fun showCurrentPlan(plan: List<Plan>) {
    plan.forEach {
        DAYS_OF_WEEK[it.day].let(::println)
        for (i in 0..2) {
            String.format("%s: %s", VALID_CATEGORIES[i], it.meals[i].name).let(::println)
        }
        println()
    }
}

fun showMeals(meals: List<Meal>) {
    if (meals.isEmpty()) {
        TXT_NO_MEALS.let(::println)
    } else {
        println()
        (TXT_CATEGORY + meals[0].category).let(::println).also { println() }
        for (meal in meals) {
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
    val category = categoryFormatCheck(TXT_CHOOSE_CATEGORY_TO_ADD, TXT_MEAL_WRONG_CATEGORY)
    val name = nameFormatCheck()
    val ingredients = ingredientsFormatCheck()
    TXT_MEAL_ADDED.let(::println)
    return Meal(category, name, ingredients)
}

fun addPlan(meals: List<Meal>): List<Plan> {
    val schedule = mutableListOf<Plan>()
    for (day in DAYS_OF_WEEK) {
        day.value.let(::println)
        val plan = Plan(day.key, mutableListOf())
        for (i in 0..2) {
            meals.filter { it.category == VALID_CATEGORIES[i] }.sortedBy { it.name }.forEach { it.name.let(::println) }
            String.format(TXT_CHOOSE_FROM_LIST_ABOVE, VALID_CATEGORIES[i], day.value).let(::println)
            var selectedMeal: String
            while (true) {
                selectedMeal = readln()
                if (selectedMeal in meals.filter { it.category == VALID_CATEGORIES[i] }.map { it.name })
                    break
                TXT_MEAL_DONT_EXIST.let(::println)
            }
            plan.meals.add(meals.find { it.name == selectedMeal && it.category == VALID_CATEGORIES[i] }!!) // assured above
        }
        schedule.add(plan)
        String.format(TXT_MEALS_PLANNED_FOR_DAY, day.value).let(::println).also { println() }
    }
    return schedule.toList()
}

// ---------------------------------------------------------------------------------------------------------------------
// --USER INPUT FORMAT CHECKS-------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
fun categoryFormatCheck(message: String, errorMsg: String): String {
    while (true) {
        message.let(::println)
        val input = readln()
        if (input in VALID_CATEGORIES) return input
        else errorMsg.let(::println)
    }
}

fun nameFormatCheck(): String {
    val regex = "[a-zA-Z ]*".toRegex()
    while (true) {
        TXT_INPUT_NAME.let(::println)
        val input = readln().trim()
        if (input.matches(regex) && input != "") return input
        else TXT_MEAL_WRONG_FORMAT.let(::println)
    }
}

fun ingredientsFormatCheck(): List<String> {
    val regex = "[a-zA-Z ]*".toRegex()
    loop@ while (true) {
        TXT_INPUT_INGREDIENTS.let(::println)
        val ingredients = readln().split(",").map { it.trim().lowercase() }
        for (i in ingredients) {
            if (!i.matches(regex) || i == "") {
                TXT_MEAL_WRONG_FORMAT.let(::println)
                continue@loop
            }
        }
        return ingredients
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// --DATA CLASSES-------------------------------------------------------------------------------------------------------
// ---------------------------------------------------------------------------------------------------------------------
data class Meal(val category: String, val name: String, val ingredients: List<String>, val id: Int = Int.MAX_VALUE)
data class Plan(val day: Int, val meals: MutableList<Meal>)
