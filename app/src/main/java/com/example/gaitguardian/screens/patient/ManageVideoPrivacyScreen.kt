import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.gaitguardian.screens.VideoPrivacyDialog
import com.example.gaitguardian.viewmodels.PatientViewModel


@Composable
fun ManageVideoPrivacyScreen(navController: NavHostController, patientViewModel: PatientViewModel) {
    val saveVideos by patientViewModel.saveVideos.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Manage Video Privacy", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.padding(8.dp))
        Text("Video saving is currently: ${if (saveVideos) "ENABLED" else "DISABLED"}")

        Spacer(modifier = Modifier.padding(8.dp))
        Button(onClick = { showDialog = true }) {
            Text(if (saveVideos) "Disable Saving" else "Enable Saving")
        }

        if (showDialog) {
            VideoPrivacyDialog(
                saveVideos = saveVideos,
                onConfirm = {
                    patientViewModel.setSaveVideos(!saveVideos)
                    showDialog = false
                },
                onDismiss = { showDialog = false }
            )
        }
    }
}
