package com.djsunilsonu.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DjSunilSonuApp()
        }
    }
}

@Composable
fun DjSunilSonuApp() {
    // App ka Premium Dark Gradient Background
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
    )

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        
        // Main Mixer Area
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 50.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DJ SUNIL SONU",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(text = "Premium Mixing Studio", color = Color.Cyan, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Deck A aur Deck B ka Liquid Glass Design
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), 
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassCard(modifier = Modifier.weight(1f).height(220.dp), title = "DECK A")
                Spacer(modifier = Modifier.width(16.dp))
                GlassCard(modifier = Modifier.weight(1f).height(220.dp), title = "DECK B")
            }
        }

        // Niche ka Floating Navigation Bar (Liquid Glass Style)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp)
                .fillMaxWidth()
                .height(70.dp)
                .clip(RoundedCornerShape(35.dp))
                .background(Color.White.copy(alpha = 0.1f)) // Transparent Glass Effect
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(35.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Home", color = Color.White)
                Text("Library", color = Color.Gray)
                Text("Mixer", color = Color.Cyan, fontWeight = FontWeight.Bold) // Active Tab
                Text("Settings", color = Color.Gray)
            }
        }
    }
}

// Ye function Glass jaisa design banata hai
@Composable
fun GlassCard(modifier: Modifier, title: String) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
