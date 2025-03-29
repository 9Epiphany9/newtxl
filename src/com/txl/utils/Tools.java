package com.txl.utils;

import javax.swing.*;
import java.awt.*;

public class Tools {
    /**
     * 窗口居中
     * @param jFrame
     * @param width
     * @param height
     */
    public static void setPos(JFrame jFrame,int width,int height){
        Toolkit tk =Toolkit.getDefaultToolkit();
        Dimension screenSize =tk.getScreenSize();
        int x=(int)screenSize.getWidth()/2-width/2;
        int y=(int)screenSize.getHeight()/2-height/2;
        jFrame.setBounds(x,y,width,height);
    }
}
