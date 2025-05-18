package ahmat.dafa.pamsimas.network

import PemakaianBayarResponse
import TransaksiBayarRequest
import TransaksiBayarResponse
import ahmat.dafa.pamsimas.model.KeluhanResponse
import ahmat.dafa.pamsimas.model.PelangganBerandaResponse
import ahmat.dafa.pamsimas.model.PemakaianResponse
import ahmat.dafa.pamsimas.model.PemakaianStoreResponse
import ahmat.dafa.pamsimas.model.PetugasBerandaResponse
import ahmat.dafa.pamsimas.model.TransaksiResponse
import ahmat.dafa.pamsimas.model.UserResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("user")
    fun getUser(): Call<UserResponse>

    @GET("pemakaian")
    fun getPemakaian(
        @Query("search") search: String,
        @Query("page") page: Int
    ): Call<PemakaianResponse>

    @GET("pemakaian")
    fun getPemakaianById(
        @Query("id_users") id_users: Int
    ): Call<PemakaianResponse>

    @GET("keluhan")
    fun getKeluhan(
        @Query("search") search: String,
        @Query("page") page: Int,
    ): Call<KeluhanResponse>

    @Multipart
    @POST("keluhan")
    fun storeKeluhan(
        @Part("keterangan") keterangan: RequestBody,
        @Part foto_keluhan: MultipartBody.Part?
    ): Call<ResponseBody>

    @GET("transaksi")
    fun getTransaksi(
        @Query("search") search: String,
        @Query("page") page: Int,
    ): Call<TransaksiResponse>

    @Multipart
    @POST("pemakaian/store")
    fun storePemakaian(
        @Part("id_users") idUsers: RequestBody,
        @Part("meter_awal") meterAwal: RequestBody,
        @Part("meter_akhir") meterAkhir: RequestBody,
        @Part fotoMeteran: MultipartBody.Part?
    ): Call<PemakaianStoreResponse>

    @Multipart
    @POST("pemakaian/bayar/langsung")
    fun bayarPemakaian(
        @Part("id_users") idUsers: RequestBody,
        @Part("meter_awal") meterAwal: RequestBody,
        @Part("meter_akhir") meterAkhir: RequestBody,
        @Part fotoMeteran: MultipartBody.Part?
    ): Call<PemakaianBayarResponse>

    @PUT("pemakaian/bayar/lunas")
    fun lunasPemakaian(
        @Body request: TransaksiBayarRequest
    ): Call<TransaksiBayarResponse>

    @GET("transaksi/{id}")
    fun getDetailTransaksi(
        @Path("id") idTransaksi: String
    ): Call<TransaksiBayarResponse>

    @GET("dashboard/petugas")
    fun getDashboardPetugas(): Call<PetugasBerandaResponse>

    @GET("dashboard/pelanggan")
    fun getDashboardPelanggan(): Call<PelangganBerandaResponse>

    @FormUrlEncoded
    @POST("ubah-password")
    fun ubahPassword(
        @Field("current_password") currentPassword: String,
        @Field("new_password") newPassword: String,
        @Field("new_password_confirmation") newPasswordConfirmation: String
    ): Call<ResponseBody>



}