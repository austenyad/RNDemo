package com.tuwan.g2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuwan.g2.ui.theme.G2Theme
import com.tuwan.rnlib.RNManager
import kotlin.math.*
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            G2Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF080818)
                ) { innerPadding ->
                    Box(Modifier.padding(innerPadding).fillMaxSize()) {
                        FlipCardDemo(Modifier.fillMaxSize())

                        // 打开 React Native 页面的按钮
                        val context = LocalContext.current
                        Button(
                            onClick = {
                                RNManager.startRNActivity(context, "G2RN")
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6200EE)
                            )
                        ) {
                            Text("打开 React Native 页面", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// 粒子系统
// ═══════════════════════════════════════
data class Particle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    var life: Float = 1f,
    var decay: Float = Random.nextFloat() * 0.012f + 0.004f,
    var size: Float = Random.nextFloat() * 5f + 2f,
    var color: Color,
    // 拖尾用
    var prevX: Float = x, var prevY: Float = y
)

data class FloatingOrb(
    var x: Float, var y: Float,
    val radius: Float,
    val speedX: Float, val speedY: Float,
    val color: Color
)

val sparkleColors = listOf(
    Color(0xFFFF6D00), Color(0xFFFF3D00), Color(0xFF7C4DFF),
    Color(0xFFFFD600), Color(0xFF00E5FF), Color(0xFFFF4081),
    Color(0xFF76FF03), Color(0xFFEA80FC)
)

fun burstParticles(cx: Float, cy: Float, count: Int = 40): List<Particle> = List(count) {
    val angle = Random.nextFloat() * 2 * PI.toFloat()
    val speed = Random.nextFloat() * 8f + 3f
    Particle(
        x = cx, y = cy,
        vx = cos(angle) * speed,
        vy = sin(angle) * speed,
        color = sparkleColors.random()
    )
}

// ═══════════════════════════════════════
// 浮动光斑背景
// ═══════════════════════════════════════
@Composable
fun FloatingOrbsBackground() {
    val orbs = remember {
        List(8) {
            FloatingOrb(
                x = Random.nextFloat(), y = Random.nextFloat(),
                radius = Random.nextFloat() * 60f + 30f,
                speedX = (Random.nextFloat() - 0.5f) * 0.002f,
                speedY = (Random.nextFloat() - 0.5f) * 0.002f,
                color = sparkleColors[it % sparkleColors.size].copy(alpha = 0.25f)
            )
        }.toMutableStateList()
    }

    val time by rememberInfiniteTransition(label = "orbTime").animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(100_000, easing = LinearEasing)),
        label = "t"
    )

    Canvas(modifier = Modifier.fillMaxSize().blur(40.dp)) {
        orbs.forEachIndexed { i, orb ->
            val ox = ((orb.x + orb.speedX * time + i * 0.1f) % 1.2f)
            val oy = ((orb.y + orb.speedY * time + i * 0.07f) % 1.2f)
            drawCircle(
                color = orb.color,
                radius = orb.radius,
                center = Offset(ox * size.width, oy * size.height)
            )
        }
    }
}

// ═══════════════════════════════════════
// Demo 入口
// ═══════════════════════════════════════
@Composable
fun FlipCardDemo(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        FloatingOrbsBackground()
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "拖拽倾斜 · 点击翻转",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(40.dp))
            FlipCard()
        }
    }
}

