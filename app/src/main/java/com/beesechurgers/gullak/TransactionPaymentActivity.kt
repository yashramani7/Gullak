package com.beesechurgers.gullak

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beesechurgers.gullak.ui.theme.GullakTheme
import com.beesechurgers.gullak.ui.theme.backgroundColor
import com.beesechurgers.gullak.ui.theme.googleSansFont
import com.beesechurgers.gullak.ui.theme.monoFont
import com.beesechurgers.gullak.utils.DBConst
import com.beesechurgers.gullak.utils.DBListeners
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@OptIn(ExperimentalMaterial3Api::class)
class TransactionPaymentActivity : ComponentActivity() {

    companion object {
        var mOpened = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mOpened = true

        var title = ""
        var paymentAmount = ""

        val data = intent.extras
        if (data != null) {
            title = data.getString("title", "")
            paymentAmount = data.getString("_amount", "")
        }

        setContent {
            window.statusBarColor = backgroundColor().toArgb()

            GullakTheme(context = this) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = backgroundColor()),
                    contentColor = contentColorFor(backgroundColor = backgroundColor()),
                    topBar = {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color = backgroundColor())
                                .animateContentSize(),
                            color = backgroundColor(),
                            contentColor = contentColorFor(backgroundColor = backgroundColor()),
                        ) {
                            Text(
                                text = "Pay to\n${title}",
                                fontFamily = googleSansFont,
                                fontSize = 28.sp,
                                modifier = Modifier.padding(start = 40.dp, top = 40.dp),
                                lineHeight = 36.sp
                            )
                        }
                    },
                    content = {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color = backgroundColor())
                                .animateContentSize(),
                            color = backgroundColor(),
                            contentColor = contentColorFor(backgroundColor = backgroundColor()),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.CenterVertically)
                                ) {
                                    Text(
                                        text = "Amount to be paid",
                                        fontFamily = googleSansFont,
                                        fontSize = 20.sp,
                                        modifier = Modifier
                                            .padding(bottom = 16.dp, start = 24.dp)
                                    )

                                    var amount by rememberSaveable { mutableStateOf(paymentAmount) }
                                    OutlinedTextField(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                            .padding(start = 24.dp, end = 24.dp),
                                        value = amount,
                                        onValueChange = { amount = it },
                                        label = { Text(text = "Amount", fontFamily = googleSansFont) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(24.dp),
                                        textStyle = TextStyle.Default.copy(fontFamily = monoFont, fontSize = 24.sp),
                                        enabled = false,
                                        leadingIcon = {
                                            Icon(painter = painterResource(id = R.drawable.ic_rupee), contentDescription = "")
                                        }
                                    )

                                    Text(
                                        text = "Payment Mode",
                                        modifier = Modifier
                                            .padding(start = 24.dp, top = 56.dp),
                                        fontSize = 20.sp,
                                        fontFamily = googleSansFont
                                    )
                                    val (selectedOption, onOptionSelected) = remember { mutableStateOf(if (DBListeners.isWalletSetup.value) "Wallet" else "UPI") }
                                    listOf("Wallet", "UPI").forEach { text ->
                                        Card(modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp, start = 24.dp, end = 24.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            onClick = {
                                                if (text == "Wallet") {
                                                    if (!DBListeners.isWalletSetup.value) return@Card
                                                }
                                                onOptionSelected(text)
                                                Toast.makeText(this@TransactionPaymentActivity, text, Toast.LENGTH_LONG).show()
                                            }
                                        ) {
                                            Row {
                                                RadioButton(
                                                    selected = (text == selectedOption), modifier = Modifier.padding(8.dp),
                                                    onClick = {
                                                        onOptionSelected(text)
                                                        Toast.makeText(this@TransactionPaymentActivity, text, Toast.LENGTH_LONG).show()
                                                    }, enabled = if (text == "Wallet") DBListeners.isWalletSetup.value else true
                                                )
                                                Text(
                                                    text = text,
                                                    modifier = Modifier
                                                        .padding(start = 16.dp)
                                                        .align(Alignment.CenterVertically)
                                                )
                                            }
                                        }
                                    }

                                    val upiLauncher = rememberLauncherForActivityResult(
                                        contract = ActivityResultContracts.StartActivityForResult(),
                                        onResult = {
                                            val user = FirebaseAuth.getInstance().currentUser?: return@rememberLauncherForActivityResult
                                            val intent = it.data ?: return@rememberLauncherForActivityResult

                                            val extra = intent.extras ?: return@rememberLauncherForActivityResult
                                            if (!extra.getBoolean("success", false)) {
                                                Toast.makeText(this@TransactionPaymentActivity, "Payment Cancelled", Toast.LENGTH_SHORT)
                                                    .show()
                                            } else {
                                                FirebaseDatabase.getInstance().reference.child(DBConst.HISTORY_KEY).child(user.uid)
                                                    .child(System.currentTimeMillis().toString())
                                                    .updateChildren(HashMap<String, Any>().apply {
                                                        this[DBConst.AMOUNT_KEY] = amount.toDouble()
                                                        this[DBConst.PAYMENT_DESC_KEY] = title
                                                    })

                                                Toast.makeText(this@TransactionPaymentActivity, "Payment Success", Toast.LENGTH_SHORT)
                                                    .show()
                                            }
                                            startActivity(
                                                Intent(
                                                    this@TransactionPaymentActivity,
                                                    MainActivity::class.java
                                                ).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        }
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            if (selectedOption == "Wallet") {
                                                startActivity(
                                                    Intent(
                                                        this@TransactionPaymentActivity,
                                                        InvestmentTransactionActivity::class.java
                                                    ).putExtra("_amount", amount)
                                                )
                                            } else {
                                                upiLauncher.launch(
                                                    Intent(
                                                        this@TransactionPaymentActivity,
                                                        DummyUPIScreen::class.java
                                                    ).putExtra("_to", title)
                                                        .putExtra("_amount", amount.toDouble())
                                                )
                                            }
                                        }, modifier = Modifier
                                            .align(Alignment.End)
                                            .padding(top = 16.dp, end = 24.dp)
                                    ) {
                                        Icon(Icons.Rounded.ArrowForward, contentDescription = "")
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        mOpened = true
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        mOpened = false
    }
}