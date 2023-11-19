package bdic.comp3011j.readpaper.BmobEntity;

import androidx.room.PrimaryKey;

import cn.bmob.v3.BmobObject;

public class Chat extends BmobObject {

    private Paper paper;

    private String content;  //消息内容

    private String type; //消息类型

    public Paper getPaper() {
        return paper;
    }

    public void setPaper(Paper paper) {
        this.paper = paper;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Chat(String primaryKey, String content, String type) {
        setObjectId(primaryKey);
        this.content = content;
        this.type = type;
    }

    public Chat() {

    }
}
