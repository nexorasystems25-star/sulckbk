# Sulck App — Locked-Savings Blueprint

## Paradigm Shift: Debt → Savings Incentive
**Original (M-KOPA)**: Phone locked as penalty for missed loan payment  
**New (Sulck)**: Phone unlocked as reward for meeting savings goals — lock protects the user's micro-investment pool when streak pauses

---

## Terminology Mapping (Clean-Room)

| Original Asset Financing | New Locked-Savings App | Technical Reality |
|-------------------------|------------------------|-------------------|
| Loan Balance / Amount Remaining | **Savings Target Shortfall** | Remaining amount to unlock phone lifecycle/milestone |
| Daily Repayment Amount | **Daily Auto-Save Target** | Calculated micro-saving step to keep device active |
| Missed Payments / Arrears | **Savings Streak Pause** | Trigger state when daily/weekly commitment isn't met |
| Device Locking (Collateral) | **Goal Lock Commitment** | Phone hardware committed to target; locks to protect savings pool |
| Phone Catalog (Upgrades) | **Goals Catalog** | Asset targets: Solar Tier 2, Education Vault, Cash Reserve |
| Repayment Onboarding | **Savings Commitment Setup** | Guided goal configuration |
| Offers Feed (Promotions) | **Boosts & Milestones** | Bonus interest, streak rewards, milestone unlocks |

---

## New Data Models (`.proto` — Clean Room)

```protobuf
// com.sulck.app.data.accounts.local
syntax = "proto3";
option java_package = "com.sulck.app.data.accounts.local";
option java_multiple_files = true;

message CurrencyAmountDto {
  int64 amountMicros = 1;      // Store in micros (cents * 10000) for precision
  string currencyCode = 2;     // ISO 4217
}

message SavingsAccountDto {
  string accountId = 1;
  string accountNumber = 2;
  CurrencyAmountDto currentSavingsBalance = 3;    // Formerly amountPaid
  CurrencyAmountDto aggregateSavingsTarget = 4;   // Formerly totalPrice
  CurrencyAmountDto distanceToTarget = 5;         // Formerly remainingAmount
  CurrencyAmountDto dailySavingsCommitment = 6;   // Formerly dailyAmount
  bool allowsSavingsDeposits = 7;                 // Formerly allowsPayments
  bool isDeviceCompliant = 8;                     // Device structural state
  repeated GoalLockDto activeGoals = 9;           // Formerly paymentPlans/loans
}

message GoalLockDto {
  string goalId = 1;
  string displayName = 2;                         // "Emergency Fund", "Solar Setup"
  int32 savingsCadenceInDays = 3;                 // Daily (1), Weekly (7)
  int64 startDate = 4;
  int64 targetLockReleaseDate = 5;                // Formerly expectedFinishedDate
  string lockedAssetHardwareId = 6;               // Hardware lock serial hook
  CurrencyAmountDto targetAmount = 7;             // Goal target
  CurrencyAmountDto currentProgress = 8;          // Current savings toward this goal
}

// com.sulck.app.data.savings.onboarding.local
message SavingsOnboardingStateDto {
  enum VaultStatus {
    ACTIVE_STREAK = 0;
    SAVINGS_PAUSED = 1;
    PENDING_LOCKOUT = 2;                          // Formerly pending_blocking
  }
  VaultStatus current_vault_status = 1;
  int32 missed_savings_days = 2;                  // Formerly missed_payments
  int64 last_successful_deposit_ts = 3;
  int32 current_streak_days = 4;
}

// com.sulck.app.data.goals.catalog.local
message GoalsCatalogDto {
  string header = 1;
  repeated GoalCatalogItemDto goals = 2;
  int64 saved_at_timestamp = 3;
}

message GoalCatalogItemDto {
  string goalId = 1;
  string title = 2;
  string description = 3;
  string imageUrl = 4;
  repeated string tags = 5;                       // "solar", "education", "emergency"
  CurrencyAmountDto targetAmount = 6;
  int32 estimatedDaysToComplete = 7;
}

// com.sulck.app.data.personalisation.home.local
message SavingsHomeConfigDto {
  repeated string headerWidgets = 1;              // Streak, Next Milestone
  repeated string contentWidgets = 2;             // Active Goals, Boosts
  repeated string footerWidgets = 3;              // Settings, Support
}

// com.sulck.app.data.customer.local
message CustomerProfileDto {
  string id = 1;
  string firstName = 2;
  string surname = 3;
  string phoneNumber = 4;                         // E.164 format
  int64 lastUpdated = 5;
}
```

---

## UI Architecture: Jetpack Compose (Material 3)

