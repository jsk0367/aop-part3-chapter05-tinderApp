package fastcampus.aop.part3.chapter05

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.yuyakaido.android.cardstackview.*
import fastcampus.aop.part3.chapter05.DBKey.Companion.DIS_LIKE
import fastcampus.aop.part3.chapter05.DBKey.Companion.LIKE
import fastcampus.aop.part3.chapter05.DBKey.Companion.LIKED_BY
import fastcampus.aop.part3.chapter05.DBKey.Companion.MATCH
import fastcampus.aop.part3.chapter05.DBKey.Companion.NAME
import fastcampus.aop.part3.chapter05.DBKey.Companion.USERS
import fastcampus.aop.part3.chapter05.DBKey.Companion.USER_ID

class LikeActivity: AppCompatActivity(), CardStackListener {


    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var userDB: DatabaseReference

    private val adapter = CardStackAdapter()
    private val cardItems = mutableListOf<CardItem>()

    private val manager by lazy {
        CardStackLayoutManager(this, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_like)

        userDB = Firebase.database.reference.child(USERS)

        val currentUserDB = userDB.child(getCurrentUserID())
        currentUserDB.addListenerForSingleValueEvent(object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.child(NAME).value == null){
                    showNameInputPopup()
                    return
                }
                //유저정보 갱신해라
                getUnSelectedUsers()
            }


            override fun onCancelled(error: DatabaseError) {

            }

        })

        initCardStackView()
        initSignOutButton()
        initMatchedListButton()
    }

    private fun initCardStackView(){
        val stackView = findViewById<CardStackView>(R.id.cardStackView)

        stackView.layoutManager = manager
        stackView.adapter = adapter

        manager.setStackFrom(StackFrom.Top)
        manager.setTranslationInterval(8.0f)
        manager.setSwipeThreshold(0.1f)
    }

    private fun initSignOutButton(){
        val signOutButton = findViewById<Button>(R.id.signOutButton)
        signOutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

    }
    private fun initMatchedListButton() {
        val matcedListButton = findViewById<Button>(R.id.matchListButton)
        matcedListButton.setOnClickListener {
            startActivity(Intent(this, MatchedUserActivity::class.java))
            finish()
        }
    }

    private fun getCurrentUserID(): String {
        if (auth.currentUser == null){
            Toast.makeText(this, "로그인이 되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
        return auth.currentUser!!.uid
    }


    private fun getUnSelectedUsers(){
        userDB.addChildEventListener(object : ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.child(USER_ID).value != getCurrentUserID() //유저 아이디가 나와 같지 않아야한다
                    && snapshot.child(LIKED_BY).child(LIKE).hasChild(getCurrentUserID()).not()
                    // 내가 이유저를 라이크 했을때 그 유저의 정보에 내가 라이크한것을 저장
                    && snapshot.child(LIKED_BY).child(DIS_LIKE).hasChild(getCurrentUserID()).not()) {//상대방의 값에 내가 있는지 확인
                    // 내가 이유저를 디스라이크 했을때 그 유저의 정보에 내가 디스라이크한것을 저장
                    //-> 이유저는 내가 한번도 선택한적이 없는 유저 조건이 성립

                    val userId = snapshot.child(USER_ID).value.toString()
                    var name = "undecided"
                    if (snapshot.child(NAME).value != null) {
                        name = snapshot.child(NAME).value.toString()
                    }

                    //리사이클러뷰 갱신하기
                    cardItems.add(CardItem(userId, name))
                    adapter.submitList(cardItems)
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
                //상대방의 이름이 변경되었을떄 갱신해주기
                cardItems.find { it.userId == dataSnapshot.key }?.let {//만약 카드 아이템스에 파인드 함수로 가져온 유저아이디가 스냅샷 키와 동일하면 무시
                    it.name = dataSnapshot.child("name").value.toString()
                }
                //만약 수정이 되었다면 어댑터의 서밋리스트를 통해 카드 아이템을 갱신한다
                adapter.submitList(cardItems)
                adapter.notifyDataSetChanged()

            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    private fun showNameInputPopup(){
        val editText = EditText(this)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.write_name))
            .setView(editText)
            .setPositiveButton("저장"){ _, _ ->
                if (editText.text.isEmpty()) {
                    showNameInputPopup()
                } else {
                    saveUserName(editText.text.toString())
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun saveUserName(name: String){

        val userId = getCurrentUserID()
        val currenUserDB = userDB.child(userId)
        val user = mutableMapOf<String, Any>()
        user[USER_ID] = userId
        user[NAME] = name
        currenUserDB.updateChildren(user)

        //유저정보가져오는 함수
        getUnSelectedUsers()

    }


    private fun like() {

        //카드 가져오기
        val card = cardItems[manager.topPosition - 1]
        cardItems.removeFirst()

        //카드저장
        userDB.child(card.userId)
            .child(LIKED_BY)
            .child(LIKE)
            .child(getCurrentUserID())
            .setValue(true)

        saveMatchIfOtherUserLikeMe(card.userId)
        Toast.makeText(this,"Like 하셨습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun saveMatchIfOtherUserLikeMe(otherUserId: String){
        val otherUserDB = userDB.child(getCurrentUserID()).child(LIKED_BY).child(LIKE).child(otherUserId)
        otherUserDB.addListenerForSingleValueEvent(object : ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == true){
                    userDB.child(getCurrentUserID())
                        .child(LIKED_BY)
                        .child(MATCH)
                        .child(otherUserId)
                        .setValue(true)

                    userDB.child(otherUserId)
                        .child(LIKED_BY)
                        .child(MATCH)
                        .child(getCurrentUserID())
                        .setValue(true)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }//즉시 값을 알수있는 함수


        })

    }

    private fun disLike(){
        //카드 가져오기
        val card = cardItems[manager.topPosition - 1]
        cardItems.removeFirst()

        //카드저장
        userDB.child(card.userId)
            .child(LIKED_BY)
            .child(DIS_LIKE)
            .child(getCurrentUserID())
            .setValue(true)


        Toast.makeText(this,"DisLike 하셨습니다.", Toast.LENGTH_SHORT).show()

    }
    override fun onCardDragging(direction: Direction?, ratio: Float) {}

    override fun onCardSwiped(direction: Direction?) {
        when (direction) {
            Direction.Right -> like()
                Direction.Left -> disLike()
            else -> {

            }

        }

    }

    override fun onCardRewound() {}

    override fun onCardCanceled() {}

    override fun onCardAppeared(view: View?, position: Int) {}

    override fun onCardDisappeared(view: View?, position: Int) {}
}