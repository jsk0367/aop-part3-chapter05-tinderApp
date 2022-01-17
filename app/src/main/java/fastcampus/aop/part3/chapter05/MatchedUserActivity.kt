package fastcampus.aop.part3.chapter05

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MatchedUserActivity: AppCompatActivity() {

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var usersDB: DatabaseReference
    private val adapter = MatchedUserAdapter()
    private val cardItems = mutableListOf<CardItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match_list)

        val recyclerView = findViewById<RecyclerView>(R.id.matchedUserRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        usersDB = FirebaseDatabase.getInstance().reference.child("Users")
        getMatchUsers()
    }

    private fun getMatchUsers(){
        val matchedDB = usersDB.child(getCurrentUserID()).child("likedBy").child("match")
        matchedDB.addChildEventListener(object : ChildEventListener{
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                if (dataSnapshot.key?.isNotEmpty() == true){
                    getMatchUser(dataSnapshot.key.orEmpty())
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun getMatchUser(userId: String){
        val mathedDb = usersDB.child(userId)
        mathedDb.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                cardItems.add(CardItem(userId, snapshot.child("name").value.toString()))
                adapter.submitList(cardItems)
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun getCurrentUserID(): String {
        if (auth.currentUser == null){
            Toast.makeText(this, "로그인이 되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
        //return auth.currentUser?.uid.orEmpty()
        return auth.currentUser!!.uid
    }

}