### ViewModel — Core State Logic
```kotlin
// data/model/SavingsUiState.kt
package com.sulck.app.data.model

data class SavingsUiState(
    val currentSavings: CurrencyAmountDto = CurrencyAmountDto(0, "KES"),
    val totalGoal: CurrencyAmountDto = CurrencyAmountDto(0, "KES"),
    val dailyCommitment: CurrencyAmountDto = CurrencyAmountDto(0, "KES"),
    val distanceToTarget: CurrencyAmountDto = CurrencyAmountDto(0, "KES"),
    val daysSavedStreak: Int = 0,
    val vaultStatus: VaultStatus = VaultStatus.ACTIVE_STREAK,
    val isDeviceCompliant: Boolean = true,
    val activeGoals: List<GoalLockDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val progressPercent: Float
        get() = if (totalGoal.amountMicros > 0) 
            (currentSavings.amountMicros.toFloat() / totalGoal.amountMicros).coerceIn(0f, 1f) 
        else 0f
    
    val isGoalMaintained: Boolean
        get() = vaultStatus == VaultStatus.ACTIVE_STREAK && isDeviceCompliant
}

// ui/viewmodel/SavingsDashboardViewModel.kt
package com.sulck.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.sulck.app.data.model.*
import com.sulck.app.data.repository.SavingsRepository

@HiltViewModel
class SavingsDashboardViewModel @Inject constructor(
    private val repository: SavingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavingsUiState())
    val uiState: StateFlow<SavingsUiState> = _uiState

    init { loadDashboard() }

    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val account = repository.getSavingsAccount()
                val onboarding = repository.getOnboardingState()
                val goals = repository.getActiveGoals()
                _uiState.value = SavingsUiState(
                    currentSavings = account.currentSavingsBalance,
                    totalGoal = account.aggregateSavingsTarget,
                    dailyCommitment = account.dailySavingsCommitment,
                    distanceToTarget = account.distanceToTarget,
                    daysSavedStreak = onboarding.current_streak_days,
                    vaultStatus = onboarding.current_vault_status,
                    isDeviceCompliant = account.isDeviceCompliant,
                    activeGoals = goals,
                    isLoading = false
                )
                // Sync device lock state
                repository.syncDeviceLockState(_uiState.value.isGoalMaintained)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun processDeposit(amountMicros: Long) {
        viewModelScope.launch {
            val result = repository.depositSavings(amountMicros)
            loadDashboard() // Refresh state
            if (!result.isGoalMaintained) {
                // Trigger haptic/visual feedback for lockout
                repository.triggerLockoutWarning()
            }
        }
    }

    fun skipDepositToday(reason: String) {
        viewModelScope.launch {
            repository.recordMissedSavings(reason)
            loadDashboard()
        }
    }
}
```

