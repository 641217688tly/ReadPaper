package bdic.comp3011j.readpaper.Util;

import java.util.List;

public interface InsertCallback {

    void onSuccess(String primaryKey);

    void onFail(String errorMessage);

}
