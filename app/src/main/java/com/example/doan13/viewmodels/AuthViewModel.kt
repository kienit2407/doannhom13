package com.example.doan13.viewmodels

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doan13.R
import com.example.doan13.data.models.auth.CreateUserModel
import com.example.doan13.data.models.auth.UserModel
import com.example.doan13.data.models.songs.SongModels
import com.example.doan13.data.repositories.AuthRepositories
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseUser

import com.google.firebase.firestore.FieldValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository : AuthRepositories
)  : ViewModel() { //kế thừa view model: tránh mất dư xiêu khi xoay màn hình, tách biệt hoàn toàn với logic UI, giúp

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001
    private val firebaseFireStore = FirebaseFirestore.getInstance()

    val registrationSuccess = MutableLiveData<Boolean>()
    val loginSuccess = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String?>()
    val signOutSuccess = MutableLiveData<Boolean>()
    val documentData = MutableLiveData<UserModel?>()
    val userInfo = MutableLiveData<String>()
    val resetSuccess = MutableLiveData<Boolean>()
    val creatorName = MutableLiveData<String>()
    val recentlyPlayedUpdated = MutableLiveData<Boolean>() // Thêm LiveData cho trạng thái cập nhật
    val stateModifyName = MutableLiveData<Boolean>() // Thêm LiveData cho trạng thái cập nhật
    private val _userInfoPublic = MutableLiveData<UserModel?>()
    val userInfoPublic: LiveData<UserModel?> get() = _userInfoPublic
    private val _myTracks = MutableLiveData<List<SongModels>>()
    val myTracks: LiveData<List<SongModels>> get() = _myTracks

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading


// Lấy danh sách bài hát của người dùng
    fun getMyTracksByUserId(userId: String) {
        viewModelScope.launch {
            val tracks = authRepository.getMyTracksByUserId(userId)
            _myTracks.postValue(tracks)
        }
    }
    // Lấy danh sách bài hát mà người dùng đã đăng dựa trên userId
    fun updateRecentlyPlayed(userId: String, songId: String) {
        viewModelScope.launch {
            val result = authRepository.updateRecentlyPlayed(userId, songId)
            if (result.isSuccess) {
                recentlyPlayedUpdated.postValue(true) // Báo hiệu cập nhật thành công
                Log.d("AuthViewModel", "Cập nhật recentlyPlayed thành công cho $userId với $songId")
            } else {
                errorMessage.postValue(result.exceptionOrNull()?.message)
                Log.e("AuthViewModel", "Lỗi cập nhật recentlyPlayed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun modifyName(userId: String, newName: String) {
        viewModelScope.launch {
           authRepository.updateUserName(userId, newName)
            stateModifyName.postValue(true)
        }
    }

    fun getuserId(): String?{
        return authRepository.getCurrentUserId()
    }
    fun initGoogle(context: Context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.clientId))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }


    fun register(user: CreateUserModel) {
        _loading.value = true
        viewModelScope.launch {
            val result = authRepository.register(user)
            if (result.isSuccess) {
                registrationSuccess.postValue(true)
            } else {
                errorMessage.postValue(result.exceptionOrNull()?.message)
            }
            _loading.value = false
        }
    }

    fun login(email: String, password: String) {
        _loading.value = true
        viewModelScope.launch {
            val result = authRepository.login(email, password)
            if (result.isSuccess) {
                loginSuccess.postValue(true)
            } else {
                errorMessage.postValue(result.exceptionOrNull()?.message)
            }
            _loading.value = false
        }
    }

    fun signInWithGoogle(idToken: String) {
        _loading.value = true
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            if (result.isSuccess) {
                loginSuccess.postValue(true)
            } else {
                errorMessage.postValue(result.exceptionOrNull()?.message)
            }
            _loading.value = false
        }
    }

    fun loadUser() {
        _loading.value = true
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                val result = authRepository.loadUser(userId)
                if (result.isSuccess) {
                    documentData.postValue(result.getOrNull())
                    if (result.getOrNull() == null) {
                        userInfo.postValue("Không tìm thấy user")
                    }
                } else {
                    errorMessage.postValue(result.exceptionOrNull()?.message)
                }
            } else {
                userInfo.postValue("Chưa có người dùng đăng nhập")
            }
            _loading.value = false
        }
    }


    fun signOut() {
        _loading.value = true
        viewModelScope.launch {
            authRepository.signOut()
            googleSignInClient.signOut()
            signOutSuccess.postValue(true)
            _loading.value = false
        }

    }

    fun resetPassword(email: String) {
        _loading.value = true
        viewModelScope.launch {
            val result = authRepository.resetPassword(email)
            if (result.isSuccess) {
                resetSuccess.postValue(true)
            } else {
                errorMessage.postValue(result.exceptionOrNull()?.message)
            }
            _loading.value = false
        }
    }
