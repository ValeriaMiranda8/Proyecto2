package com.example.proyecto

import android.content.Context
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.proyecto.ui.theme.ProyectoTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Room
import kotlinx.coroutines.launch
import java.io.File

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val password: String
)
@Dao
interface UserDao {

    @Insert
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    suspend fun login(username: String, password: String): User?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findUser(username: String): User?
}
@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
object DatabaseProvider {
    private var db: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        if (db == null) {
            db = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "animal_app_db"
            ).build()
        }
        return db!!
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Borra la base para pruebas
        //deleteDatabase("animal_app_db")


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
    val context = LocalContext.current

    // Prepara carpetas y crea el JSON si no existe
    LaunchedEffect(Unit) {
        prepareAnimalStorage(context)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopSection(modifier = Modifier.height(100.dp),username)

        Box(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = "inicio"
            ) {
                composable("inicio") { CenterSection(username = username, navController = navController) }
//                composable("buscar") { SearchScreen() }
                composable("perfil") { ProfileScreen(username = username,
                    onLogin = { newUser -> username = newUser }, navController = navController)
                }
                composable("registro") {
                    RegistroScreen(
                        onRegister = { username = it },
                        navController = navController
                    )
                }
                composable("subir") {
                    UploadAnimalScreen(
                        username = username,
                        navController = navController
                    )
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
            .background(Color(red = 49,72,122))
            .padding(top = 30.dp, start = 16.dp, bottom=16.dp)
    ) {
        Row {

            Image(painter = painterResource(id = R.drawable.logo),
                contentDescription = "logo",
                modifier = Modifier
                    .fillMaxHeight()
                    //.width(20.dp)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.FillHeight
            )

            Text(
                text = if (username != null) "Bienvenido, $username a F!" else "Bienvenido a F!",
                color = Color.White,
                fontSize = 30.sp,
                modifier = Modifier.padding(8.dp),
                fontFamily = FontFamily(Font(R.font.berkshireswash_regular))


            )
        }
    }
}

@Composable
fun CenterSection(modifier: Modifier = Modifier,
                  username: String?,
                  navController: NavHostController) {
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
            username = username,
            navController = navController)

    } else {
        // Si no hay selección → mostrar galería scrollable

        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .background(Color(red = 217,225,241))
                .padding(8.dp)
        ) {
            Text(
                text = "ADOPTA UN AMIGO",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.dynapuff_regular)),
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp).align(Alignment.CenterHorizontally)
            )
            Text(
                text = "PARA TODA LA VIDA",
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp).align(Alignment.CenterHorizontally),
                fontFamily = FontFamily(Font(R.font.playpensans_bold, FontWeight.Bold))
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
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                    fontFamily = FontFamily(Font(R.font.dynapuff_regular)),
                    color = Color(red = 49,72,122)

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
fun AnimalDetail(animal: Animal, onBack: () -> Unit, context: Context, username: String?, navController: NavHostController) {
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
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily(Font(R.font.dynapuff_regular)),

        )

        Text(
            text = animal.description,
            fontSize = 16.sp,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp),
            fontFamily = FontFamily(Font(R.font.playpensans_bold, FontWeight.Bold))
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

            // Si NO ha iniciado sesión → mostrar Adoptar y enviar al login
            if (username == null || username == "") {

                Button(
                    onClick = {
                        Toast.makeText(context, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
                        navController.navigate("perfil")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(red = 49, 72, 122))
                ) {
                    Text("Adoptar")
                }

            } else {
                // Si YA inició sesión → mostrar EN ADOPCIÓN
                Button(
                    onClick = {
                        Toast.makeText(context, "Solicitud de adopción enviada", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(red = 49, 122, 72))
                ) {
                    Text("Adoptar")
                }
            }

            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
            ) {
                Text("Regresar", color = Color(red = 49, 72, 122))
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
    val rawList: List<Animal> = run {
        val inputStream = context.resources.openRawResource(R.raw.animals)
        val json = inputStream.bufferedReader().use { it.readText() }
        Gson().fromJson(json, object : TypeToken<List<Animal>>() {}.type)
    }

    val internalFile = File(context.filesDir, "animales.json")
    val userList: List<Animal> =
        if (internalFile.exists()) {
            val json = internalFile.readText()
            Gson().fromJson(json, object : TypeToken<List<Animal>>() {}.type)
        } else {
            emptyList()
        }

    return rawList + userList
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
            Text("Inicio", color = Color(red = 49,72,122), fontSize = 14.sp)
        }
        if (username != null && username != ""){
            TextButton(onClick = { navController.navigate("subir") }) {
                Text("Subir", color = Color(red = 49,72,122), fontSize = 14.sp)
            }
        }

        TextButton(onClick = { navController.navigate("perfil") }) {
            Text("Perfil", color = Color(red = 49,72,122), fontSize = 14.sp)
        }
    }
}

