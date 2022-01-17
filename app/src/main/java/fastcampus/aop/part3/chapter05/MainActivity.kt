package fastcampus.aop.part3.chapter05

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.helloworldTextView).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        //앱이 실행 되었을때
        if (auth.currentUser == null) { // 로그인이 되면 auth의 currentUser에 로그인한 정보가 저장된다
            //currentUser == null ->로그인 실패
            startActivity(Intent(this, LoginActivity::class.java))
        } else{
            startActivity(Intent(this, LikeActivity::class.java))
            finish()
        }
    }
}