//    //Lưu trữ và quản lý các dữ liệu của ui
//    //Bảo tồn dữ liệu khi có tác đụng ví dụ xoay màn hình
//    //ViewModel gắn với lifecycle của activity/fragment thông qua viewmodelProvider và nó chỉ bị huỷ khi mà 2 cái đó bị huỷ mà thôi
//
//    //---------------------
//
//    // livedata và state flow hoạt động hiêu quả
//    private val firebaseAuth = FirebaseAuth.getInstance() //tạo instance cho firebase
//    private val firebaseFireStore = FirebaseFirestore.getInstance()
//    private lateinit var googleSignInClient: GoogleSignInClient //khởi tạo đối tượng kêt nối với google
//    private val RC_SIGN_IN = 9001 // mã yêu cầu để nhận kết quả
//    //tạo instance quản lý state
//    val registrationSuccess = MutableLiveData<Boolean>()
//    val loginSuccess = MutableLiveData<Boolean>()
//    val errolMessage = MutableLiveData<String>()
//    val signOutSuccess = MutableLiveData<Boolean>()
//
//    val documentData = MutableLiveData<Map<String, Any>>()
//
//    val userInfo = MutableLiveData<String>()
//    val resetSuccess = MutableLiveData<Boolean>()
//
//    fun register (user:CreateUserModel){
//
//      firebaseAuth.createUserWithEmailAndPassword(user.email, user.password)
//            .addOnCompleteListener { state->
//                 if(state.isSuccessful){ //kiểm tra stare đã đăng kí đư chưa
//                     val userId = state.result?.user?.uid ?: "" //kiểm tra
//                     //tạo map để lưu vào fire store
//                     // Tạo UserModel với uid
//                     val userData = UserModel(
//                         uid = userId,
//                         name = user.fullName,
//                         email = user.email,
//                         imageUrl = null,
//                         createdAt = null, // ServerTimestamp sẽ được Firestore gán
//                         provider = "email",
//                         uploadedSongs = emptyList(),
//                         recentlyPlayed = emptyList(),
//                         playlists = emptyList()
//                     )
//                     firebaseFireStore.collection("users").document(userId)
//                         .set(userData)
//                         .addOnSuccessListener {
//                             registrationSuccess.value = true
//                         }
//                         .addOnFailureListener {
//                             e-> errolMessage.value = e.message
//                         }
//                 }else{
//                    errolMessage.value = state.exception?.message
//                 }
//            }
//
//
//    }
//    fun login(email: String, password: String) {
//        firebaseAuth.signInWithEmailAndPassword(email, password)
//            .addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    loginSuccess.value = true
//                } else {
//                   errolMessage.value = task.exception?.message
//                }
//            }
//    }
//
//    fun initGoogle(context: Context){
//        //cấu hình google
//        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN) //đưa cho gg idClient để xin lấy token từ gg
//            .requestIdToken(context.getString(R.string.clientId)) //đưa cho gg idClient để xin lấy token từ gg/ có context để biết gọi từ đau. cho là lớp này
//            //nó không gắn với dữ liệu giống như activity
//            .requestEmail() //yêu cầu email ngươi dùng
//            .build() //dựng cấu hình
//        googleSignInClient = GoogleSignIn.getClient(context, gso) //tạo client gg
//    }
//
//    fun getSignInIntent(): Intent {
//        return googleSignInClient.signInIntent
//    }
//
//    fun getRequestCode() = RC_SIGN_IN
//
//    fun signInWithGoogle(idToken: String) {
//        val credential = GoogleAuthProvider.getCredential(idToken, null)
//        firebaseAuth.signInWithCredential(credential)
//            .addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    val user = firebaseAuth.currentUser
//                    if (user != null) {
//                        checkAndCreateUserDocument(user)
//                    }
//                    loginSuccess.value = true
//                } else {
//                    errolMessage.value = task.exception?.message
//                }
//            }
//    }
//    //kiểm tra user
//    private fun checkAndCreateUserDocument(firebaseUser: FirebaseUser) {
//        val userId = firebaseUser.uid
//        firebaseFireStore.collection("users").document(userId).get()
//            .addOnSuccessListener { document ->
//                if (!document.exists()) {
//                    val userData = UserModel(
//                        uid = userId,
//                        name = firebaseUser.displayName ?: "",
//                        email = firebaseUser.email ?: "",
//                        imageUrl = firebaseUser.photoUrl?.toString(),
//                        createdAt = null, // ServerTimestamp sẽ được Firestore gán
//                        provider = "google",
//                        uploadedSongs = emptyList(),
//                        recentlyPlayed = emptyList(),
//                        playlists = emptyList()
//                    )
//                    firebaseFireStore.collection("users").document(userId).set(userData)
//                        .addOnFailureListener { e -> errolMessage.value = e.message }
//                }
//            }
//            .addOnFailureListener { e -> errolMessage.value = e.message }
//    }
//    fun loadUser (){
//        val userId = firebaseAuth.currentUser?.uid
//        if(userId != null){
//            firebaseFireStore.collection("users")
//                .document(userId)
//                .get()
//                .addOnSuccessListener {document->
//                    if (document.exists()){
//                        documentData.value = document.data
//                    }
//                    else{
//                        userInfo.value = "Không tìm thấy user"
//                    }
//                }
//                .addOnFailureListener {e->
//                    errolMessage.value = e.message
//                }
//        }else{
//            userInfo.value ="chưa có người dùng đăng nhaoaj"
//        }
//
//    }
//    fun signOut(){
//        firebaseAuth.signOut()
//        signOutSuccess.value = true
//    }
//    fun resetPassword(email: String){
//        firebaseAuth.sendPasswordResetEmail(email)
//        resetSuccess.value = true
//    }



}