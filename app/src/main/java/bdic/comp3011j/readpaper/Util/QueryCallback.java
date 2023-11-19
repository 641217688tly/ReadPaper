package bdic.comp3011j.readpaper.Util;

import java.util.List;

public interface QueryCallback<T> {
    void onSuccess(List<T> result);

    void onFail(String errorMessage);
}
