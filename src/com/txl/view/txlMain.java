package com.txl.view;

import javax.crypto.SealedObject;
import javax.naming.CompositeName;
import javax.swing.text.DefaultEditorKit;

public class txlMain {
    public static void main(String[] args) {
        //txlView txlview=new txlView();//打开通讯录窗口
        AddressBookApp addressBookApp=new AddressBookApp();
        addressBookApp.setVisible(true);
    }

}
