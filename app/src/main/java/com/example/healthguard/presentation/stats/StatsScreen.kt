package com.example.healthguard.presentation.stats

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthguard.viewmodel.StepUiState
import com.example.healthguard.viewmodel.StepViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── Enums ───────────────────────────────────────────────────────────────────

enum class RangeOption(val label: String, val days: Int) {
    Today("Today", 1),
    Week("7 days", 7),
    Month30("30 days", 30),
    Month90("3 months", 90),
    Month180("6 months", 180)
}

enum class GoalFilter(val label: String) {
    All("All"),
    Reached("✓ Goal"),
    Missed("✗ Missed")
}

enum class SortMode(val label: String) {
    Newest("Newest"),
    Oldest("Oldest"),
    Most("Most steps"),
    Least("Least steps")
}

enum class ChartType(val label: String) {
    Bar("Bar"),
    Line("Line"),
    Heat("Heat")
}

// ─── Fixed accent colours (brand colours, same in light & dark) ──────────────

private val AccentTeal   = Color(0xFF2EC4A6)
private val AccentAmber  = Color(0xFFF5A623)
private val AccentRed    = Color(0xFFE8504A)
private val AccentTealSoft  = Color(0x1F2EC4A6)
private val AccentAmberSoft = Color(0x1AF5A623)
private val AccentRedSoft   = Color(0x1AE8504A)

// ─── Data helpers ─────────────────────────────────────────────────────────────

data class DayEntry(val date: String, val steps: Int)

private fun Map<String, Int>.toEntries(rangeDays: Int): List<DayEntry> {
    val cutoff = LocalDate.now().minusDays((rangeDays - 1).toLong()).toString()
    return entries.filter { it.key >= cutoff }
        .map { DayEntry(it.key, it.value) }
        .sortedBy { it.date }
}

private fun List<DayEntry>.applyFilters(goalFilter: GoalFilter, activeOnly: Boolean, goal: Int): List<DayEntry> {
    var r = this
    if (activeOnly) r = r.filter { it.steps > 0 }
    r = when (goalFilter) {
        GoalFilter.Reached -> r.filter { it.steps >= goal }
        GoalFilter.Missed  -> r.filter { it.steps in 1 until goal }
        GoalFilter.All     -> r
    }
    return r
}

private fun List<DayEntry>.applySort(sort: SortMode): List<DayEntry> = when (sort) {
    SortMode.Newest -> sortedByDescending { it.date }
    SortMode.Oldest -> sortedBy { it.date }
    SortMode.Most   -> sortedByDescending { it.steps }
    SortMode.Least  -> sortedBy { it.steps }
}

private fun formatDate(iso: String): String = try {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH))
} catch (_: Exception) { iso }

private fun Int.toShortString(): String =
    if (this >= 1000) String.format(Locale.US, "%.1fk", this / 1000.0) else this.toString()

private fun Int.toLocaleString(): String = String.format(Locale.US, "%,d", this)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun StatsScreen(viewModel: StepViewModel) {
    val state by viewModel.stepState.collectAsState()

    var rangeOption by remember { mutableStateOf(RangeOption.Week) }
    var goalFilter  by remember { mutableStateOf(GoalFilter.All) }
    var activeOnly  by remember { mutableStateOf(false) }
    var sortMode    by remember { mutableStateOf(SortMode.Newest) }
    var chartType   by remember { mutableStateOf(ChartType.Bar) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val s = state) {
            is StepUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Loading…",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                }
            }

            is StepUiState.Success -> {
                val allEntries by remember(s.fullHistory, rangeOption) {
                    derivedStateOf { s.fullHistory.toEntries(rangeOption.days) }
                }
                val filtered by remember(allEntries, goalFilter, activeOnly, s.dailyTarget) {
                    derivedStateOf { allEntries.applyFilters(goalFilter, activeOnly, s.dailyTarget) }
                }
                val sorted by remember(filtered, sortMode) {
                    derivedStateOf { filtered.applySort(sortMode) }
                }

                val total      = filtered.sumOf { it.steps }
                val activeDays = filtered.count { it.steps > 0 }
                val avg        = if (activeDays > 0) total / activeDays else 0
                val goalDays   = filtered.count { it.steps >= s.dailyTarget }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        ActivityHeader(
                            rangeLabel = if (rangeOption == RangeOption.Today) "Today"
                            else "Last ${rangeOption.days} days",
                            streak = s.currentStreak
                        )
                    }
                    item { RangeChips(selected = rangeOption, onSelect = { rangeOption = it }) }
                    item { SummaryCards(total, avg, goalDays, filtered.size) }
                    item {
                        FilterBar(
                            goalFilter = goalFilter,
                            activeOnly = activeOnly,
                            onGoalFilter = { goalFilter = it },
                            onActiveOnly = { activeOnly = !activeOnly }
                        )
                    }
                    item {
                        ChartSection(
                            data = filtered,
                            goal = s.dailyTarget,
                            avg = avg,
                            chartType = chartType,
                            onChartType = { chartType = it }
                        )
                    }
                    item {
                        ListHeader(count = sorted.size, sortMode = sortMode, onSort = {
                            val modes = SortMode.values()
                            sortMode = modes[(sortMode.ordinal + 1) % modes.size]
                        })
                    }
                    itemsIndexed(sorted) { _, entry ->
                        DayItem(entry = entry, goal = s.dailyTarget)
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ─── Header ──────────────────────────────────────────────────────────────────

@Composable
fun ActivityHeader(rangeLabel: String, streak: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Activity", fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground, letterSpacing = (-0.5).sp)
            Text(rangeLabel, fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp))
        }
        if (streak > 0) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    .background(AccentTealSoft)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("🔥 $streak day streak", fontSize = 12.sp,
                    fontWeight = FontWeight.Medium, color = AccentTeal)
            }
        }
    }
}

