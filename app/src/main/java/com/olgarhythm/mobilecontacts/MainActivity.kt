package com.olgarhythm.mobilecontacts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.parcelize.Parcelize

@Parcelize
data class Contact(val name: String, val number: String) : Parcelable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ContactsScreen()
                }
            }
        }
    }

    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun loadContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            null
        )
        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val name = it.getString(nameIndex)
                val number = it.getString(numberIndex)
                contacts.add(Contact(name, number))
            }
        }
        return contacts
    }

    private fun openDialer(number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        startActivity(intent)
    }

    @Composable
    fun ContactsScreen() {
        var hasPermission by rememberSaveable { mutableStateOf(hasContactsPermission()) }
        var contacts by rememberSaveable(
            stateSaver = listSaver(save = { it }, restore = { it })
        ) { mutableStateOf(listOf<Contact>()) }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasPermission = granted
        }

        LaunchedEffect(Unit) {
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }

        LaunchedEffect(hasPermission) {
            if (hasPermission && contacts.isEmpty()) {
                contacts = loadContacts()
                Toast.makeText(
                    this@MainActivity,
                    "Найдено ${contacts.size} контактов",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        if (!hasPermission) {
            Text(
                text = "Нет доступа к контактам. Разрешите доступ в настройках приложения.",
                modifier = Modifier.padding(16.dp)
            )
            return
        }

        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(contacts) { contact ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openDialer(contact.number) }
                        .padding(16.dp)
                ) {
                    Text(text = contact.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(text = contact.number, fontSize = 14.sp)
                }
            }
        }
    }
}
