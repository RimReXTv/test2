package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.blockchain.crypto.CryptoEngine
import com.example.blockchain.database.PeerEntity
import com.example.blockchain.database.TransactionEntity
import com.example.blockchain.model.Block
import com.example.blockchain.model.Transaction
import com.example.blockchain.viewmodel.TakiumViewModel
import com.example.ui.theme.MyApplicationTheme
import java.security.MessageDigest
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {
    private val viewModel: TakiumViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFFFDF8F6) // Theme Peach/White Background
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        TakiumApp(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun TakiumApp(viewModel: TakiumViewModel) {
    val isWalletLoaded by viewModel.isWalletLoaded.collectAsStateWithLifecycle()

    if (!isWalletLoaded) {
        OnboardingScreen(viewModel)
    } else {
        MainDashboardScreen(viewModel)
    }
}

/* ==========================================
   1. ONBOARDING / WALLET SETUP SCREEN
   ========================================== */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(viewModel: TakiumViewModel) {
    var step by remember { mutableStateOf("welcome") } // welcome, show_seed
    var importText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var newlyCreatedPhrase by remember { mutableStateOf("") }

    val context = LocalContext.current

    if (step == "show_seed") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Icon Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFFFEE2E2), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Cüzdan Təhlükəsizliyi",
                    tint = Color(0xFFB3261E),
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = "Toxum Frazası (Seed Phrase)",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1C1B1F),
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Text(
                text = "Aşağıdakı 12 söz cüzdanınızın YEGƏNƏ bərpa açarıdır. Onları kağıza yazın və heç kimlə paylaşmayın. Bu sözlər itərsə, coinləriniz həmişəlik itəcək!",
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = Color(0xFFB3261E),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 12-Word Grid representation
            val words = newlyCreatedPhrase.split(" ")
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (i in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (j in 0 until 3) {
                            val index = i * 3 + j
                            if (index < words.size) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                                    border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF6750A4),
                                            modifier = Modifier
                                                .background(Color(0xFFEADDFF), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = words[index],
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF21005D),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Takium Seed", newlyCreatedPhrase)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Mnemonic nüsxələndi!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    border = BorderStroke(1.dp, Color(0xFF6750A4))
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = Color(0xFF6750A4))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nüsxələ", fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                }

                Button(
                    onClick = {
                        viewModel.completeWalletSetup(newlyCreatedPhrase)
                        Toast.makeText(context, "Cüzdan uğurla aktivləşdirildi!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21005D)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp)
                ) {
                    Text("Yadda Saxladım", fontWeight = FontWeight.Black, color = Color.White)
                }
            }

            TextButton(
                onClick = { step = "welcome" },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Geri qayıt", color = Color(0xFF49454F))
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo Vector-Like Representation
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color(0xFFEADDFF), RoundedCornerShape(32.dp))
                    .border(2.dp, Color(0xFFD0BCFF), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Takium Emblem",
                    tint = Color(0xFF21005D),
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title and Brand Philosophy
            Text(
                text = "TAKIUM",
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1C1B1F),
                letterSpacing = (-1).sp
            )

            Text(
                text = "GENESIS v0.3",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6750A4),
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Dahi Satoshi Nakamotonun fəlsəfəsindən ilhamlanan, tam mərkəzsizləşdirilmiş, mobil mərkəzli P2P yeni nəsil pul sistemi.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = Color(0xFF49454F),
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Create Wallet Action
            Card(
                onClick = {
                    // Generate mnemonic offline first to display it
                    newlyCreatedPhrase = viewModel.generateNewMnemonic()
                    step = "show_seed"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_wallet_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                border = BorderStroke(1.dp, Color(0xFFD0BCFF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = Color(0xFF21005D),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Yeni Cüzdan Yarat",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF21005D)
                        )
                        Text(
                            text = "Təhlükəsiz 12-sözlük BIP-39 toxum frazası ilə",
                            fontSize = 12.sp,
                            color = Color(0xFF21005D).copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Import Wallet Action
            Card(
                onClick = { showImportDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import_wallet_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Mövcud Cüzdanı İdxal Et",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1C1B1F)
                        )
                        Text(
                            text = "Seed frazanı daxil edərək bərpa et",
                            fontSize = 12.sp,
                            color = Color(0xFF49454F)
                        )
                    }
                }
            }
        }
    }

    // Dialog: Import Seed Phrase
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Text(
                    text = "Cüzdanı Bərpa Et",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "12 sözdən ibarət BIP-39 mnemonic seed frazanızı aralarında boşluq qoyaraq bura daxil edin:",
                        fontSize = 13.sp,
                        color = Color(0xFF49454F),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("import_seed_input"),
                        placeholder = { Text("abandon ability able...") },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = importText.trim()
                        if (viewModel.importWallet(trimmed)) {
                            showImportDialog = false
                            Toast.makeText(context, "Cüzdan uğurla idxal olundu!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Xəta! Toxum frazası düzgün deyil və ya 12 sözdən ibarət deyil.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21005D)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("İdxal Et", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("İmtina", color = Color(0xFF49454F))
                }
            }
        )
    }
}

