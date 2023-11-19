package bdic.comp3011j.readpaper.BmobEntity;

import cn.bmob.v3.BmobObject;
import cn.bmob.v3.BmobUser;

public class Paper extends BmobObject{
    private BmobUser user;
    private String title;
    private String author;
    private String url;

    // 考虑是否创建一个year属性来存储论文发表年份

    public BmobUser getUser() {
        return user;
    }

    public void setUser(BmobUser user) {
        this.user = user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
