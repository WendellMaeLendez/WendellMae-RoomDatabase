package ph.edu.comteq.wendellmae_roomdatabase

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import ph.edu.comteq.wendellmae_roomdatabase.ui.theme.WendellMaeRoomDatabaseTheme

class MainActivity : ComponentActivity() {
    private val viewModel: NoteViewModel by viewModels()


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WendellMaeRoomDatabaseTheme {
                // Navigation controller - like a GPS for your app
                val navController = rememberNavController()

                // Define all the "roads" (screens) in your app
                NavHost(
                    navController = navController,
                    startDestination = "notes_list"  // Start here
                ) {
                    // Main list screen
                    composable("notes_list") {
                        NotesListScreenWithSearch(
                            viewModel = viewModel,
                            onAddNote = {
                                navController.navigate("note_edit/new")
                            },
                            onEditNote = { noteId ->
                                navController.navigate("note_edit/$noteId")
                            }
                        )
                    }

                    // Edit/Add note screen
                    composable(
                        route = "note_edit/{noteId}",
                        arguments = listOf(
                            navArgument("noteId") {
                                type = NavType.StringType
                            }
                        )
                    ) { backStackEntry ->
                        val noteIdString = backStackEntry.arguments?.getString("noteId")
                        val noteId = if (noteIdString == "new") null else noteIdString?.toIntOrNull()

                        NoteEditScreen(
                            noteId = noteId,
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

// Separate the main screen into its own composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotesListScreenWithSearch(
    viewModel: NoteViewModel,
    onAddNote: () -> Unit,
    onEditNote: (Int) -> Unit
){
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val notes by viewModel.allNotes.collectAsState(initial = emptyList())
    // Use notesWithTags instead of plain notes
    val notesWithTags by viewModel.allNotesWithTags.collectAsState(initial = emptyList())
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if(isSearchActive){
                // Search mode: Show the SearchBar
                SearchBar(
                    modifier = Modifier.fillMaxWidth(),
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = {
                                searchQuery = it
                                viewModel.updateSearchQuery(it)
                            },
                            onSearch = {},
                            expanded = true,
                            onExpandedChange = { shouldExpand ->
                                // This is called when the system wants to change expanded state
                                if (!shouldExpand) {
                                    // User wants to collapse/exit search
                                    isSearchActive = false
                                    searchQuery = ""
                                    viewModel.clearSearch()
                                }
                            },
                            placeholder = {Text("Search notes...")},
                            leadingIcon = {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    searchQuery = ""
                                    viewModel.clearSearch()
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Close search"
                                    )
                                }
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        viewModel.clearSearch()
                                    }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear search"
                                        )
                                    }
                                }
                            }
                        )
                    },
                    expanded = true,
                    onExpandedChange = { shouldExpand ->
                        // Handle when SearchBar wants to change expanded state
                        if (!shouldExpand) {
                            isSearchActive = false
                            searchQuery = ""
                            viewModel.clearSearch()
                        }
                    }
                ) {
                    // content shown inside the search view
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        if (notesWithTags.isEmpty()) {
                            item {
                                Text(
                                    text = "No notes found",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            items(notesWithTags) { note ->
                                NoteCard(note = note.note, tags = note.tags)
                            }
                        }
                    }
                }
            } else {
                // Normal Mode
                TopAppBar(
                    title = { Text("Notes") },
                    actions = {
                        IconButton(onClick = {isSearchActive = true}) {
                            Icon(Icons.Filled.Search, "Search")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNote) {
                Icon(Icons.Filled.Add, "Add Note")
            }
        }
    ) { innerPadding ->
        NotesListScreen(
            notes = notesWithTags,
            modifier = Modifier.padding(innerPadding),
            onEditNote = onEditNote
        )
    }
}



