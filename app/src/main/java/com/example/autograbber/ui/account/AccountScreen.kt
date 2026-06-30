package com.example.autograbber.ui.account

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.autograbber.data.models.UserProfile
import com.example.autograbber.ui.theme.*
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.example.autograbber.data.UserRepository
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import kotlinx.coroutines.launch

private enum class AuthState {
    LANDING, LOGIN, SIGN_UP, FORGOT_PASSWORD, PROFILE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onNavigateToSubscription: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val userRepository = remember { UserRepository() }
    val scope = rememberCoroutineScope()
    val colors = LocalV2Colors.current
    
    var authState by remember { mutableStateOf(AuthState.LANDING) }
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("Please wait...") }
    
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }

    // Update state based on auth
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            loadingMessage = "Signing in..."
            isLoading = true
            val profile = userRepository.getUserProfile(currentUser!!.uid)
            if (profile != null) {
                userProfile = profile
            } else {
                val newProfile = UserProfile(
                    id = currentUser!!.uid,
                    fullName = currentUser!!.displayName ?: "",
                    email = currentUser!!.email ?: ""
                )
                userRepository.saveUserProfile(newProfile)
                userProfile = newProfile
            }
            authState = AuthState.PROFILE
            isLoading = false
        } else {
            authState = AuthState.LANDING
            userProfile = null
        }
    }

    BackHandler {
        when (authState) {
            AuthState.SIGN_UP, AuthState.LOGIN, AuthState.FORGOT_PASSWORD -> authState = AuthState.LANDING
            AuthState.LANDING -> onNavigateBack()
            else -> onNavigateBack()
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = colors.background,
        modifier = modifier.fillMaxSize()
    ) { _ -> 
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxSize()
        ) {
            // Header Row (Matches Account/Filter style) - Moved closer to top
            if (authState != AuthState.PROFILE) {
                 Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        when (authState) {
                            AuthState.SIGN_UP, AuthState.LOGIN, AuthState.FORGOT_PASSWORD -> authState = AuthState.LANDING
                            AuthState.LANDING -> onNavigateBack()
                            else -> onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                    Text(
                        when (authState) {
                            AuthState.LANDING -> "Welcome to AutoGrabber"
                            AuthState.LOGIN -> "Login"
                            AuthState.SIGN_UP -> "Create"
                            AuthState.FORGOT_PASSWORD -> "Reset Password"
                            else -> ""
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textPrimary)
                    }
                    
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(onClick = {
                        auth.signOut()
                        currentUser = null
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = V2Error)
                    }
                }
            }
            
            HorizontalDivider(color = colors.textPrimary.copy(alpha = 0.05f))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(
                        if (authState == AuthState.PROFILE) {
                            Modifier.verticalScroll(scrollState)
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))
                
                when (authState) {
                    AuthState.LANDING -> {
                        LandingView(
                            onLoginClick = { authState = AuthState.LOGIN },
                            onSignUpClick = { authState = AuthState.SIGN_UP }
                        )
                    }
                    AuthState.PROFILE -> {
                        userProfile?.let { profile ->
                            ProfileView(
                                profile = profile,
                                onNavigateToSubscription = onNavigateToSubscription,
                                onSaveProfile = { updated ->
                                    scope.launch {
                                        loadingMessage = "Updating profile..."
                                        isLoading = true
                                        if (userRepository.saveUserProfile(updated)) {
                                            userProfile = updated
                                            Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                                        }
                                        isLoading = false
                                    }
                                },
                                onUploadImage = { uri ->
                                    scope.launch {
                                        loadingMessage = "Uploading image..."
                                        isLoading = true
                                        val url = userRepository.uploadProfilePicture(profile.id, uri)
                                        if (url != null) {
                                            val updated = profile.copy(profilePictureUrl = url)
                                            if (userRepository.saveUserProfile(updated)) {
                                                userProfile = updated
                                                Toast.makeText(context, "Image updated", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                                        }
                                        isLoading = false
                                    }
                                }
                            )
                        }
                    }
                    AuthState.LOGIN -> {
                        LoginView(
                            onForgotPasswordClick = { authState = AuthState.FORGOT_PASSWORD },
                            onLogin = { email, password ->
                                loadingMessage = "Authenticating..."
                                isLoading = true
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        isLoading = false
                                        if (task.isSuccessful) {
                                            currentUser = auth.currentUser
                                        } else {
                                            Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            }
                        )
                    }
                    AuthState.SIGN_UP -> {
                        SignUpView(
                            onSignUp = { name, email, password, phone ->
                                loadingMessage = "Creating account..."
                                isLoading = true
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val user = auth.currentUser
                                            val newProfile = UserProfile(
                                                id = user?.uid ?: "",
                                                fullName = name,
                                                email = email,
                                                phoneNumber = phone,
                                                approved = false
                                            )
                                            scope.launch {
                                                userRepository.saveUserProfile(newProfile)
                                                currentUser = user
                                                userProfile = newProfile
                                                isLoading = false
                                                Toast.makeText(context, "Account Created Successfully", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            isLoading = false
                                            Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            }
                        )
                    }
                    AuthState.FORGOT_PASSWORD -> {
                        ForgotPasswordView(
                            onBackToLoginClick = { authState = AuthState.LOGIN },
                            onResetPassword = { email ->
                                loadingMessage = "Sending reset link..."
                                isLoading = true
                                auth.sendPasswordResetEmail(email)
                                    .addOnCompleteListener { task ->
                                        isLoading = false
                                        if (task.isSuccessful) {
                                            Toast.makeText(context, "Reset link sent to your email", Toast.LENGTH_LONG).show()
                                            authState = AuthState.LOGIN
                                        } else {
                                            Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            }
                        )
                    }
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }

        if (isLoading) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.85f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = V2Primary,
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = loadingMessage,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileView(
    profile: UserProfile,
    onNavigateToSubscription: () -> Unit,
    onSaveProfile: (UserProfile) -> Unit,
    onUploadImage: (Uri) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalV2Colors.current
    
    var editingPersonal by remember { mutableStateOf(false) }
    var editingPassword by remember { mutableStateOf(false) }
    
    var fullName by remember(profile.fullName) { mutableStateOf(profile.fullName) }
    var phoneNumber by remember(profile.phoneNumber) { mutableStateOf(profile.phoneNumber) }
    
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    
    val cropLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { onUploadImage(it) }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            cropLauncher.launch(
                CropImageContractOptions(
                    uri = it,
                    cropImageOptions = CropImageOptions().apply {
                        guidelines = CropImageView.Guidelines.ON
                        cropShape = CropImageView.CropShape.OVAL
                        fixAspectRatio = true
                        aspectRatioX = 1
                        aspectRatioY = 1
                    }
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Picture with pencil icon
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(colors.surface)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (profile.profilePictureUrl != null) {
                    AsyncImage(
                        model = profile.profilePictureUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = colors.textSecondary
                    )
                }
            }
            
            Surface(
                color = V2Primary,
                shape = CircleShape,
                modifier = Modifier
                    .size(32.dp)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .border(2.dp, colors.background, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { galleryLauncher.launch("image/*") }
            ) {
                Icon(
                    Icons.Default.Edit, 
                    null, 
                    tint = Color.White, 
                    modifier = Modifier.padding(6.dp).size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        InfoCard(
            title = "Manage Subscription",
            icon = Icons.Default.Star,
            isEditing = false,
            showEdit = false, 
            onEditClick = onNavigateToSubscription
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (profile.hasLifetimeAccess) "Lifetime Access" else "Free Account",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = if (profile.hasLifetimeAccess) V2Primary else colors.textPrimary
                    )
                    Text(
                        text = if (profile.hasLifetimeAccess) "All features unlocked" else "Upgrade to unlock all features",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                
                Button(
                    onClick = onNavigateToSubscription,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = V2Primary.copy(alpha = 0.1f), 
                        contentColor = V2Primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        if (profile.hasLifetimeAccess) "View" else "Upgrade", 
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        InfoCard(
            title = "Personal Information",
            icon = Icons.Default.Info,
            isEditing = editingPersonal,
            onEditClick = { 
                if (editingPersonal) {
                    onSaveProfile(profile.copy(fullName = fullName, phoneNumber = phoneNumber))
                }
                editingPersonal = !editingPersonal 
            }
        ) {
            if (editingPersonal) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = V2Primary,
                        unfocusedBorderColor = colors.textPrimary.copy(alpha = 0.1f),
                        unfocusedTextColor = colors.textPrimary,
                        focusedTextColor = colors.textPrimary,
                        focusedLabelColor = V2Primary,
                        unfocusedLabelColor = colors.textSecondary
                    )
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = V2Primary,
                        unfocusedBorderColor = colors.textPrimary.copy(alpha = 0.1f),
                        unfocusedTextColor = colors.textPrimary,
                        focusedTextColor = colors.textPrimary,
                        focusedLabelColor = V2Primary,
                        unfocusedLabelColor = colors.textSecondary
                    )
                )
            } else {
                InfoRow(label = "Full Name", value = profile.fullName)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = colors.textPrimary.copy(alpha = 0.05f))
                InfoRow(label = "Phone", value = profile.phoneNumber)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = colors.textPrimary.copy(alpha = 0.05f))
            InfoRow(label = "Email", value = profile.email)
            // Removed Date of Birth row as requested
        }

        Spacer(modifier = Modifier.height(16.dp))

        InfoCard(
            title = "Security",
            icon = Icons.Default.Lock,
            isEditing = editingPassword,
            onEditClick = {
                if (editingPassword) {
                    if (currentPassword.isNotEmpty() && newPassword.length >= 6) {
                        val user = FirebaseAuth.getInstance().currentUser
                        val credential = EmailAuthProvider.getCredential(user?.email!!, currentPassword)
                        
                        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                            if (reauthTask.isSuccessful) {
                                user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                                    if (updateTask.isSuccessful) {
                                        Toast.makeText(context, "Password updated", Toast.LENGTH_SHORT).show()
                                        editingPassword = false
                                        currentPassword = ""
                                        newPassword = ""
                                    } else {
                                        Toast.makeText(context, "Update failed: ${updateTask.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else if (newPassword.isNotEmpty() && newPassword.length < 6) {
                        Toast.makeText(context, "New password too short", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    editingPassword = true
                }
            }
        ) {
            if (editingPassword) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = V2Primary,
                        unfocusedBorderColor = colors.textPrimary.copy(alpha = 0.1f),
                        unfocusedTextColor = colors.textPrimary,
                        focusedTextColor = colors.textPrimary,
                        focusedLabelColor = V2Primary,
                        unfocusedLabelColor = colors.textSecondary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = V2Primary,
                        unfocusedBorderColor = colors.textPrimary.copy(alpha = 0.1f),
                        unfocusedTextColor = colors.textPrimary,
                        focusedTextColor = colors.textPrimary,
                        focusedLabelColor = V2Primary,
                        unfocusedLabelColor = colors.textSecondary
                    )
                )
                if (editingPassword) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(onClick = { 
                            editingPassword = false
                            currentPassword = ""
                            newPassword = ""
                        }) {
                            Text("Cancel", color = V2Error)
                        }
                    }
                }
            } else {
                InfoRow(label = "Password", value = "••••••••")
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    icon: ImageVector,
    isEditing: Boolean = false,
    showEdit: Boolean = true,
    onEditClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalV2Colors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.textPrimary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = V2Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = colors.textSecondary,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                
                if (showEdit) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (isEditing) Icons.Default.Check else Icons.Default.Edit, 
                            null, 
                            tint = if (isEditing) V2Success else V2Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colors = LocalV2Colors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )
        Text(
            text = value.ifEmpty { "Not set" },
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LandingView(
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    val colors = LocalV2Colors.current
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = null,
            tint = V2Primary,
            modifier = Modifier.size(100.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "AutoGrabber",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = colors.textPrimary
        )
        
        Text(
            text = "Automate your gig platform offers with ease",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = V2Primary)
        ) {
            Text("Login", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onSignUpClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, V2Primary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = V2Primary)
        ) {
            Text("Sign Up", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun LoginView(
    onForgotPasswordClick: () -> Unit,
    onLogin: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val colors = LocalV2Colors.current

    Spacer(modifier = Modifier.height(32.dp))

    Text(
        text = "Welcome Back",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary
    )
    Text(
        text = "Sign in to continue",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.textSecondary,
        modifier = Modifier.padding(bottom = 32.dp)
    )

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        leadingIcon = { Icon(Icons.Default.Email, null, tint = V2Primary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = V2Primary,
            unfocusedBorderColor = colors.textPrimary.copy(alpha = 0.1f),
            unfocusedTextColor = colors.textPrimary,
            focusedTextColor = colors.textPrimary,
            focusedLabelColor = V2Primary,
            unfocusedLabelColor = colors.textSecondary
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        leadingIcon = { Icon(Icons.Default.Lock, null, tint = V2Primary) },
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = colors.textSecondary
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = V2Primary,
            unfocusedBorderColor = colors.textPrimary.copy(alpha = 0.1f),
            unfocusedTextColor = colors.textPrimary,
            focusedTextColor = colors.textPrimary,
            focusedLabelColor = V2Primary,
            unfocusedLabelColor = colors.textSecondary
        )
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        TextButton(onClick = onForgotPasswordClick) {
            Text("Forgot Password?", color = V2Primary)
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = { 
            if (email.isNotEmpty() && password.isNotEmpty()) {
                onLogin(email, password) 
            }
        },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = V2Primary)
    ) {
        Text("Login", style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

private enum class SignUpStep {
    PERSONAL, ACCOUNT
}

@Composable
private fun SignUpView(
    onSignUp: (String, String, String, String) -> Unit
) {
    var step by remember { mutableStateOf(SignUpStep.PERSONAL) }
    
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val colors = LocalV2Colors.current
    
    val passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,}$".toRegex()
    val passwordErrorMessage = "Min 6 characters, uppercase, lowercase and special character"

    BackHandler(enabled = step == SignUpStep.ACCOUNT) {
        step = SignUpStep.PERSONAL
    }

    Spacer(modifier = Modifier.height(32.dp))

    Text(
        text = if (step == SignUpStep.PERSONAL) "Step 1: Personal Details" else "Step 2: Account Details",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary,
    )
    Text(
        text = if (step == SignUpStep.PERSONAL) "" else "",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.textSecondary,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    // Progress Indicator
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(CircleShape)
                .background(if (step == SignUpStep.PERSONAL) V2Primary else colors.textPrimary.copy(alpha = 0.1f))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(CircleShape)
                .background(if (step == SignUpStep.ACCOUNT) V2Primary else colors.textPrimary.copy(alpha = 0.1f))
        )
    }

    if (step == SignUpStep.PERSONAL) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, null, tint = V2Primary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = V2Primary,
                unfocusedBorderColor = colors.textPrimary.copy(alpha = 0.1f),
                unfocusedTextColor = colors.textPrimary,
                focusedTextColor = colors.textPrimary,
                focusedLabelColor = V2Primary,
                unfocusedLabelColor = colors.textSecondary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            leadingIcon = { Icon(Icons.Default.Phone, null, tint = V2Primary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = V2Primary,
                unfocusedBorderColor = colors.textPrimary.copy(alpha = 0.1f),
                unfocusedTextColor = colors.textPrimary,
                focusedTextColor = colors.textPrimary,
                focusedLabelColor = V2Primary,
                unfocusedLabelColor = colors.textSecondary
            )
        )
    } else {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, null, tint = V2Primary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = V2Primary,
                unfocusedBorderColor = colors.textPrimary.copy(alpha = 0.1f),
                unfocusedTextColor = colors.textPrimary,
                focusedTextColor = colors.textPrimary,
                focusedLabelColor = V2Primary,
                unfocusedLabelColor = colors.textSecondary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it 
                passwordError = if (it.isNotEmpty() && !it.matches(passwordRegex)) passwordErrorMessage else null
            },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = V2Primary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = passwordError != null,
            supportingText = {
                if (passwordError != null) {
                    Text(text = passwordError!!, color = V2Error, fontSize = 11.sp)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = V2Primary,
                unfocusedBorderColor = colors.textPrimary.copy(alpha = 0.1f),
                unfocusedTextColor = colors.textPrimary,
                focusedTextColor = colors.textPrimary,
                errorBorderColor = V2Error,
                focusedLabelColor = V2Primary,
                unfocusedLabelColor = colors.textSecondary
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = V2Primary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword,
            supportingText = {
                if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
                    Text(text = "Password does not match", color = V2Error, fontSize = 11.sp)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = V2Primary,
                unfocusedBorderColor = colors.textPrimary.copy(alpha = 0.1f),
                unfocusedTextColor = colors.textPrimary,
                focusedTextColor = colors.textPrimary,
                errorBorderColor = V2Error,
                focusedLabelColor = V2Primary,
                unfocusedLabelColor = colors.textSecondary
            )
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = { 
            if (step == SignUpStep.PERSONAL) {
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    step = SignUpStep.ACCOUNT
                } else {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (email.isNotEmpty() && password.matches(passwordRegex)) {
                    if (password == confirmPassword) {
                        onSignUp(name, email, password, phone)
                    } else {
                        Toast.makeText(context, "Password does not match", Toast.LENGTH_SHORT).show()
                    }
                } else if (!password.matches(passwordRegex)) {
                    passwordError = passwordErrorMessage
                    Toast.makeText(context, "Password does not meet requirements", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
        },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = V2Primary)
    ) {
        Text(
            if (step == SignUpStep.PERSONAL) "Continue" else "Create Account", 
            style = MaterialTheme.typography.titleMedium, 
            color = Color.White
        )
    }
}

@Composable
private fun ForgotPasswordView(
    onBackToLoginClick: () -> Unit,
    onResetPassword: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    val colors = LocalV2Colors.current

    Text(
        text = "Reset Password",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary
    )
    Text(
        text = "Enter your email to receive a reset link",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.textSecondary,
        modifier = Modifier.padding(bottom = 32.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        leadingIcon = { Icon(Icons.Default.Email, null, tint = V2Primary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = V2Primary,
            unfocusedBorderColor = colors.textPrimary.copy(alpha = 0.1f),
            unfocusedTextColor = colors.textPrimary,
            focusedTextColor = colors.textPrimary,
            focusedLabelColor = V2Primary,
            unfocusedLabelColor = colors.textSecondary
        )
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = { if (email.isNotEmpty()) onResetPassword(email) },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = V2Primary)
    ) {
        Text("Send Reset Link", style = MaterialTheme.typography.titleMedium, color = Color.White)
    }

    Spacer(modifier = Modifier.height(16.dp))

    TextButton(onClick = onBackToLoginClick) {
        Text("Back to Login", fontWeight = FontWeight.Bold, color = V2Primary)
    }
}

@Preview(showBackground = true)
@Composable
fun AccountScreenPreview() {
    AutoGrabberTheme(darkTheme = true) {
        AccountScreen(onNavigateToSubscription = {}, onNavigateBack = {})
    }
}
