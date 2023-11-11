package main.java.Bean;

public class UserBean {
    private static final String[] fields = new String[]{"userid", "avatar", "title", "phone", "password"};
    private String userid;
    private String avatar;
    private String title;
    private String phone;
    private String password;

    public static String[] getFields() {
        return fields;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