/* ==========================================
   2. MAIN DASHBOARD & TABS COORDINATION
   ========================================== */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(viewModel: TakiumViewModel) {
    var activeTab by remember { mutableStateOf("wallet") } // wallet, mining, scan, network, settings

    val activePeers by viewModel.p2pNetwork.activePeers.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Core Header (Consistent with the requested "Bold Typography" design style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Genesis v0.1",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4),
                    letterSpacing = 2.sp
                )
                Text(
                    text = "TAKIUM",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1C1B1F),
                    letterSpacing = (-1).sp,
                    lineHeight = 32.sp
                )
            }

            // Green Online Nodes / Peers Badge Pill
            Row(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(100.dp))
                    .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(100.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated green pulse point
                val transition = rememberInfiniteTransition(label = "pulse")
                val opacity by transition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = opacity))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${activePeers.size} PEERS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1F)
                )
            }
        }

        // Active View Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                "wallet" -> WalletTab(viewModel)
                "mining" -> MiningTab(viewModel)
                "scan" -> MerchantScanTab(viewModel)
                "network" -> NetworkTab(viewModel)
                "settings" -> SettingsTab(viewModel)
            }
        }

        // Custom Navigation Bar exactly representing Design aesthetics
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.4f)))
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarButton(
                title = "Cüzdan",
                icon = Icons.Default.Home,
                selected = activeTab == "wallet",
                onClick = { activeTab = "wallet" },
                modifier = Modifier.testTag("nav_wallet")
            )
            NavBarButton(
                title = "Mining",
                icon = Icons.Default.Settings,
                selected = activeTab == "mining",
                onClick = { activeTab = "mining" },
                modifier = Modifier.testTag("nav_mining")
            )
            
            // Middle QR Action Button representing the core Scan/Merchant hub
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFF6750A4), CircleShape)
                        .clickable { activeTab = "scan" }
                        .testTag("nav_scan_qr"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "QR Scanner and Merchant Hub",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            NavBarButton(
                title = "Şəbəkə",
                icon = Icons.Default.Share,
                selected = activeTab == "network",
                onClick = { activeTab = "network" },
                modifier = Modifier.testTag("nav_network")
            )
            NavBarButton(
                title = "Ayarlar",
                icon = Icons.Default.Settings,
                selected = activeTab == "settings",
                onClick = { activeTab = "settings" },
                modifier = Modifier.testTag("nav_settings")
            )
        }
    }
}

@Composable
fun NavBarButton(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color(0xFF6750A4) else Color(0xFF49454F).copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color(0xFF6750A4) else Color(0xFF49454F).copy(alpha = 0.5f),
            letterSpacing = 0.5.sp
        )
    }
}

