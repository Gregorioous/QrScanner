package com.example.qrscanner

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.qrscanner.DB.MainDb
import com.example.qrscanner.DB.Product
import com.example.qrscanner.ui.theme.Purple80
import com.example.qrscanner.ui.theme.QRScannerTheme
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var mainDb: MainDb
    var counter = 0

    //    private var textFieldValue: String = ""
    private val scanLauncher = registerForActivityResult(
        ScanContract()
    ) { result ->
        if (result.contents == null) {
            Toast.makeText(this, "Scan data is null", Toast.LENGTH_SHORT).show()
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                val productByQr = mainDb.dao.getProductsByQr(result.contents)
                if (productByQr == null) {
                    mainDb.dao.insertProduct(
                        Product(
                            null,
                            "Product - ${counter++}",
                            result.contents
                        )
                    )
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Item saved",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Item Duplicated",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            }
        }
    }

    private val scanCheckLauncher = registerForActivityResult(
        ScanContract()
    ) { result ->
        if (result.contents == null) {
            Toast.makeText(this, "Scan data is null", Toast.LENGTH_SHORT).show()
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                val productByQr = mainDb.dao.getProductsByQr(result.contents)
                if (productByQr == null) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Product not added", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    mainDb.dao.updateProduct(productByQr.copy(isChecked = true))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val productStateList = mainDb.dao.getAllProducts()
                .collectAsState(initial = emptyList())

            QRScannerTheme {
                MainUi(productStateList)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PopupWithTextField(
        isOpen: Boolean,
        onClose: () -> Unit,
        onTextChanged: (String) -> Unit,
        textFieldValue: String
    ) {
        val currentTextFieldValue = remember { mutableStateOf(textFieldValue) }
        if (isOpen) {
            DisposableEffect(currentTextFieldValue.value) {
                onTextChanged(currentTextFieldValue.value)
                onDispose { }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        TextField(
                            value = textFieldValue,
                            onValueChange = {
                                onTextChanged(it)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Enter some text...")
                            },
                            keyboardActions = KeyboardActions.Default,
                            keyboardOptions = KeyboardOptions.Default
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            onTextChanged("")
                            onClose()
                            scan()
                        }) {
                            Text("Next step")
                        }
                    }
                }
            }
        }
    }


    @Composable
    private fun MainUi(productStateList: State<List<Product>>) {
        var isPopupOpen by remember { mutableStateOf(false) }
        var textFieldValue by rememberSaveable { mutableStateOf("") }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
            ) {
                items(productStateList.value) { product ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (product.isChecked) {
                                Color.Blue
                            } else {
                                Purple80
                            },
                            contentColor = if (product.isChecked) {
                                Purple80
                            } else {
                                Color.Blue
                            },
                        )
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(15.dp),
                            text = product.name,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Button(onClick = { isPopupOpen = true }) {
                Text(text = "Add new product")
            }
            Button(onClick = { scanCheck() }) {
                Text(text = "Check product")
            }
        }

        if (isPopupOpen) {
            PopupWithTextField(
                isOpen = isPopupOpen,
                onClose = { isPopupOpen = false },
                onTextChanged = { textFieldValue = it },
                textFieldValue = textFieldValue
            )
        }
    }

    private fun scan() {
        scanLauncher.launch(getScanOptions())
    }

    private fun scanCheck() {
        scanCheckLauncher.launch(getScanOptions())
    }

    private fun getScanOptions(): ScanOptions {
        return ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan a barcode")
            setCameraId(0)
            setBeepEnabled(false)
            setBarcodeImageEnabled(true)
        }
    }

}