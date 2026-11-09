package id.tirtawijata.crumina.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.tirtawijata.crumina.R
import id.tirtawijata.crumina.data.Repo

/** Sign-in screen, matching app/login.html: navy gradient, logo, white Google pill. */
@Composable
fun LoginScreen(expired: Boolean, onLogin: () -> Unit, onLocal: (String?) -> Unit = {}) {
    val r = Repo
    var askName by remember { mutableStateOf(false) }
    StatusBarIcons(lightBackground = false)
    Box(
        Modifier.fillMaxSize()
            .background(Brush.linearGradient(0f to Navy2, 0.72f to Navy, start = Offset(0f, 0f), end = Offset(1400f, 2400f)))
            .drawBehind {
                drawRect(Brush.radialGradient(listOf(Color(0xFF16C7A8).copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * 0.8f, -size.height * 0.1f), radius = size.width * 1.3f))
            }
            .windowInsetsPadding(WindowInsets.systemBars).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(Modifier.widthIn(max = 340.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painterResource(R.drawable.ic_logo_mark), contentDescription = null,
                modifier = Modifier.size(64.dp).shadow(18.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black, spotColor = Color.Black)
            )
            Spacer(Modifier.height(16.dp))
            Text("Crumina", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(8.dp))
            Text(r.t("login_sub"), color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 19.sp)
            if (expired) {
                Spacer(Modifier.height(14.dp))
                Text(
                    r.t("expired"), color = Color(0xFFFFC9D4), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0x33F0476A)).padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
            Spacer(Modifier.height(26.dp))
            Row(
                Modifier.fillMaxWidth().shadow(14.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(CircleShape).background(Color.White).clickable(onClick = onLogin).padding(vertical = 15.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                Image(painterResource(R.drawable.ic_google_g), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(r.t("login_google"), color = Color(0xFF1F1F1F), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            // Divider and outlined "use locally" pill, as on app/login.html (.ordiv, .g2).
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.25f)))
                Text(r.t("or").uppercase(), color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 10.dp))
                Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.25f)))
            }
            Box(
                Modifier.fillMaxWidth().clip(CircleShape).border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                    .clickable { askName = true }.padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) { Text(r.t("local_btn"), color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(18.dp))
            Text(r.t("login_fine"), color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 16.sp)
        }
    }
    if (askName) {
        var name by remember { mutableStateOf("") }
        CrDialog(
            onDismissRequest = { askName = false },
            title = { Text(r.t("local_title")) },
            text = {
                Column {
                    Muted(r.t("local_body"), 13.sp)
                    Spacer(Modifier.height(10.dp))
                    CrField(name, { name = it.take(40) }, label = { Text(r.t("your_name")) })
                }
            },
            confirmButton = { TextButton(onClick = { askName = false; onLocal(name) }) { Text(r.t("start")) } },
            dismissButton = { TextButton(onClick = { askName = false }) { Text(r.t("cancel")) } }
        )
    }
}
