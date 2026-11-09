package id.tirtawijata.crumina.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import id.tirtawijata.crumina.R
import id.tirtawijata.crumina.data.Categorize
import id.tirtawijata.crumina.data.Repo
import id.tirtawijata.crumina.data.Txn

// ---- colours that the web app assigns per row type (TXCOL, COLS, CATCOL in app/app.js) ----

private fun grad(a: Long, b: Long) = Brush.linearGradient(listOf(Color(a), Color(b)), Offset.Zero, Offset.Infinite)

val SpendGrad = grad(0xFFFF6F91, 0xFFF0476A)
val IncomeGrad = grad(0xFF22D3B6, 0xFF0FA98E)
val NeutralGrad = grad(0xFF7C8DB5, 0xFF4E5F86)
val NavyGrad = grad(0xFF16336E, 0xFF0A1B3D)

fun accountGrad(type: String?): Brush = when (type) {
    "cash" -> IncomeGrad
    "savings", "bank" -> NavyGrad
    "ewallet" -> grad(0xFFFFD27A, 0xFFF6B73C)
    "deposit", "credit_card" -> NeutralGrad
    else -> grad(0xFF8AA0C6, 0xFF5B6E96)
}

fun txGrad(cls: String?): Brush = when (cls) {
    "income", "transfer_in" -> IncomeGrad
    "transfer", "card_payment" -> NeutralGrad
    "topup" -> grad(0xFFFFD27A, 0xFFF6B73C)
    else -> SpendGrad
}

private val catColors = mapOf(
    "Food & Dining" to 0xFF16336E, "Groceries" to 0xFF13B97A, "Transport" to 0xFF2F86D6, "Shopping" to 0xFF7C5CFF,
    "Travel" to 0xFF9B5DE5, "Bills & Subs" to 0xFFF6B73C, "Health" to 0xFFF0476A, "Entertainment" to 0xFF1FB8A6,
    "Income" to 0xFF0FA98E, "Other" to 0xFF8AA0C6
)
fun catColor(cat: String): Color = Color(catColors[cat] ?: 0xFF8AA0C6)

// ---- surfaces ----

/** Page background: flat bg plus the two soft radial glows the web app paints on <body>. */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    val cr = LocalCr.current
    Box(
        Modifier.fillMaxSize().background(cr.bg).drawBehind {
            if (cr.contrast) return@drawBehind // plain white in high contrast
            drawRect(Brush.radialGradient(listOf(Color(0xFF16C7A8).copy(alpha = 0.10f), Color.Transparent),
                center = Offset(size.width * 0.88f, -size.height * 0.04f), radius = size.width * 1.2f))
            drawRect(Brush.radialGradient(listOf(Color(0xFF2D5BD0).copy(alpha = if (cr.dark) 0.16f else 0.10f), Color.Transparent),
                center = Offset(-size.width * 0.08f, size.height * 0.03f), radius = size.width * 1.1f))
        }
    ) { content() }
}

private fun Modifier.softShadow(shape: Shape, dark: Boolean) =
    shadow(10.dp, shape, ambientColor = if (dark) Color.Black else Navy.copy(alpha = 0.25f), spotColor = if (dark) Color.Black else Navy.copy(alpha = 0.25f))

/** White (or dark navy) card with 24dp corners, like `.card` on the web. */
@Composable
fun CrCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cr = LocalCr.current
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier.fillMaxWidth().softShadow(shape, cr.dark).clip(shape).background(cr.card)
            .then(if (cr.contrast) Modifier.border(1.5.dp, cr.line, shape) else Modifier)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        if (title != null) {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = cr.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                if (action != null && onAction != null) {
                    Text(action, color = cr.teal, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(4.dp))
                }
            }
        }
        content()
    }
}

/** Navy gradient hero card (`.hero`). */
@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val cr = LocalCr.current
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier.fillMaxWidth().softShadow(shape, cr.dark).clip(shape)
            .background(Brush.linearGradient(0f to Navy2, 0.72f to Navy, start = Offset(0f, 0f), end = Offset(900f, 1300f)))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(horizontal = 22.dp, vertical = 24.dp),
        content = content
    )
}

@Composable
fun HeroLabel(text: String) = Text(
    text.uppercase(), color = Color.White.copy(alpha = 0.66f), fontSize = 11.sp,
    fontWeight = FontWeight.SemiBold, letterSpacing = 0.14.em
)

