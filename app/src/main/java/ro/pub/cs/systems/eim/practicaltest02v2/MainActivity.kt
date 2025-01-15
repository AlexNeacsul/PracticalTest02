package ro.pub.cs.systems.eim.practicaltest02v2

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import ro.pub.cs.systems.eim.practicaltest02v2.ui.theme.PracticalTest02v2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PracticalTest02v2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainContent(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainContent(modifier: Modifier = Modifier) {
    // Variabile de stare
    var word by remember { mutableStateOf("") }
    var definition by remember { mutableStateOf("Definiția va apărea aici.") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Layout principal
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Titlu "Client"
        Text(
            text = "Client",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Câmp de introducere pentru cuvânt
        OutlinedTextField(
            value = word,
            onValueChange = { word = it },
            label = { Text("Introduceți un cuvânt") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Titlu "Definiție"
        Text(
            text = "Definiție",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Câmp pentru afișarea definiției
        Text(
            text = definition,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp)
        )

        // Afișare eroare, dacă există
        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Buton pentru a genera definiția
        Button(onClick = {
            if (word.isNotBlank()) {
                coroutineScope.launch {
                    val result = fetchDefinition(word)
                    if (result != null) {
                        definition = result
                        errorMessage = null
                    } else {
                        errorMessage = "Nu am găsit definiția pentru \"$word\"."
                        definition = "Definiția va apărea aici."
                    }
                }
            } else {
                errorMessage = "Vă rugăm să introduceți un cuvânt."
            }
        }) {
            Text(text = "Generează definiția")
        }
    }
}

// Funcție pentru a efectua cererea HTTP
suspend fun fetchDefinition(word: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://api.dictionaryapi.dev/api/v2/entries/en/$word")
                .build()
            val response = client.newCall(request).execute()

            // Verificăm dacă răspunsul este cu succes
            if (response.isSuccessful) {
                val responseBody = response.body?.string()

                // Adăugăm un log pentru a vedea răspunsul complet
                println("Response: $responseBody")

                // Verificăm dacă răspunsul conține datele corecte
                if (responseBody != null && responseBody.isNotEmpty()) {
                    val jsonArray = try {
                        JSONArray(responseBody)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return@withContext null
                    }

                    // Verificăm dacă există date în răspuns
                    if (jsonArray.length() > 0) {
                        // Obținem prima definiție din răspuns
                        val firstMeaning = jsonArray
                            .getJSONObject(0) // primul obiect (pentru cuvântul căutat)
                            .getJSONArray("meanings") // extragem lista de sensuri
                            .getJSONObject(0) // primul sens
                            .getJSONArray("definitions") // extragem lista de definiții
                            .getJSONObject(0) // prima definiție
                            .getString("definition") // textul definiției

                        return@withContext firstMeaning
                    } else {
                        return@withContext "Nu am găsit definiția pentru \"$word\"."
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext "Eroare la obținerea definiției."
    }
}

@Preview(showBackground = true)
@Composable
fun MainContentPreview() {
    PracticalTest02v2Theme {
        MainContent()
    }
}