### Dashboard Screen — Growth Visualization
```kotlin
// ui/screens/SavingsDashboardScreen.kt
package com.sulck.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sulck.app.ui.viewmodel.SavingsDashboardViewModel
import com.sulck.app.data.model.VaultStatus

@Composable
fun SavingsDashboardScreen(
    viewModel: SavingsDashboardViewModel = hiltViewModel(),
    onGoalClick: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Sulck", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surfaceContainerLow)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: Streak & Status
                StreakStatusCard(
                    streakDays = state.daysSavedStreak,
                    vaultStatus = state.vaultStatus,
                    isDeviceCompliant = state.isDeviceCompliant
                )

                // Primary Progress Ring
                PrimaryGoalProgressCard(state = state)

                // Active Goals Carousel
                if (state.activeGoals.isNotEmpty()) {
                    ActiveGoalsCarousel(
                        goals = state.activeGoals,
                        onGoalClick = onGoalClick
                    )
                }

                // Quick Actions
                QuickActionsRow(
                    onDepositClick = { viewModel.processDeposit(500_000) }, // 50 KES demo
                    onSkipClick = { viewModel.skipDepositToday("User skipped") }
                )

                // Boosts & Milestones
                BoostsSection()
            }
        }
    }
}

@Composable
fun StreakStatusCard(
    streakDays: Int,
    vaultStatus: VaultStatus,
    isDeviceCompliant: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val (label, icon, bgColor) = when (vaultStatus) {
        VaultStatus.ACTIVE_STREAK -> "Active Streak" to Icons.Default.LocalFireDepartment to colors.primaryContainer
        VaultStatus.SAVINGS_PAUSED -> "Streak Paused" to Icons.Default.PauseCircleOutline to colors.secondaryContainer
        VaultStatus.PENDING_LOCKOUT -> "Lockout Pending" to Icons.Default.WarningAmber to colors.errorContainer
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant)
            Column {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text("$streakDays Days 🔥", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (!isDeviceCompliant) {
                Text("Device Locked", style = MaterialTheme.typography.labelLarge, color = colors.error)
            }
        }
    }
}

@Composable
fun PrimaryGoalProgressCard(state: SavingsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Primary Goal", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${formatCurrency(state.currentSavings)} / ${formatCurrency(state.totalGoal)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = state.progressPercent,
                modifier = Modifier.fillMaxWidth().height(16.dp),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                color = if (state.isGoalMaintained) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${(state.progressPercent * 100).toInt()}% to unlock",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActiveGoalsCarousel(
    goals: List<GoalLockDto>,
    onGoalClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Your Goals", style = MaterialTheme.typography.titleMedium, modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(goals) { goal ->
                GoalCard(goal = goal, onClick = { onGoalClick(goal.goalId) })
            }
        }
    }
}

@Composable
fun GoalCard(goal: GoalLockDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(goal.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = (goal.currentProgress.amountMicros.toFloat() / goal.targetAmount.amountMicros).coerceIn(0f, 1f),
                color = MaterialTheme.colorScheme.primary
            )
            Text("${formatCurrency(goal.currentProgress)} / ${formatCurrency(goal.targetAmount)}", style = MaterialTheme.typography.bodySmall)
            Text("${goal.savingsCadenceInDays}-day cadence • ${goal.estimatedDaysToComplete}d left", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun QuickActionsRow(onDepositClick: () -> Unit, onSkipClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onSkipClick, modifier = Modifier.weight(1f)) {
            Text("Skip Today", style = MaterialTheme.typography.labelLarge)
        }
        Button(onClick = onDepositClick, modifier = Modifier.weight(1f)) {
            Text("Save Now", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun BoostsSection() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Boosts & Milestones", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BoostChip("7-Day Streak", "+2% APY", Icons.Default.EmojiEvents)
                BoostChip("Milestone: \$250", "Unlock Solar Tier", Icons.Default.SolarPower)
                BoostChip("Referral Bonus", "+\$50 on friend join", Icons.Default.PersonAdd)
            }
        }
    }
}

@Composable
fun BoostChip(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

// Helper
fun formatCurrency(amount: CurrencyAmountDto): String {
    val major = amount.amountMicros / 1_000_000
    val minor = (amount.amountMicros % 1_000_000) / 10_000
    return "${amount.currencyCode} ${major}.${minor.toString().padStart(2, '0')}"
}
```

---

## Device Lock Integration (Platform Channel)

```kotlin
// platform/DeviceLockManager.kt
package com.sulck.app.platform

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

interface DeviceLockManager {
    fun setDeviceLocked(locked: Boolean, reason: String)
    fun isDeviceLocked(): Boolean
    fun setLockScreenMessage(message: String)
}

// Implementation uses DevicePolicyManager (COPE/DPC) or OEM lock APIs
@RequiresApi(Build.VERSION_CODES.M)
class AndroidDeviceLockManager @Inject constructor(
    private val context: Context
) : DeviceLockManager {
    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(context, DeviceAdminReceiver::class.java)

    override fun setDeviceLocked(locked: Boolean, reason: String) {
        if (locked) {
            dpm.lockNow()
            setLockScreenMessage("Savings streak paused. Deposit to unlock.")
        } else {
            setLockScreenMessage("")
        }
    }

    override fun isDeviceLocked(): Boolean = dpm.isDeviceLocked()

    override fun setLockScreenMessage(message: String) {
        dpm.setLockTaskFeatures(adminComponent, 0)
        // Custom lock screen message via DevicePolicyManager
    }
}
```

---

## Repository & Sync Layer

```kotlin
// data/repository/SavingsRepository.kt
package com.sulck.app.data.repository

import com.sulck.app.data.local.SavingsDao
import com.sulck.app.data.remote.SavingsApi
import com.sulck.app.data.model.*
import kotlinx.coroutines.flow.Flow

class SavingsRepository @Inject constructor(
    private val dao: SavingsDao,
    private val api: SavingsApi,
    private val deviceLock: DeviceLockManager
) {
    suspend fun getSavingsAccount(): SavingsAccountDto = 
        api.syncAccount().also { dao.upsertAccount(it) } ?: dao.getAccount()

    suspend fun getOnboardingState(): SavingsOnboardingStateDto = dao.getOnboardingState()

    suspend fun getActiveGoals(): List<GoalLockDto> = dao.getActiveGoals()

    @Suppress("UNUSED_PARAMETER")
    suspend fun depositSavings(amountMicros: Long): DepositResult = 
        api.deposit(amountMicros).also { dao.recordDeposit(it) }

    suspend fun recordMissedSavings(reason: String) = api.recordMissed(reason)

    suspend fun syncDeviceLockState(isCompliant: Boolean) {
        deviceLock.setDeviceLocked(!isCompliant, if (isCompliant) "" else "Savings streak paused")
    }

    suspend fun triggerLockoutWarning() {
        // Haptic, notification, lock screen message
    }
}

data class DepositResult(
    val newBalance: CurrencyAmountDto,
    val isGoalMaintained: Boolean,
    val unlockedGoals: List<String> = emptyList()
)
```

