package fastcampus.aop.part3.chapter05

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LoginActivity: AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signUpButton = findViewById<Button>(R.id.signUpButton)
        val facebookLoginButton = findViewById<LoginButton>(R.id.facebookLoginButton)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        successLogin()
                    } else {
                        Toast.makeText(this, "로그인에 실패했습니다. 이메일 또는 비밀번호를 확인해 주세요.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
        //이메일과 패스워드 에딧텍스트가 비어있을때 로그인과 회원가입을 비활성화 시키는 방법
        emailEditText.addTextChangedListener {
            val enable = emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()
            loginButton.isEnabled = enable
            signUpButton.isEnabled = enable
        }

        passwordEditText.addTextChangedListener {
            val enable = emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()
            loginButton.isEnabled = enable
            signUpButton.isEnabled = enable
        }

        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "회원가입에 성공했습니다. 로그인 버튼을 눌러 로그인해주세요.", Toast.LENGTH_SHORT).show()
                    } else { Toast.makeText(this, "이미 가입한 이메일이거나, 회원가입에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        callbackManager = CallbackManager.Factory.create()
        facebookLoginButton.setPermissions("email", "public_profile")//권한 요청
        facebookLoginButton.registerCallback(callbackManager, object: FacebookCallback<LoginResult>{
            override fun onSuccess(loginResult: LoginResult) {
                Log.d("Facebook", "facebook:onSuccess:$loginResult")
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {
                Log.d("Facebook", "facebook:onCancel")
            }

            override fun onError(error: FacebookException) {
                Log.d("Facebook", "facebook:onError", error)
                Toast.makeText(this@LoginActivity, "페이스북 로그인에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    successLogin()
                } else {
                    Toast.makeText(this, "페이스북 로그인에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun successLogin(){
        if(auth.currentUser == null){
            Toast.makeText(this, "로그인에 실패했습니다.",Toast.LENGTH_SHORT).show()
            return
        }

        //val userId = auth.currentUser?.uid.orEmpty()
        val userId: String = auth.currentUser!!.uid
        val currenUserDB = Firebase.database.reference.child("Users").child(userId)
        val user = mutableMapOf<String, Any>()
        user["userId"] = userId
        currenUserDB.updateChildren(user)

        finish()
    }
}