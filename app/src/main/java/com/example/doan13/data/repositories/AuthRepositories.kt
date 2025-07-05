package com.example.doan13.data.repositories

import android.util.Log
import com.example.doan13.data.models.auth.CreateUserModel
import com.example.doan13.data.models.auth.UserModel
import com.example.doan13.data.models.songs.SongModels
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepositories {
    private val firebaseAuth = FirebaseAuth.getInstance() //tạo instance cho firebase
    private val firebaseFireStore = FirebaseFirestore.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient //khởi tạo đối tượng kêt nối với google
    private val RC_SIGN_IN = 9001 // mã yêu cầu để nhận kết quả

    suspend fun updateRecentlyPlayed(userId: String, songId: String): Result<Unit> {
        return try {
            val userDoc = firebaseFireStore.collection("users").document(userId).get().await()

            firebaseFireStore.collection("users").document(userId)
                .update("recentlyPlayed", FieldValue.arrayUnion(songId))
                .await()
            Log.d("AuthRepositories", "Đã cập nhật recentlyPlayed cho $userId với $songId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepositories", "Lỗi cập nhật recentlyPlayed: ${e.message}")
            Result.failure(e)
        }
    }


    suspend fun getMyTracksByUserId(userId: String): List<SongModels> {
        return try {
            val snapshot = firebaseFireStore.collection("users")
                .whereEqualTo("uid", userId) // Giả sử trường userId trong collection songs
                .get()
                .await()
            snapshot.toObjects(SongModels::class.java)
        } catch (e: Exception) {
            Log.e("SongRepository", "Error fetching my tracks: ${e.message}")
            emptyList()
        }
    }
    suspend fun updateUserName(userId: String, newName: String): Result<Unit> {
        return try {
            FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .update("name", newName)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Repository", "Lỗi cập nhật tên: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun register(user: CreateUserModel): Result<Unit> {
        return try {
            val state = firebaseAuth.createUserWithEmailAndPassword(user.email, user.password).await()
            val userId = state.user?.uid ?: throw Exception("Không lấy được userId")
            val userData = UserModel(
                uid = userId,
                name = user.fullName,
                email = user.email,
                imageUrl = null,
                createdAt = null,
                provider = "email",
                uploadedSongs = emptyList(),
                recentlyPlayed = emptyList(),
                playlists = emptyList(),
            )
            firebaseFireStore.collection("users").document(userId).set(userData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Lỗi đăng ký: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Lỗi đăng nhập: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val task = firebaseAuth.signInWithCredential(credential).await()
            val user = task.user ?: throw Exception("Không lấy được user")
            checkAndCreateUserDocument(user)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Lỗi đăng nhập Google: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun checkAndCreateUserDocument(firebaseUser: FirebaseUser) {
        try {
            val userId = firebaseUser.uid
            val document = firebaseFireStore.collection("users").document(userId).get().await()
            if (!document.exists()) {
                val userData = UserModel(
                    uid = userId,
                    name = firebaseUser.displayName ?: "",
                    email = firebaseUser.email ?: "",
                    imageUrl = firebaseUser.photoUrl?.toString(),
                    createdAt = null,
                    provider = "google",
                    uploadedSongs = emptyList(),
                    recentlyPlayed = emptyList(),
                    playlists = emptyList()
                )
                firebaseFireStore.collection("users").document(userId).set(userData).await()
                Log.d("AuthRepository", "Tạo tài liệu người dùng mới: $userId")
            } else {
                Log.d("AuthRepository", "Tài liệu người dùng đã tồn tại: $userId")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Lỗi tạo tài liệu người dùng: ${e.message}")
            throw e
        }
    }

    suspend fun loadUser(userId: String): Result<UserModel?> {
        return try {
            val document = firebaseFireStore.collection("users").document(userId).get().await()
            if (document.exists()) {
                val userData = document.toObject(UserModel::class.java)
                Log.d("AuthRepository", "Tải user thành công: $userId")
                Result.success(userData)
            } else {
                Log.w("AuthRepository", "Không tìm thấy tài liệu user: $userId")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Lỗi tải user: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Lỗi đặt lại mật khẩu: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        firebaseAuth.signOut()
    }

    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }


}