@Composable
fun ProfileScreen(username: String?, onLogin: (String) -> Unit, navController: NavHostController) {
    var inputName by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var password by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

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
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(red = 49, 72, 122)),
                    onClick = {
                        if (inputName.isNotBlank() && password.isNotBlank()) {
                            coroutineScope.launch {
                                val dao = DatabaseProvider.getDatabase(context).userDao()
                                val user = dao.login(inputName.trim(), password.trim())

                                if (user != null) {
                                    onLogin(user.username)
                                    Toast.makeText(
                                        context,
                                        "Welcome",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                } else {
                                    Toast.makeText(
                                        context,
                                        "Usuario o contraseña incorrectos",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                }
                            }

                        }
                    }
                ) {
                    Text("Entrar")
                }
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(red = 49, 72, 122)),
                    onClick = {
                    navController.navigate("registro")
                }
                ){
                    Text("Registrar")
                }
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
            Text("Hola, $username ", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Button(colors = ButtonDefaults.buttonColors(containerColor = Color(red = 49,72,122)),
                onClick = { onLogin("") }) {
                Text("Cerrar sesión")
            }
        }
    }
}

@Composable
fun RegistroScreen(
    onRegister: (String) -> Unit,
    navController: NavHostController

) {
    var newUser by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf("") }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Crear usuario", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = newUser,
            onValueChange = { newUser = it },
            label = { Text("Nombre de usuario") },
            singleLine = true
        )
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("Contraseña") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (newUser.isNotBlank() && newPassword.isNotBlank()) {
                    val user = User(username = newUser.trim(), password = newPassword.trim())

                    coroutineScope.launch {
                        val dao = DatabaseProvider.getDatabase(context).userDao()

                        // Evitar registrar usuarios duplicados
                        val exists = dao.findUser(newUser.trim())
                        if (exists == null) {
                            dao.insertUser(user)
                            onRegister(newUser.trim())
                            Toast.makeText(
                                context,
                                "Usuario registrado",
                                Toast.LENGTH_SHORT
                            ).show()
                            navController.popBackStack()

                        } else {
                            Toast.makeText(
                                context,
                                "El usuario ya existe",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        ) {
            Text("Registrar")
        }
    }
}

@Composable
fun UploadAnimalScreen(username: String?, navController: NavHostController) {
    val context = LocalContext.current

    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var imageName by rememberSaveable { mutableStateOf("") } // nombre del drawable
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Subir nuevo animal", fontSize = 26.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre del animal") },
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descripción") }
        )

        OutlinedTextField(
            value = imageName,
            onValueChange = { imageName = it },
            label = { Text("Nombre de la imagen en drawable (sin .png)") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            colors = ButtonDefaults.buttonColors(containerColor = Color(red = 49,72,122)),
            onClick = {
                if (name.isNotBlank() && description.isNotBlank() && imageName.isNotBlank()) {
                    val newAnimal = Animal(
                        name = name,
                        description = description,
                        image = imageName
                    )

                    coroutineScope.launch {
                        saveAnimalToInternalJson(context, newAnimal)
                        Toast.makeText(context, "Animal subido", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                } else {
                    Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Text("Guardar animal")
        }
    }
}
fun saveAnimalToInternalJson(context: Context, newAnimal: Animal) {
    val file = File(context.filesDir, "animales.json")

    // Si NO existe → crear lista vacía
    val animals: MutableList<Animal> = if (file.exists()) {
        val json = file.readText()
        Gson().fromJson(json, object : TypeToken<MutableList<Animal>>() {}.type)
    } else {
        mutableListOf()
    }

    animals.add(newAnimal)

    // Guardar nuevamente el JSON
    file.writeText(Gson().toJson(animals))
}

fun prepareAnimalStorage(context: Context): File {
    val filesDir = context.filesDir

    // Carpeta donde se guardarán las imágenes de los animales
    val imagenesDir = File(filesDir, "imagenes")
    if (!imagenesDir.exists()) {
        imagenesDir.mkdirs()
    }

    // Archivo JSON donde se guardará la lista de animales
    val jsonFile = File(filesDir, "animales.json")

    // ----------------------------------------------------------
    // 🔥 DEBUG: BORRAR EL ARCHIVO SI EXISTE (comentar después)
    if (jsonFile.exists()) {
        jsonFile.delete()
        // Log.d("ANIMALES", "Archivo animales.json borrado por DEBUG")
    }
    // ----------------------------------------------------------

    if (!jsonFile.exists()) {
        jsonFile.writeText("[]")   // JSON vacío
    }

    return jsonFile
}

@Preview
@Composable
fun MainPrev(){
    MainScreen()
}