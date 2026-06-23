package com.sharry.badmintonproscore.presentation

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Text

data class GameSnapshot(
    val scoreA: Int,
    val scoreB: Int,
    val server: String?,
    val serviceSide: String,
    val gameOver: Boolean,
    val winner: String?
)

class MainActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {

    private var isAmbientState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        AmbientModeSupport.attach(this)

        setContent {
            BadmintonApp(isAmbient = isAmbientState)
        }
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback {
        return object : AmbientModeSupport.AmbientCallback() {
            override fun onEnterAmbient(ambientDetails: Bundle?) {
                super.onEnterAmbient(ambientDetails)
                isAmbientState = true
            }

            override fun onExitAmbient() {
                super.onExitAmbient()
                isAmbientState = false
            }
        }
    }
}

@Composable
fun BadmintonApp(isAmbient: Boolean) {
    val context = LocalContext.current
    val vibrator = remember {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    var scoreA by remember { mutableStateOf(0) }
    var scoreB by remember { mutableStateOf(0) }
    var server by remember { mutableStateOf<String?>(null) }
    var serviceSide by remember { mutableStateOf("right") }
    var gameOver by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf<String?>(null) }
    var isSwapped by remember { mutableStateOf(false) }

    val history = remember { mutableStateListOf<GameSnapshot>() }

    fun triggerHaptic(patternType: String) {
        if (isAmbient) return

        val timings: LongArray
        val amplitudes: IntArray

        when (patternType) {
            "TEAM_A" -> {
                timings = longArrayOf(0, 60)
                amplitudes = intArrayOf(0, 255)
            }

            "TEAM_B" -> {
                timings = longArrayOf(0, 50, 40, 50)
                amplitudes = intArrayOf(0, 255, 0, 255)
            }

            "WIN" -> {
                timings = longArrayOf(0, 150, 100, 150, 100, 300)
                amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
            }

            else -> return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }

    fun hasWon(myScore: Int, otherScore: Int): Boolean {
        return (myScore >= 21 && myScore - otherScore >= 2) || myScore == 30
    }

    fun saveSnapshot() {
        history.add(
            GameSnapshot(
                scoreA = scoreA,
                scoreB = scoreB,
                server = server,
                serviceSide = serviceSide,
                gameOver = gameOver,
                winner = winner
            )
        )
    }

    fun updateServiceSide(currentServer: String) {
        val currentScore = if (currentServer == "A") scoreA else scoreB
        serviceSide = if (currentScore % 2 == 0) "right" else "left"
    }

    fun addPointToA() {
        if (gameOver || isAmbient) return

        saveSnapshot()

        scoreA++
        server = "A"

        if (hasWon(scoreA, scoreB)) {
            gameOver = true
            winner = "TEAM A"
            triggerHaptic("WIN")
        } else {
            triggerHaptic("TEAM_A")
        }

        updateServiceSide("A")
    }

    fun addPointToB() {
        if (gameOver || isAmbient) return

        saveSnapshot()

        scoreB++
        server = "B"

        if (hasWon(scoreB, scoreA)) {
            gameOver = true
            winner = "TEAM B"
            triggerHaptic("WIN")
        } else {
            triggerHaptic("TEAM_B")
        }

        updateServiceSide("B")
    }

    fun undo() {
        if (history.isEmpty() || isAmbient) return

        val lastState = history.removeAt(history.size - 1)

        scoreA = lastState.scoreA
        scoreB = lastState.scoreB
        server = lastState.server
        serviceSide = lastState.serviceSide
        gameOver = lastState.gameOver
        winner = lastState.winner

        triggerHaptic("TEAM_A")
    }

    fun resetGame() {
        if (isAmbient) return

        scoreA = 0
        scoreB = 0
        server = null
        serviceSide = "right"
        gameOver = false
        winner = null
        history.clear()

        triggerHaptic("TEAM_B")
    }

    if (isAmbient) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isSwapped) {
                    Text("B: $scoreB", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("A: $scoreA", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("A: $scoreA", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("B: $scoreB", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                }

                if (server != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Server: Team $server ($serviceSide)",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isSwapped) {
                    TeamBBox(scoreB, server == "B", serviceSide, true) { addPointToB() }
                    TeamABox(scoreA, server == "A", serviceSide, false) { addPointToA() }
                } else {
                    TeamABox(scoreA, server == "A", serviceSide, true) { addPointToA() }
                    TeamBBox(scoreB, server == "B", serviceSide, false) { addPointToB() }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { undo() },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("Undo", color = Color.LightGray, fontSize = 9.sp)
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFF111111), shape = CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (scoreA >= 20 && scoreB >= 20) "DEUCE" else "MATCH",
                        color = if (scoreA >= 20 && scoreB >= 20) Color(0xFFFBBF24) else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Button(
                    onClick = { isSwapped = !isSwapped },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("Swap", color = Color.White, fontSize = 8.sp)
                }
            }

            if (gameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("WINNER", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                        Text(
                            text = winner ?: "",
                            color = if (winner == "TEAM A") Color(0xFF3B82F6) else Color(0xFFEF4444),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            text = "$scoreA - $scoreB",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { resetGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Text(
                                "NEW GAME",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColumnScope.TeamABox(
    scoreA: Int,
    isServing: Boolean,
    serviceSide: String,
    isTopView: Boolean,
    onPoint: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(Color(0xFF0A192F))
            .clickable { onPoint() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TEAM A", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(scoreA.toString(), color = Color(0xFF3B82F6), fontSize = 70.sp, fontWeight = FontWeight.Black)
        }

        if (isServing) {
            val alignIndicator =
                if ((serviceSide == "right" && !isTopView) || (serviceSide == "left" && isTopView)) {
                    Alignment.BottomEnd
                } else {
                    Alignment.BottomStart
                }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 30.dp, vertical = 20.dp),
                contentAlignment = alignIndicator
            ) {
                // Changed from a raw text arrow to a clean, highly visible circular indicator / shuttle dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFF22C55E), shape = CircleShape)
                )
            }
        }
    }
}

@Composable
fun ColumnScope.TeamBBox(
    scoreB: Int,
    isServing: Boolean,
    serviceSide: String,
    isTopView: Boolean,
    onPoint: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(Color(0xFF2D080A))
            .clickable { onPoint() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(scoreB.toString(), color = Color(0xFFEF4444), fontSize = 70.sp, fontWeight = FontWeight.Black)
            Text("TEAM B", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        if (isServing) {
            val alignIndicator =
                if ((serviceSide == "right" && !isTopView) || (serviceSide == "left" && isTopView)) {
                    Alignment.TopEnd
                } else {
                    Alignment.TopStart
                }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 30.dp, vertical = 20.dp),
                contentAlignment = alignIndicator
            ) {
                // Changed from a raw text arrow to a clean, highly visible circular indicator / shuttle dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFF22C55E), shape = CircleShape)
                )
            }
        }
    }
}