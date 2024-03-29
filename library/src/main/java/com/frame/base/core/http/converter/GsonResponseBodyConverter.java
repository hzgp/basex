package com.frame.base.core.http.converter;

import android.util.Log;

import com.frame.base.BuildConfig;
import com.google.gson.Gson;

import java.io.IOException;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Converter;

/**
 * Desc:
 * Author:zhujb
 * Date:2020/8/12
 */
final class GsonResponseBodyConverter<T> implements Converter<ResponseBody, T> {
    private final Gson gson;
    private final Type type;

    GsonResponseBodyConverter(Gson gson, Type type) {
        this.gson = gson;
        this.type = type;
    }
    @Override
    public T convert(ResponseBody value) throws IOException {
        String info = value.string();
        if (NoFilter.instance().isFilter(type)) {
            info = info.replace("\\\"", "\"");
            info = info.replace("\"[", "[");
        }
      //  info= info.replace("]\"","]");
        //CrashHandler.getInstance().context(SourceApplication.getAppContext()).saveCrashInfoToFile(new NullPointerException(info));
        if (type == String.class){
            return (T) info;
        }
        if (BuildConfig.DEBUG){
            Log.d("Response",info);
        }
        return gson.fromJson(info,type);
    }
}

