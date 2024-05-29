package com.example.mycardgame

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycardgame.models.BoardSize
import com.example.mycardgame.models.MemoryGame
import com.example.mycardgame.models.UserImage
import com.example.mycardgame.utils.EXTRA_BOARD_SIZE
import com.example.mycardgame.utils.EXTRA_GAME_NAME
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase



class MainActivity : AppCompatActivity() {

    companion object{
        private const val TAG = "MainActivity"
        private const val CREATE_REQUEST_CODE = 248
    }

    private lateinit var movetv : TextView
    private lateinit var pairtv : TextView
    private lateinit var mainboard : RecyclerView
    private lateinit var clroot : CoordinatorLayout

    private val db = Firebase.firestore
    private var gameName: String? = null
    private var customGameImages: List<String>? = null
    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryAdaptor


    private var boardSize:BoardSize = BoardSize.EASY

    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContentView(R.layout.activity_main)
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.clroot)) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
        insets
    }

        clroot = findViewById(R.id.clroot)
        movetv = findViewById(R.id.movetextView)
        pairtv = findViewById(R.id.pairstextView)
        mainboard = findViewById(R.id.mainboard)

//        val intent = Intent(this,CreateActivity::class.java)
//        intent.putExtra(EXTRA_BOARD_SIZE,BoardSize.HARD)
//        startActivity(intent)

        setupBoard()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_refresh -> {
                if (memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGame()) {
                    showAlertDialog("Quit your Current Game?", null, View.OnClickListener {setupBoard() })
                } else {
//                    Toast.makeText(this,"From Refresh",Toast.LENGTH_SHORT).show()
                    setupBoard()
                }
            }
            R.id.ic_menu ->{
//                Toast.makeText(this,"From size",Toast.LENGTH_SHORT).show()
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom ->{
                showCreationDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == CREATE_REQUEST_CODE && requestCode == RESULT_OK) {
            val customGameName: String? = data?.getStringExtra(EXTRA_GAME_NAME)
            if(customGameName == null) {
                Log.e(TAG,"Got null custom game from CreateActivity")
                return
            }
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun downloadGame(customgameName: String) {
        db.collection("games").document(customgameName).get().addOnSuccessListener { document ->
            val userImagelist = document.toObject(UserImage::class.java)
            if(userImagelist?.images == null) {
                Log.e(TAG,"Invalid custom game data from FireStore..")
                Snackbar.make(clroot,"Sorry, we couldn't find any such game, '$gameName'",Snackbar.LENGTH_LONG).show()
                return@addOnSuccessListener
            }
            val numCards = userImagelist.images.size *2
            boardSize = BoardSize.getByValue(numCards)
            customGameImages = userImagelist.images
            setupBoard()
            gameName = customgameName
        }.addOnFailureListener{ exception ->
            Log.e(TAG,"Exception when retrieving the game",exception)
        }
    }

    private fun showCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radiogroup: RadioGroup = boardSizeView.findViewById(R.id.rbgroup)

        showAlertDialog("Create your own Memory Board...",boardSizeView,View.OnClickListener {
            val desiredBoardSize: BoardSize = when(radiogroup.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            val intent = Intent(this,CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE,desiredBoardSize)
            startActivityForResult(intent,CREATE_REQUEST_CODE)
        })
    }

    private fun showNewSizeDialog() {

        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radiogroup: RadioGroup = boardSizeView.findViewById(R.id.rbgroup)
        when (boardSize) {
            BoardSize.EASY -> radiogroup.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radiogroup.check(R.id.rbMedium)
            BoardSize.HARD -> radiogroup.check(R.id.rbHard)
        }
        showAlertDialog("Choose Difficulty Level",boardSizeView,View.OnClickListener {
            boardSize = when(radiogroup.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            gameName = null
            customGameImages = null
            setupBoard()
        })
    }

    private fun showAlertDialog(title: String, view: View?,positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("OK"){_, _ ->
                positiveClickListener.onClick(null)
            }
            .show()
    }

    private fun setupBoard() {
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
        when(boardSize) {
            BoardSize.EASY -> {
                movetv.text = "Easy: 4 x 2"
                pairtv.text = "Pairs: 0 / 4"
            }
            BoardSize.MEDIUM -> {
                movetv.text = "Medium: 6 x 3"
                pairtv.text = "Pairs: 0 / 9"
            }
            BoardSize.HARD -> {
                movetv.text = "Hard: 6 x 4"
                pairtv.text = "Pairs: 0 / 12"
            }
        }

        pairtv.setTextColor(ContextCompat.getColor(this,R.color.color_progress_none))
        memoryGame = MemoryGame(boardSize,customGameImages)

        mainboard.setHasFixedSize(true)
        adapter = MemoryAdaptor(this,boardSize,memoryGame.cards,object: MemoryAdaptor.CardClickListener{
            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }
        })
        mainboard.adapter = adapter
        mainboard.layoutManager = GridLayoutManager(this,boardSize.getwidth())

    }

    private fun updateGameWithFlip(position: Int) {
        if(memoryGame.haveWonGame()) {
            Snackbar.make(clroot,"You already Won the Game",Snackbar.LENGTH_LONG).show()
            return
        }
        if(memoryGame.isCardFaceUp(position)) {
            Snackbar.make(clroot,"Invalid Move!..",Snackbar.LENGTH_SHORT).show()
            return
        }
        if(memoryGame.flipcards(position)) {
            Log.i(TAG,"Found a Match!..Num pairs Found: ${memoryGame.numPairsfound}")
            val colors = ArgbEvaluator().evaluate(
                memoryGame.numPairsfound.toFloat() / boardSize.getNumPair(),
                ContextCompat.getColor(this,R.color.color_progress_none),
                ContextCompat.getColor(this,R.color.color_progress_full),
            ) as Int
            pairtv.setTextColor(colors)
            pairtv.text = "Pairs: ${memoryGame.numPairsfound} / ${boardSize.getNumPair()}"
            if(memoryGame.haveWonGame()){
                Snackbar.make(clroot,"You Won Congratulations..",Snackbar.LENGTH_LONG).show()
            }
        }
        movetv.text =  "Moves: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()
    }
}