/* ==========================================
   3. TAB VIEW 1: WALLET (BALANCES, SEND/RECV)
   ========================================== */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletTab(viewModel: TakiumViewModel) {
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val address by viewModel.walletAddress.collectAsStateWithLifecycle()
    val blocks by viewModel.blockchainEngine.blocks.collectAsStateWithLifecycle()

    var showSendDialog by remember { mutableStateOf(false) }
    var showReceiveDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Balance Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
            border = BorderStroke(1.dp, Color(0xFFD0BCFF))
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ümumi Balans",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF21005D).copy(alpha = 0.7f)
                    )
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "TKM/USD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF21005D)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "%.4f".format(balance),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF21005D),
                        letterSpacing = (-1).sp,
                        lineHeight = 46.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TKM",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF21005D).copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Text(
                    text = "≈ $${"%,.2f".format(balance * 30.0)} USD", // 1 TKM is styled as 30 USD representation
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF21005D).copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showSendDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("send_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21005D)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GÖNDƏR", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = { showReceiveDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("receive_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFD0BCFF))
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF21005D))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AL", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Ledger Statistics Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mining Statistics Card
            val isMining by viewModel.miningEngine.isMining.collectAsStateWithLifecycle()
            val hashesBySec by viewModel.miningEngine.hashRate.collectAsStateWithLifecycle()
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "MINING NODE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4),
                        letterSpacing = 1.sp
                    )

                    Column {
                        Text(
                            text = if (isMining) "${"%,d".format(hashesBySec)} H/s" else "Aktiv deyil",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isMining) Color(0xFF10B981) else Color(0xFF1C1B1F)
                        )
                        Text(
                            text = "NODE TEMPERATUR: 34°C",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF49454F).copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Block Statistics Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SON BLOK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4),
                        letterSpacing = 1.sp
                    )

                    Column {
                        val lastBlockIndex = (blocks.size - 1).coerceAtLeast(0)
                        Text(
                            text = "#$lastBlockIndex",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1C1B1F)
                        )
                        val lastHash = blocks.lastOrNull()?.hash ?: "0000...x00"
                        Text(
                            text = "HASH: ${lastHash.take(8)}...${lastHash.takeLast(4)}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF49454F).copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Activity List Header
        Text(
            text = "YAXINDAKI ƏMƏLİYYATLAR",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF49454F),
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Compile Live Activity Transactions directly from Blockchain database block history
        val txEntities by viewModel.blockchainEngine.dao.getAllTransactionsFlow().collectAsStateWithLifecycle(emptyList())

        if (txEntities.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .border(BorderStroke(1.dp, Color(0xFFCAC4D0)), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF6750A4).copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hələ ki əməliyyat yoxdur",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1B1F).copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Gözlənilən və ya təsdiqlənmiş əməliyyat tapılmadı.",
                        fontSize = 11.sp,
                        color = Color(0xFF49454F).copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .border(BorderStroke(1.dp, Color(0xFFCAC4D0)), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(txEntities, key = { it.txId }) { entity ->
                    TransactionItem(entity, address)
                }
            }
        }
    }

    // Dialog: Send TKM Form
    if (showSendDialog) {
        var recipientAddr by remember { mutableStateOf("") }
        var sendAmountStr by remember { mutableStateOf("") }
        var isSending by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSending) showSendDialog = false },
            title = {
                Text("Coin Göndər", fontWeight = FontWeight.Black, fontSize = 18.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = recipientAddr,
                        onValueChange = { recipientAddr = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("send_recipient_input"),
                        label = { Text("Qəbuledən Ünvan") },
                        placeholder = { Text("TKM_...") },
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = sendAmountStr,
                        onValueChange = { sendAmountStr = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_amount_input"),
                        label = { Text("Məbləğ (TKM)") },
                        placeholder = { Text("10.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = sendAmountStr.toDoubleOrNull() ?: 0.0
                        if (recipientAddr.isEmpty() || amt <= 0.0) {
                            Toast.makeText(context, "Düzgün məlumat daxil edin!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSending = true
                        viewModel.sendTransaction(recipientAddr, amt) { success, msg ->
                            isSending = false
                            showSendDialog = false
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21005D)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("İndi Göndər", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isSending) {
                    TextButton(onClick = { showSendDialog = false }) {
                        Text("Ləğv Et", color = Color(0xFF49454F))
                    }
                }
            }
        )
    }

    // Dialog: Receive / Address QR view
    if (showReceiveDialog) {
        AlertDialog(
            onDismissRequest = { showReceiveDialog = false },
            title = {
                Text("Cüzdan Ünvanınız", fontWeight = FontWeight.Black, fontSize = 18.sp)
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Digər istifadəçilərdən Takium (TKM) almaq üçün bu ünvanı onlarla paylaşın.",
                        fontSize = 12.sp,
                        color = Color(0xFF49454F),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Draw a gorgeous pixelated vector-representation of QR Code
                    StyledQrCodeCanvas(address)

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF3EDF7), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = address,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF21005D),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Takium Address", address)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Cüzdan ünvanı kopyalandı!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21005D)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ünvanı Kopyala", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReceiveDialog = false }) {
                    Text("Bağla", fontWeight = FontWeight.Black, color = Color(0xFF6750A4))
                }
            }
        )
    }
}