---

## Goals Catalog (Replaces Phone Catalog)

| Goal ID | Title | Target | Cadence | Tags | Unlock Reward |
|---------|-------|--------|---------|------|---------------|
| `emergency_fund` | Emergency Vault | KES 50,000 | Daily | emergency, safety | Unlock: Insurance marketplace |
| `solar_tier_1` | Solar Starter Kit | KES 120,000 | Daily | solar, energy | Unlock: Pay-as-you-go solar panel |
| `education_vault` | Education Fund | KES 200,000 | Weekly | education, family | Unlock: Course marketplace |
| `cash_reserve` | Cash Reserve | KES 100,000 | Daily | liquidity, business | Unlock: Merchant credit line |

---

## Navigation Flow

1. **Onboarding** → Set first goal + daily commit → `SavingsOnboardingStateDto`
2. **Dashboard** → `SavingsDashboardScreen` (streak, progress, goals, actions)
3. **Deposit Flow** → Amount entry → Confirmation → `DepositResult` → Lock sync
4. **Goal Detail** → Milestone timeline, boost activation, progress projection
5. **Streak Recovery** → Missed day → Catch-up deposit or pause → `SAVINGS_PAUSED`
6. **Settings** → Device compliance, notifications, security, export data

---

## Build / Tech Stack (Preserved from M-KOPA)

- **Language**: Kotlin + Jetpack Compose (Material 3)
- **Architecture**: Clean Architecture + Hilt DI + Flow/StateFlow
- **Data**: Protobuf → Room (local) + Retrofit (remote)
- **Lock**: DevicePolicyManager (COPE/DPC) or OEM lock API
- **Messaging**: Firebase Cloud Messaging (streak reminders, lockout warnings)
- **Analytics**: Custom events (deposit, streak, lockout, goal_complete)

---

## Files to Create in New Workspace

```
D:\Desktop\AppHub\SulckProject\
├── app\src\main\
│   ├── java\com\sulck\app\
│   │   ├── data\
│   │   │   ├── accounts\SavingsAccountDto.kt (from proto)
│   │   │   ├── goals\GoalLockDto.kt
│   │   │   ├── onboarding\SavingsOnboardingStateDto.kt
│   │   │   ├── catalog\GoalsCatalogDto.kt
│   │   │   ├── personalisation\SavingsHomeConfigDto.kt
│   │   │   ├── customer\CustomerProfileDto.kt
│   │   │   ├── local\SavingsDao.kt
│   │   │   ├── remote\SavingsApi.kt
│   │   │   ├── repository\SavingsRepository.kt
│   │   │   └── model\SavingsUiState.kt
│   │   ├── ui\
│   │   │   ├── viewmodel\SavingsDashboardViewModel.kt
│   │   │   ├── screens\SavingsDashboardScreen.kt
│   │   │   └── components\ (StreakCard, ProgressCard, GoalCard, BoostChip)
│   │   ├── platform\DeviceLockManager.kt
│   │   └── di\AppModule.kt
│   ├── proto\
│   │   ├── accounts.proto
│   │   ├── goals.proto
│   │   ├── onboarding.proto
│   │   ├── catalog.proto
│   │   └── home_config.proto
│   └── res\ (Compose resources)
├── build.gradle.kts (Compose BOM, Hilt, Room, Retrofit, Protobuf)
└── settings.gradle.kts
```

---

## Execution Checklist

- [ ] Create `savings_blueprint.md` (this file)
- [ ] Generate `.proto` files in `SulckProject/app/src/main/proto/`
- [ ] Run Protobuf Gradle plugin → generates Kotlin data classes
- [ ] Implement `SavingsRepository`, `SavingsDao`, `SavingsApi`
- [ ] Build `SavingsDashboardViewModel` + `SavingsDashboardScreen`
- [ ] Wire `DeviceLockManager` (test on device with DPC)
- [ ] Add streak/lockout notifications (FCM)
- [ ] QA: Simulate streak pause → verify lock → deposit → verify unlock