package com.tea.httplibrary

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tea.httplibrary.config.BaseConfig
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.tea.httplibrary.interceptor.BaseUrlInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * @des
 * @author zc
 * @date 2021/11/24 18:12
 **/
class ApiClient {

    /**
     * @param interceptors
     * @return
     */
    fun getOkHttpClient(interceptors: List<Interceptor>): OkHttpClient {
        val builder = OkHttpClient.Builder()
        //处理新增加的多个baseurl,当设置了多个baseurl再做这个的处理
        if (BaseConfig.getMoreBaseUrl()) {
            builder.addInterceptor(BaseUrlInterceptor())
        }
        //这个是通用设置的
        for (interceptor in interceptors) {
            builder.addInterceptor(interceptor)
        }
        return builder.connectTimeout(
            BaseConfig.getConnectTimeout(),
            TimeUnit.SECONDS
        ) //设置请求超时时间
            .readTimeout(BaseConfig.getReadTimeout(), TimeUnit.SECONDS)
            .writeTimeout(BaseConfig.getWriteTimeout(), TimeUnit.SECONDS)
            .retryOnConnectionFailure(true) //设置出现错误进行重新连接。
            .build()
    }

    /**
     * @param okHttpClient
     * @return
     */
    fun getRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BaseConfig.getBaseUrl())
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder().registerTypeAdapterFactory(
                        NullStringToEmptyAdapterFactory()
                    ).setPrettyPrinting().disableHtmlEscaping().create()
                )
            )
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .client(okHttpClient)
            .build()
    }

    private inner class NullStringToEmptyAdapterFactory : TypeAdapterFactory {
        override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
            val rawType = type.rawType as Class<T>
            return if (rawType != String::class.java) {
                null
            } else StringNullAdapter() as TypeAdapter<T>
        }
    }


    //对String类型 返回为null  解析为""
    private class StringNullAdapter : TypeAdapter<String?>() {

        @Throws(IOException::class)
        override fun read(reader: JsonReader): String? {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                return ""
            }
            return reader.nextString()
        }

        @Throws(IOException::class)
        override fun write(
            writer: JsonWriter,
            value: String?
        ) {
            if (value == null) {
                writer.nullValue()
                return
            }
            writer.value(value)
        }


    }


    companion object {
        fun getInstance() = SingletonHolder.INSTANCE
    }

    private object SingletonHolder {
        val INSTANCE = ApiClient()
    }

}