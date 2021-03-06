package com.beesechurgers.gullak

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beesechurgers.gullak.ui.theme.GullakTheme
import com.beesechurgers.gullak.ui.theme.backgroundColor
import com.beesechurgers.gullak.ui.theme.googleSansFont
import com.beesechurgers.gullak.ui.theme.strokeColor
import com.beesechurgers.gullak.utils.Auth
import com.beesechurgers.gullak.utils.Auth.oneTapGoogleSignIn
import com.beesechurgers.gullak.utils.DBConst
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoadingActivity : ComponentActivity() {

    companion object {
        private const val TAG = "LoadingActivity"
    }

    private lateinit var activityRequestLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
            FirebaseDatabase.getInstance().reference.child(DBConst.USER_KEY).get()
            FirebaseDatabase.getInstance().reference.child(DBConst.DATA_KEY).child(user.uid).get()

            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
            return
        }

        activityRequestLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { Auth.activityCallback(it) }
        setContent {
            window.statusBarColor = backgroundColor().toArgb()
            LoadingScreen()
        }
    }

    @Preview(showBackground = true, showSystemUi = true, name = "MainActivity")
    @Composable
    fun LoadingScreen() {
        GullakTheme(this) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = backgroundColor())
                    .animateContentSize(),
                color = backgroundColor(),
                contentColor = contentColorFor(backgroundColor = backgroundColor()),
            ) {
                Row(
                    Modifier
                        .padding(16.dp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(start = 100.dp, end = 100.dp),
                            painter = painterResource(id = R.drawable.ic_gullak),
                            contentDescription = "SignInButton",
                            tint = Color.Unspecified
                        )

                        Text(
                            text = "Welcome to Gullak",
                            fontFamily = googleSansFont,
                            fontSize = 24.sp,
                            color = Color(0xff35b276),
                            modifier = Modifier
                                .padding(top = 56.dp)
                                .align(Alignment.CenterHorizontally)
                        )

                        SignInButton(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 150.dp)
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun SignInButton(modifier: Modifier = Modifier) {
        val isLoading = remember { mutableStateOf(false) }

        val oneTapClient = remember { Identity.getSignInClient(this) }
        val signInRequest = remember {
            BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                    BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(getString(R.string.default_web_client_id))
                        .setFilterByAuthorizedAccounts(false).build()
                ).build()
        }

        Surface(
            modifier = modifier
                .clickable(
                    enabled = !isLoading.value,
                    onClick = {
                        isLoading.value = true
                        Log.d(TAG, "BottomSection: clicked")
                        oneTapGoogleSignIn(oneTapClient, signInRequest, isLoading, activityRequestLauncher)
                    }
                ),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(width = 1.dp, color = strokeColor()),
            color = Color.White
        ) {
            Row(
                modifier = Modifier.padding(end = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_logo_google),
                    contentDescription = "SignInButton",
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(8.dp))

                Text(text = if (isLoading.value) "Signing in ..." else "Sign in with Google", color = Color.Black)
                if (isLoading.value) {
                    Spacer(modifier = Modifier.width(16.dp))
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(16.dp)
                            .width(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}