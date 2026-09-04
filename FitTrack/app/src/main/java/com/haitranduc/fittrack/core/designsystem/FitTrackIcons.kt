package com.haitranduc.fittrack.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object FitTrackIcons {
    val ArrowBack: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowBack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(20f, 11f)
                horizontalLineTo(7.83f)
                lineTo(13.42f, 5.41f)
                lineTo(12f, 4f)
                lineTo(4f, 12f)
                lineTo(12f, 20f)
                lineTo(13.41f, 18.59f)
                lineTo(7.83f, 13f)
                horizontalLineTo(20f)
                verticalLineTo(11f)
                close()
            }
        }.build()
    }

    val Add: ImageVector by lazy {
        ImageVector.Builder(
            name = "Add",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(19f, 13f)
                horizontalLineTo(13f)
                verticalLineTo(19f)
                horizontalLineTo(11f)
                verticalLineTo(13f)
                horizontalLineTo(5f)
                verticalLineTo(11f)
                horizontalLineTo(11f)
                verticalLineTo(5f)
                horizontalLineTo(13f)
                verticalLineTo(11f)
                horizontalLineTo(19f)
                verticalLineTo(13f)
                close()
            }
        }.build()
    }

    val Close: ImageVector by lazy {
        ImageVector.Builder(
            name = "Close",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(19f, 6.41f)
                lineTo(17.59f, 5f)
                lineTo(12f, 10.59f)
                lineTo(6.41f, 5f)
                lineTo(5f, 6.41f)
                lineTo(10.59f, 12f)
                lineTo(5f, 17.59f)
                lineTo(6.41f, 19f)
                lineTo(12f, 13.41f)
                lineTo(17.59f, 19f)
                lineTo(19f, 17.59f)
                lineTo(13.41f, 12f)
                close()
            }
        }.build()
    }

    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "Check",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(9f, 16.17f)
                lineTo(4.83f, 12f)
                lineTo(3.41f, 13.41f)
                lineTo(9f, 19f)
                lineTo(21f, 7f)
                lineTo(19.59f, 5.59f)
                close()
            }
        }.build()
    }

    val Exercises: ImageVector by lazy {
        ImageVector.Builder(
            name = "Exercises",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(6f, 5f)
                horizontalLineTo(8f)
                verticalLineTo(19f)
                horizontalLineTo(6f)
                close()
                moveTo(16f, 5f)
                horizontalLineTo(18f)
                verticalLineTo(19f)
                horizontalLineTo(16f)
                close()
                moveTo(8f, 11f)
                horizontalLineTo(16f)
                verticalLineTo(13f)
                horizontalLineTo(8f)
                close()
                moveTo(4f, 7f)
                horizontalLineTo(6f)
                verticalLineTo(17f)
                horizontalLineTo(4f)
                close()
                moveTo(18f, 7f)
                horizontalLineTo(20f)
                verticalLineTo(17f)
                horizontalLineTo(18f)
                close()
            }
        }.build()
    }

    val Workouts: ImageVector by lazy {
        ImageVector.Builder(
            name = "Workouts",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(3f, 13f)
                horizontalLineTo(5f)
                verticalLineTo(11f)
                horizontalLineTo(3f)
                close()
                moveTo(3f, 17f)
                horizontalLineTo(5f)
                verticalLineTo(15f)
                horizontalLineTo(3f)
                close()
                moveTo(3f, 9f)
                horizontalLineTo(5f)
                verticalLineTo(7f)
                horizontalLineTo(3f)
                close()
                moveTo(7f, 13f)
                horizontalLineTo(21f)
                verticalLineTo(11f)
                horizontalLineTo(7f)
                close()
                moveTo(7f, 17f)
                horizontalLineTo(21f)
                verticalLineTo(15f)
                horizontalLineTo(7f)
                close()
                moveTo(7f, 9f)
                horizontalLineTo(21f)
                verticalLineTo(7f)
                horizontalLineTo(7f)
                close()
            }
        }.build()
    }

    val History: ImageVector by lazy {
        ImageVector.Builder(
            name = "History",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(13f, 3f)
                curveTo(8.03f, 3f, 4f, 7.03f, 4f, 12f)
                horizontalLineTo(1f)
                lineTo(4.89f, 15.89f)
                lineTo(5f, 16f)
                lineTo(9f, 12f)
                horizontalLineTo(6f)
                curveTo(6f, 8.13f, 9.13f, 5f, 13f, 5f)
                curveTo(16.87f, 5f, 20f, 8.13f, 20f, 12f)
                curveTo(20f, 15.87f, 16.87f, 19f, 13f, 19f)
                curveTo(11.07f, 19f, 9.32f, 18.21f, 8.06f, 16.94f)
                lineTo(6.64f, 18.36f)
                curveTo(8.27f, 19.99f, 10.51f, 21f, 13f, 21f)
                curveTo(17.97f, 21f, 22f, 16.97f, 22f, 12f)
                curveTo(22f, 7.03f, 17.97f, 3f, 13f, 3f)
                close()
                moveTo(12f, 8f)
                verticalLineTo(13f)
                lineTo(16.2f, 15.5f)
                lineTo(17f, 14.2f)
                lineTo(13.5f, 12.1f)
                verticalLineTo(8f)
                horizontalLineTo(12f)
                close()
            }
        }.build()
    }

    val Play: ImageVector by lazy {
        ImageVector.Builder(
            name = "Play",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(8f, 5f)
                verticalLineTo(19f)
                lineTo(19f, 12f)
                close()
            }
        }.build()
    }

    val Delete: ImageVector by lazy {
        ImageVector.Builder(
            name = "Delete",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(6f, 19f)
                curveTo(6f, 20.1f, 6.9f, 21f, 8f, 21f)
                horizontalLineTo(16f)
                curveTo(17.1f, 21f, 18f, 20.1f, 18f, 19f)
                verticalLineTo(7f)
                horizontalLineTo(6f)
                verticalLineTo(19f)
                close()
                moveTo(19f, 4f)
                horizontalLineTo(15.5f)
                lineTo(14.5f, 3f)
                horizontalLineTo(9.5f)
                lineTo(8.5f, 4f)
                horizontalLineTo(5f)
                verticalLineTo(6f)
                horizontalLineTo(19f)
                verticalLineTo(4f)
                close()
            }
        }.build()
    }

    val ArrowUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(4f, 12f)
                lineTo(5.41f, 13.41f)
                lineTo(11f, 7.83f)
                verticalLineTo(20f)
                horizontalLineTo(13f)
                verticalLineTo(7.83f)
                lineTo(18.59f, 13.41f)
                lineTo(20f, 12f)
                lineTo(12f, 4f)
                close()
            }
        }.build()
    }

    val ArrowDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(20f, 12f)
                lineTo(18.59f, 10.59f)
                lineTo(13f, 16.17f)
                verticalLineTo(4f)
                horizontalLineTo(11f)
                verticalLineTo(16.17f)
                lineTo(5.41f, 10.59f)
                lineTo(4f, 12f)
                lineTo(12f, 20f)
                close()
            }
        }.build()
    }

    val Star: ImageVector by lazy {
        ImageVector.Builder(
            name = "Star",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 17.27f)
                lineTo(18.18f, 21f)
                lineTo(16.54f, 13.97f)
                lineTo(22f, 9.24f)
                lineTo(14.81f, 8.63f)
                lineTo(12f, 2f)
                lineTo(9.19f, 8.63f)
                lineTo(2f, 9.24f)
                lineTo(7.46f, 13.97f)
                lineTo(5.82f, 21f)
                close()
            }
        }.build()
    }

    val StarBorder: ImageVector by lazy {
        ImageVector.Builder(
            name = "StarBorder",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(22f, 9.24f)
                lineTo(14.81f, 8.62f)
                lineTo(12f, 2f)
                lineTo(9.19f, 8.63f)
                lineTo(2f, 9.24f)
                lineTo(7.46f, 13.97f)
                lineTo(5.82f, 21f)
                lineTo(12f, 17.27f)
                lineTo(18.18f, 21f)
                lineTo(16.55f, 13.97f)
                lineTo(22f, 9.24f)
                close()
                moveTo(12f, 15.4f)
                lineTo(8.24f, 17.67f)
                lineTo(9.24f, 13.39f)
                lineTo(5.92f, 10.51f)
                lineTo(10.3f, 10.13f)
                lineTo(12f, 6.1f)
                lineTo(13.71f, 10.14f)
                lineTo(18.09f, 10.52f)
                lineTo(14.77f, 13.4f)
                lineTo(15.77f, 17.68f)
                lineTo(12f, 15.4f)
                close()
            }
        }.build()
    }

    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(19.14f, 12.94f)
                curveTo(19.18f, 12.63f, 19.2f, 12.32f, 19.2f, 12f)
                curveTo(19.2f, 11.68f, 19.18f, 11.37f, 19.14f, 11.06f)
                lineTo(21.41f, 9.29f)
                curveTo(21.62f, 9.13f, 21.67f, 8.83f, 21.53f, 8.59f)
                lineTo(19.38f, 4.86f)
                curveTo(19.25f, 4.62f, 18.96f, 4.53f, 18.72f, 4.62f)
                lineTo(16.05f, 5.7f)
                curveTo(15.49f, 5.27f, 14.88f, 4.93f, 14.22f, 4.69f)
                lineTo(13.82f, 1.85f)
                curveTo(13.78f, 1.59f, 13.55f, 1.4f, 13.29f, 1.4f)
                lineTo(8.97f, 1.4f)
                curveTo(8.71f, 1.4f, 8.48f, 1.59f, 8.44f, 1.85f)
                lineTo(8.04f, 4.69f)
                curveTo(7.38f, 4.93f, 6.77f, 5.28f, 6.21f, 5.7f)
                lineTo(3.54f, 4.62f)
                curveTo(3.3f, 4.53f, 3.01f, 4.62f, 2.88f, 4.86f)
                lineTo(0.73f, 8.59f)
                curveTo(0.59f, 8.83f, 0.64f, 9.13f, 0.85f, 9.29f)
                lineTo(3.12f, 11.06f)
                curveTo(3.08f, 11.37f, 3.06f, 11.69f, 3.06f, 12f)
                curveTo(3.06f, 12.31f, 3.08f, 12.63f, 3.12f, 12.94f)
                lineTo(0.85f, 14.71f)
                curveTo(0.64f, 14.87f, 0.59f, 15.17f, 0.73f, 15.41f)
                lineTo(2.88f, 19.14f)
                curveTo(3.01f, 19.38f, 3.3f, 19.47f, 3.54f, 19.38f)
                lineTo(6.21f, 18.3f)
                curveTo(6.77f, 18.73f, 7.38f, 19.07f, 8.04f, 19.31f)
                lineTo(8.44f, 22.15f)
                curveTo(8.48f, 22.41f, 8.71f, 22.6f, 8.97f, 22.6f)
                lineTo(13.29f, 22.6f)
                curveTo(13.55f, 22.6f, 13.78f, 22.41f, 13.82f, 22.15f)
                lineTo(14.22f, 19.31f)
                curveTo(14.88f, 19.07f, 15.49f, 18.73f, 16.05f, 18.3f)
                lineTo(18.72f, 19.38f)
                curveTo(18.96f, 19.47f, 19.25f, 19.38f, 19.38f, 19.14f)
                lineTo(21.53f, 15.41f)
                curveTo(21.67f, 15.17f, 21.62f, 14.87f, 21.41f, 14.71f)
                lineTo(19.14f, 12.94f)
                close()
                moveTo(12f, 15.5f)
                curveTo(10.07f, 15.5f, 8.5f, 13.93f, 8.5f, 12f)
                curveTo(8.5f, 10.07f, 10.07f, 8.5f, 12f, 8.5f)
                curveTo(13.93f, 8.5f, 15.5f, 10.07f, 15.5f, 12f)
                curveTo(15.5f, 13.93f, 13.93f, 15.5f, 12f, 15.5f)
                close()
            }
        }.build()
    }
}
