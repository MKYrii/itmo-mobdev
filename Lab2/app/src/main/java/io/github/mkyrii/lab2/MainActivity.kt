package io.github.mkyrii.lab2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import io.github.mkyrii.lab2.ui.theme.Lab2Theme
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.database.getStringOrNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab2Theme {
                // 1. Храним состояние: дали нам разрешение или нет?
                var hasPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                                == PackageManager.PERMISSION_GRANTED
                    )
                }

                // 2. "Пусковая установка" для запроса разрешения
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasPermission = isGranted // Обновляем состояние, экран сам перерисуется
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (hasPermission) {
                            // 3. Если разрешение есть — показываем список
                            ContactsListScreen()
                        } else {
                            // 4. Если нет — кнопку запроса (как в задании)
                            PermissionDeniedScreen { launcher.launch(Manifest.permission.READ_CONTACTS) }
                        }
                    }
                }
            }
        }
    }
}


data class Contact(val name: String?, val phoneNumber: String?, val email: String?)

@SuppressLint("Range")
fun Context.fetchAllContacts(): List<Contact> {
    Log.d("FETCH", "fetchAllContacts called")

    // Используем CommonDataKinds.Phone.CONTENT_URI (с буквой S)
    contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        null, null, null, null
    ).use { cursor: Cursor? ->
        if (cursor == null) return emptyList()
        return buildList {
            while (cursor.moveToNext()) {
                // Везде добавляем 's' к CommonDataKinds
                val name = cursor.getStringOrNull(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val phoneNumber = cursor.getStringOrNull(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val email = cursor.getStringOrNull(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS))

                add(Contact(name, phoneNumber, email))
            }
        }
    }
}


@Composable
fun PermissionDeniedScreen(onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center, // Центрируем по вертикали
        horizontalAlignment = Alignment.CenterHorizontally // Центрируем по горизонтали
    ) {
        Text(text = stringResource(R.string.no_permission))
        Spacer(modifier = Modifier.height(16.dp)) // Небольшой отступ
        Button(onClick = onGrantClick) {
            Text(text = stringResource(R.string.grant_permission))
        }
    }
}

@Composable
fun ContactsListScreen() {
    val context = LocalContext.current
    // Запоминаем список, чтобы не перечитывать его при каждом клике (минус лаг)
    val contacts by remember { mutableStateOf(context.fetchAllContacts()) }

    // Храним выбранный контакт. Изначально — null (ничего не выбрано)
    var selectedContact by remember { mutableStateOf<Contact?>(null) }

    // Если переменная selectedContact пустая — показываем список
    if (selectedContact == null) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(contacts) { contact ->
                Text(
                    text = contact.name ?: "Unknown",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // КЛИК: записываем контакт в переменную.
                            // Compose увидит это и перерисует экран (выполнит ветку else)
                            selectedContact = contact
                        }
                        .padding(16.dp)
                )
            }
        }
    } else {
        // Если контакт выбран — показываем его детали
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Берем данные из selectedContact
            Text(
                text = stringResource(
                    R.string.contact_details,
                    selectedContact?.name ?: "—",
                    selectedContact?.phoneNumber ?: "—",
                    // Если почта совпадает с номером телефона — выводим "—"
                    if (selectedContact?.email == selectedContact?.phoneNumber || selectedContact?.email == null)
                        "—"
                    else
                        selectedContact?.email!!
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Кнопка возврата: просто обнуляем переменную
            Button(onClick = { selectedContact = null }) {
                Text("Назад к списку")
            }
        }
    }
}