@Composable
fun HeroValue(text: String) = Text(
    // Step down for long amounts so they fit a narrow phone instead of being clipped.
    text, color = Color.White, fontSize = if (text.length > 16) 24.sp else if (text.length > 13) 27.sp else 30.sp, fontWeight = FontWeight.ExtraBold,
    letterSpacing = (-1.2).sp, modifier = Modifier.padding(top = 8.dp), maxLines = 1
)

/** Translucent chip inside the hero (`.bdchip`). */
@Composable
fun RowScope.HeroChip(label: String, value: String) = HeroChipBox(label, value, Modifier.weight(1f))

@Composable
fun HeroChipBox(label: String, value: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Column(modifier.clip(shape).background(Color.White.copy(alpha = 0.08f)).border(1.dp, Color.White.copy(alpha = 0.12f), shape).padding(horizontal = 13.dp, vertical = 10.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.62f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(3.dp))
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** White quick-action button inside the hero (`.qadd button`). */
@Composable
fun RowScope.HeroAction(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.weight(1f).clip(shape).background(Color.White.copy(alpha = 0.96f)).clickable(onClick = onClick).padding(vertical = 13.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Navy, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
}

// ---- headers ----

/** Logo + name on the left, avatar on the right (`.mtop`). */
@Composable
fun TopBar(initial: String) {
    val cr = LocalCr.current
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_logo_mark), contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(9.dp))
        Text("Crumina", color = cr.ink, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, modifier = Modifier.weight(1f))
        Avatar(initial, 38)
    }
}

@Composable
fun Avatar(initial: String, sizeDp: Int) {
    Box(Modifier.size(sizeDp.dp).clip(CircleShape).background(NavyGrad), contentAlignment = Alignment.Center) {
        Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (sizeDp * 0.4f).sp)
    }
}

/** Page title + subtitle, with an optional trailing action (`.head`). */
@Composable
fun PageHeader(title: String, sub: String? = null, trailing: (@Composable () -> Unit)? = null) {
    val cr = LocalCr.current
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = cr.ink, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, letterSpacing = (-0.6).sp)
            if (sub != null) Text(sub, color = cr.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
        }
        trailing?.invoke()
    }
}

/** Uppercase section label above settings cards. */
@Composable
fun SectionLabel(text: String) {
    val cr = LocalCr.current
    Text(text.uppercase(), color = cr.muted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.1.em,
        modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 10.dp))
}

// ---- buttons and chips ----

/** Filled pill (`.btn`), navy in light mode and blue in dark mode. */
@Composable
fun PillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val cr = LocalCr.current
    Box(
        modifier.softShadow(CircleShape, cr.dark).clip(CircleShape).background(cr.btn.copy(alpha = if (enabled) 1f else 0.6f))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1) }
}

/** Outlined pill on the card colour (`.btn.ghost`). */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val cr = LocalCr.current
    Box(
        modifier.clip(CircleShape).background(cr.card).border(1.dp, cr.line, CircleShape)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = cr.ink.copy(alpha = if (enabled) 1f else 0.5f), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1) }
}

/** Range / filter chip (`.chips button`). */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val cr = LocalCr.current
    Box(
        Modifier.clip(CircleShape).background(if (selected) cr.btn else cr.card)
            .border(1.dp, if (selected) cr.btn else cr.line, CircleShape)
            .clickable(onClick = onClick).padding(horizontal = 15.dp, vertical = 8.dp)
    ) { Text(text, color = if (selected) Color.White else cr.muted, fontWeight = FontWeight.Bold, fontSize = 12.5.sp) }
}

// ---- rows ----

/** Rounded gradient tile with an initial (`.ic`). */
@Composable
fun IconTile(text: String, brush: Brush) {
    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(brush), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1)
    }
}

