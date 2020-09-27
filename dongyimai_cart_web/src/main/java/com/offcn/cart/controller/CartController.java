package com.offcn.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.offcn.cart.service.CartService;
import com.offcn.entity.Result;
import com.offcn.group.Cart;
import com.offcn.util.CookieUtil;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {
    @Reference(timeout = 6000)
    private CartService cartService;

    /**
     * 购物车列表
     * @param request
     * @return
     */
    @RequestMapping("/findCartList")
    public List<Cart> findCartList(HttpServletRequest request,HttpServletResponse response){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

            //从cookie中获取购物车信息
            String cartList = CookieUtil.getCookieValue(request, "cartList", "UTF-8");
            if(cartList==null||cartList==""){
                cartList="[]";
            }
            List<Cart> list = JSON.parseArray(cartList, Cart.class);
            if(username.equals("anonymousUser")){
                return list;
        }else {
            //如果已登录
            List<Cart> cartList_redis =cartService.findCartListFromRedis(username);//从redis中提取
            if(list.size()>0){
                //合并购物车
                cartList_redis=cartService.mergeCartList(cartList_redis, list);
                //清除本地cookie的数据
                CookieUtil.deleteCookie(request, response, "cartList");
                //将合并后的数据存入redis
                cartService.saveCartListToRedis(username, cartList_redis);
            }

            return cartList_redis;
        }
    }

    /**
     * 添加sku到购物车
     * @param itemId
     * @param num
     * @return
     */
    @RequestMapping("/addGoodsToCartList")
    @CrossOrigin(origins="http://localhost:9105")
    public Result addGoodsToCartList(HttpServletRequest request, HttpServletResponse response,Long itemId, Integer num){
       /* response.setHeader("Access-Control-Allow-Origin", "http://localhost:9105");
        response.setHeader("Access-Control-Allow-Credentials", "true");*/
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("当前登陆用户"+username);
        try {
           List<Cart> cartList = findCartList(request,response);//获取购物车列表
           //添加新的商品到购物车
           List<Cart> cartList1 = cartService.addGoodsToCartList(cartList, itemId, num);
            if(username.equals("anonymousUser")){
                CookieUtil.setCookie(request, response, "cartList", JSON.toJSONString(cartList1), 3600 * 24, "UTF-8");
                System.out.println("向cookie存入数据");
            }else {
                cartService.saveCartListToRedis(username, cartList);
            }

           return new Result(true, "添加成功");
       }catch (RuntimeException e) {
            e.printStackTrace();
            return new Result(false, e.getMessage());
        }catch(Exception e) {
           e.printStackTrace();
           return new Result(false, "添加失败");
       }
    }


}