// ─── Range chips ─────────────────────────────────────────────────────────────

@Composable
fun RangeChips(selected: RangeOption, onSelect: (RangeOption) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RangeOption.values().forEach { option ->
            val isActive = option == selected
            val bg by animateColorAsState(
                if (isActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
                tween(150), label = "cBg"
            )
            val tc by animateColorAsState(
                if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                tween(150), label = "cTc"
            )
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    .background(bg).clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(option.label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = tc)
            }
        }
    }
}

// ─── Summary cards ───────────────────────────────────────────────────────────

@Composable
fun SummaryCards(total: Int, avg: Int, goalDays: Int, totalDays: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryCard("Total",     total.toShortString(), AccentTeal,                              Modifier.weight(1f))
        SummaryCard("Daily avg", avg.toShortString(),   AccentAmber,                             Modifier.weight(1f))
        SummaryCard("Goal days", "$goalDays/$totalDays",MaterialTheme.colorScheme.onSurface,     Modifier.weight(1f))
    }
}

@Composable
fun SummaryCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                color = valueColor, letterSpacing = (-0.5).sp)
            Text(label.uppercase(), fontSize = 10.sp, letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 3.dp))
        }
    }
}

// ─── Filter bar ──────────────────────────────────────────────────────────────

@Composable
fun FilterBar(
    goalFilter: GoalFilter,
    activeOnly: Boolean,
    onGoalFilter: (GoalFilter) -> Unit,
    onActiveOnly: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("SHOW", fontSize = 10.sp, letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))

        GoalFilter.values().forEach { f ->
            val isActive = f == goalFilter
            val bg by animateColorAsState(
                if (isActive) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                tween(150), label = "fBg"
            )
            val tc by animateColorAsState(
                if (isActive) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                tween(150), label = "fTc"
            )
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(bg).clickable { onGoalFilter(f) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(f.label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = tc)
            }
        }

        Spacer(Modifier.weight(1f))

        val toggleBg by animateColorAsState(
            if (activeOnly) AccentTealSoft else Color.Transparent, tween(150), label = "tBg")
        val toggleTc by animateColorAsState(
            if (activeOnly) AccentTeal
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), tween(150), label = "tTc")
        Box(
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                .background(toggleBg).clickable { onActiveOnly() }
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text("Active only", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = toggleTc)
        }
    }
}

// ─── Chart section ───────────────────────────────────────────────────────────

@Composable
fun ChartSection(data: List<DayEntry>, goal: Int, avg: Int, chartType: ChartType, onChartType: (ChartType) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Steps per day", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ChartType.values().forEach { type ->
                    val isActive = type == chartType
                    val bg by animateColorAsState(
                        if (isActive) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                        tween(150), label = "ctBg")
                    val tc by animateColorAsState(
                        if (isActive) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        tween(150), label = "ctTc")
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .background(bg).clickable { onChartType(type) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(type.label, fontSize = 11.sp, color = tc)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface).padding(12.dp)) {
            when (chartType) {
                ChartType.Bar  -> BarChart(data, goal, avg)
                ChartType.Line -> LineChart(data, goal, avg)
                ChartType.Heat -> HeatmapChart(data, goal)
            }
        }
    }
}

// ─── Bar chart ───────────────────────────────────────────────────────────────

@Composable
fun BarChart(data: List<DayEntry>, goal: Int, avg: Int) {
    if (data.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
            Text("No data", color = MaterialTheme.colorScheme.onSurface.copy(0.3f), fontSize = 13.sp)
        }; return
    }
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val w = size.width; val h = size.height
        val padT = 16f; val padB = 20f; val cH = h - padT - padB
        val maxV = maxOf(goal * 1.2f, data.maxOf { it.steps.toFloat() })
        fun y(s: Int) = padT + cH - (s / maxV) * cH

        drawLine(AccentTeal.copy(.25f), Offset(0f, y(goal)), Offset(w, y(goal)),
            1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        if (avg > 0) drawLine(AccentAmber.copy(.5f), Offset(0f, y(avg)), Offset(w, y(avg)),
            1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))

        val n = data.size; val slotW = w / n; val barW = (slotW * .55f).coerceAtLeast(2f)
        data.forEachIndexed { i, e ->
            val x = i * slotW + (slotW - barW) / 2f
            val top = y(e.steps.coerceAtLeast(0)); val bH = y(0) - top
            if (bH <= 0f) return@forEachIndexed
            val brush = when {
                e.steps == 0    -> Brush.verticalGradient(listOf(Color.Gray.copy(.3f), Color.Gray.copy(.1f)), top, top + bH)
                e.steps >= goal -> Brush.verticalGradient(listOf(AccentTeal.copy(.9f), AccentTeal.copy(.3f)), top, top + bH)
                else            -> Brush.verticalGradient(listOf(AccentAmber.copy(.8f), AccentAmber.copy(.2f)), top, top + bH)
            }
            drawRoundRect(brush, Offset(x, top), Size(barW, bH),
                CornerRadius((barW / 2).coerceAtMost(4.dp.toPx())))
        }
    }
}

