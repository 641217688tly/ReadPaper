package bdic.comp3011j.readpaper.Util;

public interface InsertCallback {

    void onSuccess(String primaryKey);

    void onFail(String errorMessage);

}
