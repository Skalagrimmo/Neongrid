package com.example.model

enum class CivilianActivity {
    IDLE,
    WALKING,
    SOCIALIZE,
    GESTURE,
    VENDOR,
    HURT,
    DOWNED,
    DEAD
}

data class CivilianNpc(
    val id: String,
    var pos: Point3D,
    val route: List<Point3D> = emptyList(),
    var routeIndex: Int = 0,
    var activity: CivilianActivity = CivilianActivity.IDLE,
    var visualVariantId: String = "civilian_01",
    var moveSpeed: Float = 1.0f,
    var activityTimer: Float = 0.8f,
    var animationTime: Float = 0f,
    var facingX: Float = 1f,
    var facingY: Float = 0f
) {
    val isActive: Boolean
        get() = activity != CivilianActivity.DEAD && activity != CivilianActivity.DOWNED

    val isMoving: Boolean
        get() = activity == CivilianActivity.WALKING
}
