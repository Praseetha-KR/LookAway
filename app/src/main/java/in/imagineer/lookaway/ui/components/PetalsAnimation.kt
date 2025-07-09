package `in`.imagineer.lookaway.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.sin

data class Petal(
    val id: Int,
    val x: Float,
    val y: Float,
    val rotation: Float,
    val scale: Float,
    val color: Color
)

@Composable
fun PetalsAnimation(
    isActive: Boolean,
    petalCount: Int = 5,
    animationDuration: Long = 5000L,
    spawnInterval: Long = 3000L,
    petalSize: Dp = 12.dp,
    colors: List<Color> = listOf(
        Color(0xFFFFB3BA),
        Color(0xFFFFD1DC),
        Color(0xFFFFC0CB),
        Color(0xFFFFE4E6)
    ),
    modifier: Modifier = Modifier
) {
    var petals by remember { mutableStateOf<List<Petal>>(emptyList()) }
    
    LaunchedEffect(isActive) {
        if (isActive) {
            while (true) {
                delay(spawnInterval)
                val newPetals = (1..petalCount).map { i ->
                    Petal(
                        id = System.currentTimeMillis().toInt() + i,
                        x = (-500..500).random().toFloat(),
                        y = (-150 + (-50..50).random()).toFloat(),
                        rotation = (0..360).random().toFloat(),
                        scale = (50..120).random() / 100f,
                        color = colors.random()
                    )
                }
                petals = newPetals
                
                val steps = (animationDuration / 50).toInt()
                for (step in 0..steps) {
                    delay(50)
                    petals = petals.map { petal ->
                        petal.copy(
                            y = petal.y + 6f + (step * 0.08f),
                            x = petal.x + sin((step + petal.id) * 0.15f) * 1.5f,
                            rotation = petal.rotation + (1f + petal.id % 3)
                        )
                    }
                }
                petals = emptyList()
            }
        } else {
            petals = emptyList()
        }
    }
    
    Box(modifier = modifier) {
        petals.forEach { petal ->
            Box(
                modifier = Modifier
                    .offset(x = petal.x.dp, y = petal.y.dp)
                    .size(petalSize)
                    .scale(petal.scale)
                    .rotate(petal.rotation)
                    .background(petal.color, shape = CircleShape)
            )
        }
    }
}