@Composable
fun TransactionItem(entity: TransactionEntity, userAddress: String) {
    val isIncoming = entity.outputsJson.contains(userAddress) && !entity.isCoinbase
    val isMine = entity.isCoinbase

    val icon = when {
        isMine -> Icons.Default.Settings
        isIncoming -> Icons.Default.Refresh
        else -> Icons.Default.Add
    }

    val iconColor = when {
        isMine -> Color(0xFF6750A4)
        isIncoming -> Color(0xFF10B981)
        else -> Color(0xFFE11D48)
    }

    val labelText = when {
        isMine -> "Mining Mükafatı"
        isIncoming -> "Qəbul Edildi"
        else -> "Gördərilən Ödəniş"
    }

    val signSymbol = when {
        isMine || isIncoming -> "+"
        else -> "-"
    }

    val amountColor = when {
        isMine || isIncoming -> Color(0xFF15803D)
        else -> Color(0xFF1C1B1F)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF3EDF7), CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = labelText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1F)
                )
                // Epoch timestamp conversion
                Text(
                    text = if (entity.blockHash == null) "Gözlənilir (Mempool)" else "Təsdiqləndi",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (entity.blockHash == null) Color(0xFFD97706) else Color(0xFF49454F).copy(alpha = 0.5f)
                )
            }
        }

        Text(
            text = "$signSymbol${"%.2f".format(entity.amount)}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = amountColor
        )
    }
}

/* ==========================================
   4. TAB VIEW 2: ACTIVE MINING CONTROLS
   ========================================== */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiningTab(viewModel: TakiumViewModel) {
    val isMining by viewModel.miningEngine.isMining.collectAsStateWithLifecycle()
    val hashRate by viewModel.miningEngine.hashRate.collectAsStateWithLifecycle()
    val batterySafeMode by viewModel.miningEngine.batterySafeMode.collectAsStateWithLifecycle()
    val blocksMined by viewModel.miningEngine.minedBlocksCount.collectAsStateWithLifecycle()
    val mempoolTxs by viewModel.blockchainEngine.mempool.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF21005D)),
            border = BorderStroke(1.dp, Color(0xFF6750A4))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DECENTRALIZED MOBILE MINING",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEADDFF),
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mining speed gauge display
                Text(
                    text = if (isMining) "${"%,d".format(hashRate)} H/s" else "Miner dayandırılıb",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isMining) Color(0xFF10B981) else Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress animation or idle emblem
                if (isMining) {
                    CircularProgressIndicator(
                        color = Color(0xFF10B981),
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Mining action button
                Button(
                    onClick = {
                        if (isMining) {
                            viewModel.stopMining()
                        } else {
                            viewModel.startMining()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMining) Color(0xFFE11D48) else Color(0xFF10B981)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("toggle_mining_button")
                ) {
                    Icon(
                        imageVector = if (isMining) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isMining) "Mədənçiliyi Dayandır" else "Mobil Mining Başlat",
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Device Controls & CPU Protection configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFCAC4D0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CİHAZ SƏHƏTİ & PARAMETRLƏR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Thermal Throttling / Battery Safe configuration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Batareya Qorunma Rejimi",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F)
                        )
                        Text(
                            text = "Mining prosesini kiçik fasilələrlə yavaşladaraq prosessoru qızmağa qoymur və şarjı saxlayır.",
                            fontSize = 11.sp,
                            color = Color(0xFF49454F)
                        )
                    }
                    Switch(
                        checked = batterySafeMode,
                        onCheckedChange = { viewModel.miningEngine.setBatterySafeMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF6750A4)),
                        modifier = Modifier.testTag("battery_safe_switch")
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFCAC4D0).copy(alpha = 0.4f))

                // Stats: Mined Blocks locally
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Bu Cihazda Qazılan Bloklar:", fontSize = 13.sp, color = Color(0xFF1C1B1F), fontWeight = FontWeight.Bold)
                    Text("$blocksMined block", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, color = Color(0xFF6750A4))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Live Mempool Pending Transactions Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GÖZLƏYƏN ƏMƏLİYYATLAR (MEMPOOL)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF49454F),
                letterSpacing = 1.5.sp
            )
            Box(
                modifier = Modifier
                    .background(Color(0xFFEADDFF), RoundedCornerShape(100.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${mempoolTxs.size} TX",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF21005D)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mempool list mapping
        if (mempoolTxs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .border(BorderStroke(1.dp, Color(0xFFCAC4D0)), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Mempool boşdur. Bütün əməliyyatlar zəncirdə təsdiqlənib.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF49454F).copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    mempoolTxs.forEachIndexed { idx, tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "TxID: ${tx.txId.take(12)}...", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text(text = "Komissiya: ${tx.fee} TKM", fontSize = 10.sp, color = Color(0xFF49454F))
                            }
                            Text(
                                text = "${"%.2f".format(tx.outputs.sumOf { it.amount })} TKM",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF21005D)
                            )
                        }
                        if (idx < mempoolTxs.size - 1) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFCAC4D0).copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }
    }
}

