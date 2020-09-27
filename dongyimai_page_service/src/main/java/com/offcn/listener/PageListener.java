package com.offcn.listener;

import com.offcn.page.service.ItemPageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@Component
public class PageListener implements MessageListener {
    @Autowired
    private ItemPageService itemPageService;
    @Override
    public void onMessage(Message message){
        TextMessage message1 = (TextMessage) message;
        try{
            String text = message1.getText();
            System.out.println("接受到信息："+text);
            boolean b = itemPageService.genItemHtml(Long.parseLong(text));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