@Composable
fun NotesListScreen(
    notes: List<NoteWithTags>,
    modifier: Modifier = Modifier,
    onEditNote: (Int) -> Unit
) {
    LazyColumn(modifier = modifier.padding(8.dp)) {
        items(notes, key = { it.note.id }) { noteWithTags ->
            NoteCard(
                note = noteWithTags.note,
                tags = noteWithTags.tags,
                onClick = { onEditNote(noteWithTags.note.id) }
            )
        }
    }
}




@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    note: Note,
    tags: List<Tag> = emptyList(),
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onDelete: (Note) -> Unit = {}
    ){
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog){

    }
    Card (
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ){
        Column (
            modifier = Modifier.padding(16.dp)){

            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ){
                Text(
                    text = DateUtils.formatDateTime(note.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                if (note.category.isNotEmpty()){
                    Surface (
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ){
                        Text(
                            text = note.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
//            title
            Text(
                text = note.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )

            // Content preview
            if (note.content.isNotEmpty()) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // show tags
            if (tags.isNotEmpty()) {
                FlowRow (
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ){
                    tags.forEach { tag ->
                        TagChip(tag = tag)
                    }
                }
            }
        }
    }
}

// NEW: Composable for displaying a single tag
@Composable
fun TagChip(
    tag: Tag,
    onRemove: (() -> Unit)? = null  // Optional: for removing tags
) {
    Surface(
        color = Color(android.graphics.Color.parseColor(tag.color)).copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            1.dp,
            Color(android.graphics.Color.parseColor(tag.color))
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = tag.name,
                style = MaterialTheme.typography.labelSmall,
                color = Color(android.graphics.Color.parseColor(tag.color))
            )

            // Optional X button to remove tag
            onRemove?.let {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove tag",
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { it() },
                    tint = Color(android.graphics.Color.parseColor(tag.color))
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: Int?,
    viewModel: NoteViewModel,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(noteId) {
        if (noteId != null) {
            viewModel.getNoteById(noteId)?.let { note ->
                title = note.title
                content = note.content
                category = note.category
            }
        }
    }

    IconButton(onClick = {
        scope.launch {
            noteId?.let {
                val note = viewModel.getNoteById(it)
                if (note != null) {
                    viewModel.delete(note)
                    Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                } else {
                    Toast.makeText(context, "Note not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }) {
        Icon(Icons.Default.Delete, contentDescription = "Delete Note")
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null) "Add Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Simple Delete Button (only if editing an existing note)
                    if (noteId != null) {
                        IconButton(onClick = {
                            scope.launch {
                                val note = viewModel.getNoteById(noteId)
                                if (note != null) {
                                    viewModel.delete(note)
                                    Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                } else {
                                    Toast.makeText(context, "Note not found", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Note")
                        }
                    }

                    // Save Button
                    IconButton(onClick = {
                        if (title.isNotBlank() && content.isNotBlank()) {
                            scope.launch {
                                if (noteId == null) {
                                    // Add new note
                                    val newNote = Note(
                                        title = title.trim(),
                                        content = content.trim(),
                                        category = category.trim(),
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    viewModel.insert(newNote)
                                    Toast.makeText(context, "Note added", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Update existing note
                                    val existing = viewModel.getNoteById(noteId)
                                    existing?.let {
                                        viewModel.update(
                                            it.copy(
                                                title = title.trim(),
                                                content = content.trim(),
                                                category = category.trim(),
                                                updatedAt = System.currentTimeMillis()
                                            )
                                        )
                                        Toast.makeText(context, "Note updated", Toast.LENGTH_SHORT).show()
                                    } ?: Toast.makeText(context, "Note not found", Toast.LENGTH_SHORT).show()
                                }
                                onNavigateBack()
                            }
                        } else {
                            Toast.makeText(context, "Title and content cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Done, contentDescription = "Save Note")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                placeholder = { Text("Enter category...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                maxLines = Int.MAX_VALUE
            )
        }
    }
}



















//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    WendellMaeRoomDatabaseTheme {
//        NotesListScreen("Android")
//    }
//}