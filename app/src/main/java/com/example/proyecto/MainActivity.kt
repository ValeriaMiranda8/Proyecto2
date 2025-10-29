package com.example.proyecto

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Horizontal
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.proyecto.ui.theme.ProyectoTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var username by rememberSaveable { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopSection(modifier = Modifier.height(100.dp),username)

        Box(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = "inicio"
            ) {
                composable("inicio") { CenterSection() }
//                composable("buscar") { SearchScreen() }
                composable("perfil") { ProfileScreen(username = username,
                    onLogin = { newUser -> username = newUser })
                }
            }
        }

        BottomMenu(username,navController)
    }
}

@Composable
fun TopSection(modifier: Modifier = Modifier, username: String?) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1565C0))
            .padding(top = 30.dp, start = 16.dp, bottom=16.dp)
    ) {
        Text(
            text = if (username != null) "Bienvenido, $username" else "Bienvenido",
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
fun CenterSection(modifier: Modifier = Modifier) {
    //val adoptaImg: Int = R.drawable.adopta
    val adoptaList = listOf<Int>(R.drawable.adopta, R.drawable.adopta2)
    // Lista de animales (simulada)
    val context = LocalContext.current
    val animals = remember { loadAnimalsFromJson(context) }

    // Estado: animal seleccionado (para modo zoom)
    var selectedAnimal by remember { mutableStateOf<Animal?>(null) }

    // Si hay un animal seleccionado → mostrar detalle
    if (selectedAnimal != null) {
        AnimalDetail(
            context = context,
            animal = selectedAnimal!!,
            onBack = { selectedAnimal = null },
            onAdopt = { /* Acción de adoptar */ }
        )
    } else {
        // Si no hay selección → mostrar galería scrollable

        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF5F5F5))
                .padding(8.dp)
        ) {
            Text(
                text = "Adopta a tu perro o gato",
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp).align(Alignment.CenterHorizontally)
            )
            Text(
                text = "Adopta, no compres. Al abrir las puertas de tu hogar a un perro o un gato rescatado, no solo estás ganando un amigo incondicional, estás salvando una vida y dándole una segunda oportunidad que se merecen.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp).align(Alignment.CenterHorizontally)
            )
            Image(painter = painterResource(id = adoptaList.random()),
                contentDescription = "Adopta",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.FillWidth
            )
            Spacer(modifier = Modifier.height(100.dp))
            animals.forEach { animal ->
                val imageRes = getDrawableId(context, animal.image)
                Text(
                    text = animal.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = animal.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedAnimal = animal }
                    ,
                    contentScale = ContentScale.Crop
                )

            }
        }
    }
}

// Vista detallada con zoom, descripción y botones
@Composable
fun AnimalDetail(animal: Animal, onBack: () -> Unit, onAdopt: () -> Unit, context: Context) {
    val scale = remember { Animatable(1f) }
    val imageRes = getDrawableId(context, animal.image)
    LaunchedEffect(Unit) {
        scale.animateTo(1.2f) // animación de zoom suave
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = animal.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(16.dp))
                .graphicsLayer(scaleX = scale.value, scaleY = scale.value),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = animal.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = animal.description,
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onAdopt) {
                Text("Adoptar")
            }
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)) {
                Text("Regresar")
            }
        }
    }
}



// Modelo de datos
data class Animal(
    val name: String,
    val description: String,
    val image: String
)

fun loadAnimalsFromJson(context: Context): List<Animal> {
    val inputStream = context.resources.openRawResource(R.raw.animals)
    val json = inputStream.bufferedReader().use { it.readText() }
    val type = object : TypeToken<List<Animal>>() {}.type
    return Gson().fromJson(json, type)
}
fun getDrawableId(context: Context, name: String): Int {
    return context.resources.getIdentifier(name, "drawable", context.packageName)
}
@Composable
fun BottomMenu(username: String?,navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEEEEEE))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        TextButton(onClick = { navController.navigate("inicio") }) {
            Text("Inicio")
        }
        if (username != null && username != ""){
            TextButton(onClick = { navController.navigate("inicio") }) {
                Text("Subir")
            }
        }

        TextButton(onClick = { navController.navigate("perfil") }) {
            Text("Perfil")
        }
    }
}

@Composable
fun ProfileScreen(username: String?, onLogin: (String) -> Unit) {
    var inputName by rememberSaveable { mutableStateOf("") }

    if (username == null || username == "") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Iniciar sesión", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = inputName,
                onValueChange = { inputName = it },
                label = { Text("Usuario") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (inputName.isNotBlank()) {
                        onLogin(inputName.trim())
                    }
                }
            ) {
                Text("Entrar")
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Hola, $username 👋", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onLogin("") }) {
                Text("Cerrar sesión")
            }
        }
    }
}



@Preview
@Composable
fun MainPrev(){
    MainScreen()
}