// ─── Line chart ──────────────────────────────────────────────────────────────

@Composable
fun LineChart(data: List<DayEntry>, goal: Int, avg: Int) {
    if (data.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
            Text("No data", color = MaterialTheme.colorScheme.onSurface.copy(0.3f), fontSize = 13.sp)
        }; return
    }
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val w = size.width; val h = size.height
        val padT = 16f; val padB = 20f; val cH = h - padT - padB
        val maxV = maxOf(goal * 1.2f, data.maxOf { it.steps.toFloat() })
        val n = data.size; val slotW = w / n
        fun y(s: Int) = padT + cH - (s / maxV) * cH
        fun x(i: Int) = i * slotW + slotW / 2f

        drawLine(AccentTeal.copy(.25f), Offset(0f, y(goal)), Offset(w, y(goal)),
            1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        if (avg > 0) drawLine(AccentAmber.copy(.5f), Offset(0f, y(avg)), Offset(w, y(avg)),
            1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))

        val fill = Path().apply {
            moveTo(x(0), y(data[0].steps))
            data.forEachIndexed { i, e -> lineTo(x(i), y(e.steps)) }
            lineTo(x(n - 1), h - padB); lineTo(x(0), h - padB); close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(AccentTeal.copy(.18f), AccentTeal.copy(0f)), padT, h - padB))

        val line = Path().apply {
            moveTo(x(0), y(data[0].steps))
            data.forEachIndexed { i, e -> lineTo(x(i), y(e.steps)) }
        }
        drawPath(line, AccentTeal.copy(.8f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))

        data.forEachIndexed { i, e ->
            drawCircle(
                color = if (e.steps >= goal) AccentTeal else AccentAmber.copy(.8f),
                radius = if (n <= 14) 3.dp.toPx() else 2.dp.toPx(),
                center = Offset(x(i), y(e.steps))
            )
        }
    }
}

// ─── Heatmap ─────────────────────────────────────────────────────────────────

@Composable
fun HeatmapChart(data: List<DayEntry>, goal: Int) {
    if (data.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
            Text("No data", color = MaterialTheme.colorScheme.onSurface.copy(0.3f), fontSize = 13.sp)
        }; return
    }
    val maxSteps = data.maxOf { it.steps }.coerceAtLeast(1)
    val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        data.chunked(10).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { e ->
                    val ratio = e.steps.toFloat() / maxSteps
                    val alpha = if (e.steps == 0) 0.06f else 0.15f + ratio * 0.85f
                    val color = when {
                        e.steps >= goal -> AccentTeal.copy(alpha)
                        e.steps > 0     -> AccentAmber.copy(alpha)
                        else            -> emptyColor
                    }
                    Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(4.dp)).background(color))
                }
                repeat(10 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ─── List header ─────────────────────────────────────────────────────────────

@Composable
fun ListHeader(count: Int, sortMode: SortMode, onSort: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$count day${if (count != 1) "s" else ""}", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onSort() }
            .padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text("Sort: ${sortMode.label} ↕", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
        }
    }
}

// ─── Day item ────────────────────────────────────────────────────────────────

@Composable
fun DayItem(entry: DayEntry, goal: Int) {
    val pct     = if (goal > 0) (entry.steps * 100 / goal).coerceIn(0, 100) else 0
    val reached = entry.steps >= goal
    val empty   = entry.steps == 0

    val barColor = when {
        empty   -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        reached -> AccentTeal
        else    -> AccentAmber
    }
    val stepsColor by animateColorAsState(barColor, tween(300), label = "sc")

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            val fill by animateFloatAsState(pct / 100f, tween(400), label = "fill")
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxSize(fill)
                    .clip(RoundedCornerShape(2.dp)).background(barColor)
                    .align(Alignment.BottomCenter)
            )
        }

        Column(Modifier.weight(1f)) {
            Text(formatDate(entry.date), fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(entry.steps.toLocaleString(), fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold, color = stepsColor, letterSpacing = (-0.5).sp)
            Text(if (empty) "no data" else "$pct% of goal", fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }

        val (badgeText, badgeBg, badgeFg) = when {
            empty   -> Triple("rest",     MaterialTheme.colorScheme.onSurface.copy(.04f), MaterialTheme.colorScheme.onSurface.copy(.3f))
            reached -> Triple("✓ goal",   AccentTealSoft, AccentTeal)
            else    -> Triple("✗ missed", AccentRedSoft,  AccentRed)
        }
        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(badgeBg)
            .padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = badgeFg)
        }
    }
}