/** Divider-separated list row (`.row2`): tile, name + detail, trailing content. */
@Composable
fun ListRow(
    first: Boolean,
    tile: @Composable () -> Unit,
    title: String,
    detail: String?,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit
) {
    val cr = LocalCr.current
    Column {
        if (!first) Box(Modifier.fillMaxWidth().height(1.dp).background(cr.line))
        Row(
            Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tile()
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = cr.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!detail.isNullOrBlank()) Text(detail, color = cr.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/** Locale for dates and month names, following the app language (Okt, Des in Indonesian). */
fun uiLocale(): java.util.Locale = if (Repo.lang == "id") java.util.Locale("id") else java.util.Locale.US

fun fmtShortDate(date: String?): String? {
    val d = date?.trim()?.take(10) ?: return null
    return runCatching { java.time.LocalDate.parse(d).format(java.time.format.DateTimeFormatter.ofPattern("d MMM", uiLocale())) }.getOrNull() ?: d
}

/** A transaction row as the web app draws it: "Account · 8 Nov · Category", signed coloured amount. */
@Composable
fun TxRow(t: Txn, first: Boolean) {
    val cr = LocalCr.current
    val name = t.merchant ?: t.desc ?: "Transaction"
    val income = t.cls == "income" || t.cls == "transfer_in" || (t.cls == null && t.amount > 0)
    val spend = t.cls == null || t.cls == "spend" || t.cls == "fee"
    val label = if (income) "Income" else if (spend) Categorize.category(t.merchant) else t.cls!!.replace('_', ' ').replaceFirstChar { it.uppercase() }
    val detail = listOfNotNull(t.account ?: "Card", fmtShortDate(t.date), label).joinToString(" · ")
    ListRow(first, { IconTile(name.take(1).uppercase(), txGrad(t.cls)) }, name, detail) {
        val amt = Repo.inMain(kotlin.math.abs(t.amount), "IDR", signed = false)
        Text(
            (if (t.amount > 0) "+" else "−") + amt,
            color = if (income) cr.green else if (spend) cr.red else cr.muted,
            fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1
        )
    }
}

/** Small "×" remove control used in account and holding rows. */
@Composable
fun RemoveX(onClick: () -> Unit) {
    val cr = LocalCr.current
    Box(Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text("×", color = cr.muted.copy(alpha = 0.7f), fontSize = 18.sp)
    }
}

/** Grey stat tile (`.stat`): uppercase label, big number. */
@Composable
fun RowScope.StatTile(label: String, value: String) {
    val cr = LocalCr.current
    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(cr.bg).padding(horizontal = 14.dp, vertical = 13.dp)) {
        Text(label.uppercase(), color = cr.muted, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.06.em, maxLines = 2)
        Spacer(Modifier.height(5.dp))
        Text(value, color = cr.ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp, maxLines = 1)
    }
}

/** Thin rounded progress bar (`.barwrap`). */
@Composable
fun Bar(fraction: Float, color: Color) = BarBrush(fraction, SolidColor(color))

@Composable
fun BarBrush(fraction: Float, brush: Brush) {
    val cr = LocalCr.current
    Box(Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(6.dp)).background(cr.line)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(9.dp).clip(RoundedCornerShape(6.dp)).background(brush))
    }
}

@Suppress("ModifierParameter") // text first, then size: reads better at call sites
@Composable
fun Muted(text: String, size: TextUnit = 13.sp, modifier: Modifier = Modifier) =
    Text(text, color = LocalCr.current.muted, fontSize = size, modifier = modifier)

/** Centered empty-state text (`.empty`). */
@Composable
fun EmptyText(text: String) =
    Box(Modifier.fillMaxWidth().padding(vertical = 26.dp, horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Text(text, color = LocalCr.current.muted, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }

@Suppress("ModifierParameter") // text first, then size: reads better at call sites
@Composable
fun Ink(text: String, size: TextUnit = 14.sp, weight: FontWeight = FontWeight.Normal, modifier: Modifier = Modifier, color: Color? = null) =
    Text(text, color = color ?: LocalCr.current.ink, fontSize = size, fontWeight = weight, modifier = modifier)


// ---- dialogs and form fields ----

/** AlertDialog on the card colour with 24dp corners and a bold title, like the web sheets. */
@Composable
fun CrDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null
) {
    val cr = LocalCr.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        title = title?.let { t -> { androidx.compose.material3.ProvideTextStyle(androidx.compose.ui.text.TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)) { t() } } },
        text = text,
        shape = RoundedCornerShape(24.dp),
        containerColor = cr.card,
        titleContentColor = cr.ink,
        textContentColor = cr.ink,
        tonalElevation = 0.dp
    )
}

/** Rounded outlined text field with visible borders on white cards. */
@Composable
fun CrField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
) {
    val cr = LocalCr.current
    androidx.compose.material3.OutlinedTextField(
        value = value, onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        label = label, singleLine = singleLine, visualTransformation = visualTransformation,
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = cr.muted.copy(alpha = 0.35f), focusedBorderColor = cr.teal,
            focusedLabelColor = cr.teal, unfocusedLabelColor = cr.muted, cursorColor = cr.ink,
            focusedTextColor = cr.ink, unfocusedTextColor = cr.ink
        )
    )
}

/** Status bar icon colour for the current screen: dark icons on light pages, light on the navy login. */
@Composable
fun StatusBarIcons(lightBackground: Boolean) {
    val view = androidx.compose.ui.platform.LocalView.current
    if (view.isInEditMode) return
    androidx.compose.runtime.SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = lightBackground
    }
}