/* ==========================================
   5. TAB VIEW 3: SCAN PAY & MERCHANT INTENTS
   ========================================== */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantScanTab(viewModel: TakiumViewModel) {
    var showMerchantAmountInput by remember { mutableStateOf(true) }
    var inputAmountStr by remember { mutableStateOf("") }

    val generatedQrCodeText by viewModel.generatedQrCodeText.collectAsStateWithLifecycle()

    var showCustomerManualInput by remember { mutableStateOf(false) }
    var customerScannedPayload by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Toggle tabs between: Merchant vs Customer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF3EDF7), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { showCustomerManualInput = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showCustomerManualInput) Color.White else Color.Transparent
                ),
                elevation = null,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Sahibkar Modu", color = if (!showCustomerManualInput) Color(0xFF21005D) else Color(0xFF49454F), fontWeight = FontWeight.Black, fontSize = 12.sp)
            }

            Button(
                onClick = { showCustomerManualInput = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showCustomerManualInput) Color.White else Color.Transparent
                ),
                elevation = null,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Müştəri (Ödəniş)", color = if (showCustomerManualInput) Color(0xFF21005D) else Color(0xFF49454F), fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (!showCustomerManualInput) {
            /* SAHİBKAR / MERCHANT MODE */
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MAĞAZA ÖDƏNİŞİ YARAT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (showMerchantAmountInput) {
                        OutlinedTextField(
                            value = inputAmountStr,
                            onValueChange = { inputAmountStr = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("merchant_amount_input"),
                            label = { Text("Tələb Olunan Məbləğ (TKM)") },
                            placeholder = { Text("100") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val amt = inputAmountStr.toDoubleOrNull() ?: 0.0
                                if (amt <= 0.0) {
                                    Toast.makeText(context, "Düzgün məbləğ daxil edin", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.generateMerchantPaymentQr(amt)
                                showMerchantAmountInput = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21005D)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("generate_merchant_qr_button")
                        ) {
                            Text("QR Code Ödəniş Linki Yarat", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Generated QR code representation
                        Text(
                            text = "$inputAmountStr TKM",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF21005D)
                        )
                        Text(
                            text = "Müştəri bu QR kodu skan edərək ödənişi təsdiqləyəcək.",
                            fontSize = 11.sp,
                            color = Color(0xFF49454F),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Styled dynamic visual representation of QR payload
                        StyledQrCodeCanvas(generatedQrCodeText)

                        Spacer(modifier = Modifier.height(16.dp))

                        // QR Payload string block with copy button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF3EDF7), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = generatedQrCodeText,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF49454F),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Takium Payment QR", generatedQrCodeText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Ödəniş məlumatı kopyalandı!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21005D)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Linki Kopyala", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showMerchantAmountInput = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color(0xFFCAC4D0))
                            ) {
                                Text("Yeni Məbləğ", fontSize = 12.sp, color = Color(0xFF1C1B1F))
                            }
                        }
                    }
                }
            }
        } else {
            /* MÜŞTƏRİ / CUSTOMER SCAN&PAY MODE */
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "MAĞAZA QR KODU SKAN ET",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4),
                        letterSpacing = 1.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Mağazanın hazırladığı ödəniş QR kodunu/faylını aşağıdakı qutuya daxil edin və ya kopyalanmış linki yapışdırın:",
                        fontSize = 12.sp,
                        color = Color(0xFF49454F)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customerScannedPayload,
                        onValueChange = { customerScannedPayload = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("scanned_qr_input"),
                        placeholder = { Text("{\"protocol\":\"takium\",...}") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val item = clipboard.primaryClip?.getItemAt(0)
                                if (item != null) {
                                    customerScannedPayload = item.text.toString()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Yaddaşdan Yapışdır", fontSize = 11.sp, color = Color(0xFF1C1B1F))
                        }

                        Button(
                            onClick = {
                                if (customerScannedPayload.isEmpty()) {
                                    Toast.makeText(context, "Ödəniş linkini daxil edin!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.executeQrPayment(customerScannedPayload) { success, msg ->
                                    if (success) {
                                        customerScannedPayload = ""
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("confirm_pay_button")
                        ) {
                            Text("Ödənişi Sifariş Et", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/* ==========================================
   6. TAB VIEW 4: NETWORK PEERS & NAT TRAVERSAL
   ========================================== */
@Composable
fun NetworkTab(viewModel: TakiumViewModel) {
    val localPeerId by viewModel.p2pNetwork.localPeerId.collectAsStateWithLifecycle()
    val activePeers by viewModel.p2pNetwork.activePeers.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.p2pNetwork.connectionStatus.collectAsStateWithLifecycle()
    val holePunchingStatus by viewModel.p2pNetwork.holePunchingStatus.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Peer Identity Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
            border = BorderStroke(1.dp, Color(0xFFD0BCFF))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LOKAL NODE ŞƏBƏKƏ İDENTİFİKATORU",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF21005D),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = localPeerId,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF21005D),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Bağlantı:", fontSize = 11.sp, color = Color(0xFF21005D).copy(alpha = 0.6f))
                        Text(connectionStatus, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("NAT / STUN Rejimi:", fontSize = 11.sp, color = Color(0xFF21005D).copy(alpha = 0.6f))
                        Text(holePunchingStatus, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Peer List Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AKTİV PEER ROUTING CƏDVƏLİ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF49454F),
                letterSpacing = 1.5.sp
            )

            Text(
                text = "${activePeers.size} / 500 nodes",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6750A4)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Peer List Card Scroll view
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFCAC4D0))
        ) {
            if (activePeers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF6750A4))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(activePeers, key = { it.peerId }) { peer ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = peer.peerId,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1B1F),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${peer.ipAddress}:${peer.port} • ${if (peer.isFullNode) "Full Node" else "Lite Node"}",
                                    fontSize = 9.sp,
                                    color = Color(0xFF49454F)
                                )
                            }

                            // Peer Status indicators
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (peer.isOnline) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (peer.isOnline) "Aktif" else "Cavabsız",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (peer.isOnline) Color(0xFF065F46) else Color(0xFF991B1B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ==========================================
   7. TAB VIEW 5: SETTINGS, RPC ENGINE & UTILS
   ========================================== */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(viewModel: TakiumViewModel) {
    val nodeMode by viewModel.nodeMode.collectAsStateWithLifecycle()
    val rpcCommand by viewModel.rpcCommand.collectAsStateWithLifecycle()
    val rpcParams by viewModel.rpcParams.collectAsStateWithLifecycle()
    val rpcResponseText by viewModel.rpcResponseText.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Node Type Configuration Block
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFCAC4D0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LEDGER SAXLAMA SEÇİMİ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        onClick = { viewModel.setNodeMode("Lite Node") },
                        modifier = Modifier.weight(1f).testTag("select_lite_node"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (nodeMode == "Lite Node") Color(0xFFEADDFF) else Color(0xFFF3EDF7)
                        ),
                        border = BorderStroke(1.dp, if (nodeMode == "Lite Node") Color(0xFF6750A4) else Color.Transparent)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Lite Node", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Telefonlar üçün tövsiyə olunan sürətli sinxronizasiya rejimi.", fontSize = 10.sp, color = Color(0xFF49454F))
                        }
                    }

                    Card(
                        onClick = { viewModel.setNodeMode("Full Node") },
                        modifier = Modifier.weight(1f).testTag("select_full_node"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (nodeMode == "Full Node") Color(0xFFEADDFF) else Color(0xFFF3EDF7)
                        ),
                        border = BorderStroke(1.dp, if (nodeMode == "Full Node") Color(0xFF6750A4) else Color.Transparent)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Full Node", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Bütün blockchain tarixçəsini saxlayır və şəbəkəyə dəstək verir.", fontSize = 10.sp, color = Color(0xFF49454F))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cryptographic Wallet Credentials Block
        var keysVisible by remember { mutableStateOf(false) }
        val mnemonic by viewModel.mnemonic.collectAsStateWithLifecycle()
        val walletAddress by viewModel.walletAddress.collectAsStateWithLifecycle()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFCAC4D0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KRİPTOQRAFİK CÜZDAN DETALLARI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4),
                        letterSpacing = 1.sp
                    )
                    TextButton(
                        onClick = { keysVisible = !keysVisible }
                    ) {
                        Text(
                            text = if (keysVisible) "GİZLƏ" else "GÖSTƏR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (keysVisible) Color(0xFFE11D48) else Color(0xFF6750A4)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Public Address
                Text("Cüzdan Ünvanı (Public Address):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3EDF7), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = walletAddress,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF1C1B1F),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Address", walletAddress)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Ünvan kopyalandı!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Kopyala", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Public Key
                val pubKeyBase64 = remember(walletAddress) { viewModel.getPublicKeyBase64() }
                Text("İctimai Açar (Public Key - Base64):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3EDF7), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (pubKeyBase64.isNotEmpty()) {
                            pubKeyBase64.take(24) + "..." + pubKeyBase64.takeLast(10)
                        } else {
                            "Yüklənir..."
                        },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF1C1B1F),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            if (pubKeyBase64.isNotEmpty()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("PubKey", pubKeyBase64)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "İctimai açar kopyalandı!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Kopyala", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Private Key
                val privKeyBase64 = remember(walletAddress) { viewModel.getPrivateKeyBase64() }
                Text("Gizli Açar (Private Key - Base64):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3EDF7), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (keysVisible) {
                            if (privKeyBase64.isNotEmpty()) {
                                privKeyBase64.take(18) + "..." + privKeyBase64.takeLast(10)
                            } else {
                                "Yüklənir..."
                            }
                        } else {
                            "••••••••••••••••••••••••••••••••"
                        },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (keysVisible) Color(0xFFE11D48) else Color(0xFF1C1B1F),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            if (keysVisible && privKeyBase64.isNotEmpty()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("PrivKey", privKeyBase64)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Gizli açar kopyalandı!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Əvvəlcə açarları görünən edin!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Kopyala", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // BIP-39 Seed Phrase
                Text("BIP-39 Mnemonic Seed (12 Söz):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3EDF7), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (keysVisible) {
                            mnemonic
                        } else {
                            mnemonic.split(" ").getOrNull(0) + " " + mnemonic.split(" ").getOrNull(1) + " •••••••••••• " + mnemonic.split(" ").lastOrNull()
                        },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (keysVisible) Color(0xFF6750A4) else Color(0xFF1C1B1F),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            if (keysVisible && mnemonic.isNotEmpty()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Mnemonic", mnemonic)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Seed phrase kopyalandı!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Əvvəlcə açarları görünən edin!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Kopyala", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Embedded RPC interactive terminal
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFCAC4D0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DÖVLƏT RPC APİ TERMINALI (GELİSMİS KODLAR)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                var expandedCmdDropdown by remember { mutableStateOf(false) }
                val commands = listOf("getbalance", "getheight", "getblock", "getpeerinfo")

                // Command select dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedCmdDropdown = true },
                        modifier = Modifier.fillMaxWidth().testTag("rpc_cmd_selector"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Komanda: $rpcCommand", color = Color(0xFF1C1B1F), fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.Menu, contentDescription = null, tint = Color(0xFF1C1B1F))
                        }
                    }

                    DropdownMenu(
                        expanded = expandedCmdDropdown,
                        onDismissRequest = { expandedCmdDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        commands.forEach { cmd ->
                            DropdownMenuItem(
                                text = { Text(cmd, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    viewModel.setRpcCommand(cmd)
                                    expandedCmdDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Params Input Field
                OutlinedTextField(
                    value = rpcParams,
                    onValueChange = { viewModel.setRpcParams(it) },
                    modifier = Modifier.fillMaxWidth().testTag("rpc_params_input"),
                    label = { Text("Parametrlər (Vergüllə ayırın)") },
                    placeholder = { Text("TKM_address, blockIndex və s.") },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.executeRpc() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21005D)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("execute_rpc_button")
                ) {
                    Text("RPC sorğusunu icra et", fontWeight = FontWeight.Bold)
                }

                if (rpcResponseText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("RPC CAVABI (JSON):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color(0xFF21005D), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = rpcResponseText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Reset Chain and Logout Blocks
        Button(
            onClick = {
                viewModel.resetChain()
                Toast.makeText(context, "Blockchain yenidən Genesis-ə qaytarıldı!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE11D48)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("reset_ledger_button")
        ) {
            Text("Ledger-i Genesis-ə Sıfırla", color = Color(0xFFE11D48), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("logout_button")
        ) {
            Text("Çıxış (Cüzdan açarını sil)", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

/* ==========================================
   8. SYSTEM VECTOR VISUAL DRAWING SUITE
   ========================================== */
@Composable
fun StyledQrCodeCanvas(payloadText: String) {
    // Generate a deterministically hashed seed for grid matrix mapping
    val hashBytes = remember(payloadText) {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.digest(payloadText.toByteArray(Charsets.UTF_8))
        } catch (e: Exception) {
            ByteArray(32)
        }
    }

    Canvas(
        modifier = Modifier
            .size(160.dp)
            .background(Color.White)
            .border(2.dp, Color(0xFFCAC4D0))
            .padding(10.dp)
    ) {
        val matrixSize = 10
        val cellWidth = size.width / matrixSize
        val cellHeight = size.height / matrixSize

        // Paint static outer bounding locator corners to make it visually represent a premium QR pattern
        val locatorPaint = Color(0xFF21005D)

        // Top Left corner locator
        drawRect(color = locatorPaint, topLeft = Offset(0f, 0f), size = Size(cellWidth * 3, cellHeight * 3))
        drawRect(color = Color.White, topLeft = Offset(cellWidth, cellHeight), size = Size(cellWidth, cellHeight))

        // Top Right corner locator
        drawRect(color = locatorPaint, topLeft = Offset(cellWidth * 7, 0f), size = Size(cellWidth * 3, cellHeight * 3))
        drawRect(color = Color.White, topLeft = Offset(cellWidth * 8, cellHeight), size = Size(cellWidth, cellHeight))

        // Bottom Left corner locator
        drawRect(color = locatorPaint, topLeft = Offset(0f, cellHeight * 7), size = Size(cellWidth * 3, cellHeight * 3))
        drawRect(color = Color.White, topLeft = Offset(cellWidth, cellHeight * 8), size = Size(cellWidth, cellHeight))

        // Fill inner pixels deterministically based on payload hash bytes
        for (row in 0 until matrixSize) {
            for (col in 0 until matrixSize) {
                // Ignore locator corners
                val isTopLeftLocator = row < 3 && col < 3
                val isTopRightLocator = row < 3 && col >= 7
                val isBottomLeftLocator = row >= 7 && col < 3
                if (isTopLeftLocator || isTopRightLocator || isBottomLeftLocator) continue

                // Check bit state in hashBytes
                val byteIndex = (row * matrixSize + col) / 8
                val bitIndex = (row * matrixSize + col) % 8
                val byteVal = hashBytes.getOrNull(byteIndex % hashBytes.size)?.toInt() ?: 0
                val bitState = (byteVal ushr bitIndex) and 1

                if (bitState == 1) {
                    drawRect(
                        color = Color(0xFF1C1B1F),
                        topLeft = Offset(col * cellWidth, row * cellHeight),
                        size = Size(cellWidth, cellHeight)
                    )
                }
            }
        }
    }
}

// Inline helper to convert raw values to dp representation
private fun Int.toDp() = this.dp
