package com.asm.asminjectdemo;

import android.view.View;
import android.widget.Toast;

/**
 * @author: yufang
 * @date: 2019/1/23 20:14
 * desc:
 */
public class ClickFun {

    public static void doClick(View view){
        Toast.makeText(view.getContext() , "do click"  , Toast.LENGTH_SHORT).show();
    }

}
