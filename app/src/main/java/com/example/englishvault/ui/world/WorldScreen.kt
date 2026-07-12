package com.example.englishvault.ui.world

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.englishvault.R
import data.database.entities.UserProfileEntity
import kotlin.math.hypot

/**
 * World map (Phase 7, beta).
 *
 * Minimalist Super Mario Bros-style level selector with a **horizontal**
 * map: the canvas is wider than the screen, so the user scrolls right
 * to discover the next nodes. The scene contains:
 *
 *  - 10 level waypoints laid out along an almost-straight path that
 *    zig-zags subtly inside the grass strip.
 *  - A castle standing on the last waypoint (clear SMB homage).
 *  - 5 clouds floating in the sky band for visual texture.
 *  - A shop reached through a branching dirt path from node 3.
 *  - A HUD with the player's persistent hearts and coins, surfaced
 *    via [WorldViewModel].
 *
 * The current node is selected by tapping the next waypoint only —
 * any other tap is ignored so the experience matches the "linear
 * progress" feel of the original SMB world map. Movement is animated
 * with an [Animatable] so the protagonist slides smoothly between
 * the previous and next waypoint.
 */
@Composable
fun WorldScreen(
    modifier: Modifier = Modifier,
    viewModel: WorldViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    var currentLevel by rememberSaveable { mutableIntStateOf(0) }
    val maxLevel = LevelWaypoints.lastIndex
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        WorldHeader(
            hearts = profile?.hearts ?: UserProfileEntity.DEFAULT_HEARTS,
            coins = profile?.coins ?: 0
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Horizontal scroll host: the inner WorldMap has a fixed width
        // of WORLD_WIDTH_DP, which is wider than any phone screen, so
        // the user must swipe right to see the rest of the path.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WORLD_HEIGHT_DP)
                .clip(RoundedCornerShape(20.dp))
                .horizontalScroll(scroll)
        ) {
            WorldMap(
                currentLevel = currentLevel,
                scrollX = scroll.value,
                onAdvance = { tapped ->
                    if (tapped == currentLevel + 1 && tapped <= maxLevel) {
                        currentLevel = tapped
                    }
                },
                modifier = Modifier
                    .width(WORLD_WIDTH_DP)
                    .fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        StatusLine(currentLevel = currentLevel, maxLevel = maxLevel)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** Width of the world map canvas. Wider than any typical phone so the
 *  user must scroll horizontally to reach the castle. */
private val WORLD_WIDTH_DP: Dp = 2200.dp

/** Visible height of the world map strip. */
private val WORLD_HEIGHT_DP: Dp = 540.dp

/**
 * Header row: title + subtitle on the left, hearts and coins pills in
 * the middle and the BETA badge on the right. Keeping the player
 * counters visually grouped next to the title reinforces that they
 * belong to the world map (not the level they will eventually enter).
 */
@Composable
private fun WorldHeader(hearts: Int, coins: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.world_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(id = R.string.world_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeartsPill(hearts)
            CoinsPill(coins)
            BetaBadge()
        }
    }
}

@Composable
private fun BetaBadge() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary
    ) {
        Text(
            text = stringResource(id = R.string.world_beta),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun HeartsPill(hearts: Int) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFE5E5),
        contentColor = Color(0xFFB71C1C)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\u2665",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HeartRed
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(id = R.string.world_hearts_format, hearts),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CoinsPill(coins: Int) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFF6D6),
        contentColor = Color(0xFF7A5500)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\u25CF",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CoinGold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(id = R.string.world_coins_format, coins),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatusLine(currentLevel: Int, maxLevel: Int) {
    val text = when {
        currentLevel >= maxLevel -> stringResource(id = R.string.world_status_completed)
        currentLevel == 0 -> stringResource(id = R.string.world_status_current)
        else -> stringResource(
            id = R.string.world_level_format,
            currentLevel + 1
        )
    }
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun WorldMap(
    currentLevel: Int,
    scrollX: Int,
    onAdvance: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val nodeRadius = 22.dp
    val pathStroke = 8.dp
    val characterRadius = 18.dp

    // Animatable drives the protagonist's interpolated position along
    // the path. animateTo(...) is launched inside LaunchedEffect so it
    // tracks `currentLevel` reactively.
    val animated = remember { Animatable(0f) }
    LaunchedEffect(currentLevel) {
        animated.animateTo(
            targetValue = currentLevel.toFloat(),
            animationSpec = tween(durationMillis = 450)
        )
    }

    Box(
        modifier = modifier
            .pointerInput(currentLevel) {
                detectTapGestures { tap ->
                    // Account for horizontal scroll so the tap lands on
                    // the absolute coordinate of the waypoint, not the
                    // screen-space one.
                    val widthPx = size.width.toFloat()
                    val heightPx = size.height.toFloat()
                    val nodeR = with(this) { nodeRadius.toPx() }
                    val absTapX = tap.x + scrollX.toFloat()
                    LevelWaypoints.forEachIndexed { index, frac ->
                        val cx = frac.x * widthPx
                        val cy = frac.y * heightPx
                        if (hypot(absTapX - cx, tap.y - cy) <= nodeR) {
                            onAdvance(index)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawWorld(
                currentLevel = currentLevel,
                animatedProgress = animated.value,
                nodeRadiusPx = nodeRadius.toPx(),
                pathStrokePx = pathStroke.toPx(),
                characterRadiusPx = characterRadius.toPx()
            )
        }
    }
}

/**
 * Pure draw routine. Lives separately so [WorldMap] stays small and
 * the math is easy to tweak without touching the gesture/UI logic.
 *
 * Render order (back to front):
 *  1. Sky band
 *  2. Clouds (5 of them)
 *  3. Grass band with subtle stripes
 *  4. Branching shop path (drawn before the main path so the
 *     intersection looks like a fork, not an overlap)
 *  5. Main level path (10 segments)
 *  6. Shop building
 *  7. Castle (at the last waypoint)
 *  8. Level nodes
 *  9. Protagonist interpolating between currentLevel and currentLevel+1
 */
private fun DrawScope.drawWorld(
    currentLevel: Int,
    animatedProgress: Float,
    nodeRadiusPx: Float,
    pathStrokePx: Float,
    characterRadiusPx: Float
) {
    val w = size.width
    val h = size.height

    // 1) Sky strip — a little taller than the original screen-sized
    //    map so the clouds have room to breathe.
    val skyHeight = h * 0.20f
    drawRect(
        color = SkyBlue,
        topLeft = Offset.Zero,
        size = Size(w, skyHeight)
    )

    // 2) Clouds. Each cloud is two overlapping rounded rectangles —
    //    a body and a smaller bump — with a soft shadow rectangle
    //    behind it. Positions are fractions of the canvas so they
    //    stay well-distributed across the entire 2200dp width.
    val cloudPositions = listOf(
        Triple(0.07f, 0.05f, 1.0f),
        Triple(0.22f, 0.10f, 0.85f),
        Triple(0.42f, 0.04f, 1.1f),
        Triple(0.63f, 0.11f, 0.9f),
        Triple(0.83f, 0.06f, 1.0f)
    )
    cloudPositions.forEach { (cx, cy, scale) ->
        drawCloud(cx * w, cy * h, scale)
    }

    // 3) Grass background.
    val grassTop = skyHeight
    drawRect(
        color = GrassGreen,
        topLeft = Offset(0f, grassTop),
        size = Size(w, h - grassTop)
    )

    // 4) Subtle horizontal stripes to fake grass texture.
    val stripeHeight = 6.dp.toPx()
    var stripeY = grassTop + stripeHeight
    while (stripeY < h) {
        drawRect(
            color = GrassGreenDark.copy(alpha = 0.25f),
            topLeft = Offset(0f, stripeY),
            size = Size(w, stripeHeight)
        )
        stripeY += stripeHeight * 4
    }

    // 5) Resolve absolute waypoints in pixels.
    val points = LevelWaypoints.map { Offset(it.x * w, it.y * h) }
    val shop = Offset(ShopWaypoint.x * w, ShopWaypoint.y * h)

    // 6) Branching shop path — drawn from the 3rd node down to the
    //    shop, then back up so it looks like a fork in the road.
    val shopBranchStart = points[2]
    val shopBranchMid = Offset(
        (shopBranchStart.x + shop.x) / 2f,
        (shopBranchStart.y + shop.y) / 2f
    )
    val shopPath = Path().apply {
        moveTo(shopBranchStart.x, shopBranchStart.y)
        lineTo(shopBranchMid.x, shopBranchMid.y)
        lineTo(shop.x, shop.y)
    }
    drawPath(
        path = shopPath,
        color = PathBrown,
        style = Stroke(width = pathStrokePx, cap = StrokeCap.Round)
    )

    // 7) Main level path — 9 segments connecting the 10 waypoints.
    for (i in 0 until points.size - 1) {
        drawLine(
            color = PathBrown,
            start = points[i],
            end = points[i + 1],
            strokeWidth = pathStrokePx,
            cap = StrokeCap.Round
        )
    }

    // 8) Shop building: a small hut with a slanted roof, a door and a
    //    "SHOP" sign above the door. Sized relative to the canvas so
    //    it scales with the world map.
    drawShop(shop)

    // 9) Castle standing on the last waypoint.
    drawCastle(points.last(), nodeRadiusPx * 2.2f)

    // 10) Level nodes.
    points.forEachIndexed { index, p ->
        val (fill, labelColor) = when {
            index < currentLevel -> NodeCleared to Color.White
            index == currentLevel -> NodeCurrent to Color.Black
            else -> NodeLocked to Color.DarkGray
        }
        // Soft drop shadow.
        drawCircle(
            color = Color.Black.copy(alpha = 0.18f),
            radius = nodeRadiusPx + 3.dp.toPx(),
            center = p.copy(y = p.y + 3.dp.toPx())
        )
        drawCircle(
            color = fill,
            radius = nodeRadiusPx,
            center = p
        )
        // Tiny inner dot — minimalist stand-in for the level number.
        drawCircle(
            color = labelColor,
            radius = nodeRadiusPx * 0.32f,
            center = p
        )
    }

    // 11) Protagonist: interpolate between currentLevel and currentLevel+1
    //     based on the animatable's float value (so it slides along the
    //     path during the animation).
    val fromIdx = animatedProgress.toInt().coerceIn(0, points.lastIndex)
    val toIdx = (fromIdx + 1).coerceAtMost(points.lastIndex)
    val t = (animatedProgress - fromIdx).coerceIn(0f, 1f)
    val from = points[fromIdx]
    val to = points[toIdx]
    val cx = from.x + (to.x - from.x) * t
    val cy = from.y + (to.y - from.y) * t - characterRadiusPx * 0.4f

    // Body.
    drawCircle(
        color = CharacterRed,
        radius = characterRadiusPx,
        center = Offset(cx, cy)
    )
    // Eyes.
    val eyeOffset = characterRadiusPx * 0.35f
    val eyeRadius = characterRadiusPx * 0.18f
    val eyeY = cy - eyeOffset * 0.2f
    drawCircle(
        color = CharacterEye,
        radius = eyeRadius,
        center = Offset(cx - eyeOffset, eyeY)
    )
    drawCircle(
        color = CharacterEye,
        radius = eyeRadius,
        center = Offset(cx + eyeOffset, eyeY)
    )
}

/**
 * Draws a single cloud composed of a soft shadow, a flat-bottom body
 * and a smaller bump on top. Coordinates are absolute pixels.
 */
private fun DrawScope.drawCloud(cx: Float, cy: Float, scale: Float) {
    val w = 90.dp.toPx() * scale
    val h = 36.dp.toPx() * scale
    val bumpW = 60.dp.toPx() * scale
    val bumpH = 28.dp.toPx() * scale
    val shadowOffset = 4.dp.toPx()

    val bodyLeft = cx - w / 2f
    val bodyTop = cy - h / 2f
    val cornerRadius = h / 2f
    val bumpLeft = cx - bumpW / 2f
    val bumpTop = bodyTop - bumpH * 0.55f

    // Shadow.
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.08f),
        topLeft = Offset(bodyLeft, bodyTop + shadowOffset),
        size = Size(w, h),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
    )
    // Body.
    drawRoundRect(
        color = CloudWhite,
        topLeft = Offset(bodyLeft, bodyTop),
        size = Size(w, h),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
    )
    // Bump on top.
    drawRoundRect(
        color = CloudWhite,
        topLeft = Offset(bumpLeft, bumpTop),
        size = Size(bumpW, bumpH),
        cornerRadius = CornerRadius(bumpH / 2f, bumpH / 2f)
    )
}

/**
 * Draws the in-world shop: a brown rectangular body, a slanted red
 * roof approximated by a triangle, a dark door and a small white
 * "SHOP" sign.
 */
private fun DrawScope.drawShop(center: Offset) {
    val bodyW = 70.dp.toPx()
    val bodyH = 70.dp.toPx()
    val roofOverhang = 10.dp.toPx()
    val bodyLeft = center.x - bodyW / 2f
    val bodyTop = center.y - bodyH / 2f

    // Body.
    drawRect(
        color = ShopBrown,
        topLeft = Offset(bodyLeft, bodyTop),
        size = Size(bodyW, bodyH)
    )

    // Slanted roof (drawn as a triangle path).
    val roofPath = Path().apply {
        moveTo(bodyLeft - roofOverhang, bodyTop)
        lineTo(bodyLeft + bodyW + roofOverhang, bodyTop)
        lineTo(center.x, bodyTop - 28.dp.toPx())
        close()
    }
    drawPath(path = roofPath, color = ShopRoof)

    // Door.
    val doorW = 18.dp.toPx()
    val doorH = 28.dp.toPx()
    drawRect(
        color = Color(0xFF3E2723),
        topLeft = Offset(center.x - doorW / 2f, bodyTop + bodyH - doorH),
        size = Size(doorW, doorH)
    )

    // Sign board above the door.
    val signW = 46.dp.toPx()
    val signH = 14.dp.toPx()
    drawRect(
        color = Color.White,
        topLeft = Offset(center.x - signW / 2f, bodyTop + 8.dp.toPx()),
        size = Size(signW, signH)
    )
    // Approximate "SHOP" lettering with 4 short rectangles (placeholder
    // visual; a real drawable would go here in a future phase).
    val letterW = 4.dp.toPx()
    val letterH = 8.dp.toPx()
    val letterGap = 6.dp.toPx()
    val totalLettersW = letterW * 4 + letterGap * 3
    val letterStartX = center.x - totalLettersW / 2f
    val letterY = bodyTop + 11.dp.toPx()
    for (i in 0 until 4) {
        drawRect(
            color = Color(0xFF3E2723),
            topLeft = Offset(letterStartX + i * (letterW + letterGap), letterY),
            size = Size(letterW, letterH)
        )
    }
}

/**
 * Draws the castle at the end of the path: two grey towers with red
 * conical caps, a connecting wall, a dark entrance and a yellow flag
 * on the taller tower — pure visual, no interaction.
 */
private fun DrawScope.drawCastle(anchor: Offset, scale: Float) {
    val towerW = 36.dp.toPx()
    val towerH = 90.dp.toPx()
    val bigTowerH = 110.dp.toPx()
    val wallH = 60.dp.toPx()
    val gap = 8.dp.toPx()
    val coneH = 22.dp.toPx()
    val flagW = 18.dp.toPx()
    val flagH = 12.dp.toPx()

    val baseY = anchor.y + scale * 0.1f
    val leftTowerX = anchor.x - towerW - gap / 2f
    val rightTowerX = anchor.x + gap / 2f
    val bigTowerX = anchor.x + towerW + gap / 2f

    // Big tower (taller, hosts the flag).
    val bigTowerTop = baseY - bigTowerH
    drawRect(
        color = CastleStone,
        topLeft = Offset(bigTowerX, bigTowerTop),
        size = Size(towerW, bigTowerH)
    )
    // Big tower cone.
    val bigConePath = Path().apply {
        moveTo(bigTowerX, bigTowerTop - coneH)
        lineTo(bigTowerX + towerW, bigTowerTop - coneH)
        lineTo(bigTowerX + towerW / 2f, bigTowerTop)
        close()
    }
    drawPath(path = bigConePath, color = CastleRoof)

    // Connecting wall between the two shorter towers.
    val wallTop = baseY - wallH
    drawRect(
        color = CastleStone,
        topLeft = Offset(leftTowerX + towerW, wallTop),
        size = Size(bigTowerX - (leftTowerX + towerW), wallH)
    )
    // Wall crenellations.
    val crenelW = 10.dp.toPx()
    val crenelH = 8.dp.toPx()
    val wallLeft = leftTowerX + towerW
    val wallRight = bigTowerX
    var x = wallLeft
    while (x + crenelW <= wallRight) {
        drawRect(
            color = CastleStone,
            topLeft = Offset(x, wallTop - crenelH),
            size = Size(crenelW, crenelH)
        )
        x += crenelW * 2
    }

    // Left tower.
    val leftTowerTop = baseY - towerH
    drawRect(
        color = CastleStone,
        topLeft = Offset(leftTowerX, leftTowerTop),
        size = Size(towerW, towerH)
    )
    // Left tower cone.
    val leftConePath = Path().apply {
        moveTo(leftTowerX, leftTowerTop - coneH)
        lineTo(leftTowerX + towerW, leftTowerTop - coneH)
        lineTo(leftTowerX + towerW / 2f, leftTowerTop)
        close()
    }
    drawPath(path = leftConePath, color = CastleRoof)

    // Entrance (arched door approximated by a tall rounded rectangle
    // centered between the two shorter towers).
    val doorW = 16.dp.toPx()
    val doorH = 32.dp.toPx()
    val doorLeft = (leftTowerX + towerW + bigTowerX) / 2f - doorW / 2f
    val doorTop = baseY - doorH
    drawRoundRect(
        color = Color(0xFF1A1A1A),
        topLeft = Offset(doorLeft, doorTop),
        size = Size(doorW, doorH),
        cornerRadius = CornerRadius(doorW / 2f, doorW / 2f)
    )

    // Flag pole and banner on top of the big tower.
    val poleBaseX = bigTowerX + towerW / 2f
    val poleBaseY = bigTowerTop - coneH
    val poleTopY = poleBaseY - 22.dp.toPx()
    drawLine(
        color = Color(0xFF3E2723),
        start = Offset(poleBaseX, poleBaseY),
        end = Offset(poleBaseX, poleTopY),
        strokeWidth = 3.dp.toPx()
    )
    drawRect(
        color = FlagYellow,
        topLeft = Offset(poleBaseX, poleTopY),
        size = Size(flagW, flagH)
    )
}

// region: Map palette (independent of Material theme on purpose — the
// SMB look needs fixed grass/sky/path colors regardless of dark mode).
private val SkyBlue = Color(0xFF7EC8E3)
private val CloudWhite = Color(0xFFFAFAFA)
private val GrassGreen = Color(0xFF7BC74D)
private val GrassGreenDark = Color(0xFF5BAE3A)
private val PathBrown = Color(0xFF8B5A2B)
private val NodeCleared = Color(0xFF58CC02)
private val NodeLocked = Color(0xFFB7B7B7)
private val NodeCurrent = Color(0xFFFFC107)
private val CharacterRed = Color(0xFFE53935)
private val CharacterEye = Color(0xFFFFFFFF)
private val HeartRed = Color(0xFFE53935)
private val CoinGold = Color(0xFFFFC107)
private val ShopBrown = Color(0xFFA0522D)
private val ShopRoof = Color(0xFFB71C1C)
private val CastleStone = Color(0xFF9E9E9E)
private val CastleRoof = Color(0xFFB71C1C)
private val FlagYellow = Color(0xFFFFD600)
// endregion

// region: Path waypoints (fractions of the canvas). 10 nodes laid out
// along an almost-straight horizontal path with subtle zig-zag inside
// the grass strip. Y values stay within [0.45, 0.65] so the path never
// touches the sky band or the shop branch.
private val LevelWaypoints: List<Offset> = listOf(
    Offset(0.06f, 0.55f),
    Offset(0.16f, 0.50f),
    Offset(0.26f, 0.58f),
    Offset(0.36f, 0.52f),
    Offset(0.46f, 0.58f),
    Offset(0.56f, 0.52f),
    Offset(0.66f, 0.58f),
    Offset(0.76f, 0.52f),
    Offset(0.86f, 0.58f),
    Offset(0.94f, 0.50f)
)
// Shop branching waypoint (single node, off the main path).
private val ShopWaypoint: Offset = Offset(0.30f, 0.85f)
// endregion