// ═══════════════════════════════════════
// 卡牌主体
// ═══════════════════════════════════════
@Composable
fun FlipCard() {
    var flipped by remember { mutableStateOf(false) }

    // 弹性翻转
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 80f),
        label = "flip"
    )

    // 缩放脉冲
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(flipped) {
        pulse.animateTo(0.80f, tween(100))
        pulse.animateTo(1.12f, tween(200, easing = FastOutSlowInEasing))
        pulse.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 120f))
    }

    // 触摸倾斜
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    val aTiltX by animateFloatAsState(tiltX, spring(0.55f, 300f), label = "tx")
    val aTiltY by animateFloatAsState(tiltY, spring(0.55f, 300f), label = "ty")

    // 呼吸浮动
    val inf = rememberInfiniteTransition(label = "breathe")
    val floatY by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY"
    )
    val breatheScale by inf.animateFloat(
        1f, 1.015f,
        infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breatheScale"
    )

    // 光泽
    val shimmer by inf.animateFloat(
        -1f, 2f,
        infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "shimmer"
    )

    // 彩虹边框
    val borderAngle by inf.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(2500, easing = LinearEasing)),
        label = "border"
    )

    // 全息棱镜色相偏移
    val hueShift by inf.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "hue"
    )

    // 粒子
    val particles = remember { mutableStateListOf<Particle>() }
    var flipCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(flipCount) {
        if (flipCount == 0) return@LaunchedEffect
        particles.addAll(burstParticles(0f, 0f, 50))
    }

    // 粒子物理帧
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis {
                val dead = mutableListOf<Particle>()
                particles.forEach { p ->
                    p.prevX = p.x; p.prevY = p.y
                    p.x += p.vx; p.y += p.vy
                    p.vy += 0.12f
                    p.vx *= 0.99f
                    p.life -= p.decay
                    if (p.life <= 0f) dead.add(p)
                }
                particles.removeAll(dead)
            }
        }
    }

    val norm = (rotation % 360 + 360) % 360
    val isFront = norm <= 90f || norm > 270f

    Box(contentAlignment = Alignment.Center) {
        // 粒子层
        Canvas(Modifier.size(360.dp, 480.dp)) {
            particles.forEach { p ->
                val alpha = p.life.coerceIn(0f, 1f)
                // 拖尾线
                drawLine(
                    color = p.color.copy(alpha = alpha * 0.4f),
                    start = Offset(size.width / 2 + p.prevX, size.height / 2 + p.prevY),
                    end = Offset(size.width / 2 + p.x, size.height / 2 + p.y),
                    strokeWidth = p.size * 0.6f
                )
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = p.size * alpha,
                    center = Offset(size.width / 2 + p.x, size.height / 2 + p.y)
                )
            }
        }

        // 发光底光（卡牌下方模糊光晕）
        Canvas(
            Modifier
                .width(200.dp).height(40.dp)
                .offset(y = 190.dp)
                .blur(20.dp)
        ) {
            drawOval(
                color = if (isFront) Color(0xFF7C4DFF).copy(alpha = 0.5f)
                else Color(0xFFFF3D00).copy(alpha = 0.5f),
                size = size
            )
        }

        // 彩虹旋转边框
        Canvas(
            Modifier
                .width(254.dp).height(374.dp)
                .clip(RoundedCornerShape(18.dp))
                .graphicsLayer {
                    translationY = -8f + floatY * 16f
                    scaleX = breatheScale * pulse.value
                    scaleY = breatheScale * pulse.value
                }
        ) {
            val rainbow = listOf(
                Color(0xFFFF0040), Color(0xFFFF8800), Color(0xFFFFEE00),
                Color(0xFF00FF88), Color(0xFF0088FF), Color(0xFFAA00FF),
                Color(0xFFFF0080), Color(0xFFFF0040)
            )
            rotate(borderAngle) {
                drawRect(
                    brush = Brush.sweepGradient(rainbow),
                    size = size,
                    style = Stroke(width = 5.dp.toPx())
                )
            }
        }

        // 卡牌
        Card(
            modifier = Modifier
                .width(240.dp).height(360.dp)
                .graphicsLayer {
                    rotationY = rotation
                    rotationX = aTiltX
                    rotationZ = aTiltY * 0.25f
                    translationY = -8f + floatY * 16f
                    scaleX = breatheScale * pulse.value
                    scaleY = breatheScale * pulse.value
                    cameraDistance = 18f * density
                    // 边缘发光
                    shadowElevation = if (norm in 50f..130f || norm in 230f..310f) 2f else 20f
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            val cx = size.width / 2f; val cy = size.height / 2f
                            tiltX = -((change.position.y - cy) / cy) * 18f
                            tiltY = ((change.position.x - cx) / cx) * 10f
                        },
                        onDragEnd = { tiltX = 0f; tiltY = 0f },
                        onDragCancel = { tiltX = 0f; tiltY = 0f }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { flipped = !flipped; flipCount++ }
                },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            if (isFront) {
                CardFront(shimmer, hueShift)
            } else {
                Box(Modifier.graphicsLayer { rotationY = 180f }) {
                    CardBack(shimmer, borderAngle)
                }
            }
        }
    }
}

// ═══════════════════════════════════════
// 光泽扫过
// ═══════════════════════════════════════
@Composable
fun ShimmerOverlay(offset: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width * 0.5f
        val sx = offset * size.width
        drawRect(
            brush = Brush.linearGradient(
                listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.08f),
                    Color.White.copy(alpha = 0.30f),
                    Color.White.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                start = Offset(sx, 0f),
                end = Offset(sx + w, size.height)
            )
        )
    }
}

// ═══════════════════════════════════════
// 全息棱镜叠加层
// ═══════════════════════════════════════
@Composable
fun HoloPrismOverlay(hueShift: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val colors = listOf(
            Color.Red.copy(alpha = 0.06f),
            Color.Yellow.copy(alpha = 0.06f),
            Color.Green.copy(alpha = 0.06f),
            Color.Cyan.copy(alpha = 0.06f),
            Color.Blue.copy(alpha = 0.06f),
            Color.Magenta.copy(alpha = 0.06f),
            Color.Red.copy(alpha = 0.06f)
        )
        // 对角线渐变随 hueShift 偏移
        val shift = hueShift / 360f
        val startX = -size.width * 0.5f + shift * size.width * 2f
        drawRect(
            brush = Brush.linearGradient(
                colors,
                start = Offset(startX, 0f),
                end = Offset(startX + size.width * 1.5f, size.height)
            )
        )
    }
}

