package com.offcn.content.controller;

import com.offcn.entity.Result;
import com.offcn.util.FastDFSClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class UploadController {
    @Value("${FILE_SERVER_URL}")
    private  String FILE_SERVER_URL;
    @RequestMapping("/upload")
    public Result upload(MultipartFile file){
        //取出文件的初始名
        String filename = file.getOriginalFilename();
        //取出文件的扩展名
        String extName = filename.substring(filename.lastIndexOf(".") + 1);
        //创建一个FastDFS的客户端
        try {
            FastDFSClient fastDFSClient = new FastDFSClient("classpath:config/fdfs_client.conf");
            //执行上传处理
            String path = fastDFSClient.uploadFile(file.getBytes(), extName);
            //拼接返回的url和ip地址，拼装成完整的url
            String url = FILE_SERVER_URL+path;
            return  new Result(true,url);
        } catch (Exception e) {
            e.printStackTrace();
            return  new Result(false,"失败");
        }


    }



}
