package com.txl.dean;

public class personalInfo {
    private String name;            // 姓名
    private String telephone;       // 电话
    private String email;          // 电子邮箱
    private String homepage;       // 个人主页
    private String birthday;       // 生日
    private String photo;          // 照片路径
    private String company;        // 工作单位
    private String address;        // 家庭地址
    private String zipCode;        // 邮编
    private String group;          // 所属组
    private String notes;          // 备注信息

    public personalInfo() {
    }

    public personalInfo(String name) {
        this.name = name;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getHomepage() { return homepage; }
    public void setHomepage(String homepage) { this.homepage = homepage; }

    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return "姓名: " + name + "\n" +
               "电话: " + telephone + "\n" +
               "邮箱: " + email + "\n" +
               "主页: " + homepage + "\n" +
               "生日: " + birthday + "\n" +
               "照片: " + photo + "\n" +
               "单位: " + company + "\n" +
               "地址: " + address + "\n" +
               "邮编: " + zipCode + "\n" +
               "分组: " + group + "\n" +
               "备注: " + notes;
    }
}