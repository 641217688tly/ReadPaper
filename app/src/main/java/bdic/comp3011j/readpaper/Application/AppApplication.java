package bdic.comp3011j.readpaper.Application;

import android.app.Application;

import cn.bmob.v3.Bmob;


public class AppApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Bmob.initialize(this, "9d8e85b0505e3f85d9b4aa7c70d1df66");
    }

}
