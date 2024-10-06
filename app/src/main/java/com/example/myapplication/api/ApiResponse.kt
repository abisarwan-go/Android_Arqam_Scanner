data class StudentExitPermission(
    val firstName: String,
    val lastName: String,
    val canGoOutInCaseOfAbsence: Boolean
)

enum class BoardingStatus {
    EXTERNAL,
    HALF_BOARD
}

data class StudentBoardingStatus(
    val firstName: String,
    val lastName: String,
    val boardingStatus: BoardingStatus
)

enum class MealType {
    HOT,
    COLD,
    NONE
}

data class StudentMealTypeInfoToday(
    val firstName: String,
    val lastName: String,
    val mealTypeToday: MealType,
    val alreadyTakenToday: Boolean
)

data class ApiErrorResponse(
    val statusCode: Int,
    val error: String,
    val message: String
)