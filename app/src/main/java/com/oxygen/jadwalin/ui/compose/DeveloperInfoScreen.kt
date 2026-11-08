package com.oxygen.jadwalin.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DeveloperInfoScreen(
    onBackClick: () -> Unit = {}
) {
    val colors = JadwalinTheme.colors
    Scaffold(
        containerColor = colors.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                DeveloperTopNav(onBackClick)
            }
            
            item { DeveloperHeader() }
            
            item { TeamHeroCard() }
            
            item { SectionTitle("ANGGOTA TIM", "2 Anggota Inti") }
            
            item {
                DeveloperProfileCard(
                    initials = "KG",
                    name = "Khansa Gunawan",
                    role = "Lead Frontend Developer",
                    bio = "Seniman di balik antarmuka yang indah dan intuitif. Memastikan setiap interaksi dan transisi Jadwalin terasa mulus, responsif, dan menyenangkan.",
                    skills = listOf("UI/UX Design", "Tailwind CSS", "Micro-animations")
                )
            }
            
            item {
                DeveloperProfileCard(
                    initials = "AR",
                    name = "M. Akmalu Rizki",
                    role = "Backend Architect & Designer",
                    bio = "Arsitek sistem yang menggerakkan mesin penjadwalan serta konseptor estetika aplikasi. Fokus pada keandalan alarm dan sinkronisasi real-time.",
                    skills = listOf("System Arch", "Kotlin", "Cloud Sync", "Database")
                )
            }
            
            item { SectionTitle("FITUR UNGGULAN", "Aplikasi") }
            
            item { FeatureGrid() }
            
            item { TechStackSection() }
            
            item { FeedbackBanner() }
            
            item { DeveloperFooter() }
            
            item { Spacer(modifier = Modifier.height(140.dp)) }
        }
    }
}

@Composable
fun DeveloperTopNav(onBackClick: () -> Unit) {
    val colors = JadwalinTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onBackClick() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null, tint = colors.primary, modifier = Modifier.size(28.dp))
            Text("Pengaturan", color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        
        Surface(color = colors.primary.copy(alpha = 0.1f), shape = CircleShape) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.primary))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tim Oxygen v1.0", color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DeveloperHeader() {
    val colors = JadwalinTheme.colors
    Column {
        Text("Developer Info", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Mengenal tim berdedikasi di balik pengalaman penjadwalan cerdas dan estetika harmonis Jadwalin.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, lineHeight = 22.sp)
    }
}

@Composable
fun TeamHeroCard() {
    Card(modifier = Modifier.fillMaxWidth(), shape = JadwalinShapes.Card, elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().background(JadwalinTheme.gradient).padding(24.dp)) {
            Surface(color = Color.White.copy(alpha = 0.15f), shape = CircleShape) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.StarOutline, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TIM PENGEMBANG", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Dibalik setiap fitur hebat ada dedikasi yang tak kenal lelah. Tim Oxygen berkomitmen memberikan solusi teknologi terbaik untuk Anda.", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
            Spacer(modifier = Modifier.height(28.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HeroStatItem("2", "Core Creators")
                HeroStatItem("100%", "Dedikasi")
                HeroStatItem("Oxygen", "Studio Lab")
            }
        }
    }
}

@Composable
fun HeroStatItem(value: String, label: String) {
    Column(modifier = Modifier.width(90.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SectionTitle(title: String, badge: String) {
    val colors = JadwalinTheme.colors
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = colors.textHint, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(badge, color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DeveloperProfileCard(initials: String, name: String, role: String, bio: String, skills: List<String>) {
    val colors = JadwalinTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(), 
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground), 
        shape = JadwalinShapes.Card, 
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSystemInDarkTheme()) 0.dp else 2.dp),
        border = if (isSystemInDarkTheme()) androidx.compose.foundation.BorderStroke(1.dp, colors.divider) else null
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(colors.primary), Alignment.Center) { Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp) }
                    Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(colors.cardBackground).padding(2.dp)) { Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(colors.success), Alignment.Center) { Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(12.dp)) } }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text(role, style = MaterialTheme.typography.bodyMedium, color = colors.primary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(bio, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, lineHeight = 22.sp)
            Spacer(modifier = Modifier.height(20.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                skills.forEach { skill -> 
                    Surface(
                        color = colors.pillBackground, 
                        shape = CircleShape
                    ) { 
                        Text(skill, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary) 
                    } 
                }
            }
        }
    }
}

@Composable
fun FeatureGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureItem(Modifier.weight(1f), Icons.Rounded.PrecisionManufacturing, "Presisi Maksimal", JadwalinTheme.colors.catKuliah.copy(alpha = 0.15f))
            FeatureItem(Modifier.weight(1f), Icons.Rounded.AutoAwesome, "Desain Elegan", JadwalinTheme.colors.catKuliah.copy(alpha = 0.15f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureItem(Modifier.weight(1f), Icons.Rounded.EventAvailable, "Fleksibel", JadwalinTheme.colors.success.copy(alpha = 0.15f))
            FeatureItem(Modifier.weight(1f), Icons.Rounded.NotificationsActive, "Handal", JadwalinTheme.colors.catKerja.copy(alpha = 0.15f))
        }
    }
}

@Composable
fun FeatureItem(modifier: Modifier, icon: ImageVector, title: String, color: Color) {
    val colors = JadwalinTheme.colors
    Card(
        modifier = modifier, 
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground), 
        shape = JadwalinShapes.Card, 
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSystemInDarkTheme()) 0.dp else 1.dp),
        border = if (isSystemInDarkTheme()) androidx.compose.foundation.BorderStroke(1.dp, colors.divider) else null
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(color), Alignment.Center) { Icon(icon, null, tint = colors.primary, modifier = Modifier.size(24.dp)) }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = colors.textPrimary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(horizontalArrangement: Arrangement.Horizontal = Arrangement.Start, verticalArrangement: Arrangement.Vertical = Arrangement.Top, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = horizontalArrangement, verticalArrangement = verticalArrangement, content = { content() })
}

@Composable
fun TechStackSection() {
    val colors = JadwalinTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(), 
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground), 
        shape = JadwalinShapes.Card, 
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSystemInDarkTheme()) 0.dp else 2.dp),
        border = if (isSystemInDarkTheme()) androidx.compose.foundation.BorderStroke(1.dp, colors.divider) else null
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Terminal, null, tint = colors.primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("TECH STACK", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary, letterSpacing = 0.5.sp)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("Dibangun dengan Jetpack Compose, Room, Retrofit, DataStore, dan arsitektur Clean & Modular.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, lineHeight = 22.sp)
        }
    }
}

@Composable
fun FeedbackBanner() {
    val colors = JadwalinTheme.colors
    Surface(color = Color(0xFF0F172A), shape = JadwalinShapes.Card, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Ada ide fitur?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Bantu kami menyempurnakan Jadwalin.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
            Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = colors.primary), shape = RoundedCornerShape(12.dp)) { Text("Kirim Pesan", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }
    }
}

@Composable
fun DeveloperFooter() {
    val colors = JadwalinTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Crafted with passion by Oxygen Team • Yogyakarta, ID", fontSize = 12.sp, color = colors.textHint, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
        Text("Jadwalin Mobile • Build 2026.1", fontSize = 11.sp, color = colors.textHint.copy(alpha = 0.6f), modifier = Modifier.padding(top = 4.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDeveloperInfoScreen() {
    JadwalinTheme { DeveloperInfoScreen() }
}