// ═══════════════════════════════════════
// 正面
// ═══════════════════════════════════════
@Composable
fun CardFront(shimmer: Float, hueShift: Float) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF9C27B0), Color(0xFF4A148C), Color(0xFF1A237E)),
                    start = Offset.Zero,
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            // 全息色彩叠加
            .drawWithContent {
                drawContent()
                val holoColors = listOf(
                    Color.Transparent,
                    Color(0x15FF0000), Color(0x15FFFF00),
                    Color(0x1500FF00), Color(0x1500FFFF),
                    Color(0x150000FF), Color(0x15FF00FF),
                    Color.Transparent
                )
                val shift = hueShift / 360f
                val sx = -size.width + shift * size.width * 3f
                drawRect(
                    brush = Brush.linearGradient(
                        holoColors,
                        start = Offset(sx, 0f),
                        end = Offset(sx + size.width, size.height)
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 星空背景
        Canvas(Modifier.fillMaxSize()) {
            val stars = listOf(
                0.1f to 0.15f, 0.85f to 0.1f, 0.2f to 0.7f,
                0.75f to 0.85f, 0.5f to 0.4f, 0.92f to 0.45f,
                0.08f to 0.55f, 0.65f to 0.2f, 0.3f to 0.92f,
                0.45f to 0.12f, 0.78f to 0.6f, 0.15f to 0.88f
            )
            stars.forEachIndexed { i, (rx, ry) ->
                val twinkle = (sin(hueShift * 0.05f + i * 1.2f) * 0.5f + 0.5f)
                drawCircle(
                    Color.White.copy(alpha = 0.1f + twinkle * 0.25f),
                    radius = 1.5f + twinkle * 1.5f,
                    center = Offset(size.width * rx, size.height * ry)
                )
            }
        }

        // 左上角标
        Column(
            Modifier.align(Alignment.TopStart).padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("A", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("♠", fontSize = 22.sp, color = Color.White)
        }
        // 右下角标
        Column(
            Modifier.align(Alignment.BottomEnd).padding(14.dp)
                .graphicsLayer { rotationZ = 180f },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("A", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("♠", fontSize = 22.sp, color = Color.White)
        }

        // 中心
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("♠", fontSize = 80.sp, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(
                "ACE OF SPADES", fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 5.sp
            )
        }

        ShimmerOverlay(shimmer)
    }
}

// ═══════════════════════════════════════
// 背面
// ═══════════════════════════════════════
@Composable
fun CardBack(shimmer: Float, spinAngle: Float) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFFF6D00), Color(0xFFD50000), Color(0xFF880E4F)),
                    start = Offset.Zero,
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // 网格装饰
        Canvas(Modifier.fillMaxSize().padding(16.dp).clip(RoundedCornerShape(8.dp))) {
            val gs = 26f
            val cols = (size.width / gs).toInt()
            val rows = (size.height / gs).toInt()
            for (r in 0..rows) for (c in 0..cols) {
                if ((r + c) % 2 == 0) {
                    drawRect(
                        Color.White.copy(alpha = 0.05f),
                        topLeft = Offset(c * gs, r * gs),
                        size = Size(gs * 0.7f, gs * 0.7f)
                    )
                }
            }
        }

        // 能量脉冲波纹
        val inf = rememberInfiniteTransition(label = "pulse")
        val pulse1 by inf.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(2000, easing = LinearEasing)),
            label = "p1"
        )
        val pulse2 by inf.animateFloat(
            0f, 1f,
            infiniteRepeatable(
                tween(2000, easing = LinearEasing),
                initialStartOffset = StartOffset(700)
            ),
            label = "p2"
        )
        val pulse3 by inf.animateFloat(
            0f, 1f,
            infiniteRepeatable(
                tween(2000, easing = LinearEasing),
                initialStartOffset = StartOffset(1400)
            ),
            label = "p3"
        )

        Canvas(Modifier.size(180.dp)) {
            val cx = size.width / 2; val cy = size.height / 2
            listOf(pulse1, pulse2, pulse3).forEach { p ->
                val r = p * size.minDimension / 2
                val a = (1f - p).coerceIn(0f, 1f) * 0.4f
                drawCircle(
                    Color.White.copy(alpha = a),
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(2.dp.toPx())
                )
            }
        }

        // 双层旋转光环
        Canvas(Modifier.size(130.dp)) {
            val ring = listOf(
                Color(0xFFFFD600), Color(0xFFFF6D00), Color(0xFFFF1744),
                Color(0xFFD500F9), Color(0xFF2979FF), Color(0xFF00E5FF),
                Color(0xFFFFD600)
            )
            rotate(spinAngle) {
                drawCircle(
                    Brush.sweepGradient(ring),
                    radius = size.minDimension / 2,
                    style = Stroke(4.dp.toPx())
                )
            }
            rotate(-spinAngle * 0.6f) {
                drawCircle(
                    Brush.sweepGradient(ring.reversed()),
                    radius = size.minDimension / 2 - 14.dp.toPx(),
                    style = Stroke(2.dp.toPx())
                )
            }
        }

        // 中心
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✦", fontSize = 48.sp, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(
                "FLIP ME", fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.8f),
                letterSpacing = 8.sp,
                textAlign = TextAlign.Center
            )
        }

        ShimmerOverlay(shimmer)
    }
}

@Preview(showBackground = true)
@Composable
fun FlipCardPreview() {
    G2Theme { FlipCardDemo() }
}
