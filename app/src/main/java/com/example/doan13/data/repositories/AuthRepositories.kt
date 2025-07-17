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
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
interface AuthRepositories {
    suspend fun updateRecentlyPlayed(userId: String, songId: String): Result<Unit>
    suspend fun updateUserName(userId: String, newName: String): Result<Unit>
    suspend fun register(user: CreateUserModel): Result<Unit>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    suspend fun loadUser(userId: String): UserModel?
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun signOut()
    fun getCurrentUserId(): String?
}
class AuthRepositoriesImpl : AuthRepositories {
    private val firebaseAuth = FirebaseAuth.getInstance() //tạo instance cho firebase
    private val firebaseFireStore = FirebaseFirestore.getInstance()

    override suspend fun updateRecentlyPlayed(userId: String, songId: String): Result<Unit> {
        return try {
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


    override suspend fun updateUserName(userId: String, newName: String): Result<Unit> {
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

    override suspend fun register(user: CreateUserModel): Result<Unit> {
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
                playlistLiked = emptyList()
            )
            firebaseFireStore.collection("users").document(userId).set(userData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Lỗi đăng ký: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Lỗi đăng nhập: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null) //tạo credential
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
                    playlists = emptyList(),
                    playlistLiked = emptyList()
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

    override suspend fun loadUser(userId: String): UserModel?{
        return try {
            firebaseFireStore.collection("users")
                .document(userId)
                .get(Source.SERVER)
                .await()
                .toObject(UserModel::class.java)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Lỗi tải user: ${e.message}")
            null
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Lỗi đặt lại mật khẩu: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }


}